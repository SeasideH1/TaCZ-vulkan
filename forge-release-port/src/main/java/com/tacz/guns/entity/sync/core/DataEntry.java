/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.entity.sync.core;

import net.minecraft.nbt.Tag;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.Validate;

public class DataEntry<E extends Entity, T> {
    private final SyncedDataKey<E, T> key;
    private T value;
    private boolean dirty;

    public DataEntry(SyncedDataKey<E, T> key) {
        this.key = key;
        this.value = key.defaultValueSupplier().get();
    }

    public static DataEntry<?, ?> read(FriendlyByteBuf buffer) {
        SyncedDataKey<?, ?> key = SyncedEntityData.instance().getKey(buffer.readVarInt());
        Validate.notNull(key, "Synced key does not exist for id");
        DataEntry<?, ?> entry = new DataEntry<>(key);
        entry.readValue(buffer);
        return entry;
    }

    public SyncedDataKey<E, T> getKey() {
        return this.key;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value, boolean dirty) {
        this.value = value;
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void clean() {
        this.dirty = false;
    }

    public void write(FriendlyByteBuf buffer) {
        int id = SyncedEntityData.instance().getInternalId(this.key);
        buffer.writeVarInt(id);
        this.key.serializer().write(buffer, this.value);
    }

    public void readValue(FriendlyByteBuf buffer) {
        this.value = this.getKey().serializer().read(buffer);
    }

    public DataEntry<E, T> copy() {
        DataEntry<E, T> copy = new DataEntry<>(key);
        copy.setValue(key.serializer().copy(value), dirty);
        return copy;
    }

    public <N> DataResult<N> encodeValue(DynamicOps<N> ops) {
        return key.serializer().encode(value, ops);
    }

    public <N> DataResult<T> decodeValue(DynamicOps<N> ops, N input) {
        return key.serializer().decode(ops, input).map(decoded -> {
            value = decoded;
            return decoded;
        });
    }

    public Tag writeValue() {
        return this.key.serializer().write(this.value);
    }

    public void readValue(Tag nbt) {
        this.value = this.key.serializer().read(nbt);
    }
}
