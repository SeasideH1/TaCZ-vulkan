/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.nativeapi.NativeHandRenderer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Protect alternate vanilla hand-submission callers from drawing the same native gun twice. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void tacz$nativeHand(float partial,PoseStack pose,SubmitNodeCollector collector,PlayerRenderState player,
                                 FirstPersonHandsAndItemsRenderState hands,CallbackInfo ci){if(NativeHandRenderer.INSTANCE.replacesVanillaHands())ci.cancel();}
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void tacz$nativeMainHand(PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,
            InteractionHand hand,float attack,ItemStack stack,float height,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci) {
        if(NativeHandRenderer.INSTANCE.hasNativeMainHand()&&(hand==InteractionHand.MAIN_HAND||NativeHandRenderer.INSTANCE.replacesVanillaHands()))ci.cancel();
    }
}

