/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import java.util.concurrent.CompletableFuture;

public final class ReloadCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("reload").executes(ReloadCommand::reloadAllPack);
    }
    private static int reloadAllPack(CommandContext<CommandSourceStack> context) {
        long started = System.nanoTime();
        CompletableFuture<Void> reload = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? reloadClient() : CommonAssetsManager.reloadAllPack();
        reload.whenComplete((ignored, failure) -> context.getSource().getServer().execute(() -> {
            if (failure == null) context.getSource().sendSystemMessage(Component.translatable("commands.tacz.reload.success", (System.nanoTime() - started) / 1_000_000.0));
            else {
                com.tacz.guns.GunMod.LOGGER.error("Gunpack reload failed", failure);
                context.getSource().sendFailure(Component.literal("Gunpack reload failed: " + failure.getMessage()));
            }
        }));
        return Command.SINGLE_SUCCESS;
    }
    @Environment(EnvType.CLIENT)
    public static CompletableFuture<Void> reloadClient() { return ClientAssetsManager.reloadAllPack(); }
}
