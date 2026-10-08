/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.papi.PapiManager;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import com.tacz.guns.client.renderer.nativeapi.NativeRenderQueue;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class TextShowRender implements IFunctionalRenderer {
    private final BedrockModel bedrockModel;
    private final TextShow textShow;
    private final ItemStack gunStack;

    public TextShowRender(BedrockModel bedrockModel, TextShow textShow, ItemStack gunStack) {
        this.bedrockModel = bedrockModel;
        this.textShow = textShow;
        this.gunStack = gunStack;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay) {
        if (!transformType.firstPerson()) {
            return;
        }
        String text = PapiManager.getTextShow(textShow.getTextKey(), gunStack);
        if (StringUtils.isBlank(text)) {
            return;
        }
        poseStack.rotate(Axis.ZP.rotationDegrees(180f));
        Matrix3f normal = new Matrix3f(poseStack.last().normal());
        Matrix4f pose = new Matrix4f(poseStack.last().pose());

        // 和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
        bedrockModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
            Font font = Minecraft.getInstance().font;
            boolean shadow = textShow.isShadow();
            int color = textShow.getColorInt();
            float scale = textShow.getScale();
            int packLight = LightCoordsUtil.pack(textShow.getTextLight(), textShow.getTextLight());
            int width = font.width(text);
            int xOffset;
            switch (textShow.getAlign()) {
                case CENTER -> xOffset = width / 2;
                case RIGHT -> xOffset = width;
                default -> xOffset = 0;
            }

            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(normal);
            poseStack2.last().pose().mul(pose);
            poseStack2.scale(2 / 300f * scale, -2 / 300f * scale, -2 / 300f);

            NativeRenderQueue bufferSource = NativeRenderQueue.current();
            font.prepareText(text, -xOffset, -font.lineHeight / 2f, color, shadow, 0).visit(new Font.GlyphVisitor() {
                @Override public void acceptRenderable(net.minecraft.client.gui.font.TextRenderable glyph) {
                    glyph.render(poseStack2.last().pose(), bufferSource.getBuffer(glyph.renderType(Font.DisplayMode.NORMAL)), packLight, false);
                }
            });
            bufferSource.endBatch();
        });
    }
}
