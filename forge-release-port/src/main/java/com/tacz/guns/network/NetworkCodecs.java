/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Counted-map encoding retained from the release after FriendlyByteBuf removed its map helpers. */
public final class NetworkCodecs {
    private NetworkCodecs() { }

    public static <K, V> void writeMap(FriendlyByteBuf buffer, Map<K, V> values,
            BiConsumer<FriendlyByteBuf, K> keyWriter, BiConsumer<FriendlyByteBuf, V> valueWriter) {
        buffer.writeVarInt(values.size());
        values.forEach((key, value) -> {
            keyWriter.accept(buffer, key);
            valueWriter.accept(buffer, value);
        });
    }

    public static <K, V> Map<K, V> readMap(FriendlyByteBuf buffer, int maxEntries,
            Function<FriendlyByteBuf, K> keyReader, Function<FriendlyByteBuf, V> valueReader) {
        int size = readCount(buffer, maxEntries);
        Map<K, V> values = new HashMap<>(Math.min(size, 4096));
        for (int i = 0; i < size; i++) {
            K key = keyReader.apply(buffer);
            V value = valueReader.apply(buffer);
            if (values.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Duplicate TACZ packet map key");
            }
        }
        return values;
    }

    public static int readCount(FriendlyByteBuf buffer, int maxEntries) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maxEntries) {
            throw new IllegalArgumentException("Invalid TACZ packet entry count: " + count);
        }
        return count;
    }
}
