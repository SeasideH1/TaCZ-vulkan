/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.*;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.event.TaczEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;

/** Production first-person bridge: authored animation/model extraction separated from native GPU execution. */
public final class NativeHandRenderer implements KeepingItemRenderer,AutoCloseable {
    public static final NativeHandRenderer INSTANCE=new NativeHandRenderer();
    private record Snapshot(NativeRenderQueue.Frame frame,float fov,boolean blocksOffhand,Matrix3fc normalToWorld) {}
    private volatile Snapshot snapshot;
    private float handFov=70;
    private ItemStack kept=ItemStack.EMPTY;private long keepUntil;
    private NativeFrameExecutor executor;
    private NativeHandRenderer() {}
    public void setHandFov(float fov){handFov=fov;}
    public boolean hasNativeMainHand(){return snapshot!=null;}
    public boolean replacesVanillaHands(){Snapshot frame=snapshot;return frame!=null&&frame.blocksOffhand();}
    public void extract(float partialTick) {
        Minecraft mc=Minecraft.getInstance();var player=mc.player;snapshot=null;
        if(player==null||mc.level==null||!mc.options.getCameraType().isFirstPerson()||player.isSpectator()||player.isSleeping()||mc.gui.hud.isHidden()||mc.gameRenderer.mainCamera().isPanoramicMode())return;
        var animation=FirstPersonRenderHandler.getActiveAnimationInstance();
        ItemStack stack=animation==null?getCurrentItem():animation.currentItem();
        var renderer=FirstPersonRenderHandler.getRenderer(stack);if(renderer.isEmpty())return;
        // Resource reload replaces GunDisplay/state-machine objects without changing this slot.
        // An already-drawn animation instance must initialize the new machine, but never revive
        // a sheathing/kept item that no longer matches the actual main hand.
        if(renderer.get() instanceof com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer<?,?> animated
                &&ItemStack.matches(player.getMainHandItem(),stack)&&animated.needReInit(stack)) {
            animated.tryInit(stack,player,partialTick);
        }
        try(NativeRenderQueue queue=new NativeRenderQueue()) {
            PoseStack pose=new PoseStack();
            var renderState=mc.gameRenderer.gameRenderState();
            NativeHandPose.apply(renderState.levelRenderState.cameraRenderState,renderState.optionsRenderState,pose);
            TaczEvents.BUS.post(new BeforeRenderHandEvent(pose));
            // The Forge hand event ran after vanilla's positive X/Y view-lag rotations.
            // TACZ's authored renderer reverses those rotations, so retain both sides in order.
            var hands=renderState.levelRenderState.playerRenderState.firstPersonHandsAndItems;
            if(hands!=null) {
                pose.rotateDegrees(com.mojang.math.Axis.XP,(hands.viewXRot-hands.xBob)*.1f);
                pose.rotateDegrees(com.mojang.math.Axis.YP,(hands.viewYRot-hands.yBob)*.1f);
            }
            int light=LightCoordsUtil.getLightCoords(player.level(),player.blockPosition());
            renderer.get().renderFirstPerson(player,stack,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,pose,queue,light,partialTick);
            // Native entity shaders light normals in world space. Keep camera-space positions
            // for the exact optical aperture, but rotate normals like vanilla's inverse-view pose.
            Matrix3fc normalToWorld=new Matrix3f(renderState.levelRenderState.cameraRenderState.viewRotationMatrix).transpose();
            snapshot=new Snapshot(queue.snapshot(),handFov,renderer.get().blockOffhandRender(),normalToWorld);
        }
    }
    /** Called at the engine's real hand pass with its freshly cleared depth attachment. */
    public void render(GpuTextureView handDepth) {
        Snapshot frame=snapshot;if(frame==null||frame.frame().commands().isEmpty())return;
        RenderSystem.assertOnRenderThread();
        if(executor==null)executor=new NativeFrameExecutor();
        var target=Minecraft.getInstance().gameRenderer.mainRenderTarget();
        var modelView=RenderSystem.getModelViewStack();modelView.pushMatrix();modelView.identity();
        try {executor.render(frame.frame(),target,RenderSystem.getProjectionMatrixBuffer(),handDepth,frame.normalToWorld());}
        finally {modelView.popMatrix();}
    }
    /** Loose optical attachments keep vanilla hand motion but require the same mask executor. */
    public void renderVanillaItems(GpuTextureView handDepth) {
        var frames=NativeHandItemPass.finish();if(frames.isEmpty())return;
        RenderSystem.assertOnRenderThread();
        if(executor==null)executor=new NativeFrameExecutor();
        var target=Minecraft.getInstance().gameRenderer.mainRenderTarget();
        var modelView=RenderSystem.getModelViewStack();modelView.pushMatrix();modelView.identity();
        try {
            // Vanilla's feature pass is closed; its hand depth remains attached and contains
            // both arms and ordinary items. Each captured optical item retains its own mask.
            for(var frame:frames)executor.render(frame,target,RenderSystem.getProjectionMatrixBuffer(),handDepth,new Matrix3f());
        } finally {modelView.popMatrix();}
    }
    public void invalidateResources(){snapshot=null;NativeHandItemPass.clear();if(executor!=null){executor.close();executor=null;}}
    @Override public void close(){invalidateResources();snapshot=null;}
    @Override public void keep(ItemStack stack,long milliseconds){if(System.currentTimeMillis()<keepUntil)return;kept=stack;keepUntil=System.currentTimeMillis()+milliseconds;}
    @Override public ItemStack getCurrentItem(){
        if(System.currentTimeMillis()<keepUntil)return kept;
        var animation=FirstPersonRenderHandler.getActiveAnimationInstance();if(animation!=null)return animation.currentItem();
        var player=Minecraft.getInstance().player;return player==null?ItemStack.EMPTY:player.getMainHandItem();
    }
}
