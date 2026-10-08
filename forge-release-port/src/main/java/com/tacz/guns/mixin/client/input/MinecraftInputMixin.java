/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client.input;

import com.tacz.guns.fabric.client.ClientEventBridge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftInputMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void tacz$attack(CallbackInfoReturnable<Boolean> cir) {
        if (ClientEventBridge.cancelInteraction(true)) cir.setReturnValue(false);
    }
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void tacz$continuousAttack(boolean held, CallbackInfo ci) {
        if (held && ClientEventBridge.cancelInteraction(true)) ci.cancel();
    }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void tacz$use(CallbackInfo ci) {
        if (ClientEventBridge.cancelInteraction(false)) ci.cancel();
    }
}
