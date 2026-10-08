/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.init;

import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.client.renderer.block.StatueRenderer;
import com.tacz.guns.client.renderer.block.TargetRenderer;
import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.client.renderer.entity.TargetMinecartRenderer;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.TargetMinecart;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.EnvType;

public class ModEntitiesRender {
    public static void init() {
        EntityRendererRegistry.register(EntityKineticBullet.TYPE, EntityBulletRenderer::new);
        EntityRendererRegistry.register(TargetMinecart.TYPE, TargetMinecartRenderer::new);
        BlockEntityRendererRegistry.register(GunSmithTableBlockEntity.TYPE, GunSmithTableRenderer::new);
        BlockEntityRendererRegistry.register(TargetBlockEntity.TYPE, TargetRenderer::new);
        BlockEntityRendererRegistry.register(StatueBlockEntity.TYPE, StatueRenderer::new);
    }
}
