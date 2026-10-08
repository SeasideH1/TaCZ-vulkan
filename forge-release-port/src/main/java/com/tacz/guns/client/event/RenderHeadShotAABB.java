/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.event;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
/** Native per-frame gizmo extraction of the same authored/debug headshot bounds. */
public final class RenderHeadShotAABB {
    private RenderHeadShotAABB() {}
    public static void extract(LivingEntity entity,float partialTick) {
        Minecraft mc=Minecraft.getInstance();
        if(!mc.debugEntries.isCurrentlyEnabled(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES)||!RenderConfig.HEAD_SHOT_DEBUG_HITBOX.get())return;
        AABB box=HeadShotAABBConfigRead.getAABB(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        if(box==null){float width=entity.getBbWidth(),eye=entity.getEyeHeight();box=new AABB(-width/2,eye-.25,-width/2,width/2,eye+.25,width/2).inflate(.01);}
        try(var collection=mc.levelExtractor.collectPerFrameMainThreadGizmos()) {
            Gizmos.cuboid(box.move(entity.getPosition(partialTick)),GizmoStyle.stroke(0xFFFFFF00));
        }
    }
}
