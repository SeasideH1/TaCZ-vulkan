/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.renderpearl.api.pipeline.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Adds only the stencil-equivalent fragment predicate to the current resource-pack material shader. */
final class MaskedPipelineCache implements AutoCloseable {
    private record Key(RenderPipeline base,NativeRenderQueue.MaskRule rule,boolean depthEnabled) {}
    private record Fragment(Identifier original,NativeRenderQueue.MaskRule rule) {}
    private static final BindGroupLayout MASK_LAYOUT=BindGroupLayout.builder().withUniform("TaczScopeMask",UniformType.COMBINED_IMAGE_SAMPLER).build();
    private final Map<Key,RenderPipeline> pipelines=new HashMap<>();
    private final Map<Identifier,Fragment> fragments=new HashMap<>();
    private final Source source=new Source();
    private final PipelineBuilder builder=new PipelineBuilder(RenderSystem.getDevice());
    private final PipelineCache cache=new PipelineCache(builder,source);
    CompiledRenderPipeline get(RenderPipeline base,NativeRenderQueue.MaskRule rule,boolean depthEnabled) {
        if(rule.kind()==NativeRenderQueue.MaskKind.ALWAYS&&depthEnabled)return RenderSystem.getCompiledPipeline(base);
        RenderPipeline pipeline=pipelines.computeIfAbsent(new Key(base,rule,depthEnabled),key->{
            Identifier location=Identifier.fromNamespaceAndPath("tacz","native_masked/"+pipelines.size());
            Identifier original=base.getShaders().get(ShaderType.FRAGMENT);
            if(original==null)throw new IllegalArgumentException("Masked material has no fragment shader");
            boolean masked=rule.kind()!=NativeRenderQueue.MaskKind.ALWAYS;
            if(masked)fragments.put(location,new Fragment(original,rule));
            return new Derived(base,location,masked,depthEnabled);
        });
        CompiledRenderPipeline compiled=cache.get(pipeline);
        if(compiled==null)throw new IllegalStateException("Native scope material did not compile: "+base.getLocation());
        return compiled;
    }
    private static final class Derived extends RenderPipeline {
        Derived(RenderPipeline base,Identifier id,boolean masked,boolean depthEnabled) {
            super(id,masked?shaders(base,id):base.getShaders(),base.getShaderDefines(),masked?layouts(base):base.getBindGroupLayouts(),base.getColorTargetStates().toArray(ColorTargetState[]::new),
                    depthEnabled?base.getDepthStencilState():DepthStencilState.OFF,base.getDepthStencilFormat(),base.getPolygonMode(),base.isCull(),
                    base.getVertexFormatBindings().toArray(VertexFormat[]::new),base.getPrimitiveTopology(),base.pushConstantSize(),base.getSortKey());
        }
        private static Map<ShaderType,Identifier> shaders(RenderPipeline base,Identifier id){Map<ShaderType,Identifier> result=new EnumMap<>(ShaderType.class);result.putAll(base.getShaders());result.put(ShaderType.FRAGMENT,id);return result;}
        private static List<BindGroupLayout> layouts(RenderPipeline base){List<BindGroupLayout> result=new ArrayList<>(base.getBindGroupLayouts());result.add(MASK_LAYOUT);return result;}
    }
    private final class Source implements ShaderSource {
        private final Map<Identifier,CachedIncludeSource> includes=new HashMap<>();
        @Override public String getShader(Identifier id,ShaderType type) {
            Fragment fragment=fragments.get(id);
            Identifier original=fragment==null?id:fragment.original();
            String text=read(original,"shaders/",type==ShaderType.FRAGMENT?".fsh":".vsh");
            if(fragment==null)return text;
            String operator=fragment.rule().kind()==NativeRenderQueue.MaskKind.EQUAL?"!=":">=";
            String replacement="uniform sampler2D TaczScopeMask;\nvoid main() {\n"
                    +"    int taczStored = int(round(texelFetch(TaczScopeMask, ivec2(gl_FragCoord.xy), 0).r * 255.0));\n"
                    +"    if (taczStored "+operator+" "+fragment.rule().reference()+") discard;\n";
            Matcher main=Pattern.compile("void\\s+main\\s*\\(\\s*\\)\\s*\\{").matcher(text);
            if(!main.find())throw new IllegalStateException("Cannot locate material fragment entrypoint: "+original);
            return main.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        @Override public CachedIncludeSource getInclude(Identifier id){return includes.computeIfAbsent(id,key->CachedIncludeSource.create(key,read(key,"shaders/include/","")));}
        @Override public void close(){includes.values().forEach(CachedIncludeSource::close);includes.clear();}
        private String read(Identifier id,String prefix,String suffix) {
            Identifier path=Identifier.fromNamespaceAndPath(id.getNamespace(),prefix+id.getPath()+suffix);
            try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(path).open()) {return new String(stream.readAllBytes(),StandardCharsets.UTF_8);}
            catch(IOException error){throw new IllegalStateException("Missing native material shader resource: "+path,error);}
        }
    }
    @Override public void close(){cache.close();builder.close();pipelines.clear();fragments.clear();}
}
