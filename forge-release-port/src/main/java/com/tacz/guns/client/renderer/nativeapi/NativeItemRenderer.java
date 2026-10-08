/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
/** Native item extraction contract. Implementations emit immutable commands, never immediate GPU state. */
public abstract class NativeItemRenderer {
    public abstract void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,
                                      NativeRenderQueue queue,int light,int overlay);
    public final NativeRenderQueue.Frame extract(ItemStack stack,ItemDisplayContext context,int light,int overlay) {
        try(NativeRenderQueue queue=new NativeRenderQueue()) {
            renderByItem(stack,context,new PoseStack(),queue,light,overlay);return queue.snapshot();
        }
    }
}
