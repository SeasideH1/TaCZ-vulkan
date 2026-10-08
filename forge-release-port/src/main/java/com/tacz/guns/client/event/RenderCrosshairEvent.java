/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import com.tacz.guns.fabric.compat.ClientIntegrationHooks;
import com.tacz.guns.config.client.RenderConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;

/** Native HUD extraction replaces just the vanilla crosshair for a held TACZ gun. */
public final class RenderCrosshairEvent {
    private static final Identifier HIT_ICON=Identifier.fromNamespaceAndPath(GunMod.MOD_ID,"textures/crosshair/hit/hit_marker.png");
    private static final long KEEP_TIME=300;
    private static long hitTimestamp=-1,killTimestamp=-1,headShotTimestamp=-1;
    private static boolean registered;
    private RenderCrosshairEvent() {}
    public static void registerHud() {
        if(registered)return;
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR,original->(graphics,delta)->extractOverlay(graphics,delta,original));
        registered=true;
    }
    private static void extractOverlay(GuiGraphicsExtractor graphics,DeltaTracker delta,HudElement original) {
        Minecraft mc=Minecraft.getInstance();var player=mc.player;
        if(player==null||!IGun.mainHandHoldGun(player)){original.extractRenderState(graphics,delta);return;}
        if(mc.gui.hud.isHidden())return;
        renderHitMarker(graphics);
        if(IGunOperator.fromLivingEntity(player).getSynReloadState().getStateType().isReloading()||mc.gui.screen() instanceof GunRefitScreen)return;
        var stack=player.getMainHandItem();
        IClientPlayerGunOperator operator=IClientPlayerGunOperator.fromLocalPlayer(player);
        TimelessAPI.getGunDisplay(stack).ifPresent(display->{
            if(operator.getClientAimingProgress(delta.getGameTimeDeltaPartialTick(false))>.9&&!display.isShowCrosshair()&&!ClientIntegrationHooks.showCrosshair())return;
            var machine=display.getAnimationStateMachine();
            if(machine==null||machine.getContext()==null||!machine.getContext().shouldHideCrossHair())renderCrosshair(graphics);
        });
    }
    private static void renderCrosshair(GuiGraphicsExtractor graphics) {
        Minecraft mc=Minecraft.getInstance();
        if(!mc.options.getCameraType().isFirstPerson()&&!ClientIntegrationHooks.showCrosshair())return;
        if(mc.gameMode==null||mc.gameMode.getPlayerMode()==GameType.SPECTATOR)return;
        Identifier texture=CrosshairType.getTextureLocation(RenderConfig.CROSSHAIR_TYPE.get());
        graphics.blit(RenderPipelines.GUI_TEXTURED,texture,(int)(graphics.guiWidth()/2f-8),(int)(graphics.guiHeight()/2f-8),0,0,16,16,16,16,0xE5FFFFFF);
    }
    private static void renderHitMarker(GuiGraphicsExtractor graphics) {
        long hit=System.currentTimeMillis()-hitTimestamp,kill=System.currentTimeMillis()-killTimestamp,head=System.currentTimeMillis()-headShotTimestamp;
        float offset=RenderConfig.HIT_MARKET_START_POSITION.get().floatValue(),fade;
        if(kill>KEEP_TIME){if(hit>KEEP_TIME)return;fade=hit;}else{offset+=kill*4f/KEEP_TIME;fade=kill;}
        int alpha=(int)((1-fade/KEEP_TIME)*255);
        int tint=(alpha<<24)|(head>KEEP_TIME?0xFFFFFF:0xFF0000);
        float x=graphics.guiWidth()/2f-8,y=graphics.guiHeight()/2f-8;
        graphics.blit(RenderPipelines.GUI_TEXTURED,HIT_ICON,(int)(x-offset),(int)(y-offset),0,0,8,8,16,16,tint);
        graphics.blit(RenderPipelines.GUI_TEXTURED,HIT_ICON,(int)(x+8+offset),(int)(y-offset),8,0,8,8,16,16,tint);
        graphics.blit(RenderPipelines.GUI_TEXTURED,HIT_ICON,(int)(x-offset),(int)(y+8+offset),0,8,8,8,16,16,tint);
        graphics.blit(RenderPipelines.GUI_TEXTURED,HIT_ICON,(int)(x+8+offset),(int)(y+8+offset),8,8,8,8,16,16,tint);
    }
    public static void markHitTimestamp(){hitTimestamp=System.currentTimeMillis();}
    public static void markKillTimestamp(){killTimestamp=System.currentTimeMillis();}
    public static void markHeadShotTimestamp(){headShotTimestamp=System.currentTimeMillis();}
}
