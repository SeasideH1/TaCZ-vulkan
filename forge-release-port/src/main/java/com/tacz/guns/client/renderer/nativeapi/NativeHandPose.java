/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.util.Mth;
/** Same camera-space hand hurt/walk transform as snapshot3, with retained TACZ cancellation hooks. */
final class NativeHandPose {
    private NativeHandPose() {}
    static void apply(CameraRenderState camera,OptionsRenderState options,PoseStack pose) {
        var entity=camera.entityRenderState;
        if(!GunHurtBobTweak.onHurtBobTweak(camera,pose)&&!TaczEvents.BUS.post(new RenderItemInHandBobEvent.BobHurt())&&entity.isLiving) {
            if(entity.isDeadOrDying)pose.rotateDegrees(Axis.ZP,40-8000/(Math.min(entity.deathTime,20)+200));
            float hurt=entity.hurtTime;
            if(hurt>=0) {
                hurt/=entity.hurtDuration;hurt=Mth.sin(hurt*hurt*hurt*hurt*(float)Math.PI);
                pose.rotateDegrees(Axis.YP,-entity.hurtDir);
                pose.rotateDegrees(Axis.ZP,(float)(-(double)hurt*14.0D*options.damageTiltStrength));
                pose.rotateDegrees(Axis.YP,entity.hurtDir);
            }
        }
        if(options.bobView&&!TaczEvents.BUS.post(new RenderItemInHandBobEvent.BobView())&&entity.isPlayer) {
            float walk=entity.backwardsInterpolatedWalkDistance,bob=entity.bob;
            pose.translate(Mth.sin(walk*(float)Math.PI)*bob*.5f,-Math.abs(Mth.cos(walk*(float)Math.PI)*bob),0);
            pose.rotateDegrees(Axis.ZP,Mth.sin(walk*(float)Math.PI)*bob*3);
            pose.rotateDegrees(Axis.XP,Math.abs(Mth.cos(walk*(float)Math.PI-.2f)*bob)*5);
        }
    }
}
