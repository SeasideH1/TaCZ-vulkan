/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client;

import com.tacz.guns.client.renderer.nativeapi.NativeThirdPersonPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures pose callbacks only after the complete subclass state has been extracted. */
@Mixin(EntityRenderer.class)
public abstract class NativeEntityExtractionMixin {
    @Inject(method="createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",at=@At("RETURN"))
    private void tacz$pose(Entity entity,float partial,CallbackInfoReturnable<EntityRenderState> cir) {
        if(entity instanceof LivingEntity living&&cir.getReturnValue() instanceof LivingEntityRenderState state
                &&(Object)this instanceof LivingEntityRenderer<?,?,?> renderer) {
            NativeThirdPersonPose.capture(living,state,partial,renderer.getModel() instanceof HumanoidModel<?> model?model:null);
        }
    }
}
