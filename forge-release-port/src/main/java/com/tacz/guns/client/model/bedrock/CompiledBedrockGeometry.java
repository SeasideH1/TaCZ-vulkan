/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Model-lifetime local vertices/UVs; pose and light remain dynamic on every emission. */
final class CompiledBedrockGeometry {
    private static final int FACE_STRIDE=23;
    private final float[] faces;
    private final float diameter;
    private final float reach;
    CompiledBedrockGeometry(BedrockPolygon[] polygons) {
        faces=new float[polygons.length*FACE_STRIDE];
        float minX=Float.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        int i=0;
        for(var polygon:polygons) {
            faces[i++]=polygon.normal.x();faces[i++]=polygon.normal.y();faces[i++]=polygon.normal.z();
            for(var vertex:polygon.vertices) {
                float x=vertex.pos.x()/16f,y=vertex.pos.y()/16f,z=vertex.pos.z()/16f;
                faces[i++]=x;faces[i++]=y;faces[i++]=z;faces[i++]=vertex.u;faces[i++]=vertex.v;
                minX=Math.min(minX,x);minY=Math.min(minY,y);minZ=Math.min(minZ,z);
                maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);maxZ=Math.max(maxZ,z);
            }
        }
        diameter=(float)Math.sqrt((maxX-minX)*(maxX-minX)+(maxY-minY)*(maxY-minY)+(maxZ-minZ)*(maxZ-minZ));
        float x=Math.max(Math.abs(minX),Math.abs(maxX)),y=Math.max(Math.abs(minY),Math.abs(maxY)),z=Math.max(Math.abs(minZ),Math.abs(maxZ));
        reach=(float)Math.sqrt(x*x+y*y+z*z);
    }
    float diameter(){return diameter;}
    float reach(){return reach;}
    void compile(PoseStack.Pose pose,VertexConsumer consumer,int light,int overlay,float red,float green,float blue,float alpha) {
        // Reuse two temporaries for the entire cube rather than allocating one per normal/vertex.
        Vector3f normal=new Vector3f();Vector4f position=new Vector4f();
        for(int face=0;face<faces.length;face+=FACE_STRIDE) {
            normal.set(faces[face],faces[face+1],faces[face+2]).mul(pose.normal());
            for(int i=face+3;i<face+FACE_STRIDE;i+=5) {
                position.set(faces[i],faces[i+1],faces[i+2],1).mul(pose.pose());
                consumer.addVertex(position.x(),position.y(),position.z()).setColor(red,green,blue,alpha)
                        .setUv(faces[i+3],faces[i+4]).setOverlay(overlay).setLight(light).setNormal(normal.x(),normal.y(),normal.z());
            }
        }
    }
}
