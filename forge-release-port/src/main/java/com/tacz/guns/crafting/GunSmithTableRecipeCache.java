/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.crafting;

import net.minecraft.resources.Identifier;
import java.util.*;

/** Atomic client recipe snapshot, since modern vanilla sync only carries recipe displays. */
public final class GunSmithTableRecipeCache {
    private static volatile Map<Identifier, GunSmithTableRecipe> recipes = Map.of();
    private GunSmithTableRecipeCache() {}
    public static void replace(List<GunSmithTableRecipe> incoming) {
        Map<Identifier, GunSmithTableRecipe> next = new LinkedHashMap<>();
        for (GunSmithTableRecipe recipe : incoming) {
            if (next.putIfAbsent(recipe.getId(), recipe) != null) throw new IllegalArgumentException("Duplicate recipe ID: " + recipe.getId());
        }
        recipes = Collections.unmodifiableMap(next);
    }
    public static Collection<GunSmithTableRecipe> getRecipes() { return recipes.values(); }
    public static Optional<GunSmithTableRecipe> byId(Identifier id) { return Optional.ofNullable(recipes.get(id)); }
    public static void clear() { recipes = Map.of(); }
}
