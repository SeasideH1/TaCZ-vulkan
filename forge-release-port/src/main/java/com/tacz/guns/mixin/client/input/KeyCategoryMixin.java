/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reuse the unchanged release language key for the new identifier-based category. */
@Mixin(KeyMapping.Category.class)
public abstract class KeyCategoryMixin {
    @Shadow public abstract Identifier id();
    @Inject(method = "label", at = @At("HEAD"), cancellable = true)
    private void tacz$label(CallbackInfoReturnable<Component> cir) {
        if (id().getNamespace().equals("tacz") && id().getPath().equals("main")) {
            cir.setReturnValue(Component.translatable("key.category.tacz"));
        }
    }
}
