/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.inventory.tooltip;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class GunTooltip implements TooltipComponent {
    private final ItemStack gun;
    private final IGun iGun;
    private final Identifier ammoId;
    private final CommonGunIndex gunIndex;

    public GunTooltip(ItemStack gun, IGun iGun, Identifier ammoId, CommonGunIndex gunIndex) {
        this.gun = gun;
        this.iGun = iGun;
        this.ammoId = ammoId;
        this.gunIndex = gunIndex;
    }

    public ItemStack getGun() {
        return gun;
    }

    public IGun getIGun() {
        return iGun;
    }

    public Identifier getAmmoId() {
        return ammoId;
    }

    public CommonGunIndex getGunIndex() {
        return gunIndex;
    }
}
