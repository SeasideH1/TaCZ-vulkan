/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.common;

import com.tacz.guns.fabric.entity.ForcedPose;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerPoseMixin implements ForcedPose {
    @Unique private @Nullable Pose tacz$forcedPose;
    @Override public void tacz$setForcedPose(@Nullable Pose pose) {
        tacz$forcedPose = pose;
        if (pose != null) ((Player) (Object) this).setPose(pose);
    }
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void tacz$applyForcedPose(CallbackInfo callback) {
        if (tacz$forcedPose != null) {
            ((Player) (Object) this).setPose(tacz$forcedPose);
            callback.cancel();
        }
    }
}
