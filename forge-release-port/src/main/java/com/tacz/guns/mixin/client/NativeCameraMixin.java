/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client;
import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.fabric.client.event.ViewportEvent;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Camera.class)
public abstract class NativeCameraMixin {
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private int matrixPropertiesDirty;
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void tacz$worldFov(float partial,CallbackInfoReturnable<Float> result){
        var event=new ViewportEvent.ComputeFov((Camera)(Object)this,partial,true,result.getReturnValue());TaczEvents.BUS.post(event);result.setReturnValue((float)event.getFOV());
    }
    @Inject(method="calculateHudFov",at=@At("RETURN"),cancellable=true)
    private void tacz$handFov(float partial,CallbackInfoReturnable<Float> result){
        var event=new ViewportEvent.ComputeFov((Camera)(Object)this,partial,false,result.getReturnValue());TaczEvents.BUS.post(event);com.tacz.guns.client.renderer.nativeapi.NativeHandRenderer.INSTANCE.setHandFov((float)event.getFOV());result.setReturnValue((float)event.getFOV());
    }
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void tacz$angles(float partial,CallbackInfo ci){
        Camera camera=(Camera)(Object)this;
        var event=new ViewportEvent.ComputeCameraAngles(camera,partial,camera.yRot(),camera.xRot(),0);TaczEvents.BUS.post(event);
        setRotation(event.getYaw(),event.getPitch());
        rotation.rotateZ((float)Math.toRadians(-event.getRoll()));
        rotation.transform(0,0,-1,forwards);rotation.transform(0,1,0,up);rotation.transform(-1,0,0,left);matrixPropertiesDirty|=3;
    }
}
