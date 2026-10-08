/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.resource.pojo.AmmoIndexPOJO;
import net.minecraft.util.Mth;

public class CommonAmmoIndex {
    private int stackSize;
    private int sort;
    private AmmoIndexPOJO pojo;

    private CommonAmmoIndex() {
    }

    public static CommonAmmoIndex getInstance(AmmoIndexPOJO ammoIndexPOJO) throws IllegalArgumentException {
        CommonAmmoIndex index = new CommonAmmoIndex();
        index.pojo = ammoIndexPOJO;
        checkIndex(ammoIndexPOJO, index);
        return index;
    }

    private static void checkIndex(AmmoIndexPOJO ammoIndexPOJO, CommonAmmoIndex index) {
        Preconditions.checkArgument(ammoIndexPOJO != null, "index object file is empty");
        index.stackSize = Math.max(ammoIndexPOJO.getStackSize(), 1);
        index.sort = Math.clamp(ammoIndexPOJO.getSort(), 0, 65536);
    }

    public int getStackSize() {
        return stackSize;
    }

    public AmmoIndexPOJO getPojo() {
        return pojo;
    }

    public int getSort() {
        return sort;
    }
}
