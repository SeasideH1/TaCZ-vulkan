/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.tacz.guns.fabric.client.event.InputEvent;
import com.tacz.guns.api.event.SubscribeEvent;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@Environment(EnvType.CLIENT)
public class InteractKey {
    public static final KeyMapping INTERACT_KEY = new KeyMapping("key.tacz.interact.desc",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_O,
            TaczKeyBindings.CATEGORY);

    @SubscribeEvent
    public static void onInteractKeyPress(InputEvent.Key event) {
        if (isInGame() && event.getAction() == InputConstants.PRESS && INTERACT_KEY.matches(event.keyEvent())) {
            doInteractLogic();
        }
    }

    @SubscribeEvent
    public static void onInteractMousePress(InputEvent.MouseButton.Post event) {
        if (isInGame() && event.getAction() == InputConstants.PRESS && INTERACT_KEY.matchesMouse(event.mouseEvent())) {
            doInteractLogic();
        }
    }

    public static boolean onInteractControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            if (!IGun.mainHandHoldGun(player)) {
                return false;
            }
            HitResult hitResult = mc.hitResult;
            if (hitResult == null) {
                return false;
            }
            if (hitResult instanceof BlockHitResult blockHitResult) {
                interactBlock(blockHitResult, player, mc);
                return true;
            }
            if (hitResult instanceof EntityHitResult entityHitResult) {
                interactEntity(entityHitResult, mc);
                return true;
            }
        }
        return false;
    }

    private static void doInteractLogic() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        if (!IGun.mainHandHoldGun(player)) {
            return;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult == null) {
            return;
        }
        if (hitResult instanceof BlockHitResult blockHitResult) {
            interactBlock(blockHitResult, player, mc);
            return;
        }
        if (hitResult instanceof EntityHitResult entityHitResult) {
            interactEntity(entityHitResult, mc);
        }
    }

    private static void interactBlock(BlockHitResult blockHitResult, LocalPlayer player, Minecraft mc) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState block = player.level().getBlockState(blockPos);
        if (InteractKeyConfigRead.canInteractBlock(block)) {
            com.tacz.guns.fabric.client.ClientEventBridge.explicitInteraction(() ->
                    ((com.tacz.guns.mixin.client.input.MinecraftInputAccessor) mc).tacz$startUseItem());
        }
    }

    private static void interactEntity(EntityHitResult entityHitResult, Minecraft mc) {
        Entity entity = entityHitResult.getEntity();
        if (InteractKeyConfigRead.canInteractEntity(entity)) {
            com.tacz.guns.fabric.client.ClientEventBridge.explicitInteraction(() ->
                    ((com.tacz.guns.mixin.client.input.MinecraftInputAccessor) mc).tacz$startUseItem());
        }
    }
}
