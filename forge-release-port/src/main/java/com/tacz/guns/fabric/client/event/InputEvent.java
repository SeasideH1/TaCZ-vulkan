/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.event;

import com.tacz.guns.api.event.Cancelable;
import com.tacz.guns.api.event.Event;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.InteractionHand;

/** The target's native SDL input data, without translating back into GLFW key codes. */
public abstract class InputEvent extends Event {
    public static final class Key extends InputEvent {
        private final KeyEvent event;
        private final int action;
        public Key(KeyEvent event, int action) { this.event = event; this.action = action; }
        public KeyEvent keyEvent() { return event; }
        public int getKey() { return event.key(); }
        public int getScanCode() { return event.key(); }
        public int getAction() { return action; }
        public int getModifiers() { return event.modifiers(); }
    }

    public abstract static class MouseButton extends InputEvent {
        private final MouseButtonEvent event;
        private final int action;
        protected MouseButton(MouseButtonEvent event, int action) { this.event = event; this.action = action; }
        public MouseButtonEvent mouseEvent() { return event; }
        public int getButton() { return event.button(); }
        public int getAction() { return action; }
        public int getModifiers() { return event.modifiers(); }
        public static final class Post extends MouseButton {
            public Post(MouseButtonEvent event, int action) { super(event, action); }
        }
    }

    @Cancelable
    public static final class MouseScrollingEvent extends InputEvent {
        private final double horizontal, vertical;
        public MouseScrollingEvent(double horizontal, double vertical) { this.horizontal = horizontal; this.vertical = vertical; }
        public double getScrollDelta() { return vertical; }
        public double getScrollDeltaX() { return horizontal; }
        public double getScrollDeltaY() { return vertical; }
    }

    @Cancelable
    public static final class InteractionKeyMappingTriggered extends InputEvent {
        private final boolean attack;
        private boolean swingHand = true;
        public InteractionKeyMappingTriggered(boolean attack) { this.attack = attack; }
        public boolean isAttack() { return attack; }
        public boolean isUseItem() { return !attack; }
        public InteractionHand getHand() { return InteractionHand.MAIN_HAND; }
        public boolean shouldSwingHand() { return swingHand; }
        public void setSwingHand(boolean swingHand) { this.swingHand = swingHand; }
    }
}
