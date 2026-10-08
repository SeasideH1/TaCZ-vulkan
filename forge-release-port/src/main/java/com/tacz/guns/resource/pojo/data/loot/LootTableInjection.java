/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.resource.pojo.data.loot;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

public record LootTableInjection(List<Identifier> lootTables, LootTable lootTable) {

    public static LootTableInjection fromJson(Identifier fileId, JsonElement element, net.minecraft.core.HolderLookup.Provider registries) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "loot injection");
        List<Identifier> lootTables = readLootTables(fileId, object);
        if (!object.has("pools")) {
            throw new JsonParseException("Loot injection " + fileId + " must define pools");
        }
                JsonObject nativeJson = object.deepCopy();
        adaptLegacyFunctions(nativeJson);
        LootTable table = LootTable.DIRECT_CODEC.parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registries), nativeJson)
                .getOrThrow(JsonParseException::new);
        return new LootTableInjection(lootTables, table);
    }

    private static void adaptLegacyFunctions(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("function") && object.get("function").getAsString().equals("minecraft:set_nbt")) {
                object.addProperty("function", "minecraft:set_custom_data");
            }
            object.entrySet().forEach(entry -> adaptLegacyFunctions(entry.getValue()));
        } else if (element.isJsonArray()) element.getAsJsonArray().forEach(LootTableInjection::adaptLegacyFunctions);
    }

    private static List<Identifier> readLootTables(Identifier fileId, JsonObject object) {
        List<Identifier> lootTables = new ArrayList<>();
        if (object.has("loot_tables")) {
            for (JsonElement table : GsonHelper.getAsJsonArray(object, "loot_tables")) {
                lootTables.add(Identifier.parse(GsonHelper.convertToString(table, "loot table")));
            }
        } else if (object.has("loot_table")) {
            lootTables.add(Identifier.parse(GsonHelper.getAsString(object, "loot_table")));
        } else {
            throw new JsonParseException("Loot injection " + fileId + " must define loot_table or loot_tables");
        }
        return List.copyOf(lootTables);
    }

    public List<ItemStack> createStacks(LootContext context) {
        List<ItemStack> stacks = new ArrayList<>();
        lootTable.getRandomItemsRaw(context, stacks::add);
        return stacks;
    }
}
