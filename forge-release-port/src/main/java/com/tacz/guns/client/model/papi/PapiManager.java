/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model.papi;

import com.google.common.collect.Maps;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class PapiManager {
    private static final Map<String, Function<ItemStack, String>> PAPI = Maps.newHashMap();

    // 注册，不知道放哪里，先放这
    static {
        addPapi(PlayerNamePapi.NAME, new PlayerNamePapi());
        addPapi(AmmoCountPapi.NAME, new AmmoCountPapi());
    }

    public static void addPapi(String textKey, Function<ItemStack, String> function) {
        textKey = "%" + textKey + "%";
        PAPI.put(textKey, function);
    }

    public static String getTextShow(String textKey, ItemStack stack) {
        String text = net.minecraft.locale.Language.getInstance().getOrDefault(textKey);
        for (var entry : PAPI.entrySet()) {
            String placeholder = entry.getKey();
            String data = entry.getValue().apply(stack);
            text = text.replace(placeholder, data);
        }
        return text;
    }
}
