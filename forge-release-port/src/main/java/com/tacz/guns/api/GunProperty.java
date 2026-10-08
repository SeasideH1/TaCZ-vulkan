/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api;

import com.google.common.reflect.TypeToken;
import net.minecraft.util.Util;

public record GunProperty<T>(
        String name,
        Class<T> type
) {
    public static <T> GunProperty<T> of(String name, Class<T> type) {
        return Util.make(new GunProperty<>(name, type), property -> GunProperties.ALL.put(name, property));
    }

    @SuppressWarnings("unchecked")
    public static <T> GunProperty<T> of(String name, TypeToken<T> type) {
        return Util.make(new GunProperty<>(name, (Class<T>) type.getRawType()), property -> GunProperties.ALL.put(name, property));
    }
}
