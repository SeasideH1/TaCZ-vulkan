/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.gun.MuzzleFlash;
import com.tacz.guns.fabric.compat.ClientIntegrationHooks;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import com.tacz.guns.client.renderer.nativeapi.NativeRenderQueue;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MuzzleFlashRender implements IFunctionalRenderer {
    private static final SlotModel MUZZLE_FLASH_MODEL = new SlotModel(true);
    /**
     * 50ms 显示时间
     */
    private static final long TIME_RANGE = 50;
    public static boolean isSelf = false;
    private static long shootTimeStamp = -1;
    private static float muzzleFlashRandomRotate = 0;

    private final BedrockGunModel bedrockGunModel;

    public MuzzleFlashRender(BedrockGunModel bedrockGunModel) {
        this.bedrockGunModel = bedrockGunModel;
    }

    public static void onShoot() {
        // 记录开火时间戳
        shootTimeStamp = System.currentTimeMillis();
        // 随机给予枪口火焰的旋转
        muzzleFlashRandomRotate = (float) (Math.random() * 360);
    }

    private static void renderMuzzleFlash(GunDisplayInstance display, PoseStack poseStack, BedrockModel bedrockModel, long time) {
        MuzzleFlash muzzleFlash = display.getMuzzleFlash();
        if (muzzleFlash == null) {
            return;
        }
        // Capture this extraction's animated muzzle, not the first frame of the shot.
        // A static pose can also be overwritten by third-person item extraction before
        // the first-person hand pass; each deferred draw must own its coordinates.
        Matrix3f normal = new Matrix3f(poseStack.last().normal());
        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        float rotation = muzzleFlashRandomRotate;
        bedrockModel.delegateRender((poseStack1, vertexConsumer1, transformType1, light, overlay) ->
                doRender(light, overlay, muzzleFlash, time, normal, pose, rotation));
    }

    private static void doRender(int light, int overlay, MuzzleFlash muzzleFlash, long time,
                                 Matrix3f normal, Matrix4f pose, float rotation) {
            float scale = 0.5f * muzzleFlash.getScale();
            float scaleTime = TIME_RANGE / 2.0f;
            scale = time < scaleTime ? (scale * (time / scaleTime)) : scale;
            NativeRenderQueue multiBufferSource = NativeRenderQueue.current();

            // 推送到指定位置
            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(normal);
            poseStack2.last().pose().mul(pose);

            // 先渲染一遍半透明背景
            poseStack2.pushPose();
            {
                poseStack2.scale(scale, scale, scale);
                poseStack2.rotate(Axis.ZP.rotationDegrees(rotation));
                poseStack2.translate(0, -1, 0);
                RenderType renderTypeBg = RenderTypes.entityTranslucent(muzzleFlash.getTexture());
                MUZZLE_FLASH_MODEL.renderToBuffer(poseStack2, multiBufferSource.getBuffer(renderTypeBg), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack2.popPose();

            // 然后渲染发光效果
            poseStack2.pushPose();
            {
                poseStack2.scale(scale / 2, scale / 2, scale / 2);
                poseStack2.rotate(Axis.ZP.rotationDegrees(rotation));
                poseStack2.translate(0, -0.9, 0);
                RenderType renderTypeLight = RenderTypes.energySwirl(muzzleFlash.getTexture(), 1, 1);
                MUZZLE_FLASH_MODEL.renderToBuffer(poseStack2, multiBufferSource.getBuffer(renderTypeLight), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack2.popPose();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay) {
        if (ClientIntegrationHooks.isShadowPass()) {
            return;
        }
        if (!isSelf) {
            return;
        }
        long time = System.currentTimeMillis() - shootTimeStamp;
        if (time > TIME_RANGE) {
            return;
        }
        ItemStack currentGunItem = bedrockGunModel.getCurrentGunItem();

        TimelessAPI.getGunDisplay(currentGunItem).ifPresent(display -> {
            ItemStack muzzleAttachment = bedrockGunModel.getCurrentAttachmentItem().get(AttachmentType.MUZZLE);
            IAttachment iAttachment = IAttachment.getIAttachmentOrNull(muzzleAttachment);
            if (iAttachment != null) {
                Identifier attachmentId = iAttachment.getAttachmentId(muzzleAttachment);
                TimelessAPI.getCommonAttachmentIndex(attachmentId).ifPresent(index -> {
                    var modifier = index.getData().getModifier();
                    if (modifier.containsKey(SilenceModifier.ID) && modifier.get(SilenceModifier.ID).getValue() instanceof Pair<?, ?> pair) {
                        // 如果安装了消音器，则不渲染枪口火光
                        if (((Pair<Integer, Boolean>) pair).right()) {
                            return;
                        }
                    }
                    renderMuzzleFlash(display, poseStack, bedrockGunModel, time);
                });
            } else {
                renderMuzzleFlash(display, poseStack, bedrockGunModel, time);
            }
        });
    }
}
