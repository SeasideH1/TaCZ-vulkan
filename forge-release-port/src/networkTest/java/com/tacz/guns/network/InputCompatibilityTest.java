package com.tacz.guns.network;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.fabric.client.event.InputEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;

/** Exercises the real target's SDL matching API, not a GLFW compatibility assumption. */
public final class InputCompatibilityTest {
    private static int assertions;

    public static void main(String[] arguments) {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tacz_test", "input"));
        int[] codes = {InputConstants.KEY_H, InputConstants.KEY_R, InputConstants.KEY_O, InputConstants.KEY_G,
                InputConstants.KEY_C, InputConstants.KEY_Z, InputConstants.KEY_V, InputConstants.KEY_T};
        for (int code : codes) {
            KeyMapping mapping = new KeyMapping("tacz_test.key." + code, InputConstants.Type.KEYBOARD, code, category);
            KeyEvent nativeEvent = new KeyEvent(code, 0, 0);
            InputEvent.Key press = new InputEvent.Key(nativeEvent, InputConstants.PRESS);
            check(mapping.matches(press.keyEvent()));
            check(press.getKey() == code);
            check(press.getAction() == InputConstants.PRESS);
            check(!mapping.matches(new KeyEvent(code + 1, 0, 0)));
        }
        for (int button : new int[] {InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT}) {
            KeyMapping mapping = new KeyMapping("tacz_test.mouse." + button, InputConstants.Type.MOUSE, button, category);
            MouseButtonEvent nativeEvent = new MouseButtonEvent(10, 20, new MouseButtonInfo(button, 0));
            InputEvent.MouseButton.Post press = new InputEvent.MouseButton.Post(nativeEvent, InputConstants.PRESS);
            check(mapping.matchesMouse(press.mouseEvent()));
            check(press.getButton() == button);
            check(!mapping.matchesMouse(new MouseButtonEvent(10, 20, new MouseButtonInfo(0, 0))));
        }
        KeyEvent alt = new KeyEvent(InputConstants.KEY_T, 0, InputConstants.MOD_ALT);
        check(alt.hasAltDown());
        check(!new KeyEvent(InputConstants.KEY_T, 0, 0).hasAltDown());
        check(InputConstants.KEY_H != 72); // GLFW H is 72, which is SDL Pause.
        check(InputConstants.MOUSE_BUTTON_LEFT != 0);
        check(InputConstants.REPEAT == -1);
        InputEvent.InteractionKeyMappingTriggered interaction = new InputEvent.InteractionKeyMappingTriggered(true);
        check(interaction.isAttack());
        check(interaction.shouldSwingHand());
        interaction.setSwingHand(false);
        interaction.setCanceled(true);
        check(!interaction.shouldSwingHand() && interaction.isCanceled());
        System.out.println("InputCompatibilityTest: " + assertions + " assertions passed");
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Input compatibility assertion " + (assertions + 1) + " failed");
        assertions++;
    }
}
