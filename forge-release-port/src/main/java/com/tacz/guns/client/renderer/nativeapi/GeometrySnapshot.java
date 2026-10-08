/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import java.util.*;

/** Immutable packed extraction data; ordinary rendering allocates no per-vertex objects. */
public final class GeometrySnapshot implements SubmitNodeCollector.CustomGeometryRenderer {
    private static final int STRIDE=16;
    public static final GeometrySnapshot EMPTY=new GeometrySnapshot(new int[0],-1,-1);
    private final int[] data;
    private final boolean inheritsLight,inheritsOverlay;
    private final int light,overlay,hash;
    public record Vertex(float x,float y,float z,int color,float u,float v,int overlayU,int overlayV,
                         int lightU,int lightV,float u3,float v3,float nx,float ny,float nz,float lineWidth) {}
    public GeometrySnapshot(List<Vertex> vertices){this(pack(vertices),-1,-1);}
    private GeometrySnapshot(int[] data,int light,int overlay) {
        this.data=data;this.light=light;this.overlay=overlay;
        boolean l=false,o=false;int h=1;
        for(int i=0;i<data.length;i+=STRIDE){l|=data[i+8]==65535&&data[i+9]==65535;o|=data[i+6]==65535&&data[i+7]==65535;for(int j=0;j<STRIDE;j++)h=31*h+data[i+j];}
        inheritsLight=l;inheritsOverlay=o;hash=31*(31*h+light)+overlay;
    }
    private GeometrySnapshot(GeometrySnapshot source,int light,int overlay) {
        data=source.data;inheritsLight=source.inheritsLight;inheritsOverlay=source.inheritsOverlay;
        this.light=light;this.overlay=overlay;hash=source.hash-31*source.light-source.overlay+31*light+overlay;
    }
    private static int[] pack(List<Vertex> vertices) {
        Builder builder=new Builder();
        for(Vertex v:vertices)builder.addVertex(v.x(),v.y(),v.z()).setColor(v.color()).setUv(v.u(),v.v())
                .setUv1(v.overlayU(),v.overlayV()).setUv2(v.lightU(),v.lightV()).setUv3(v.u3(),v.v3())
                .setNormal(v.nx(),v.ny(),v.nz()).setLineWidth(v.lineWidth());
        return builder.build().data;
    }
    public int vertexCount(){return data.length/STRIDE;}
    public long packedBytes(){return (long)data.length*Integer.BYTES;}
    private static float f(int bits){return Float.intBitsToFloat(bits);}
    private int attribute(int offset,int inherited){return data[offset]==65535&&data[offset+1]==65535?inherited:(data[offset]&65535)|(data[offset+1]&65535)<<16;}
    /** Compatibility view for small ocular meshes and addons; hot paths use render directly. */
    public List<Vertex> vertices(){return new AbstractList<>() {
        @Override public int size(){return vertexCount();}
        @Override public Vertex get(int index){
            Objects.checkIndex(index,size());int i=index*STRIDE;int l=attribute(i+8,light),o=attribute(i+6,overlay);
            return new Vertex(f(data[i]),f(data[i+1]),f(data[i+2]),data[i+3],f(data[i+4]),f(data[i+5]),o&65535,o>>>16,l&65535,l>>>16,
                    f(data[i+10]),f(data[i+11]),f(data[i+12]),f(data[i+13]),f(data[i+14]),f(data[i+15]));
        }
    };}
    @Override public void render(PoseStack.Pose pose,VertexConsumer consumer) {
        var position=new org.joml.Vector3f();var normal=new org.joml.Vector3f();
        for(int i=0;i<data.length;i+=STRIDE){
            pose.pose().transformPosition(f(data[i]),f(data[i+1]),f(data[i+2]),position);
            pose.transformNormal(f(data[i+12]),f(data[i+13]),f(data[i+14]),normal);
            consumer.addVertex(position.x(),position.y(),position.z()).setColor(data[i+3]).setUv(f(data[i+4]),f(data[i+5]))
                    .setOverlay(attribute(i+6,overlay)).setLight(attribute(i+8,light)).setUv3(f(data[i+10]),f(data[i+11]))
                    .setNormal(normal.x(),normal.y(),normal.z()).setLineWidth(f(data[i+15]));
        }
    }
    public GeometrySnapshot resolveInheritedAttributes(int light,int overlay) {
        int l=inheritsLight&&this.light==-1?light:this.light,o=inheritsOverlay&&this.overlay==-1?overlay:this.overlay;
        return l==this.light&&o==this.overlay?this:new GeometrySnapshot(this,l,o);
    }
    public GeometrySnapshot concat(GeometrySnapshot other) {
        if(light!=other.light||overlay!=other.overlay)throw new IllegalArgumentException("Different inherited attributes");
        int[] joined=Arrays.copyOf(data,data.length+other.data.length);System.arraycopy(other.data,0,joined,data.length,other.data.length);
        return new GeometrySnapshot(joined,light,overlay);
    }
    public void includeBounds(float[] bounds) {
        for(int i=0;i<data.length;i+=STRIDE)for(int axis=0;axis<3;axis++){float v=f(data[i+axis]);bounds[axis]=Math.min(bounds[axis],v);bounds[axis+3]=Math.max(bounds[axis+3],v);}
    }
    @Override public int hashCode(){return hash;}
    @Override public boolean equals(Object other){return this==other||other instanceof GeometrySnapshot g&&hash==g.hash&&light==g.light&&overlay==g.overlay&&Arrays.equals(data,g.data);}
    public static final class Builder implements VertexConsumer {
        private int[] data=new int[STRIDE*32];private int size;private boolean built;
        private static int bits(float v){return Float.floatToIntBits(v);}
        public GeometrySnapshot build(){if(built)throw new IllegalStateException("Snapshot already built");built=true;return size==0?EMPTY:new GeometrySnapshot(Arrays.copyOf(data,size),-1,-1);}
        @Override public VertexConsumer addVertex(float x,float y,float z){
            if(built)throw new IllegalStateException("Snapshot already built");
            if(size+STRIDE>data.length)data=Arrays.copyOf(data,Math.max(size+STRIDE,data.length*2));
            int i=size;size+=STRIDE;Arrays.fill(data,i,i+STRIDE,0);data[i]=bits(x);data[i+1]=bits(y);data[i+2]=bits(z);data[i+3]=-1;data[i+15]=bits(1);return this;
        }
        private int offset(){if(size==0||built)throw new IllegalStateException("No writable vertex");return size-STRIDE;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return setColor((a&255)<<24|(r&255)<<16|(g&255)<<8|(b&255));}
        @Override public VertexConsumer setColor(int argb){data[offset()+3]=argb;return this;}
        @Override public VertexConsumer setUv(float u,float v){int i=offset();data[i+4]=bits(u);data[i+5]=bits(v);return this;}
        @Override public VertexConsumer setUv1(int u,int v){int i=offset();data[i+6]=u;data[i+7]=v;return this;}
        @Override public VertexConsumer setUv2(int u,int v){int i=offset();data[i+8]=u;data[i+9]=v;return this;}
        @Override public VertexConsumer setUv3(float u,float v){int i=offset();data[i+10]=bits(u);data[i+11]=bits(v);return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){int i=offset();data[i+12]=bits(x);data[i+13]=bits(y);data[i+14]=bits(z);return this;}
        @Override public VertexConsumer setLineWidth(float width){data[offset()+15]=bits(width);return this;}
    }
}
