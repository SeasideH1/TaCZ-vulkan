/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.resource.serialize;

import com.google.gson.*;
import net.minecraft.resources.Identifier;
import java.lang.reflect.Type;

/** Keeps upstream gunpack identifier JSON as namespace:path strings. */
public final class IdentifierSerializer implements JsonSerializer<Identifier>, JsonDeserializer<Identifier> {
    @Override public JsonElement serialize(Identifier id, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(id.toString());
    }
    @Override public Identifier deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) throw new JsonParseException("Expected identifier string");
        Identifier id = Identifier.tryParse(json.getAsString());
        if (id == null) throw new JsonParseException("Invalid identifier: " + json.getAsString());
        return id;
    }
}
