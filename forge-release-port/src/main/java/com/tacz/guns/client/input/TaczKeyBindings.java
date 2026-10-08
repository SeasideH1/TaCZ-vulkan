/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class TaczKeyBindings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tacz", "main"));
    private TaczKeyBindings() { }
}
