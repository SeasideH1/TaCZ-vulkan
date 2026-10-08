/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.textures.*;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.joml.Matrix3fc;
import java.util.*;

/** Executes first-person extracted commands through RenderPearl, retaining material lighting and texture logic. */
public final class NativeFrameExecutor implements AutoCloseable {
    private record PreparedDraw(PreparedRenderType material,StagedVertexBuffer.ExecuteInfo info,CompiledRenderPipeline pipeline) {}
    private final NativeScopeMaskPass scope=new NativeScopeMaskPass();
    private final MaskedPipelineCache pipelines=new MaskedPipelineCache();
    private final StagedVertexBuffer vertices=new StagedVertexBuffer(()->"tacz_hand_geometry",262144);
    private final StableGeometryBuffers stableVertices=new StableGeometryBuffers();
    public void render(NativeRenderQueue.Frame frame,RenderTarget target,GpuBufferSlice projection,GpuTextureView handDepth,Matrix3fc normalToWorld) {
        RenderSystem.assertOnRenderThread();
        PoseStack.Pose geometryPose=new PoseStack().last();
        geometryPose.normal().set(normalToWorld);
        Map<NativeRenderQueue.Draw,StagedVertexBuffer.Draw> draws=new IdentityHashMap<>();
        Map<NativeRenderQueue.Draw,StableGeometryBuffers.Entry> cached=new IdentityHashMap<>();
        stableVertices.begin();int slot=0;
        for(var command:frame.commands())if(command instanceof NativeRenderQueue.Draw draw) {
            var retained=stableVertices.find(slot++,draw,geometryPose);
            if(retained!=null){cached.put(draw,retained);continue;}
            var range=vertices.appendDraw(draw.material().format(),draw.material().primitiveTopology(),draw.material().sortOnUpload()?VertexSorting.DISTANCE_TO_ORIGIN:null);
            // Switching builders finalizes the previous mesh; upload finalizes the last one.
            // StagedVertexBuffer.endDraw() discards the whole draw list, not one mesh.
            draw.geometry().render(geometryPose,vertices.getVertexBuilder(range));draws.put(draw,range);
        }
        vertices.upload();
        RenderSystem.resizeAllAutoStorageIndexBuffers();
        // Material preparation can upload uniforms. Complete it before opening a shared pass.
        Map<NativeRenderQueue.Draw,PreparedDraw> prepared=new IdentityHashMap<>();
        for(var command:frame.commands())if(command instanceof NativeRenderQueue.Draw draw) {
            var retained=cached.get(draw);
            var info=retained==null?vertices.getExecuteInfo(draws.get(draw)):retained.info();if(info==null)continue;
            var material=draw.material().prepare();
            prepared.put(draw,new PreparedDraw(material,info,pipelines.get(material.pipeline(),draw.mask(),draw.depthEnabled())));
        }
        GpuTextureView mask=null;
        RenderPass pass=null;
        try {
            for(var command:frame.commands()) {
                // Identity writes/resolves change attachments or sample prior writes: keep those boundaries.
                if(!(command instanceof NativeRenderQueue.Draw)&&pass!=null){pass.close();pass=null;}
                if(command instanceof NativeRenderQueue.Draw draw) {
                    if(draw.mask().kind()!=NativeRenderQueue.MaskKind.ALWAYS&&mask==null)throw new IllegalStateException("Scope mask consumed before identity write");
                    var ready=prepared.get(draw);if(ready==null)continue;
                    var material=ready.material();var info=ready.info();
                    if(pass==null)pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"tacz_native_hand_material",target.getColorTextureView(),Optional.empty(),handDepth,OptionalDouble.empty());
                    pass.setPipeline(ready.pipeline());
                    RenderSystem.bindDefaultUniforms(pass);pass.setUniform("Projection",projection);pass.setUniform("DynamicTransforms",material.dynamicTransforms());
                    for(var texture:material.textures())pass.setUniform(texture.name(),texture.textureView(),texture.sampler());
                    if(draw.mask().kind()!=NativeRenderQueue.MaskKind.ALWAYS)pass.setUniform("TaczScopeMask",mask,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                    pass.setVertexBuffer(0,info.vertexBuffer().slice());pass.setIndexBuffer(info.indexBuffer(),info.indexType());
                    pass.drawIndexed(info.indexCount(),1,info.firstIndex(),info.baseVertex(),0);
                } else if(command instanceof NativeRenderQueue.WriteEyes write) {
                    var texture=write.material().prepare().textures().stream().filter(t->t.name().equals("Sampler0")).findFirst().orElseThrow();
                    mask=scope.write(target.width,target.height,eyes(write.eyes()),write.scopeOnly(),write.clear(),projection,handDepth,texture.textureView(),texture.sampler());
                } else if(command instanceof NativeRenderQueue.ResolveEyes resolve) {
                    mask=scope.resolve(eyes(resolve.eyes()),resolve.combined(),projection,handDepth);
                } else if(command instanceof NativeRenderQueue.ClearMask)mask=null;
            }
        } finally {try{if(pass!=null)pass.close();}finally{vertices.endFrame();stableVertices.end();}}
    }
    private static List<NativeScopeMaskPass.Ocular> eyes(List<NativeRenderQueue.Eye> input) {
        List<NativeScopeMaskPass.Ocular> eyes=new ArrayList<>();
        for(var eye:input) {
            var quads=eye.geometry().vertices();if(quads.size()%4!=0)throw new IllegalArgumentException("Authored ocular geometry must be quads");
            List<GeometrySnapshot.Vertex> triangles=new ArrayList<>(quads.size()/4*6);
            for(int i=0;i<quads.size();i+=4)for(int corner:new int[]{0,1,2,2,3,0})triangles.add(quads.get(i+corner));
            eyes.add(new NativeScopeMaskPass.Ocular(eye.id(),eye.scope(),triangles,eye.centerX(),eye.centerY(),eye.radius(),eye.ads()));
        }
        return eyes;
    }
    @Override public void close(){vertices.close();stableVertices.close();scope.close();pipelines.close();}
}
