/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.nativeapi.NativeRenderQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

/** Extraction-only rendering helpers. Scope state lives in NativeRenderQueue, never global OpenGL. */
@Environment(EnvType.CLIENT)
public final class RenderHelper {
    private RenderHelper() {}
    public static void renderFirstPersonArm(LocalPlayer player,HumanoidArm hand,PoseStack pose,int light) {
        if(player==null)return;
        if(!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof AvatarRenderer<?> renderer))return;
        var model=renderer.getModel();var arm=hand==HumanoidArm.RIGHT?model.rightArm:model.leftArm;
        var saved=arm.storePose();boolean visible=arm.visible,leftSleeve=model.leftSleeve.visible,rightSleeve=model.rightSleeve.visible;
        float leftRoll=model.leftArm.zRot,rightRoll=model.rightArm.zRot;
        try {
            arm.resetPose();arm.visible=true;
            model.leftSleeve.visible=player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            model.rightSleeve.visible=player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            // Official TACZ removes vanilla arm roll before authored hand-bone animation.
            arm.xRot=arm.yRot=arm.zRot=0;
            var material=RenderTypes.entityTranslucent(player.getSkin().body().texturePath());
            var queue=NativeRenderQueue.current();
            arm.render(pose,queue.getBuffer(material),light,OverlayTexture.NO_OVERLAY);
            queue.endBatch(material);
        } finally {
            arm.loadPose(saved);arm.visible=visible;model.leftSleeve.visible=leftSleeve;model.rightSleeve.visible=rightSleeve;
            model.leftArm.zRot=leftRoll;model.rightArm.zRot=rightRoll;
        }
    }
}
