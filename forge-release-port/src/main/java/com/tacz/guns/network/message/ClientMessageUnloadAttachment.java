/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import com.tacz.guns.network.NetworkContext;

import java.util.function.Supplier;

public class ClientMessageUnloadAttachment {
    private final int gunSlotIndex;
    private final AttachmentType attachmentType;

    public ClientMessageUnloadAttachment(int gunSlotIndex, AttachmentType attachmentType) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
    }

    public static void encode(ClientMessageUnloadAttachment message, FriendlyByteBuf buf) {
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
    }

    public static ClientMessageUnloadAttachment decode(FriendlyByteBuf buf) {
        return new ClientMessageUnloadAttachment(buf.readInt(), buf.readEnum(AttachmentType.class));
    }

    public static void handle(ClientMessageUnloadAttachment message, Supplier<NetworkContext> contextSupplier) {
        NetworkContext context = contextSupplier.get();
        if (context.isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                Inventory inventory = player.getInventory();
                if (com.tacz.guns.fabric.inventory.AttachmentRefit.unload(inventory,
                        message.gunSlotIndex, message.attachmentType, inventory::add,
                        gun -> AttachmentPropertyManager.postChangeEvent(player, gun),
                        gun -> ((IGun) gun.getItem()).dropAllAmmo(player, gun))) {
                    player.inventoryMenu.broadcastChanges();
                    NetworkHandler.sendToClientPlayer(new ServerMessageRefreshRefitScreen(), player);
                }
            });
        }
    }

}
