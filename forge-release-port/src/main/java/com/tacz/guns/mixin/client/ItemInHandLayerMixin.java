/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.renderer.other.HumanoidOffhandRender;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",at=@At("TAIL"))
    private void tacz$holstered(PoseStack pose,SubmitNodeCollector collector,int light,ArmedEntityRenderState state,float yaw,float pitch,CallbackInfo ci){HumanoidOffhandRender.submit(state,pose,collector,light);}
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void tacz$hideGunOffhand(ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci){
        if(state.getMainHandItemStack().getItem() instanceof IGun&&arm==HumanoidArm.LEFT)ci.cancel();
    }
}
