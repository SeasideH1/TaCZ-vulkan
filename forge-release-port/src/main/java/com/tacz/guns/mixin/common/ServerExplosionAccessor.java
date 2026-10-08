/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;

/** Reuses native block/drop/fire behavior after TACZ's custom entity-damage pass. */
@Mixin(ServerExplosion.class)
public interface ServerExplosionAccessor {
    @Invoker("interactWithBlocks") void tacz$interactWithBlocks(List<BlockPos> blocks);
    @Invoker("createFire") void tacz$createFire(List<BlockPos> blocks);
}
