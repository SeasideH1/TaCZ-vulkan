/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message.handshake;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.sync.core.SyncedDataKey;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.*;

public class ServerMessageSyncedEntityDataMapping {
    public static final Marker HANDSHAKE = MarkerManager.getMarker("TACZ_HANDSHAKE");
    private Map<Identifier, List<Pair<Identifier, Integer>>> keyMap;

    public ServerMessageSyncedEntityDataMapping() {
    }

    private ServerMessageSyncedEntityDataMapping(Map<Identifier, List<Pair<Identifier, Integer>>> keyMap) {
        this.keyMap = keyMap;
    }

    public void encode(ServerMessageSyncedEntityDataMapping message, FriendlyByteBuf buffer) {
        Set<SyncedDataKey<?, ?>> keys = SyncedEntityData.instance().getKeys();
        buffer.writeInt(keys.size());
        keys.forEach(key -> {
            int id = SyncedEntityData.instance().getInternalId(key);
            buffer.writeIdentifier(key.classKey().id());
            buffer.writeIdentifier(key.id());
            buffer.writeVarInt(id);
        });
    }

    public ServerMessageSyncedEntityDataMapping decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        if (size < 0 || size > 65_536) {
            throw new IllegalArgumentException("Invalid TaCZ synced-key count: " + size);
        }
        Set<Integer> usedIds = new HashSet<>();
        Set<Pair<Identifier, Identifier>> usedKeys = new HashSet<>();
        Map<Identifier, List<Pair<Identifier, Integer>>> keyMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Identifier classId = buffer.readIdentifier();
            Identifier keyId = buffer.readIdentifier();
            int id = buffer.readVarInt();
            if (id < 0 || !usedIds.add(id) || !usedKeys.add(Pair.of(classId, keyId))) {
                throw new IllegalArgumentException("Duplicate or invalid TaCZ synced key");
            }
            keyMap.computeIfAbsent(classId, c -> new ArrayList<>()).add(Pair.of(keyId, id));
        }
        return new ServerMessageSyncedEntityDataMapping(keyMap);
    }

    /** Must run on the client game thread before acknowledging the login query. */
    public boolean applyMappings() {
        GunMod.LOGGER.debug(HANDSHAKE, "Received synced key mappings from server");
        return SyncedEntityData.instance().updateMappings(this);
    }

    public Map<Identifier, List<Pair<Identifier, Integer>>> getKeyMap() {
        return this.keyMap;
    }
}
