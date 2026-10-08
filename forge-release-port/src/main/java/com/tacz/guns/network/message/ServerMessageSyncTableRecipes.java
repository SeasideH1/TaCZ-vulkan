/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableRecipeCache;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import com.tacz.guns.network.NetworkCodecs;
import com.tacz.guns.network.NetworkContext;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Explicit custom-recipe synchronization; vanilla no longer synchronizes arbitrary recipes. */
public record ServerMessageSyncTableRecipes(List<GunSmithTableRecipe> recipes) {
    public ServerMessageSyncTableRecipes {
        recipes = List.copyOf(recipes);
    }

    public static void encode(ServerMessageSyncTableRecipes message, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(message.recipes.size());
        message.recipes.forEach(recipe -> GunSmithTableSerializer.STREAM_CODEC.encode(buffer, recipe));
    }

    public static ServerMessageSyncTableRecipes decode(RegistryFriendlyByteBuf buffer) {
        int size = NetworkCodecs.readCount(buffer, 100_000);
        List<GunSmithTableRecipe> recipes = new ArrayList<>(Math.min(size, 4096));
        for (int i = 0; i < size; i++) {
            recipes.add(GunSmithTableSerializer.STREAM_CODEC.decode(buffer));
        }
        return new ServerMessageSyncTableRecipes(recipes);
    }

    public static void handle(ServerMessageSyncTableRecipes message, Supplier<NetworkContext> supplier) {
        NetworkContext context = supplier.get();
        if (!context.isServer()) context.enqueueWork(() -> GunSmithTableRecipeCache.replace(message.recipes));
    }
}
