/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.crafting;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.fabric.codec.LegacyPackCodec;
import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Modern registry-aware recipe codec retaining the release's unmodified JSON schema. */
public final class GunSmithTableSerializer {
    private static final int MAX_INGREDIENTS = 4096;
    public static final MapCodec<GunSmithTableRecipe> CODEC = new MapCodec<>() {
        @Override public <T> Stream<T> keys(DynamicOps<T> ops) { return Stream.of("materials", "result", "native_materials", "native_result", "group").map(ops::createString); }
        @Override public <T> DataResult<GunSmithTableRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
            try {
                JsonObject json = ops.convertTo(JsonOps.INSTANCE, ops.createMap(input.entries())).getAsJsonObject();
                List<GunSmithTableIngredient> ingredients = new ArrayList<>();
                if (input.get("native_materials") != null) {
                    List<T> rawMaterials = ops.getStream(input.get("native_materials")).getOrThrow().toList();
                    for (T raw : rawMaterials) {
                        MapLike<T> ingredient = ops.getMap(raw).getOrThrow();
                        ingredients.add(new GunSmithTableIngredient(Ingredient.CODEC.parse(ops, ingredient.get("ingredient")).getOrThrow(),
                                Codec.intRange(1, Integer.MAX_VALUE).parse(ops, ingredient.get("count")).getOrThrow()));
                    }
                } else {
                    for (JsonElement element : json.getAsJsonArray("materials")) {
                        JsonObject ingredient = element.getAsJsonObject();
                        ingredients.add(new GunSmithTableIngredient(LegacyPackCodec.readIngredient(ingredient.get("item"), ops),
                                Math.max(1, ingredient.has("count") ? ingredient.get("count").getAsInt() : 1)));
                    }
                }
                if (ingredients.size() > MAX_INGREDIENTS) return DataResult.error(() -> "Too many gunsmith ingredients");
                GunSmithTableResult result;
                if (input.get("native_result") != null) {
                    result = new GunSmithTableResult(net.minecraft.world.item.ItemStackTemplate.CODEC.parse(ops, input.get("native_result")).getOrThrow(),
                            Identifier.parse(json.get("group").getAsString()));
                } else result = com.tacz.guns.resource.serialize.GunSmithTableResultSerializer.deserialize(json.get("result"), ops);
                GunSmithTableRecipe recipe = new GunSmithTableRecipe(null, result, ingredients);
                recipe.setSourceJson(json);
                return DataResult.success(recipe);
            } catch (RuntimeException failure) { return DataResult.error(() -> "Invalid gunsmith recipe: " + failure.getMessage()); }
        }
        @Override public <T> RecordBuilder<T> encode(GunSmithTableRecipe recipe, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            JsonObject source = recipe.getSourceJson();
            if (source != null) {
                source.entrySet().forEach(entry -> {
                    if (!entry.getKey().equals("type")) prefix.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
                });
            } else {
                prefix.add("native_result", ItemStack.CODEC.encodeStart(ops, recipe.getOutput()));
                prefix.add("group", ops.createString(recipe.getTab().toString()));
                prefix.add("native_materials", ops.createList(recipe.getInputs().stream().map(ingredient -> ops.mapBuilder()
                        .add("ingredient", Ingredient.CODEC.encodeStart(ops, ingredient.getIngredient()))
                        .add("count", ops.createInt(ingredient.getCount())).build(ops.empty()).getOrThrow())));
            }
            return prefix;
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, GunSmithTableRecipe> STREAM_CODEC = StreamCodec.of(
            GunSmithTableSerializer::toNetwork, GunSmithTableSerializer::fromNetwork);
    public static final RecipeSerializer<GunSmithTableRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    private GunSmithTableSerializer() {}

    private static GunSmithTableRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        Identifier recipeId = buffer.readBoolean() ? buffer.readIdentifier() : null;
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_INGREDIENTS) throw new IllegalArgumentException("Invalid ingredient count: " + size);
        List<GunSmithTableIngredient> ingredients = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            int count = buffer.readVarInt();
            if (count < 1) throw new IllegalArgumentException("Invalid ingredient quantity: " + count);
            ingredients.add(new GunSmithTableIngredient(ingredient, count));
        }
        ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        return new GunSmithTableRecipe(recipeId, new GunSmithTableResult(result, buffer.readIdentifier()), ingredients);
    }
    private static void toNetwork(RegistryFriendlyByteBuf buffer, GunSmithTableRecipe recipe) {
        buffer.writeBoolean(recipe.hasId());
        if (recipe.hasId()) buffer.writeIdentifier(recipe.getId());
        buffer.writeVarInt(recipe.getInputs().size());
        for (GunSmithTableIngredient ingredient : recipe.getInputs()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.getIngredient());
            buffer.writeVarInt(ingredient.getCount());
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.getOutput());
        buffer.writeIdentifier(recipe.getTab());
    }
}
