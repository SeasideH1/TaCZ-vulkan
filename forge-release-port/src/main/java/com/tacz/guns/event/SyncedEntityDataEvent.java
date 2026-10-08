/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.entity.sync.core.*;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageUpdateEntityData;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

/** Fabric lifecycle bridge retaining tracking/self/death-persistence semantics. */
public final class SyncedEntityDataEvent {
    private static boolean initialized;

    private SyncedEntityDataEvent() { }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        DataHolderCapabilityProvider.init();
        EntityTrackingEvents.START_TRACKING.register(SyncedEntityDataEvent::onStartTracking);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ServerPlayer player) {
                onPlayerJoinWorld(player);
            }
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> onPlayerClone(oldPlayer, newPlayer, !alive));
        // Fabric's own attachment transfer runs in AFTER_RESPAWN's default phase and
        // copies object references for an alive clone. Reapply our deep copy afterwards.
        Identifier afterAttachments = Identifier.fromNamespaceAndPath("tacz", "after_attachment_transfer");
        ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, afterAttachments);
        ServerPlayerEvents.AFTER_RESPAWN.register(afterAttachments, (original, player, alive) -> {
            onPlayerClone(original, player, !alive);
            onPlayerJoinWorld(player);
        });
        ServerTickEvents.END_SERVER_TICK.register(SyncedEntityDataEvent::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SyncedEntityData data = SyncedEntityData.instance();
            data.getDirtyEntities().removeIf(entity -> entity.level().getServer() == server);
            data.setDirty(!data.getDirtyEntities().isEmpty());
        });
        initialized = true;
    }

    public static void onStartTracking(Entity entity, ServerPlayer observer) {
        DataHolder holder = SyncedEntityData.instance().getDataHolder(entity);
        if (holder != null) {
            List<DataEntry<?, ?>> entries = holder.gatherAll();
            entries.removeIf(entry -> !entry.getKey().syncMode().isTracking());
            if (!entries.isEmpty()) {
                NetworkHandler.sendToClientPlayer(new ServerMessageUpdateEntityData(entity.getId(), entries), observer);
            }
        }
    }

    public static void onPlayerJoinWorld(ServerPlayer player) {
        DataHolder holder = SyncedEntityData.instance().getDataHolder(player);
        if (holder != null) {
            List<DataEntry<?, ?>> entries = holder.gatherAll();
            if (!entries.isEmpty()) {
                NetworkHandler.sendToClientPlayer(new ServerMessageUpdateEntityData(player.getId(), entries), player);
            }
        }
    }

    public static void onPlayerClone(ServerPlayer original, ServerPlayer player, boolean wasDeath) {
        DataHolder oldHolder = SyncedEntityData.instance().getDataHolder(original);
        if (oldHolder != null && SyncedEntityData.instance().hasSyncedDataKey(player)) {
            // Mutable reload states, NBT and item stacks must not alias the discarded player.
            DataHolderCapabilityProvider.set(player, oldHolder.copy(wasDeath));
        }
    }

    public static void onServerTick(MinecraftServer server) {
        SyncedEntityData data = SyncedEntityData.instance();
        if (!data.isDirty()) {
            return;
        }
        var dirty = data.getDirtyEntities().iterator();
        while (dirty.hasNext()) {
            Entity entity = dirty.next();
            if (entity.level().getServer() != server) {
                continue;
            }
            dirty.remove();
            DataHolder holder = data.getDataHolder(entity);
            if (entity.isRemoved() || holder == null || !holder.isDirty()) {
                continue;
            }
            List<DataEntry<?, ?>> entries = holder.gatherDirty();
            List<DataEntry<?, ?>> selfEntries = entries.stream().filter(entry -> entry.getKey().syncMode().isSelf()).toList();
            if (!selfEntries.isEmpty() && entity instanceof ServerPlayer player) {
                NetworkHandler.sendToClientPlayer(new ServerMessageUpdateEntityData(entity.getId(), selfEntries), player);
            }
            List<DataEntry<?, ?>> trackingEntries = entries.stream().filter(entry -> entry.getKey().syncMode().isTracking()).toList();
            if (!trackingEntries.isEmpty()) {
                NetworkHandler.sendToTrackingEntity(new ServerMessageUpdateEntityData(entity.getId(), trackingEntries), entity);
            }
            holder.clean();
        }
        data.setDirty(!data.getDirtyEntities().isEmpty());
    }
}
