/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;
import com.tacz.guns.api.item.*;
import com.tacz.guns.client.renderer.item.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Loader-independent native renderer registrations, including an explicit extension point for addons. */
public final class NativeItemRenderers {
    private static final Map<Item,NativeItemRenderer> CUSTOM=new IdentityHashMap<>();
    private static final GunItemRendererWrapper GUN=new GunItemRendererWrapper();
    private static final AmmoItemRenderer AMMO=new AmmoItemRenderer();
    private static final AttachmentItemRenderer ATTACHMENT=new AttachmentItemRenderer();
    private static final GunSmithTableItemRenderer TABLE=new GunSmithTableItemRenderer();
    private NativeItemRenderers() {}
    public static void register(Item item,NativeItemRenderer renderer){CUSTOM.put(Objects.requireNonNull(item),Objects.requireNonNull(renderer));}
    public static Optional<NativeItemRenderer> get(ItemStack stack) {
        if(stack.isEmpty())return Optional.empty();
        Item item=stack.getItem();NativeItemRenderer custom=CUSTOM.get(item);if(custom!=null)return Optional.of(custom);
        if(item instanceof IGun)return Optional.of(GUN);
        if(item instanceof IAmmo)return Optional.of(AMMO);
        if(item instanceof IAttachment)return Optional.of(ATTACHMENT);
        if(item instanceof IBlock)return Optional.of(TABLE);
        return Optional.empty();
    }
}
