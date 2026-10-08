package com.tacz.guns.fabric.compat;

import com.tacz.guns.api.event.Event;
import com.tacz.guns.api.event.common.KubeJSGunEventPoster;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class IntegrationHookChecks {
    private static int assertions;
    private static void check(boolean condition) { assertions++; if (!condition) throw new AssertionError("Check " + assertions); }
    private static final class PostedEvent extends Event implements KubeJSGunEventPoster<PostedEvent> {
        final String value;
        PostedEvent(String value) { this.value = value; postEventToKubeJS(this); }
        @Override public boolean isCancelable() { return true; }
    }
    public static void main(String[] args) throws Exception {
        check(!ClientIntegrationHooks.showCrosshair());
        check(!ClientIntegrationHooks.applyCameraRecoil(1, -2));
        check(!ClientIntegrationHooks.isShadowPass());
        float[] rotations = new float[2];
        ClientIntegrationHooks.Camera camera = new ClientIntegrationHooks.Camera() {
            public boolean showCrosshair() { return true; }
            public boolean applyRecoil(float pitch, float yaw) { rotations[0] += pitch; rotations[1] += yaw; return true; }
        };
        var cameraHandle = ClientIntegrationHooks.registerCamera(camera);
        check(ClientIntegrationHooks.showCrosshair());
        check(ClientIntegrationHooks.applyCameraRecoil(-3, 2));
        check(rotations[0] == -3 && rotations[1] == 2);
        try { ClientIntegrationHooks.registerCamera(camera); throw new AssertionError("Duplicate camera accepted"); }
        catch (IllegalStateException expected) { assertions++; }
        cameraHandle.close(); check(!ClientIntegrationHooks.showCrosshair());
        var replacement = ClientIntegrationHooks.registerCamera(camera);
        cameraHandle.close(); check(ClientIntegrationHooks.showCrosshair());
        replacement.close(); check(!ClientIntegrationHooks.applyCameraRecoil(3, -2));
        AtomicBoolean shadow = new AtomicBoolean(true);
        var shadowHandle = ClientIntegrationHooks.registerShadowPass(shadow::get);
        check(ClientIntegrationHooks.isShadowPass()); shadow.set(false); check(!ClientIntegrationHooks.isShadowPass());
        shadowHandle.close(); check(!ClientIntegrationHooks.isShadowPass());
        AtomicInteger feedback = new AtomicInteger();
        BiConsumer<ItemStack,FireMode> action = (stack, mode) -> { check(stack == ItemStack.EMPTY && mode == FireMode.SEMI); feedback.incrementAndGet(); };
        var firstFeedback = ClientIntegrationHooks.registerShootFeedback(action);
        var secondFeedback = ClientIntegrationHooks.registerShootFeedback(action);
        ClientIntegrationHooks.onGunShoot(ItemStack.EMPTY, FireMode.SEMI); check(feedback.get() == 2);
        firstFeedback.close(); ClientIntegrationHooks.onGunShoot(ItemStack.EMPTY, FireMode.SEMI); check(feedback.get() == 3);
        firstFeedback.close(); secondFeedback.close(); ClientIntegrationHooks.onGunShoot(ItemStack.EMPTY, FireMode.SEMI); check(feedback.get() == 3);
        List<String> calls = new ArrayList<>();
        Consumer<Event> common = event -> { check(((PostedEvent) event).value.equals("ready")); calls.add("common"); event.setCanceled(true); };
        var common1 = ScriptIntegrationHooks.register(ScriptIntegrationHooks.Channel.COMMON, common);
        var common2 = ScriptIntegrationHooks.register(ScriptIntegrationHooks.Channel.COMMON, common);
        var client = ScriptIntegrationHooks.register(ScriptIntegrationHooks.Channel.CLIENT, event -> calls.add("client"));
        var server = ScriptIntegrationHooks.register(ScriptIntegrationHooks.Channel.SERVER, event -> calls.add("server"));
        var event = new PostedEvent("ready");
        check(event.isCanceled()); check(calls.equals(List.of("common", "common")));
        event.postClientEventToKubeJS(event); event.postServerEventToKubeJS(event);
        check(calls.equals(List.of("common", "common", "client", "server")));
        common1.close(); calls.clear(); new PostedEvent("ready"); check(calls.equals(List.of("common")));
        common1.close(); common2.close(); client.close(); server.close(); calls.clear();
        var plain = new PostedEvent("ready"); check(!plain.isCanceled());
        plain.postClientEventToKubeJS(plain); plain.postServerEventToKubeJS(plain); check(calls.isEmpty());
        System.out.println("PASS " + assertions + " optional-provider dispatch/lifecycle assertions");
    }
}
