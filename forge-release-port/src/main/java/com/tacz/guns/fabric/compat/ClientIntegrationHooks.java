/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.compat;

import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Explicit provider boundary; absent providers retain native camera/render/input behavior. */
public final class ClientIntegrationHooks {
    public interface Camera {
        boolean showCrosshair();
        /** True only when the provider applied these signed camera deltas. */
        boolean applyRecoil(float pitchDelta, float yawDelta);
    }
    private record Binding<T>(T provider) { }
    private record Feedback(Object token, BiConsumer<ItemStack, FireMode> callback) { }
    private static final AtomicReference<Binding<Camera>> CAMERA = new AtomicReference<>();
    private static final AtomicReference<Binding<BooleanSupplier>> SHADOW_PASS = new AtomicReference<>();
    private static final CopyOnWriteArrayList<Feedback> FEEDBACK = new CopyOnWriteArrayList<>();
    private ClientIntegrationHooks() { }
    public static AutoCloseable registerCamera(Camera provider) {
        Objects.requireNonNull(provider);
        Binding<Camera> binding = new Binding<>(provider);
        if (!CAMERA.compareAndSet(null, binding)) throw new IllegalStateException("A TACZ camera integration is already registered");
        return () -> CAMERA.compareAndSet(binding, null);
    }
    public static boolean showCrosshair() {
        Binding<Camera> camera = CAMERA.get();
        return camera != null && camera.provider().showCrosshair();
    }
    public static boolean applyCameraRecoil(float pitchDelta, float yawDelta) {
        Binding<Camera> camera = CAMERA.get();
        return camera != null && camera.provider().applyRecoil(pitchDelta, yawDelta);
    }
    public static AutoCloseable registerShadowPass(BooleanSupplier provider) {
        Objects.requireNonNull(provider);
        Binding<BooleanSupplier> binding = new Binding<>(provider);
        if (!SHADOW_PASS.compareAndSet(null, binding)) throw new IllegalStateException("A TACZ shader integration is already registered");
        return () -> SHADOW_PASS.compareAndSet(binding, null);
    }
    public static boolean isShadowPass() {
        Binding<BooleanSupplier> shadow = SHADOW_PASS.get();
        return shadow != null && shadow.provider().getAsBoolean();
    }
    public static AutoCloseable registerShootFeedback(BiConsumer<ItemStack, FireMode> callback) {
        Feedback feedback = new Feedback(new Object(), Objects.requireNonNull(callback));
        FEEDBACK.add(feedback);
        return () -> FEEDBACK.remove(feedback);
    }
    public static void onGunShoot(ItemStack stack, FireMode mode) {
        for (Feedback feedback : FEEDBACK) feedback.callback().accept(stack, mode);
    }
}
