/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class BedrockPart {
    @Nullable
    public final String name;
    public final ObjectList<BedrockCube> cubes = new ObjectArrayList<>();
    public final ObjectList<BedrockPart> children = new ObjectArrayList<>();
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public boolean visible = true;
    public boolean illuminated = false;
    public boolean mirror;
    /**
     * 通常用于动画旋转。
     */
    public Quaternionf additionalQuaternion = new Quaternionf(0, 0, 0, 1);
    public float xScale = 1;
    public float yScale = 1;
    public float zScale = 1;
    protected BedrockPart parent;
    private float initRotX;
    private float initRotY;
    private float initRotZ;

    public BedrockPart(@Nullable String name) {
        this.name = name;
    }

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void render(PoseStack poseStack, ItemDisplayContext transformType, VertexConsumer consumer, int light, int overlay) {
        this.render(poseStack, transformType, consumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void render(PoseStack poseStack, ItemDisplayContext transformType, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        int cubePackedLight = light;
        if (illuminated) {
            // 最大亮度
            cubePackedLight = LightCoordsUtil.pack(15, 15);
        }
        if (this.visible) {
            if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
                poseStack.pushPose();
                this.translateAndRotateAndScale(poseStack);
                this.compile(poseStack.last(), consumer, cubePackedLight, overlay, red, green, blue, alpha);

                for (BedrockPart part : this.children) {
                    part.render(poseStack, transformType, consumer, cubePackedLight, overlay, red, green, blue, alpha);
                }

                poseStack.popPose();
            }
        }
    }

    public void translateAndRotateAndScale(PoseStack poseStack) {
        poseStack.translate(this.offsetX, this.offsetY, this.offsetZ);
        poseStack.translate((this.x / 16.0F), (this.y / 16.0F), (this.z / 16.0F));
        if (this.zRot != 0.0F) {
            poseStack.rotate(Axis.ZP.rotation(this.zRot));
        }
        if (this.yRot != 0.0F) {
            poseStack.rotate(Axis.YP.rotation(this.yRot));
        }
        if (this.xRot != 0.0F) {
            poseStack.rotate(Axis.XP.rotation(this.xRot));
        }
        poseStack.rotate(additionalQuaternion);
        poseStack.scale(xScale, yScale, zScale);
    }

    public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        float largest=0;
        boolean details=!illuminated&&com.tacz.guns.util.RenderDistance.detailLodActive();
        if(details)for(BedrockCube cube:cubes)if(Float.isFinite(cube.detailDiameter()))largest=Math.max(largest,cube.detailDiameter());
        for (BedrockCube bedrockCube : this.cubes) {
            // Preserve the main structure of every bone, even in models made entirely of tiny cubes.
            if(details&&bedrockCube.detailDiameter()<largest*.25f
                    &&com.tacz.guns.util.RenderDistance.skipSmallDetail(pose,bedrockCube.detailDiameter(),bedrockCube.detailReach()))continue;
            bedrockCube.compile(pose, consumer, light, overlay, red, green, blue, alpha);
        }
    }

    public BedrockCube getRandomCube(Random random) {
        return this.cubes.get(random.nextInt(this.cubes.size()));
    }

    public boolean isEmpty() {
        return this.cubes.isEmpty();
    }

    public void setInitRotationAngle(float x, float y, float z) {
        this.initRotX = x;
        this.initRotY = y;
        this.initRotZ = z;
    }

    public float getInitRotX() {
        return initRotX;
    }

    public float getInitRotY() {
        return initRotY;
    }

    public float getInitRotZ() {
        return initRotZ;
    }

    public void addChild(BedrockPart model) {
        this.children.add(model);
    }

    public BedrockPart getParent() {
        return parent;
    }
}
