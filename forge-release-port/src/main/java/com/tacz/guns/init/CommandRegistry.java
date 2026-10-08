/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.command.RootCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class CommandRegistry {
    private CommandRegistry() {}
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> RootCommand.register(dispatcher));
    }
}
