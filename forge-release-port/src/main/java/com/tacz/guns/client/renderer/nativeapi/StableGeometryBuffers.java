/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix3f;
import java.util.*;

/** Reuses unchanged opaque draw bytes; animated/sorted draws retain the staged upload path. */
final class StableGeometryBuffers implements AutoCloseable {
    private static final long BUDGET=32L*1024*1024,MAX_ENTRY=8L*1024*1024;
    private static final int MAX_SLOTS=128;
    private final Map<Integer,Entry> entries=new HashMap<>();
    private final ByteBufferBuilder scratch=new ByteBufferBuilder(65536);
    private long used;
    private int generation;
    static final class Entry {
        GeometrySnapshot geometry;RenderType material;final Matrix3f normal=new Matrix3f();
        GpuBuffer buffer;long accounted;int generation,stable,indexCount;
        StagedVertexBuffer.ExecuteInfo info(){
            var indices=RenderSystem.getSequentialBuffer(material.primitiveTopology());
            return new StagedVertexBuffer.ExecuteInfo(buffer,null,indices.type(),0,0,indexCount,material.primitiveTopology());
        }
    }
    void begin(){generation++;}
    Entry find(int slot,NativeRenderQueue.Draw draw,PoseStack.Pose pose) {
        var material=draw.material();var topology=material.primitiveTopology();var geometry=draw.geometry();
        if(slot>=MAX_SLOTS||material.sortOnUpload()||(topology!=PrimitiveTopology.QUADS&&topology!=PrimitiveTopology.TRIANGLES))return null;
        long bytes=geometry.packedBytes()+(long)geometry.vertexCount()*material.format().getVertexSize();
        if(bytes>MAX_ENTRY)return null;
        Entry entry=entries.get(slot);
        if(entry!=null&&(entry.material!=material||!entry.normal.equals(pose.normal(),0)||!entry.geometry.equals(geometry))) {
            release(entry);entries.remove(slot);entry=null;
        }
        if(entry==null) {
            if(used+bytes>BUDGET)return null;
            entry=new Entry();entry.geometry=geometry;entry.material=material;entry.normal.set(pose.normal());entry.accounted=bytes;
            entries.put(slot,entry);used+=bytes;
        }
        entry.generation=generation;
        // Avoid allocating a persistent buffer for geometry that changes every extraction.
        if(++entry.stable<3)return null;
        if(entry.buffer==null) {
            BufferBuilder builder=new BufferBuilder(scratch,topology,material.format());
            geometry.render(pose,builder);
            try(MeshData mesh=builder.buildOrThrow()) {
                entry.indexCount=mesh.drawState().indexCount();
                entry.buffer=RenderSystem.getDevice().createBuffer(()->"tacz_stable_geometry",GpuBuffer.USAGE_VERTEX|GpuBuffer.USAGE_COPY_DST,mesh.vertexBuffer());
            }
        }
        RenderSystem.getSequentialBuffer(topology).requestIndexCount(entry.indexCount);
        return entry;
    }
    void end(){
        var iterator=entries.values().iterator();
        while(iterator.hasNext()){var entry=iterator.next();if(entry.generation!=generation){release(entry);iterator.remove();}}
    }
    private void release(Entry entry){if(entry.buffer!=null)entry.buffer.close();used-=entry.accounted;}
    @Override public void close(){entries.values().forEach(this::release);entries.clear();scratch.close();}
}
