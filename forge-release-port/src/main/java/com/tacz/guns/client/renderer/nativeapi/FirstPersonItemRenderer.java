/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
/*
 * Adapted from SimpleBedrockModel 2.2.2 (IFPGeoItemRenderer.java).
 * Original authors: TartaricAcid, MaydayMemory, MoePus, Hidomatn and xjqsh.
 * LGPL-3.0; see META-INF/licenses/simplebedrockmodel-LGPL.txt and
 * THIRD_PARTY_LICENSES.md for exact source provenance and porting scope.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface FirstPersonItemRenderer {

    default boolean isSameItem(ItemStack oldStack, ItemStack newStack) {
        return ItemStack.isSameItem(oldStack, newStack);
    }

    @Nullable
    default FirstPersonAnimation createAnimationInstance(ItemStack stack, Entity entity) {
        return null;
    }

    default long getPutAwayDuration(ItemStack stack) {
        return 0;
    }

    default boolean blockOffhandRender() {
        return false;
    }

    void renderFirstPerson(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, NativeRenderQueue bufferSource,
                                  int light, float partialTick);
}
