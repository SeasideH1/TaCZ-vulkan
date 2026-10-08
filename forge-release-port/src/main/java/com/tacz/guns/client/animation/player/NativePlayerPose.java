/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.animation.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.renderer.nativeapi.NativeThirdPersonPose;
import dev.kosmx.playerAnim.core.util.MathHelper;
import net.minecraft.world.phys.Vec3;

/** Adapter preserving the original PlayerAnimator model-part and body-root conventions. */
public record NativePlayerPose(PoseSnapshot snapshot) implements NativeThirdPersonPose.PlayerPose {
    @Override public Vec3 transformPosition(String bone, double x, double y, double z) {
        return snapshot.transformPosition(bone, (float) x, (float) y, (float) z);
    }
    @Override public Vec3 transformRotation(String bone, double x, double y, double z) {
        return snapshot.transformRotation(bone, MathHelper.clampToRadian((float) x),
                MathHelper.clampToRadian((float) y), MathHelper.clampToRadian((float) z));
    }
    @Override public boolean active() { return snapshot.isActive(); }
    @Override public void applyRoot(PoseStack pose) {
        Vec3 position = snapshot.transformPosition("body", 0, 0, 0);
        Vec3 rotation = snapshot.transformRotation("body", 0, 0, 0);
        pose.translate(position.x, position.y + 0.7, position.z);
        pose.rotate(Axis.ZP.rotation((float) rotation.z));
        pose.rotate(Axis.YP.rotation((float) rotation.y));
        pose.rotate(Axis.XP.rotation((float) rotation.x));
        pose.translate(0, -0.7, 0);
    }
}
