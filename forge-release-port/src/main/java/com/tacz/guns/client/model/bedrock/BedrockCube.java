/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public interface BedrockCube {
    /** Unknown addon geometry is never eligible for automatic small-detail LOD. */
    default float detailDiameter(){return Float.POSITIVE_INFINITY;}
    default float detailReach(){return Float.POSITIVE_INFINITY;}
    void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha);
}
