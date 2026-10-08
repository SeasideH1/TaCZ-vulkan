/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.papi;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class AmmoCountPapi implements Function<ItemStack, String> {
    public static final String NAME = "ammo_count";

    @Override
    public String apply(ItemStack stack) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun != null) {
            Identifier gunId = iGun.getGunId(stack);
            ClientGunIndex gunIndex = TimelessAPI.getClientGunIndex(gunId).orElse(null);
            if (gunIndex == null) {
                return "";
            }
            int ammoCount = iGun.getCurrentAmmoCount(stack) + (iGun.hasBulletInBarrel(stack) && gunIndex.getGunData().getBolt() != Bolt.OPEN_BOLT ? 1 : 0);
            return "" + ammoCount;
        }
        return "";
    }
}
