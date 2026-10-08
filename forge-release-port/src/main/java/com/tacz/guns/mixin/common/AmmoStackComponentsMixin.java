/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.common;

import com.tacz.guns.item.AmmoItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores the release's per-ammo stack limit before native give/loot/inventory validation. */
@Mixin(ItemStack.class)
public abstract class AmmoStackComponentsMixin {
    @Unique private void tacz$refreshAmmoLimit() {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof AmmoItem ammo) ammo.refreshStackSize(stack);
    }
    @Inject(method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/PatchedDataComponentMap;)V", at = @At("RETURN"))
    private void tacz$onCreated(Holder<Item> item, int count, PatchedDataComponentMap components, CallbackInfo ci) {
        tacz$refreshAmmoLimit();
    }
    @Inject(method = "set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;", at = @At("RETURN"))
    private void tacz$onSet(DataComponentType<?> type, Object value, CallbackInfoReturnable<Object> cir) {
        if (type == DataComponents.CUSTOM_DATA) tacz$refreshAmmoLimit();
    }
    @Inject(method = "set(Lnet/minecraft/core/component/TypedDataComponent;)Ljava/lang/Object;", at = @At("RETURN"))
    private void tacz$onTypedSet(TypedDataComponent<?> value, CallbackInfoReturnable<Object> cir) {
        if (value.type() == DataComponents.CUSTOM_DATA) tacz$refreshAmmoLimit();
    }
    @Inject(method = "remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;", at = @At("RETURN"))
    private void tacz$onRemove(DataComponentType<?> type, CallbackInfoReturnable<Object> cir) {
        if (type == DataComponents.CUSTOM_DATA) tacz$refreshAmmoLimit();
    }
    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V", at = @At("RETURN"))
    private void tacz$onPatch(DataComponentPatch patch, CallbackInfo ci) { tacz$refreshAmmoLimit(); }
    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentMap;)V", at = @At("RETURN"))
    private void tacz$onMap(DataComponentMap components, CallbackInfo ci) { tacz$refreshAmmoLimit(); }
    @Inject(method = "applyComponentsAndValidate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/component/PatchedDataComponentMap;applyPatch(Lnet/minecraft/core/component/DataComponentPatch;)V", shift = At.Shift.AFTER))
    private void tacz$beforePatchValidation(DataComponentPatch patch, CallbackInfo ci) { tacz$refreshAmmoLimit(); }
}
