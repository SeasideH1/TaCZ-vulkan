/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ChangeGunPropertyEvent {
    @ApiStatus.Internal
    public static void internalOnAttachmentPropertyEvent(AttachmentPropertyEvent event) {
        ItemStack gunItem = event.getGunItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        Identifier gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> event.getCacheProperty().eval(gunItem, gunIndex.getGunData()));
    }
}
