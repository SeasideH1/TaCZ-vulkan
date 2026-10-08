/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.crafting;

import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GunSmithTableRecipe implements Recipe<net.minecraft.world.item.crafting.RecipeInput> {
    private Identifier id;
    private com.google.gson.JsonObject sourceJson;
    private final GunSmithTableResult result;
    private final List<GunSmithTableIngredient> inputs;

    public GunSmithTableRecipe(Identifier id, GunSmithTableResult result, List<GunSmithTableIngredient> inputs) {
        this.id = id;
        this.result = result;
        this.inputs = inputs;
    }

    public GunSmithTableRecipe(Identifier id, TableRecipe tableRecipe) {
        this(id, tableRecipe.getResult(), tableRecipe.getMaterials());
    }

    @Override
    @Deprecated
    public boolean matches(net.minecraft.world.item.crafting.RecipeInput playerInventory, Level level) {
        return false;
    }

    @Override
    @Deprecated
    public ItemStack assemble(net.minecraft.world.item.crafting.RecipeInput playerInventory) {
        return ItemStack.EMPTY;
    }

    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.result.getResult().copy();
    }

    public Identifier getId() {
        return java.util.Objects.requireNonNull(this.id, "Recipe registry ID has not been bound");
    }

    @Override
    public RecipeSerializer<GunSmithTableRecipe> getSerializer() {
        return ModRecipe.GUN_SMITH_TABLE_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<GunSmithTableRecipe> getType() {
        return ModRecipe.GUN_SMITH_TABLE_CRAFTING.get();
    }

    public boolean hasId() { return id != null; }
    public void setId(Identifier id) { this.id = java.util.Objects.requireNonNull(id); }
    public com.google.gson.JsonObject getSourceJson() { return sourceJson == null ? null : sourceJson.deepCopy(); }
    public void setSourceJson(com.google.gson.JsonObject json) { sourceJson = json.deepCopy(); }
    @Override public boolean showNotification() { return true; }
    @Override public String group() { return getTab().toString(); }
    @Override public net.minecraft.world.item.crafting.PlacementInfo placementInfo() { return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE; }
    @Override public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() { return net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC; }

    public ItemStack getOutput() {
        return result.getResult();
    }

    public List<GunSmithTableIngredient> getInputs() {
        return inputs;
    }

    public GunSmithTableResult getResult() {
        return result;
    }

    public void init() {
        result.init();
    }

    public Identifier getTab() {
        return result.getGroup();
    }
}
