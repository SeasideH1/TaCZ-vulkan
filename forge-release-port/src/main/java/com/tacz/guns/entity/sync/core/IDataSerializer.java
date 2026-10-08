/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.entity.sync.core;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Author: MrCrayfish.
 * Open source at <a href="https://github.com/MrCrayfish/Framework">Github</a> under LGPL License.
 */
public interface IDataSerializer<T> {
    void write(FriendlyByteBuf buf, T value);

    T read(FriendlyByteBuf buf);

    Tag write(T value);

    T read(Tag nbt);

    default T copy(T value) {
        return read(write(value).copy());
    }

    default <N> DataResult<N> encode(T value, DynamicOps<N> ops) {
        try {
            return DataResult.success(NbtOps.INSTANCE.convertTo(ops, write(value)));
        } catch (RuntimeException invalid) {
            return DataResult.error(() -> invalid.getMessage());
        }
    }

    default <N> DataResult<T> decode(DynamicOps<N> ops, N input) {
        try {
            return DataResult.success(read(ops.convertTo(NbtOps.INSTANCE, input)));
        } catch (RuntimeException invalid) {
            return DataResult.error(() -> invalid.getMessage());
        }
    }
}
