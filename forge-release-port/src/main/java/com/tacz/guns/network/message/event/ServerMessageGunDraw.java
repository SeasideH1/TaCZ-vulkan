/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunDrawEvent;
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

public class ServerMessageGunDraw {
    private final int entityId;
    private final ItemStack previousGunItem;
    private final ItemStack currentGunItem;

    public ServerMessageGunDraw(int entityId, ItemStack previousGunItem, ItemStack currentGunItem) {
        this.entityId = entityId;
        this.previousGunItem = previousGunItem;
        this.currentGunItem = currentGunItem;
    }

    public static void encode(ServerMessageGunDraw message, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, message.previousGunItem);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, message.currentGunItem);
    }

    public static ServerMessageGunDraw decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ItemStack previousGunItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack currentGunItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return new ServerMessageGunDraw(entityId, previousGunItem, currentGunItem);
    }

    public static void handle(ServerMessageGunDraw message, Supplier<NetworkContext> contextSupplier) {
        NetworkContext context = contextSupplier.get();
        if (!context.isServer()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
    }

    @Environment(EnvType.CLIENT)
    private static void doClientEvent(ServerMessageGunDraw message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.entityId) instanceof LivingEntity livingEntity) {
            GunDrawEvent gunDrawEvent = new GunDrawEvent(livingEntity, message.previousGunItem, message.currentGunItem, LogicalSide.CLIENT);
            TaczEvents.BUS.post(gunDrawEvent);
        }
    }
}
