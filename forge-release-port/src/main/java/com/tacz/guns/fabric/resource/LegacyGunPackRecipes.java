/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.resource;

import com.google.gson.*;
import com.tacz.guns.fabric.codec.LegacyPackCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Adapts vanilla crafting recipes in external release-era gunpacks, without editing the pack. */
final class LegacyGunPackRecipes {
    private LegacyGunPackRecipes() {}

    static IoSupplier<InputStream> wrap(PackType type, Identifier id, IoSupplier<InputStream> source) {
        String path = id.getPath();
        if (type != PackType.SERVER_DATA || !path.endsWith(".json")
                || !(path.startsWith("recipe/") || path.startsWith("recipes/"))) return source;
        return () -> {
            byte[] bytes;
            try (InputStream stream = source.get()) { bytes = stream.readAllBytes(); }
            try {
                JsonObject recipe = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                String kind = recipe.has("type") ? recipe.get("type").getAsString() : "";
                if (!kind.equals("minecraft:crafting_shaped") && !kind.equals("minecraft:crafting_shapeless")) {
                    return new ByteArrayInputStream(bytes);
                }
                JsonObject before = recipe.deepCopy();
                if (recipe.has("key")) {
                    JsonObject key = recipe.getAsJsonObject("key");
                    for (var entry : key.entrySet()) entry.setValue(ingredient(entry.getValue()));
                }
                if (recipe.has("ingredients")) {
                    JsonArray ingredients = recipe.getAsJsonArray("ingredients");
                    for (int i = 0; i < ingredients.size(); i++) ingredients.set(i, ingredient(ingredients.get(i)));
                }
                if (recipe.has("result") && recipe.get("result").isJsonObject()) {
                    JsonObject result = recipe.getAsJsonObject("result");
                    if (result.has("item")) recipe.add("result", LegacyPackCodec.upgradeRecipeResult(result));
                }
                return new ByteArrayInputStream(recipe.equals(before) ? bytes : recipe.toString().getBytes(StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                throw new IOException("Unable to adapt gunpack recipe " + id, e);
            }
        };
    }

    private static JsonElement ingredient(JsonElement value) {
        if (!value.isJsonObject()) return value;
        JsonObject object = value.getAsJsonObject();
        // Leave custom ingredient codecs intact rather than discarding their extra fields.
        if (object.size() != 1) return value;
        if (object.has("item")) return object.get("item");
        if (object.has("tag")) return new JsonPrimitive("#" + LegacyPackCodec.legacyItemTag(
                Identifier.parse(object.get("tag").getAsString())));
        return value;
    }
}
