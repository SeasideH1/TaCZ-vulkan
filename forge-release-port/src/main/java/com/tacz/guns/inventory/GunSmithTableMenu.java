/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.inventory;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageCraft;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonBlockIndex;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public class GunSmithTableMenu extends AbstractContainerMenu {
    public static final MenuType<GunSmithTableMenu> TYPE = new ExtendedMenuType<>(
            GunSmithTableMenu::new, Identifier.STREAM_CODEC);

    private final Identifier blockId;
    private final RecipeFilter filter;

    public GunSmithTableMenu(int id, Inventory inventory, @Nullable Identifier resourceLocation) {
        super(TYPE, id);
        this.blockId = resourceLocation;
        this.filter = TimelessAPI.getCommonBlockIndex(getBlockId()).map(CommonBlockIndex::getFilter).orElse(null);
    }

    @Nullable
    public Identifier getBlockId() {
        return blockId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Nullable
    private GunSmithTableRecipe getRecipe(Identifier recipeId, RecipeManager recipeManager) {
        if (!DefaultAssets.DEFAULT_BLOCK_ID.equals(getBlockId()) || SyncConfig.ENABLE_TABLE_FILTER.get()) {
            if (filter != null && !filter.contains(recipeId)) {
                return null;
            }
        }

        Recipe<?> recipe = recipeManager.byKey(ResourceKey.create(Registries.RECIPE, recipeId)).map(holder -> holder.value()).orElse(null);
        if (recipe instanceof GunSmithTableRecipe gunSmithTableRecipe) {
            boolean flag = TimelessAPI.getCommonBlockIndex(getBlockId()).map(blockIndex -> {
                return blockIndex.getData().getTabs().stream().noneMatch(tab -> tab.id().equals(gunSmithTableRecipe.getTab()));
            }).orElse(true);
            if (DefaultAssets.DEFAULT_BLOCK_ID.equals(getBlockId()) && !SyncConfig.ENABLE_TABLE_FILTER.get()) {
                flag = false;
            }
            if (flag) {
                return null;
            }
            return gunSmithTableRecipe;
        }
        return null;
    }

    public void doCraft(Identifier recipeId, Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        GunSmithTableRecipe recipe = getRecipe(recipeId, serverLevel.getServer().getRecipeManager());
        if (recipe == null) {
            return;
        }
        com.tacz.guns.fabric.inventory.EntityInventory.get(player).ifPresent(handler -> {
            if (!com.tacz.guns.crafting.GunSmithTableCrafting.consumeMaterials(handler, recipe.getInputs(), player.isCreative())) {
                return;
            }

            // 给玩家对应的物品
            Level level = player.level();
            if (!level.isClientSide()) {
                for (ItemStack output : com.tacz.guns.crafting.GunSmithTableCrafting.splitOutput(recipe.getResultItem(level.registryAccess()))) {
                    ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), output);
                    itemEntity.setPickUpDelay(0);
                    level.addFreshEntity(itemEntity);
                }
            }
            // 更新，否则客户端显示不正确
            player.inventoryMenu.broadcastFullState();
            NetworkHandler.sendToClientPlayer(new ServerMessageCraft(this.containerId), player);
        });
    }
}
