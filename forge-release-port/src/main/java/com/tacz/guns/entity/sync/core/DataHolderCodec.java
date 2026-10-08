/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.entity.sync.core;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Retains ClassKey/DataKey/Value entries and forwards registry-aware ops to value codecs. */
public final class DataHolderCodec implements Codec<DataHolder> {
    public static final DataHolderCodec INSTANCE = new DataHolderCodec();

    private DataHolderCodec() { }

    @Override
    public <T> DataResult<T> encode(DataHolder holder, DynamicOps<T> ops, T prefix) {
        try {
            List<T> entries = new ArrayList<>();
            for (var entry : holder.dataMap.entrySet()) {
                SyncedDataKey<?, ?> key = entry.getKey();
                if (key.save()) {
                    T value = entry.getValue().encodeValue(ops).getOrThrow();
                    entries.add(ops.createMap(Map.of(
                            ops.createString("ClassKey"), ops.createString(key.classKey().id().toString()),
                            ops.createString("DataKey"), ops.createString(key.id().toString()),
                            ops.createString("Value"), value)));
                }
            }
            return ops.mergeToList(prefix, entries);
        } catch (RuntimeException invalid) {
            return DataResult.error(() -> "Cannot save TaCZ synced data: " + invalid.getMessage());
        }
    }

    @Override
    public <T> DataResult<Pair<DataHolder, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getStream(input).flatMap(stream -> {
            try {
                DataHolder holder = new DataHolder();
                for (T raw : stream.toList()) {
                    MapLike<T> fields = ops.getMap(raw).getOrThrow();
                    Identifier classId = Identifier.parse(ops.getStringValue(fields.get("ClassKey")).getOrThrow());
                    Identifier keyId = Identifier.parse(ops.getStringValue(fields.get("DataKey")).getOrThrow());
                    SyncedEntityData data = SyncedEntityData.instance();
                    SyncedClassKey<?> classKey = data.getClassKey(classId);
                    SyncedDataKey<?, ?> key = classKey == null ? null : data.getKey(classKey, keyId);
                    // Removed add-ons and no-longer-saved keys are safely ignored, as in the release.
                    if (key == null || !key.save()) {
                        continue;
                    }
                    DataEntry<?, ?> entry = new DataEntry<>(key);
                    entry.decodeValue(ops, fields.get("Value")).getOrThrow();
                    holder.dataMap.put(key, entry);
                }
                return DataResult.success(Pair.of(holder, ops.empty()));
            } catch (RuntimeException invalid) {
                return DataResult.error(() -> "Cannot load TaCZ synced data: " + invalid.getMessage());
            }
        });
    }
}
