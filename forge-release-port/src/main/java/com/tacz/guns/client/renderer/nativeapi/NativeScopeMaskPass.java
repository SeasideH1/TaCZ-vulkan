/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.*;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.*;
import com.tacz.guns.client.renderer.nativeapi.GeometrySnapshot.Vertex;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.*;

/** Native sampled identity masks with the upstream sequential stencil algebra. */
public final class NativeScopeMaskPass implements AutoCloseable {
    private static final String NS="tacz";
    private static final BindGroupLayout OCULAR_SAMPLER=BindGroupLayout.builder().withUniform("OcularIds",UniformType.COMBINED_IMAGE_SAMPLER).build();
    private static final BindGroupLayout MODEL_SAMPLER=BindGroupLayout.builder().withUniform("Sampler0",UniformType.COMBINED_IMAGE_SAMPLER).build();
    private static final RenderPipeline WRITE=pipeline("scope_ids",false,false);
    private static final RenderPipeline WRITE_DEPTH=pipeline("scope_ids_depth",false,true);
    private static final RenderPipeline RESOLVE=pipeline("scope_aperture",true,false);
    private static final RenderPipeline RESOLVE_DEPTH=pipeline("scope_aperture_depth",true,true);
    private final ByteBufferBuilder scratch=new ByteBufferBuilder(65536);
    private TextureTarget ocularIds, resolvedIds, alternateIds;
    private int width,height;
    private boolean closed;
    public record Ocular(int id,boolean scope,List<Vertex> triangles,float centerX,float centerY,
                         float radiusModifier,float aimingProgress) {
        public Ocular {
            if(id<1||id>128||triangles.size()%3!=0) throw new IllegalArgumentException("Invalid eye or triangles");
            triangles=List.copyOf(triangles);
        }
    }

