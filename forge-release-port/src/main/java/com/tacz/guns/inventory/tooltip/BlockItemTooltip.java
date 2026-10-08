/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.inventory.tooltip;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class BlockItemTooltip implements TooltipComponent {
    private final Identifier blockId;

    public BlockItemTooltip(Identifier blockId) {
        this.blockId = blockId;
    }

    public Identifier getBlockId() {
        return blockId;
    }
}
