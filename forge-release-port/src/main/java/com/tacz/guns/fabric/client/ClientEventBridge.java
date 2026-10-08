/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client;

import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.fabric.client.event.ClientPlayerNetworkEvent;
import com.tacz.guns.fabric.client.event.InputEvent;
import com.tacz.guns.fabric.client.event.TickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;

/** Real native callbacks for the retained TACZ event-driven input/gameplay code. */
public final class ClientEventBridge {
    private static LocalPlayer previousPlayer;
    private static boolean initialized;
    private static boolean explicitInteraction;

    private ClientEventBridge() { }

    public static void init() {
        if (initialized) return;
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != previousPlayer) {
                if (previousPlayer != null && client.player != null && client.getConnection() != null) {
                    TaczEvents.BUS.post(new ClientPlayerNetworkEvent.Clone(previousPlayer, client.player, client.getConnection().getConnection()));
                }
                previousPlayer = client.player;
            }
            ClientTickDispatch.start(client.isPaused(),
                    () -> TaczEvents.BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)),
                    () -> { if (client.player != null) TaczEvents.BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, client.player)); });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientTickDispatch.end(client.isPaused(),
                    () -> TaczEvents.BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END)),
                    () -> { if (client.player != null) TaczEvents.BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, client.player)); });
        });
        // INIT precedes the play data sync: clearing caches at JOIN could erase received gun packs.
        ClientPlayConnectionEvents.INIT.register((handler, client) ->
                TaczEvents.BUS.post(new ClientPlayerNetworkEvent.LoggingIn(client.player, handler.getConnection())));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                TaczEvents.BUS.post(new ClientPlayerNetworkEvent.LoggedIn(client.player, handler.getConnection())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TaczEvents.BUS.post(new ClientPlayerNetworkEvent.LoggingOut(client.player, handler.getConnection()));
            previousPlayer = null;
        });
        initialized = true;
    }

    public static void key(long window, int action, KeyEvent event) {
        if (isCurrentWindow(window)) TaczEvents.BUS.post(new InputEvent.Key(event, action));
    }

    public static void mouse(long window, MouseButtonInfo info, int action) {
        if (isCurrentWindow(window)) {
            var mouse = Minecraft.getInstance().mouseHandler;
            TaczEvents.BUS.post(new InputEvent.MouseButton.Post(new MouseButtonEvent(mouse.xpos(), mouse.ypos(), info), action));
        }
    }

    public static boolean scroll(long window, double horizontal, double vertical) {
        return isCurrentWindow(window) && TaczEvents.BUS.post(new InputEvent.MouseScrollingEvent(horizontal, vertical));
    }

    public static boolean cancelInteraction(boolean attack) {
        return !explicitInteraction && TaczEvents.BUS.post(new InputEvent.InteractionKeyMappingTriggered(attack));
    }

    public static void explicitInteraction(Runnable action) {
        boolean previous = explicitInteraction;
        explicitInteraction = true;
        try { action.run(); }
        finally { explicitInteraction = previous; }
    }

    public static void renderTick(TickEvent.Phase phase, float partialTick) {
        TaczEvents.BUS.post(new TickEvent.RenderTickEvent(phase, partialTick));
    }

    private static boolean isCurrentWindow(long window) {
        return window != 0 && window == Minecraft.getInstance().getWindow().handle();
    }
}
