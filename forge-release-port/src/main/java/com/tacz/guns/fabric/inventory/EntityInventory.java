/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** Native entity inventories; addons can provide an alternate view before vanilla fallback. */
public final class EntityInventory {
    private static final CopyOnWriteArrayList<Function<LivingEntity, Optional<InventoryAccess>>> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final EquipmentSlot[] EQUIPMENT = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private EntityInventory() {}
    public static AutoCloseable register(Function<LivingEntity, Optional<InventoryAccess>> provider) {
        PROVIDERS.add(provider);
        return () -> PROVIDERS.remove(provider);
    }
    public static Optional<InventoryAccess> get(LivingEntity entity) {
        for (var provider : PROVIDERS) {
            Optional<InventoryAccess> inventory = provider.apply(entity);
            if (inventory.isPresent()) return inventory;
        }
        if (entity instanceof Player player) return Optional.of(of(player.getInventory()));
        if (entity instanceof Container container) return Optional.of(of(container));
        return Optional.of(new View() {
            public int getSlots() { return EQUIPMENT.length; }
            public ItemStack getStackInSlot(int slot) { return entity.getItemBySlot(EQUIPMENT[slot]); }
            public void setStackInSlot(int slot, ItemStack stack) { entity.setItemSlot(EQUIPMENT[slot], stack); }
        });
    }
    public static InventoryAccess of(Container container) {
        return new View() {
            public int getSlots() { return container.getContainerSize(); }
            public ItemStack getStackInSlot(int slot) { return container.getItem(slot); }
            public void setStackInSlot(int slot, ItemStack stack) { container.setItem(slot, stack); container.setChanged(); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!container.canPlaceItem(slot, stack)) return stack;
                return super.insertItem(slot, stack, simulate);
            }
            @Override protected int limit(int slot, ItemStack stack) { return Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize()); }
        };
    }
    private abstract static class View implements InventoryAccess {
        protected int limit(int slot, ItemStack stack) { return stack.getMaxStackSize(); }
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) return ItemStack.EMPTY;
            ItemStack stored = getStackInSlot(slot);
            if (stored.isEmpty()) return ItemStack.EMPTY;
            int extracted = Math.min(amount, stored.getCount());
            ItemStack result = stored.copyWithCount(extracted);
            if (!simulate) setStackInSlot(slot, stored.copyWithCount(stored.getCount() - extracted));
            return result;
        }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack stored = getStackInSlot(slot);
            if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, stack)) return stack;
            int accepted = Math.min(stack.getCount(), Math.max(0, limit(slot, stack) - stored.getCount()));
            if (accepted == 0) return stack;
            if (!simulate) setStackInSlot(slot, stack.copyWithCount(stored.getCount() + accepted));
            return stack.copyWithCount(stack.getCount() - accepted);
        }
    }
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        ItemStack remainder = stack.copy();
        player.getInventory().add(remainder);
        if (!remainder.isEmpty()) player.drop(remainder, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }
}
