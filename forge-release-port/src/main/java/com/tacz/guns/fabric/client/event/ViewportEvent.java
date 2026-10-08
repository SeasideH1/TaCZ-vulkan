/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.event;
import com.tacz.guns.api.event.Event;
import net.minecraft.client.Camera;
/** Native camera extraction events, posted by exact snapshot Camera hooks. */
public abstract class ViewportEvent extends Event {
    private final Camera camera;private final double partialTick;
    protected ViewportEvent(Camera camera,double partialTick){this.camera=camera;this.partialTick=partialTick;}
    public Camera getCamera(){return camera;}public double getPartialTick(){return partialTick;}
    public static final class ComputeFov extends ViewportEvent {
        private final boolean configured;private double fov;
        public ComputeFov(Camera camera,double partialTick,boolean configured,double fov){super(camera,partialTick);this.configured=configured;this.fov=fov;}
        public boolean usedConfiguredFov(){return configured;}public double getFOV(){return fov;}public void setFOV(double value){fov=value;}
    }
    public static final class ComputeCameraAngles extends ViewportEvent {
        private float yaw,pitch,roll;
        public ComputeCameraAngles(Camera camera,double partialTick,float yaw,float pitch,float roll){super(camera,partialTick);this.yaw=yaw;this.pitch=pitch;this.roll=roll;}
        public float getYaw(){return yaw;}public float getPitch(){return pitch;}public float getRoll(){return roll;}
        public void setYaw(float value){yaw=value;}public void setPitch(float value){pitch=value;}public void setRoll(float value){roll=value;}
    }
}
