/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import com.tacz.guns.client.renderer.nativeapi.NativeItemRenderer;
import com.tacz.guns.client.renderer.nativeapi.NativeRenderQueue;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class GunSmithTableItemRenderer extends NativeItemRenderer {
    private static final SlotModel SLOT_BLOCK_MODEL = new SlotModel();
    public GunSmithTableItemRenderer() {}

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull NativeRenderQueue pBuffer, int pPackedLight, int pPackedOverlay) {
        GunSmithTableRenderer.getIndex(stack).ifPresentOrElse(index -> {
            BedrockModel model = index.getModel();
            Identifier texture = index.getTexture();
            if (model == null) {
                return;
            }
            poseStack.pushPose();

            ItemTransforms transforms = index.getTransforms();
            if (transforms != null) {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                ItemTransform transform = transforms.getTransform(transformType);
                // Native ItemTransform.apply already includes the final (-.5,-.5,-.5) centering.
                transform.apply(false, poseStack.last());
            }

            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.rotate(Axis.ZN.rotationDegrees(180));
            RenderType renderType = RenderTypes.entityTranslucent(texture);
            model.render(poseStack, transformType, renderType, pPackedLight, pPackedOverlay);
            poseStack.popPose();
        }, ()->{
            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.rotate(Axis.ZN.rotationDegrees(180));
            VertexConsumer buffer = pBuffer.getBuffer(RenderTypes.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
            SLOT_BLOCK_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1, 1, 1, 1);
        });
    }
}
