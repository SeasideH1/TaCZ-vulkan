/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client;
import com.tacz.guns.client.event.PreventsHotbarEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public abstract class NativeHudVisibilityMixin {
    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void tacz$hideWorkbenchHud(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci){if(PreventsHotbarEvent.shouldHideHud())ci.cancel();}
}
