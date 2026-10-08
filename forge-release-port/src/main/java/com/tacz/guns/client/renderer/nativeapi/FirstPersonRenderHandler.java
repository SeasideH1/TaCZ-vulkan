/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
/*
 * Adapted from SimpleBedrockModel 2.2.2 (FirstPersonRenderHandler.java).
 * Original authors: TartaricAcid, MaydayMemory, MoePus, Hidomatn and xjqsh.
 * LGPL-3.0; see META-INF/licenses/simplebedrockmodel-LGPL.txt and
 * THIRD_PARTY_LICENSES.md for exact source provenance and porting scope.
 */
package com.tacz.guns.client.renderer.nativeapi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.fabric.client.event.ClientPlayerNetworkEvent;
import com.tacz.guns.fabric.client.event.TickEvent;
import com.tacz.guns.api.event.SubscribeEvent;

import java.util.Optional;

public class FirstPersonRenderHandler {

    private static int realSelectedSlot = -1;
    private static ItemStack realMainHand = ItemStack.EMPTY;

    private static boolean transitioning = false;

    private static FirstPersonAnimation activeInstance = null;
    private static FirstPersonAnimation previousInstance = null;

    private static ItemStack pendingTarget = ItemStack.EMPTY;

    private static long switchStartTime = 0L;
    private static long currentSheatheDuration = 0L;

    private static boolean lockVanilla = false;
    private static boolean nextIsCustom = false;

    private static boolean forceHandSwapFlag = false;

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // 离开游戏时重置客户端状态
        reset();
    }

    public static void reset() {
        realSelectedSlot = -1;
        realMainHand = ItemStack.EMPTY;
        transitioning = false;
        activeInstance = null;
        previousInstance = null;
        pendingTarget = ItemStack.EMPTY;
        lockVanilla = false;
        nextIsCustom = false;
        forceHandSwapFlag = false;
    }


    public static void forceHandSwap() {
        forceHandSwapFlag = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        int newSlot = player.getInventory().getSelectedSlot();
        ItemStack newMain = player.getMainHandItem();

        boolean slotChanged = newSlot != realSelectedSlot || forceHandSwapFlag;
        boolean itemChanged = !isSameItemStacks(realMainHand, newMain);
        forceHandSwapFlag = false;

        realSelectedSlot = newSlot;
        realMainHand = newMain;

        if (slotChanged) {
            onSlotChanged(newMain);
        } else if (itemChanged) {
            onItemChangedInSameSlot(newMain);
        }if (activeInstance != null) {
            activeInstance.updateItem(newMain);
        }

        tickStates();
    }

    private static void onSlotChanged(ItemStack newStack) {
        pendingTarget = newStack;
        nextIsCustom = isCustomItem(newStack);

        if (transitioning) {
            return;
        }

        boolean oldIsCustom = activeInstance != null;

        previousInstance = activeInstance;
        activeInstance = createInstance(newStack);

        if (oldIsCustom) {
            // Custom → Any：播放 Putaway
            transitioning = true;
            lockVanilla = true;

            switchStartTime = System.currentTimeMillis();
            currentSheatheDuration = calculateSheatheDuration(previousInstance.currentItem());

            previousInstance.triggerPutAway();
        } else {
            // Vanilla → Custom / Vanilla：直接切
            transitioning = false;
            lockVanilla = false;
        }
    }

    private static void onItemChangedInSameSlot(ItemStack newStack) {
        pendingTarget = newStack;
        nextIsCustom = isCustomItem(newStack);

        if (transitioning) {
            return;
        }

        boolean oldIsCustom = activeInstance != null;

        previousInstance = activeInstance;
        activeInstance = createInstance(newStack);

        if (oldIsCustom) {
            transitioning = true;
            lockVanilla = true;

            switchStartTime = System.currentTimeMillis();
            currentSheatheDuration = calculateSheatheDuration(previousInstance.currentItem());

            previousInstance.triggerPutAway();
        } else {
            // Vanilla → Custom
            lockVanilla = false;
        }
    }

    private static void tickStates() {
        if (transitioning) {
            if (getSheatheProgress() >= 1.0f) {
                // Putaway 完成
                transitioning = false;
                lockVanilla = false;

                activeInstance = createInstance(pendingTarget);
                previousInstance = null;
            }
        }
    }

    @SubscribeEvent
    public static void tickAnimation(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        var ani = getActiveAnimationInstance();
        if (ani != null) {
            ani.triggerDraw();
            ani.tick(event.renderTickTime);
        }
    }

    public static boolean shouldLockVanilla() {
        return lockVanilla;
    }


    public static float getTargetHeight() {
        return nextIsCustom ? 1.0F : 0.0F;
    }

    public static FirstPersonAnimation getActiveAnimationInstance() {
        return transitioning ? previousInstance : activeInstance;
    }

    private static FirstPersonAnimation createInstance(ItemStack stack) {
        return getRenderer(stack)
                .map(r -> r.createAnimationInstance(stack, Minecraft.getInstance().getCameraEntity()))
                .orElse(null);
    }

    private static boolean isCustomItem(ItemStack stack) {
        return getRenderer(stack).isPresent();
    }

    private static long calculateSheatheDuration(ItemStack stack) {
        return getRenderer(stack)
                .map(r -> r.getPutAwayDuration(stack))
                .orElse(0L);
    }

    private static float getSheatheProgress() {
        if (currentSheatheDuration <= 0) return 1.0f;
        long elapsed = System.currentTimeMillis() - switchStartTime;
        return Math.min(1.0f, (float) elapsed / currentSheatheDuration);
    }

    public static Optional<FirstPersonItemRenderer> getRenderer(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (NativeItemRenderers.get(stack).orElse(null)
                instanceof FirstPersonItemRenderer renderer) {
            return Optional.of(renderer);
        }
        return Optional.empty();
    }

    private static boolean isSameItemStacks(ItemStack oldStack, ItemStack newStack) {
        if (oldStack == newStack) return true;
        if (oldStack.isEmpty() && newStack.isEmpty()) return true;
        if (oldStack.isEmpty() || newStack.isEmpty()) return false;

        return getRenderer(oldStack)
                .map(r -> r.isSameItem(oldStack, newStack))
                .orElseGet(() -> ItemStack.isSameItem(oldStack, newStack));
    }
}
