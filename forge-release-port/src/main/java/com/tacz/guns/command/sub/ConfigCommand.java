/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

public class ConfigCommand {
    private static final String CONFIG_NAME = "config";
    private static final String KEY = "key";
    private static final String ENABLE = "state";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        var config = Commands.literal(CONFIG_NAME);
        var configKey = Commands.argument(KEY, StringArgumentType.word()).suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.Arrays.stream(ConfigKey.values()).map(Enum::name), builder));
        var state = Commands.argument(ENABLE, BoolArgumentType.bool());
        return config.then(configKey.then(state.executes(ConfigCommand::setConfig)));
    }

    private static int setConfig(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ConfigKey key;
        try { key = ConfigKey.valueOf(StringArgumentType.getString(context, KEY)); }
        catch (IllegalArgumentException exception) {
            throw new DynamicCommandExceptionType(value -> Component.literal("Unknown config key: " + value)).create(StringArgumentType.getString(context, KEY));
        }
        boolean state = BoolArgumentType.getBool(context, ENABLE);

        if (key == null) {
            return 0;
        }
        switch (key) {
            case defaultTableLimit -> SyncConfig.ENABLE_TABLE_FILTER.set(state);
            case serverShootNetworkCheck -> SyncConfig.SERVER_SHOOT_NETWORK_V.set(state);
            case serverShootCooldownCheck -> SyncConfig.SERVER_SHOOT_COOLDOWN_V.set(state);
        }
        com.tacz.guns.config.ServerConfig.init().save();
        com.tacz.guns.config.ServerConfig.refreshDerived();
        com.tacz.guns.network.NetworkHandler.sendToAllPlayers(new com.tacz.guns.network.message.ServerMessageSyncConfig(com.tacz.guns.config.ServerConfig.snapshot()), context.getSource().getServer());
        context.getSource().sendSystemMessage(Component.translatable(key.lang + "." + (state ? "enabled" : "disabled")));

        return Command.SINGLE_SUCCESS;
    }

    public enum ConfigKey {
        defaultTableLimit("commands.tacz.config.default_table_limit"),
        serverShootNetworkCheck("commands.tacz.config.server_shoot_network_check"),
        serverShootCooldownCheck("commands.tacz.config.server_shoot_cooldown_check"),
        ;

        public final String lang;
        ConfigKey(String lang) {
            this.lang = lang;
        }
    }
}
