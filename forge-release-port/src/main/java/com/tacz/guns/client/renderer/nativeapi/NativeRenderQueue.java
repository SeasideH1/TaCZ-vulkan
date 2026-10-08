/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.*;

/** Extraction-stage ordered command stream. No live bones, game entities, GL state or GPU handles. */
public final class NativeRenderQueue implements AutoCloseable {
    public enum MaskKind { ALWAYS, EQUAL, LESS_THAN }
    public record MaskRule(MaskKind kind,int reference) {
        public static final MaskRule ALWAYS=new MaskRule(MaskKind.ALWAYS,0);
        public static MaskRule equal(int id){return new MaskRule(MaskKind.EQUAL,id);}
        public static MaskRule lessThan(int reference){return new MaskRule(MaskKind.LESS_THAN,reference);}
    }
    public sealed interface Command permits Draw,WriteEyes,ResolveEyes,ClearMask {}
    public record Draw(RenderType material,GeometrySnapshot geometry,MaskRule mask,boolean depthEnabled) implements Command {}
    public record Eye(int id,boolean scope,GeometrySnapshot geometry,float centerX,float centerY,float centerZ,float radius,float ads) {
        public Eye(int id,boolean scope,GeometrySnapshot geometry,float centerX,float centerY,float radius,float ads) {
            this(id,scope,geometry,centerX,centerY,0,radius,ads);
        }
    }
    public record WriteEyes(List<Eye> eyes,RenderType material,boolean scopeOnly,boolean clear) implements Command {
        public WriteEyes{eyes=List.copyOf(eyes);}
    }
    public record ResolveEyes(List<Eye> eyes,boolean combined) implements Command {public ResolveEyes{eyes=List.copyOf(eyes);}}
    public record ClearMask() implements Command {}
    public record Frame(List<Command> commands) {
        public Frame{commands=List.copyOf(commands);}
        public boolean requiresNativeExecutor() {
            return commands.stream().anyMatch(command -> command instanceof WriteEyes || command instanceof ResolveEyes
                    || command instanceof Draw draw && (draw.mask().kind()!=MaskKind.ALWAYS || !draw.depthEnabled()));
        }
        /** Non-first-person paths use native feature submission and contain no scope-mask commands. */
        public void submit(PoseStack pose,SubmitNodeCollector collector) { submit(pose,collector,0,0); }
        public void submit(PoseStack pose,SubmitNodeCollector collector,int light,int overlay) {
            if(requiresNativeExecutor()) {
                if(NativeHandItemPass.submit(this,pose.last(),light,overlay))return;
                throw new IllegalStateException("Scope operation outside the hand renderer");
            }
            int order=0;
            for(Command command:commands) {
                if(command instanceof Draw draw) {
                    if(draw.mask().kind()!=MaskKind.ALWAYS)throw new IllegalStateException("Masked hand geometry requires the native scope executor");
                    collector.order(order++).submitCustomGeometry(pose,draw.material(),draw.geometry().resolveInheritedAttributes(light,overlay));
                } else if(!(command instanceof ClearMask))throw new IllegalStateException("Scope operation outside the hand renderer");
            }
        }
    }
    private static final ThreadLocal<NativeRenderQueue> ACTIVE=new ThreadLocal<>();
    private final NativeRenderQueue previous;
    private final List<Command> commands=new ArrayList<>();
    private GeometrySnapshot.Builder currentVertices;
    private RenderType currentMaterial;
    private MaskRule mask=MaskRule.ALWAYS;
    private boolean depthEnabled=true;
    private boolean closed;
    public NativeRenderQueue(){previous=ACTIVE.get();ACTIVE.set(this);}
    public static NativeRenderQueue current(){NativeRenderQueue queue=ACTIVE.get();if(queue==null)throw new IllegalStateException("No native extraction queue is active");return queue;}
    public VertexConsumer getBuffer(RenderType material) {
        if(closed)throw new IllegalStateException("Queue closed");
        if(currentMaterial!=material){endBatch();currentMaterial=Objects.requireNonNull(material);currentVertices=new GeometrySnapshot.Builder();}
        return currentVertices;
    }
    public void endBatch(RenderType material){if(currentMaterial==material)endBatch();}
    public void endBatch() {
        if(currentVertices!=null) {
            GeometrySnapshot geometry=currentVertices.build();
            if(geometry.vertexCount()!=0) {
                // Sorting translucent primitives across an old draw boundary changes blending.
                // Merge only adjacent independent primitive lists with identical render state.
                var topology=currentMaterial.primitiveTopology();
                int primitiveSize=topology==com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS?4:
                        topology==com.mojang.renderpearl.api.pipeline.PrimitiveTopology.TRIANGLES?3:0;
                if(!currentMaterial.sortOnUpload()&&primitiveSize!=0&&geometry.vertexCount()%primitiveSize==0
                        &&!commands.isEmpty()&&commands.getLast() instanceof Draw previousDraw
                        &&previousDraw.material()==currentMaterial&&previousDraw.mask().equals(mask)&&previousDraw.depthEnabled()==depthEnabled
                        &&previousDraw.geometry().vertexCount()%primitiveSize==0) {
                    commands.set(commands.size()-1,new Draw(currentMaterial,previousDraw.geometry().concat(geometry),mask,depthEnabled));
                } else commands.add(new Draw(currentMaterial,geometry,mask,depthEnabled));
            }
            currentVertices=null;currentMaterial=null;
        }
    }
    public void setDepthTest(boolean enabled){if(depthEnabled!=enabled){endBatch();depthEnabled=enabled;}}
    public void setMask(MaskRule rule){Objects.requireNonNull(rule);if(!mask.equals(rule)){endBatch();mask=rule;}}
    public void writeEyes(List<Eye> eyes,RenderType material,boolean scopeOnly,boolean clear){endBatch();commands.add(new WriteEyes(eyes,material,scopeOnly,clear));}
    public void resolveEyes(List<Eye> eyes,boolean combined){endBatch();commands.add(new ResolveEyes(eyes,combined));}
    public void clearMask(){endBatch();mask=MaskRule.ALWAYS;commands.add(new ClearMask());}
    public Frame snapshot(){endBatch();return new Frame(commands);}
    @Override public void close(){if(closed)return;endBatch();ACTIVE.set(previous);closed=true;}
}
