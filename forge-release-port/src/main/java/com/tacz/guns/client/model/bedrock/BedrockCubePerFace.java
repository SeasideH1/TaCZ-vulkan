/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.resource.pojo.model.FaceItem;
import com.tacz.guns.client.resource.pojo.model.FaceUVsItem;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class BedrockCubePerFace implements BedrockCube {
    private static final BedrockVertex EMPTY = new BedrockVertex(0, 0, 0, 0.0F, 0.0F);
    private static final BedrockVertex[] EMPTY_VERTEX = new BedrockVertex[]{EMPTY, EMPTY, EMPTY, EMPTY};
    public final float minX;
    public final float minY;
    public final float minZ;
    public final float maxX;
    public final float maxY;
    public final float maxZ;
    private final BedrockPolygon[] polygons;
    private final CompiledBedrockGeometry geometry;

    public BedrockCubePerFace(float x, float y, float z, float width, float height, float depth, float delta, float texWidth, float texHeight, FaceUVsItem faces) {
        this.minX = x;
        this.minY = y;
        this.minZ = z;
        this.maxX = x + width;
        this.maxY = y + height;
        this.maxZ = z + depth;
        this.polygons = new BedrockPolygon[6];

        float xEnd = x + width;
        float yEnd = y + height;
        float zEnd = z + depth;
        x = x - delta;
        y = y - delta;
        z = z - delta;
        xEnd = xEnd + delta;
        yEnd = yEnd + delta;
        zEnd = zEnd + delta;

        BedrockVertex vertex1 = new BedrockVertex(x, y, z, 0.0F, 0.0F);
        BedrockVertex vertex2 = new BedrockVertex(xEnd, y, z, 0.0F, 8.0F);
        BedrockVertex vertex3 = new BedrockVertex(xEnd, yEnd, z, 8.0F, 8.0F);
        BedrockVertex vertex4 = new BedrockVertex(x, yEnd, z, 8.0F, 0.0F);
        BedrockVertex vertex5 = new BedrockVertex(x, y, zEnd, 0.0F, 0.0F);
        BedrockVertex vertex6 = new BedrockVertex(xEnd, y, zEnd, 0.0F, 8.0F);
        BedrockVertex vertex7 = new BedrockVertex(xEnd, yEnd, zEnd, 8.0F, 8.0F);
        BedrockVertex vertex8 = new BedrockVertex(x, yEnd, zEnd, 8.0F, 0.0F);

        this.polygons[2] = getTexturedQuad(new BedrockVertex[]{vertex6, vertex5, vertex1, vertex2}, texWidth, texHeight, Direction.DOWN, faces);
        this.polygons[3] = getTexturedQuad(new BedrockVertex[]{vertex3, vertex4, vertex8, vertex7}, texWidth, texHeight, Direction.UP, faces);
        this.polygons[1] = getTexturedQuad(new BedrockVertex[]{vertex1, vertex5, vertex8, vertex4}, texWidth, texHeight, Direction.WEST, faces);
        this.polygons[4] = getTexturedQuad(new BedrockVertex[]{vertex2, vertex1, vertex4, vertex3}, texWidth, texHeight, Direction.NORTH, faces);
        this.polygons[0] = getTexturedQuad(new BedrockVertex[]{vertex6, vertex2, vertex3, vertex7}, texWidth, texHeight, Direction.EAST, faces);
        this.polygons[5] = getTexturedQuad(new BedrockVertex[]{vertex5, vertex6, vertex7, vertex8}, texWidth, texHeight, Direction.SOUTH, faces);
        geometry = new CompiledBedrockGeometry(polygons);
    }

    private BedrockPolygon getTexturedQuad(BedrockVertex[] positionsIn, float texWidth, float texHeight, Direction direction, FaceUVsItem faces) {
        FaceItem face = faces.getFace(direction);
        float u1 = face.getUv()[0];
        float v1 = face.getUv()[1];
        float u2 = u1 + face.getUvSize()[0];
        float v2 = v1 + face.getUvSize()[1];
        if (face == FaceItem.EMPTY) {
            return new BedrockPolygon(EMPTY_VERTEX, u1, v1, u2, v2, texWidth, texHeight, false, direction);
        }
        return new BedrockPolygon(positionsIn, u1, v1, u2, v2, texWidth, texHeight, false, direction);
    }


    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        geometry.compile(pose, consumer, light, overlay, red, green, blue, alpha);
    }
    @Override public float detailDiameter() { return geometry.diameter(); }
    @Override public float detailReach() { return geometry.reach(); }

}
