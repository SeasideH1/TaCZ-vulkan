/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.config.client.RenderConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class RenderDistance {
    private static long GUI_RENDER_TIMESTAMP = -1L;
    private static final ThreadLocal<Double> EXTRACTED_DISTANCE_SQUARED = new ThreadLocal<>();
    private record DetailLod(net.minecraft.world.item.ItemDisplayContext context,double distance,double focalPixels,double outerScale,double outerOffset,double pixelLimit) {}
    private static final ThreadLocal<DetailLod> DETAIL_LOD=new ThreadLocal<>();

    /** Explicit extraction context prevents GUI/first-person detail loss and uses the current zoom. */
    public static DetailScope withDetailLod(net.minecraft.world.item.ItemDisplayContext context,double outerScale,double outerOffset) {
        DetailLod previous=DETAIL_LOD.get();Double distanceSquared=EXTRACTED_DISTANCE_SQUARED.get();
        var mc=net.minecraft.client.Minecraft.getInstance();
        boolean world=context==net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                ||context==net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                ||context==net.minecraft.world.item.ItemDisplayContext.GROUND||context==net.minecraft.world.item.ItemDisplayContext.FIXED;
        int minimum=RenderConfig.SMALL_DETAIL_LOD_DISTANCE==null?0:RenderConfig.SMALL_DETAIL_LOD_DISTANCE.get();
        double pixels=RenderConfig.SMALL_DETAIL_LOD_PIXELS==null?0:RenderConfig.SMALL_DETAIL_LOD_PIXELS.get();
        double distance=distanceSquared==null?0:Math.sqrt(distanceSquared);
        double fov=mc.gameRenderer.mainCamera().getFov();
        var target=mc.gameRenderer.mainRenderTarget();
        double tangent=Math.tan(Math.toRadians(fov)*.5),focal=target.height/(2*tangent);
        // A radial distance alone underestimates projected size at the screen edges.
        double aspect=(double)target.width/Math.max(1,target.height);
        double nearDepth=distance/Math.sqrt(1+tangent*tangent*(1+aspect*aspect));
        boolean active=world&&minimum>0&&distance>=minimum&&Double.isFinite(focal)&&focal>0
                &&Double.isFinite(outerScale)&&outerScale>0&&Double.isFinite(outerOffset);
        DETAIL_LOD.set(new DetailLod(context,nearDepth,focal,outerScale,outerOffset,active?pixels:0));
        return new DetailScope(previous);
    }
    public static final class DetailScope implements AutoCloseable {
        private final DetailLod previous;private boolean closed;
        private DetailScope(DetailLod previous){this.previous=previous;}
        @Override public void close(){if(closed)return;if(previous==null)DETAIL_LOD.remove();else DETAIL_LOD.set(previous);closed=true;}
    }
    public static boolean detailLodActive(){var detail=DETAIL_LOD.get();return detail!=null&&detail.pixelLimit()>0;}
    public static boolean skipSmallDetail(PoseStack.Pose pose,float diameter,float reach) {
        var detail=DETAIL_LOD.get();if(detail==null||detail.pixelLimit()<=0||!Float.isFinite(diameter)||!Float.isFinite(reach))return false;
        var m=pose.pose();
        // Frobenius norm conservatively bounds stretch, including nonuniform bone scaling.
        double stretch=Math.sqrt(m.m00()*m.m00()+m.m01()*m.m01()+m.m02()*m.m02()
                +m.m10()*m.m10()+m.m11()*m.m11()+m.m12()*m.m12()+m.m20()*m.m20()+m.m21()*m.m21()+m.m22()*m.m22())*detail.outerScale();
        double size=diameter*stretch;
        double displacement=Math.sqrt(m.m30()*m.m30()+m.m31()*m.m31()+m.m32()*m.m32())*detail.outerScale();
        double nearDistance=detail.distance()-detail.outerOffset()-displacement-reach*stretch;
        return nearDistance>0&&size*detail.focalPixels()/nearDistance<detail.pixelLimit();
    }

    /** Native item extraction has no draw-time pose; retain its captured world-space distance. */
    public static DistanceScope withDistanceSquared(double distanceSquared) {
        if (Double.isNaN(distanceSquared) || distanceSquared < 0) throw new IllegalArgumentException("Invalid LOD distance");
        Double previous = EXTRACTED_DISTANCE_SQUARED.get();
        EXTRACTED_DISTANCE_SQUARED.set(distanceSquared);
        return new DistanceScope(previous);
    }

    public static final class DistanceScope implements AutoCloseable {
        private final Double previous;
        private boolean closed;
        private DistanceScope(Double previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return;
            if (previous == null) EXTRACTED_DISTANCE_SQUARED.remove(); else EXTRACTED_DISTANCE_SQUARED.set(previous);
            closed = true;
        }
    }

    public static boolean inRenderHighPolyModelDistance(PoseStack poseStack) {
        return inRenderHighPolyModelDistance(poseStack, RenderConfig.GUN_LOD_RENDER_DISTANCE.get(), isGuiRender());
    }

    /** Explicit parameters also make the exact native cutoff and nested extraction contract testable. */
    public static boolean inRenderHighPolyModelDistance(PoseStack poseStack, int distance, boolean gui) {
        if (gui) return true;
        if (distance <= 0) return false;
        Double extracted = EXTRACTED_DISTANCE_SQUARED.get();
        double viewDistance;
        if (extracted != null) viewDistance = extracted;
        else {
            Matrix4f matrix = poseStack.last().pose();
            viewDistance = (double) matrix.m30() * matrix.m30() + (double) matrix.m31() * matrix.m31() + (double) matrix.m32() * matrix.m32();
        }
        return viewDistance < (double) distance * distance;
    }

    public static void markGuiRenderTimestamp() {
        GUI_RENDER_TIMESTAMP = System.currentTimeMillis();
    }

    private static boolean isGuiRender() {
        var detail=DETAIL_LOD.get();if(detail!=null)return detail.context()==net.minecraft.world.item.ItemDisplayContext.GUI;
        return System.currentTimeMillis() - GUI_RENDER_TIMESTAMP < 100;
    }
}
