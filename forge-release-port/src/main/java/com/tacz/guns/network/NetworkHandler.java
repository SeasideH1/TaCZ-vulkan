/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network;

import com.tacz.guns.network.message.*;
import com.tacz.guns.network.message.event.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Fabric transport for the official release's gameplay messages. */
public final class NetworkHandler {
    public static final PayloadChannel CHANNEL = new PayloadChannel("tacz");
    private static final Set<MinecraftServer> SERVERS = Collections.synchronizedSet(
            Collections.newSetFromMap(new IdentityHashMap<>()));
    private static boolean initialized;

    private NetworkHandler() { }

    /** Registers codecs on both physical sides, and server receivers only. */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        CHANNEL.registerServerbound("client_message_player_shoot", ClientMessagePlayerShoot.class, ClientMessagePlayerShoot::encode, ClientMessagePlayerShoot::decode, ClientMessagePlayerShoot::handle);
        CHANNEL.registerServerbound("client_message_player_reload_gun", ClientMessagePlayerReloadGun.class, ClientMessagePlayerReloadGun::encode, ClientMessagePlayerReloadGun::decode, ClientMessagePlayerReloadGun::handle);
        CHANNEL.registerServerbound("client_message_player_cancel_reload", ClientMessagePlayerCancelReload.class, ClientMessagePlayerCancelReload::encode, ClientMessagePlayerCancelReload::decode, ClientMessagePlayerCancelReload::handle);
        CHANNEL.registerServerbound("client_message_player_fire_select", ClientMessagePlayerFireSelect.class, ClientMessagePlayerFireSelect::encode, ClientMessagePlayerFireSelect::decode, ClientMessagePlayerFireSelect::handle);
        CHANNEL.registerServerbound("client_message_player_aim", ClientMessagePlayerAim.class, ClientMessagePlayerAim::encode, ClientMessagePlayerAim::decode, ClientMessagePlayerAim::handle);
        CHANNEL.registerServerbound("client_message_player_crawl", ClientMessagePlayerCrawl.class, ClientMessagePlayerCrawl::encode, ClientMessagePlayerCrawl::decode, ClientMessagePlayerCrawl::handle);
        CHANNEL.registerServerbound("client_message_player_draw_gun", ClientMessagePlayerDrawGun.class, ClientMessagePlayerDrawGun::encode, ClientMessagePlayerDrawGun::decode, ClientMessagePlayerDrawGun::handle);
        CHANNEL.registerClientbound("server_message_sound", ServerMessageSound.class, ServerMessageSound::encode, ServerMessageSound::decode, ServerMessageSound::handle);
        CHANNEL.registerServerbound("client_message_craft", ClientMessageCraft.class, ClientMessageCraft::encode, ClientMessageCraft::decode, ClientMessageCraft::handle);
        CHANNEL.registerClientbound("server_message_craft", ServerMessageCraft.class, ServerMessageCraft::encode, ServerMessageCraft::decode, ServerMessageCraft::handle);
        CHANNEL.registerServerbound("client_message_player_zoom", ClientMessagePlayerZoom.class, ClientMessagePlayerZoom::encode, ClientMessagePlayerZoom::decode, ClientMessagePlayerZoom::handle);
        CHANNEL.registerServerbound("client_message_refit_gun", ClientMessageRefitGun.class, ClientMessageRefitGun::encode, ClientMessageRefitGun::decode, ClientMessageRefitGun::handle);
        CHANNEL.registerClientbound("server_message_refresh_refit_screen", ServerMessageRefreshRefitScreen.class, ServerMessageRefreshRefitScreen::encode, ServerMessageRefreshRefitScreen::decode, ServerMessageRefreshRefitScreen::handle);
        CHANNEL.registerServerbound("client_message_unload_attachment", ClientMessageUnloadAttachment.class, ClientMessageUnloadAttachment::encode, ClientMessageUnloadAttachment::decode, ClientMessageUnloadAttachment::handle);
        CHANNEL.registerClientbound("server_message_swap_item", ServerMessageSwapItem.class, ServerMessageSwapItem::encode, ServerMessageSwapItem::decode, ServerMessageSwapItem::handle);
        CHANNEL.registerServerbound("client_message_player_bolt_gun", ClientMessagePlayerBoltGun.class, ClientMessagePlayerBoltGun::encode, ClientMessagePlayerBoltGun::decode, ClientMessagePlayerBoltGun::handle);
        CHANNEL.registerClientbound("server_message_level_up", ServerMessageLevelUp.class, ServerMessageLevelUp::encode, ServerMessageLevelUp::decode, ServerMessageLevelUp::handle);
        CHANNEL.registerClientbound("server_message_gun_hurt", ServerMessageGunHurt.class, ServerMessageGunHurt::encode, ServerMessageGunHurt::decode, ServerMessageGunHurt::handle);
        CHANNEL.registerClientbound("server_message_gun_kill", ServerMessageGunKill.class, ServerMessageGunKill::encode, ServerMessageGunKill::decode, ServerMessageGunKill::handle);
        CHANNEL.registerClientbound("server_message_update_entity_data", ServerMessageUpdateEntityData.class, ServerMessageUpdateEntityData::encode, ServerMessageUpdateEntityData::decode, ServerMessageUpdateEntityData::handle);
        CHANNEL.registerClientbound("server_message_sync_gun_pack", ServerMessageSyncGunPack.class, ServerMessageSyncGunPack::encode, ServerMessageSyncGunPack::decode, ServerMessageSyncGunPack::handle, 64 * 1024 * 1024);
        CHANNEL.registerServerbound("client_message_player_melee", ClientMessagePlayerMelee.class, ClientMessagePlayerMelee::encode, ClientMessagePlayerMelee::decode, ClientMessagePlayerMelee::handle);
        CHANNEL.registerClientbound("server_message_gun_draw", ServerMessageGunDraw.class, ServerMessageGunDraw::encode, ServerMessageGunDraw::decode, ServerMessageGunDraw::handle);
        CHANNEL.registerClientbound("server_message_gun_fire", ServerMessageGunFire.class, ServerMessageGunFire::encode, ServerMessageGunFire::decode, ServerMessageGunFire::handle);
        CHANNEL.registerClientbound("server_message_gun_fire_select", ServerMessageGunFireSelect.class, ServerMessageGunFireSelect::encode, ServerMessageGunFireSelect::decode, ServerMessageGunFireSelect::handle);
        CHANNEL.registerClientbound("server_message_gun_melee", ServerMessageGunMelee.class, ServerMessageGunMelee::encode, ServerMessageGunMelee::decode, ServerMessageGunMelee::handle);
        CHANNEL.registerClientbound("server_message_gun_reload", ServerMessageGunReload.class, ServerMessageGunReload::encode, ServerMessageGunReload::decode, ServerMessageGunReload::handle);
        CHANNEL.registerClientbound("server_message_gun_shoot", ServerMessageGunShoot.class, ServerMessageGunShoot::encode, ServerMessageGunShoot::decode, ServerMessageGunShoot::handle);
        CHANNEL.registerClientbound("server_message_sync_base_timestamp", ServerMessageSyncBaseTimestamp.class, ServerMessageSyncBaseTimestamp::encode, ServerMessageSyncBaseTimestamp::decode, ServerMessageSyncBaseTimestamp::handle);
        CHANNEL.registerServerbound("client_message_sync_base_timestamp", ClientMessageSyncBaseTimestamp.class, ClientMessageSyncBaseTimestamp::encode, ClientMessageSyncBaseTimestamp::decode, ClientMessageSyncBaseTimestamp::handle);
        CHANNEL.registerServerbound("client_message_laser_color", ClientMessageLaserColor.class, ClientMessageLaserColor::encode, ClientMessageLaserColor::decode, ClientMessageLaserColor::handle);
        CHANNEL.registerClientbound("server_message_sync_config", ServerMessageSyncConfig.class, ServerMessageSyncConfig::encode, ServerMessageSyncConfig::decode, ServerMessageSyncConfig::handle);
        CHANNEL.registerClientbound("server_message_sync_table_recipes", ServerMessageSyncTableRecipes.class, ServerMessageSyncTableRecipes::encode, ServerMessageSyncTableRecipes::decode, ServerMessageSyncTableRecipes::handle, 64 * 1024 * 1024);
        CHANNEL.registerClientbound("server_message_bullet_spawn", ServerMessageBulletSpawn.class, ServerMessageBulletSpawn::encode, ServerMessageBulletSpawn::decode, ServerMessageBulletSpawn::handle);
        HandshakeNetworking.init();
        ServerLifecycleEvents.SERVER_STARTED.register(SERVERS::add);
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        initialized = true;
    }

    /** A common payload packet accepts the game listener, which extends the common listener. */
    @SuppressWarnings("unchecked")
    public static net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>
            createClientboundPacket(Object message) {
        return (net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>)
                (net.minecraft.network.protocol.Packet<?>) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
                        .createClientboundPacket(CHANNEL.wrap(message, false));
    }

    public static void sendToClientPlayer(Object message, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            throw new IllegalArgumentException("Clientbound TACZ packets require a server player");
        }
        CHANNEL.sendToPlayer(message, serverPlayer);
    }

    public static void sendToTrackingEntityAndSelf(Entity centerEntity, Object message) {
        Set<ServerPlayer> recipients = Collections.newSetFromMap(new IdentityHashMap<>());
        recipients.addAll(PlayerLookup.tracking(centerEntity));
        if (centerEntity instanceof ServerPlayer player) {
            recipients.add(player);
        }
        recipients.forEach(player -> CHANNEL.sendToPlayer(message, player));
    }

    public static void sendToAllPlayers(Object message) {
        synchronized (SERVERS) {
            SERVERS.forEach(server -> sendToAllPlayers(message, server));
        }
    }

    public static void sendToAllPlayers(Object message, MinecraftServer server) {
        PlayerLookup.all(server).forEach(player -> CHANNEL.sendToPlayer(message, player));
    }

    public static void sendToTrackingEntity(Object message, Entity centerEntity) {
        PlayerLookup.tracking(centerEntity).forEach(player -> CHANNEL.sendToPlayer(message, player));
    }

    public static void sendToDimension(Object message, Entity centerEntity) {
        if (centerEntity.level() instanceof ServerLevel level) {
            PlayerLookup.level(level).forEach(player -> CHANNEL.sendToPlayer(message, player));
        }
    }
}
