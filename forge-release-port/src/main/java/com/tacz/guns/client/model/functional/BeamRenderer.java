/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.LaserColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import com.tacz.guns.client.renderer.nativeapi.NativeRenderQueue;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

public class BeamRenderer  {
    public static final Identifier LASER_BEAM_TEXTURE = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "textures/entity/beam.png");
    private static final LaserConfig DEFAULT_LASER_CONFIG = new LaserConfig();

    public static void renderLaserBeam(ItemStack stack, PoseStack poseStack, ItemDisplayContext transformType, @Nonnull List<BedrockPart> path) {
        if (stack == null || !transformType.firstPerson() && !(transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            return;
        }


        NativeRenderQueue bufferSource = NativeRenderQueue.current();
        VertexConsumer builder = bufferSource.getBuffer(LaserBeamRenderState.getLaserBeam());
        poseStack.pushPose();
        {
            for (int i = 0; i < path.size(); ++i) {
                path.get(i).translateAndRotateAndScale(poseStack);
            }

            LaserConfig laserConfig = getLaserConfig(stack);

            int color = LaserColorUtil.getLaserColor(stack, laserConfig);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            stringVertex(transformType.firstPerson() ? -laserConfig.getLength() : -laserConfig.getLengthThird(),
                    transformType.firstPerson() ? laserConfig.getWidth() : laserConfig.getWidthThird(),
                    builder, poseStack.last(), r, g, b, RenderConfig.ENABLE_LASER_FADE_OUT.get());
        }
        poseStack.popPose();
    }

    private static LaserConfig getLaserConfig(ItemStack stack) {
        if (stack == null) {
            return DEFAULT_LASER_CONFIG;
        }

        if (stack.getItem() instanceof IAttachment iAttachment) {
            return TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(stack))
                    .map(ClientAttachmentIndex::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        if (stack.getItem() instanceof IGun) {
            return TimelessAPI.getGunDisplay(stack)
                    .map(GunDisplayInstance::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        return DEFAULT_LASER_CONFIG;
    }

    private static void stringVertex(float z, float width, VertexConsumer pConsumer, PoseStack.Pose pPose, int r, int g, int b, boolean fadeOut) {
        float halfWidth = width / 2;
        int endAlpha = fadeOut ? 0 : 255;
        int light = LightCoordsUtil.pack(15, 15);
    	pConsumer.addVertex(pPose.pose(), -halfWidth, -halfWidth, 0).setColor(r, g, b, 255).setUv(0, 0).setLight(light);
        pConsumer.addVertex(pPose.pose(), -halfWidth, halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), -halfWidth, halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), -halfWidth, -halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 0).setLight(light);

        pConsumer.addVertex(pPose.pose(), -halfWidth, halfWidth, 0).setColor(r, g, b, 255).setUv(0, 0).setLight(light);
        pConsumer.addVertex(pPose.pose(), halfWidth, halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), halfWidth, halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), -halfWidth, halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 0).setLight(light);

        pConsumer.addVertex(pPose.pose(), halfWidth, halfWidth, 0).setColor(r, g, b, 255).setUv(0, 0).setLight(light);
        pConsumer.addVertex(pPose.pose(), halfWidth, -halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), halfWidth, -halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), halfWidth, halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 0).setLight(light);

        pConsumer.addVertex(pPose.pose(), halfWidth, -halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), -halfWidth, -halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), -halfWidth, -halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 1).setLight(light);
        pConsumer.addVertex(pPose.pose(), halfWidth, -halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 0).setLight(light);
    }

    /** Same authored additive beam material expressed as a native RenderPipeline. */
    public static final class LaserBeamRenderState {
        private static final com.mojang.blaze3d.pipeline.RenderPipeline PIPELINE =
                com.mojang.blaze3d.pipeline.RenderPipeline.builder()
                        .withLocation(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/laser_beam"))
                        .withVertexShader(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "core/laser_beam"))
                        .withFragmentShader(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "core/laser_beam"))
                        .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.PROJECTION)
                        .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.DYNAMIC_TRANSFORMS)
                        .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
                        .withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
                        .withCull(false)
                        .withColorTargetState(new com.mojang.renderpearl.api.pipeline.ColorTargetState(
                                new com.mojang.renderpearl.api.pipeline.BlendFunction(
                                        com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA,
                                        com.mojang.renderpearl.api.pipeline.BlendFactor.ONE,
                                        com.mojang.renderpearl.api.pipeline.BlendFactor.ONE,
                                        com.mojang.renderpearl.api.pipeline.BlendFactor.ZERO)))
                        .withDepthStencilState(com.mojang.renderpearl.api.pipeline.DepthStencilState.DEFAULT)
                        .withDepthStencilFormat(com.mojang.renderpearl.api.GpuFormat.D32_FLOAT).build();
        private static final RenderType LASER_BEAM = com.tacz.guns.mixin.client.RenderTypeInvoker.tacz$create("tacz_laser_beam",
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(PIPELINE)
                        .withTexture("Sampler0", LASER_BEAM_TEXTURE).useLightmap().sortOnUpload()
                        .setLayeringTransform(net.minecraft.client.renderer.rendertype.LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .createRenderSetup());
        public static RenderType getLaserBeam() { return LASER_BEAM; }
    }
}
