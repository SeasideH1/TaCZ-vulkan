/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client;
import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.fabric.client.event.ComputeFovModifierEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractClientPlayer.class)
public abstract class NativeFovModifierMixin {
    @Inject(method="getFieldOfViewModifier",at=@At("RETURN"),cancellable=true)
    private void tacz$movementFov(boolean firstPerson,float effectScale,CallbackInfoReturnable<Float> result){
        var event=new ComputeFovModifierEvent(result.getReturnValue());TaczEvents.BUS.post(event);result.setReturnValue(event.getNewFovModifier());
    }
}
