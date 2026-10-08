/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Retains vanilla hand item transforms while routing optical commands to the native executor. */
public final class NativeHandItemPass {
    private record Pass(Matrix4fc worldToView,List<NativeRenderQueue.Frame> frames) {}
    private static final ThreadLocal<Pass> ACTIVE=new ThreadLocal<>();
    private NativeHandItemPass() {}

    public static void begin(Matrix4fc worldToView) {
        // Each real hand pass owns its pending frames; never reuse a previous frame or reload.
        ACTIVE.set(new Pass(new Matrix4f(worldToView),new ArrayList<>()));
    }
    public static void clear(){ACTIVE.remove();}
    public static List<NativeRenderQueue.Frame> finish() {
        Pass pass=ACTIVE.get();ACTIVE.remove();
        return pass==null?List.of():List.copyOf(pass.frames());
    }
    public static boolean submit(NativeRenderQueue.Frame frame,PoseStack.Pose vanillaPose,int light,int overlay) {
        Pass pass=ACTIVE.get();if(pass==null)return false;
        PoseStack.Pose pose=vanillaPose.copy();
        // Vanilla's hand pose begins with inverse camera rotation. Its render pass applies
        // camera rotation through ModelView; bake that pair into camera-space positions.
        pose.pose().set(pass.worldToView()).mul(vanillaPose.pose());
        // Keep vanilla's world-space normal matrix AND its trusted-normal flag, which
        // controls normalization for nonuniformly scaled item transforms.
        List<NativeRenderQueue.Command> commands=new ArrayList<>();
        for(var command:frame.commands()) {
            if(command instanceof NativeRenderQueue.Draw draw) {
                commands.add(new NativeRenderQueue.Draw(draw.material(),transform(draw.geometry(),pose,light,overlay),draw.mask(),draw.depthEnabled()));
            } else if(command instanceof NativeRenderQueue.WriteEyes write) {
                commands.add(new NativeRenderQueue.WriteEyes(eyes(write.eyes(),pose,light,overlay),write.material(),write.scopeOnly(),write.clear()));
            } else if(command instanceof NativeRenderQueue.ResolveEyes resolve) {
                commands.add(new NativeRenderQueue.ResolveEyes(eyes(resolve.eyes(),pose,light,overlay),resolve.combined()));
            } else commands.add(command);
        }
        pass.frames().add(new NativeRenderQueue.Frame(commands));return true;
    }
    private static GeometrySnapshot transform(GeometrySnapshot geometry,PoseStack.Pose pose,int light,int overlay) {
        var builder=new GeometrySnapshot.Builder();geometry.resolveInheritedAttributes(light,overlay).render(pose,builder);return builder.build();
    }
    private static List<NativeRenderQueue.Eye> eyes(List<NativeRenderQueue.Eye> input,PoseStack.Pose pose,int light,int overlay) {
        List<NativeRenderQueue.Eye> result=new ArrayList<>();
        for(var eye:input) {
            var center=pose.pose().transformPosition(new Vector3f(eye.centerX(),eye.centerY(),eye.centerZ()));
            result.add(new NativeRenderQueue.Eye(eye.id(),eye.scope(),transform(eye.geometry(),pose,light,overlay),
                    center.x(),center.y(),center.z(),eye.radius(),eye.ads()));
        }
        return List.copyOf(result);
    }
}
