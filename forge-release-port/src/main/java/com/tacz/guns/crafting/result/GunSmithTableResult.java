/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.crafting.result;

import com.tacz.guns.resource.pojo.data.block.TabConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GunSmithTableResult {
    public static final String GUN = "gun";
    public static final String AMMO = "ammo";
    public static final String ATTACHMENT = "attachment";
    public static final String CUSTOM = "custom";

    private ItemStack result = ItemStack.EMPTY;
    private Identifier group = null;
    private net.minecraft.world.item.ItemStackTemplate template;

    @Nullable
    private RawGunTableResult raw = null;

    public GunSmithTableResult(ItemStack result, @Nullable Identifier group) {
        this.result = result;
        this.group = group==null ? TabConfig.TAB_EMPTY : group;
    }

    public GunSmithTableResult(net.minecraft.world.item.ItemStackTemplate template, @Nullable Identifier group) {
        this.template = java.util.Objects.requireNonNull(template);
        this.group = group == null ? TabConfig.TAB_EMPTY : group;
    }


    public GunSmithTableResult(@NotNull RawGunTableResult raw) {
        this.raw = raw;
    }

    public GunSmithTableResult(@NotNull RawGunTableResult raw, @Nullable Identifier group) {
        this.raw = raw;
        this.group = group==null ? TabConfig.TAB_EMPTY : group;
    }

    public void init() {
        if (template != null) {
            result = template.create();
            template = null;
        }
        if (raw != null) {
            GunSmithTableResult result = RawGunTableResult.init(raw);
            this.result = result.getResult();
            if (group == null || group.equals(TabConfig.TAB_EMPTY)) {
                this.group = result.getGroup();
            }
            this.raw = null;
        }
    }

    public ItemStack getResult() {
        return result;
    }

    public Identifier getGroup() {
        return group;
    }
}
