/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client.input;

import com.tacz.guns.fabric.client.WalkDistanceHistory;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WalkDistanceMixin implements WalkDistanceHistory {
    @Unique private float tacz$previousDistance;
    @Inject(method = "tick", at = @At("HEAD"))
    private void tacz$rememberDistance(CallbackInfo ci) {
        tacz$previousDistance = ((Entity) (Object) this).moveDist;
    }
    @Override public float tacz$previousWalkDistance() { return tacz$previousDistance; }
}
