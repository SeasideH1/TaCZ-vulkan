/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client.input;

import com.tacz.guns.fabric.client.ClientEventBridge;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseInputMixin {
    @Inject(method = "onButton", at = @At("RETURN"))
    private void tacz$button(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        ClientEventBridge.mouse(window, button, action);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void tacz$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ClientEventBridge.scroll(window, horizontal, vertical)) ci.cancel();
    }
}
