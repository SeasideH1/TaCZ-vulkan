/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.codec;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Decodes official 1.20.1 pack JSON without rewriting the pack on disk. */
public final class LegacyPackCodec {
    private static final int RELEASE_DATA_VERSION = 3465;
    private LegacyPackCodec() {}

    public static CompoundTag readNbt(JsonElement json) {
        try { return TagParser.parseCompoundFully(json.isJsonPrimitive() ? json.getAsString() : json.toString()); }
        catch (CommandSyntaxException e) { throw new JsonParseException("Invalid item NBT", e); }
    }

    public static ItemStack readStack(JsonObject json, boolean readNbt) {
        return readStack(json, readNbt, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    /** A world registry provider is required when legacy tags reference dynamic components. */
    public static ItemStack readStack(JsonObject json, boolean readNbt, HolderLookup.Provider registries) {
        return readStack(json, readNbt, RegistryOps.create(NbtOps.INSTANCE, registries));
    }

    /** Retains the recipe decoder's registry context when changing the serialized representation. */
    public static ItemStack readStack(JsonObject json, boolean readNbt, DynamicOps<?> ops) {
        return ItemStack.CODEC.parse(itemOps(ops), fixLegacyStack(json, readNbt)).getOrThrow(JsonParseException::new);
    }

    /** Recipe registries load before default item components are bound; templates defer stack creation. */
    public static net.minecraft.world.item.ItemStackTemplate readStackTemplate(JsonObject json, boolean readNbt, DynamicOps<?> ops) {
        return net.minecraft.world.item.ItemStackTemplate.CODEC.parse(itemOps(ops), fixLegacyStack(json, readNbt))
                .getOrThrow(JsonParseException::new);
    }

    private static Tag fixLegacyStack(JsonObject json, boolean readNbt) {
        Identifier id = Identifier.tryParse(json.get("item").getAsString());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) throw new JsonParseException("Unknown item: " + json.get("item"));
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        if (count < 1 || count > 99) throw new JsonParseException("Invalid item count: " + count);
        CompoundTag legacy = new CompoundTag();
        legacy.putString("id", id.toString());
        legacy.putByte("Count", (byte) count);
        if (readNbt && json.has("nbt")) legacy.put("tag", readNbt(json.get("nbt")));
        return DataFixers.getDataFixer().update(References.ITEM_STACK,
                new Dynamic<Tag>(NbtOps.INSTANCE, legacy), RELEASE_DATA_VERSION,
                SharedConstants.getCurrentVersion().dataVersion().version()).getValue();
    }

    /** Upgrade a vanilla recipe result before its world registry codec runs. */
    public static JsonObject upgradeRecipeResult(JsonObject json) {
        if (json.has("id") || json.has("components")) {
            throw new JsonParseException("Legacy recipe result mixes item/nbt with id/components: " + json);
        }
        return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, fixLegacyStack(json, true)).getAsJsonObject();
    }

    private static DynamicOps<Tag> itemOps(DynamicOps<?> ops) {
        return ops instanceof RegistryOps<?> registryOps
                ? registryOps.withParent(NbtOps.INSTANCE)
                : RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    public static Ingredient readIngredient(JsonElement json) {
        return readIngredient(json, BuiltInRegistries.ITEM);
    }
    public static Ingredient readIngredient(JsonElement json, DynamicOps<?> ops) {
        HolderGetter<Item> items = ops instanceof RegistryOps<?> registryOps
                ? registryOps.getter(Registries.ITEM).orElse(BuiltInRegistries.ITEM) : BuiltInRegistries.ITEM;
        return readIngredient(json, items);
    }
    private static Ingredient readIngredient(JsonElement json, HolderGetter<Item> items) {
        if (json.isJsonArray()) {
            return Ingredient.of(java.util.stream.StreamSupport.stream(json.getAsJsonArray().spliterator(), false)
                    .flatMap(entry -> readIngredient(entry, items).items()).map(holder -> holder.value()));
        }
        if (json.isJsonPrimitive()) {
            String value = json.getAsString();
            if (value.startsWith("#")) return Ingredient.of(items.getOrThrow(TagKey.create(Registries.ITEM, legacyItemTag(Identifier.parse(value.substring(1))))));
            return Ingredient.of(items.getOrThrow(ResourceKey.create(Registries.ITEM, Identifier.parse(value))).value());
        }
        JsonObject object = json.getAsJsonObject();
        if (object.has("tag")) return Ingredient.of(items.getOrThrow(TagKey.create(Registries.ITEM, legacyItemTag(Identifier.parse(object.get("tag").getAsString())))));
        if (object.has("item")) return Ingredient.of(items.getOrThrow(ResourceKey.create(Registries.ITEM, Identifier.parse(object.get("item").getAsString()))).value());
        throw new JsonParseException("Ingredient needs item or tag: " + json);
    }

    public static Identifier legacyItemTag(Identifier id) {
        if (!id.getNamespace().equals("forge")) return id;
        return switch (id.getPath()) {
            case "gunpowder" -> Identifier.parse("c:gunpowders");
            case "leather" -> Identifier.parse("c:leathers");
            case "glass" -> Identifier.parse("c:glass_blocks");
            default -> Identifier.fromNamespaceAndPath("c", id.getPath());
        };
    }
}
