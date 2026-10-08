/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.event.RenderLevelBobEvent;
import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.client.renderer.nativeapi.NativeHandRenderer;
import com.tacz.guns.client.renderer.nativeapi.NativeHandItemPass;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import com.tacz.guns.fabric.client.ClientEventBridge;
import com.tacz.guns.fabric.client.event.TickEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exact snapshot-3 extraction/hand-pass hooks; no shader API or world rerender replacement. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique private boolean tacz$renderingHand;
    @Inject(method="extract",at=@At("HEAD"))
    private void tacz$beginExtract(DeltaTracker delta,boolean renderLevel,CallbackInfo ci){ClientEventBridge.renderTick(TickEvent.Phase.START,delta.getGameTimeDeltaPartialTick(false));}
    @Inject(method="extract",at=@At("TAIL"))
    private void tacz$extractHand(DeltaTracker delta,boolean renderLevel,CallbackInfo ci){
        NativeHandRenderer.INSTANCE.extract(delta.getGameTimeDeltaPartialTick(false));ClientEventBridge.renderTick(TickEvent.Phase.END,delta.getGameTimeDeltaPartialTick(false));
    }
    @Inject(method="renderItemInHand",at=@At("HEAD"),cancellable=true)
    private void tacz$renderHand(CameraRenderState camera,PlayerRenderState player,GpuTextureView depth,CallbackInfo ci){
        tacz$renderingHand=true;
        NativeHandItemPass.begin(camera.viewRotationMatrix);
        if(NativeHandRenderer.INSTANCE.hasNativeMainHand())NativeHandRenderer.INSTANCE.render(depth);
        if(NativeHandRenderer.INSTANCE.replacesVanillaHands()) {NativeHandItemPass.clear();tacz$renderingHand=false;ci.cancel();}
    }
    @Inject(method="renderItemInHand",at=@At("RETURN"))
    private void tacz$finishHand(CameraRenderState camera,PlayerRenderState player,GpuTextureView depth,CallbackInfo ci){
        NativeHandRenderer.INSTANCE.renderVanillaItems(depth);tacz$renderingHand=false;
    }
    @Inject(method="bobHurt",at=@At("HEAD"),cancellable=true)
    private void tacz$hurtBob(CameraRenderState camera,PoseStack pose,CallbackInfo ci){
        if(GunHurtBobTweak.onHurtBobTweak(camera,pose)){ci.cancel();return;}
        if(tacz$renderingHand?TaczEvents.BUS.post(new RenderItemInHandBobEvent.BobHurt()):TaczEvents.BUS.post(new RenderLevelBobEvent.BobHurt()))ci.cancel();
    }
    @Inject(method="bobView",at=@At("HEAD"),cancellable=true)
    private void tacz$viewBob(CameraRenderState camera,PoseStack pose,CallbackInfo ci){
        if(tacz$renderingHand?TaczEvents.BUS.post(new RenderItemInHandBobEvent.BobView()):TaczEvents.BUS.post(new RenderLevelBobEvent.BobView()))ci.cancel();
    }
    @Inject(method="onResourceManagerReload",at=@At("HEAD"))
    private void tacz$reload(ResourceManager manager,CallbackInfo ci){NativeHandRenderer.INSTANCE.invalidateResources();}
    @Inject(method="close",at=@At("HEAD"))
    private void tacz$close(CallbackInfo ci){NativeHandRenderer.INSTANCE.close();}
}
