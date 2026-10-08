/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunMeleeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.api.event.LogicalSide;
import com.tacz.guns.network.NetworkContext;

import java.util.function.Supplier;

public class ServerMessageGunMelee {
    private final int shooterId;
    private final ItemStack gunItemStack;

    public ServerMessageGunMelee(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    public static void encode(ServerMessageGunMelee message, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, message.gunItemStack);
    }

    public static ServerMessageGunMelee decode(RegistryFriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        ItemStack gunItemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return new ServerMessageGunMelee(shooterId, gunItemStack);
    }

    public static void handle(ServerMessageGunMelee message, Supplier<NetworkContext> contextSupplier) {
        NetworkContext context = contextSupplier.get();
        if (!context.isServer()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
    }

    @Environment(EnvType.CLIENT)
    private static void doClientEvent(ServerMessageGunMelee message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.shooterId) instanceof LivingEntity shooter) {
            GunMeleeEvent gunMeleeEvent = new GunMeleeEvent(shooter, message.gunItemStack, LogicalSide.CLIENT);
            TaczEvents.BUS.post(gunMeleeEvent);
        }
    }
}