    /** Call after ring draws. Texture alpha and current hand depth both gate identity writes. */
    public GpuTextureView write(int width,int height,List<Ocular> eyes,boolean scopeOnly,boolean clear,
                                GpuBufferSlice projection,GpuTextureView handDepth,GpuTextureView modelTexture,GpuSampler modelSampler) {
        RenderSystem.assertOnRenderThread();
        if(closed) throw new IllegalStateException("Mask pass closed");
        ensureTargets(width,height);
        BufferBuilder builder=new BufferBuilder(scratch,PrimitiveTopology.TRIANGLES,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(Ocular eye:eyes) {
            if(scopeOnly&&!eye.scope()) continue;
            for(Vertex v:eye.triangles()) builder.addVertex(v.x(),v.y(),v.z()).setUv(v.u(),v.v()).setColor(eye.id(),0,0,255);
        }
        MeshData mesh=builder.build();
        if(mesh==null) {
            if(clear) RenderSystem.getDevice().createCommandEncoder().clearColorTexture(ocularIds.getColorTexture(),new Vector4f(0));
        } else draw(ocularIds,mesh,false,clear,projection,handDepth,modelTexture,modelSampler);
        return ocularIds.getColorTextureView();
    }

    /** Call after the scope-body draw so its depth writes participate in each inversion. */
    public GpuTextureView resolve(List<Ocular> eyes,boolean combined,GpuBufferSlice projection,GpuTextureView handDepth) {
        // The original identity texture is already a valid first source. Keep it immutable
        // and copy only into the destination of each actual inversion.
        TextureTarget source=ocularIds;
        for(Ocular eye:eyes) {
            if(combined&&!eye.scope()) continue;
            // Ping-pong is essential: ID127's complement128 can be changed by a later ID128 pass.
            TextureTarget destination=source==resolvedIds?alternateIds:resolvedIds;
            copy(source,destination);
            float[] fan=ScopeStencilReference.apertureFan(eye.centerX(),eye.centerY(),eye.radiusModifier(),eye.aimingProgress());
            BufferBuilder builder=new BufferBuilder(scratch,PrimitiveTopology.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
            for(int i=0;i<ScopeStencilReference.APERTURE_SEGMENTS;i++) {
                add(builder,fan,0,eye.id());add(builder,fan,(i+1)*3,eye.id());add(builder,fan,(i+2)*3,eye.id());
            }
            draw(destination,builder.buildOrThrow(),true,false,projection,handDepth,source.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            source=destination;
        }
        return source.getColorTextureView();
    }
    private void copy(TextureTarget from,TextureTarget to) {
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(from.getColorTexture(),to.getColorTexture(),0,0,0,0,0,width,height);
    }
    private static void add(BufferBuilder builder,float[] xyz,int offset,int id) {
        builder.addVertex(xyz[offset],xyz[offset+1],xyz[offset+2]).setColor(id,0,0,255);
    }
    private void draw(TextureTarget destination,MeshData mesh,boolean aperture,boolean clear,GpuBufferSlice projection,
                      GpuTextureView depth,GpuTextureView sampled,GpuSampler sampler) {
        try(mesh) {
            var encoder=RenderSystem.getDevice().createCommandEncoder();
            // RenderPearl owns the pooled allocation and retires it after GPU completion.
            // Each draw receives a distinct slice, including multiple eyes in the same frame.
            GpuBufferSlice vertices=encoder.transientMemory().uploadGpu(mesh.vertexBuffer(),4,GpuBuffer.USAGE_VERTEX|GpuBuffer.USAGE_COPY_DST);
            try(RenderPass pass=encoder.createRenderPass(()->"tacz_scope_mask",
                    destination.getColorTextureView(),clear?Optional.of(new Vector4f(0)):Optional.empty(),depth,OptionalDouble.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(aperture?(depth==null?RESOLVE:RESOLVE_DEPTH):(depth==null?WRITE:WRITE_DEPTH)));
                pass.setUniform("Projection",projection);
                pass.setUniform("DynamicTransforms",RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f()));
                pass.setUniform(aperture?"OcularIds":"Sampler0",sampled,sampler);
                pass.setVertexBuffer(0,vertices);
                pass.draw(mesh.drawState().vertexCount(),1,0,0);
            }
        }
    }
    private static RenderPipeline pipeline(String name,boolean aperture,boolean depth) {
        var b=RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath(NS,"pipeline/"+name))
                .withVertexShader(Identifier.fromNamespaceAndPath(NS,aperture?"core/scope_data":"core/scope_textured"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(NS,aperture?"core/scope_aperture":"core/scope_ids"))
                .withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withBindGroupLayout(aperture?OCULAR_SAMPLER:MODEL_SAMPLER)
                .withVertexBinding(0,aperture?DefaultVertexFormat.POSITION_COLOR:DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(!aperture)
                .withColorTargetState(new ColorTargetState(aperture?Optional.empty():Optional.of(BlendFunction.MAX),GpuFormat.R8_UNORM,ColorTargetState.WRITE_RED));
        if(depth) b.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(),false)).withDepthStencilFormat(GpuFormat.D32_FLOAT);
        else b.withDepthStencilState(Optional.empty());
        return b.build();
    }
    private void ensureTargets(int width,int height) {
        if(width<1||height<1) throw new IllegalArgumentException("Invalid mask size");
        if(ocularIds!=null&&this.width==width&&this.height==height) return;
        release();this.width=width;this.height=height;
        ocularIds=new TextureTarget("tacz_ocular_ids",width,height,GpuFormat.R8_UNORM,null);
        resolvedIds=new TextureTarget("tacz_resolved_ids",width,height,GpuFormat.R8_UNORM,null);
        alternateIds=new TextureTarget("tacz_alternate_ids",width,height,GpuFormat.R8_UNORM,null);
    }
    private void release() {
        if(ocularIds!=null) ocularIds.destroyBuffers();if(resolvedIds!=null) resolvedIds.destroyBuffers();if(alternateIds!=null) alternateIds.destroyBuffers();
        ocularIds=resolvedIds=alternateIds=null;
    }
    @Override public void close() { if(closed)return;release();scratch.close();closed=true; }
}
