/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.util;

import com.tacz.guns.fabric.data.ItemStackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Primitive Lua metadata access. Item-backed accessors commit every edit to CUSTOM_DATA. */
@SuppressWarnings("unused")
public final class LuaNbtAccessor {
    private final Supplier<CompoundTag> source;
    private final Consumer<CompoundTag> commit;
    private final List<String> path;

    public LuaNbtAccessor(CompoundTag nbt) {
        CompoundTag value = nbt == null ? new CompoundTag() : nbt;
        source = () -> value;
        commit = ignored -> {};
        path = List.of();
    }

    private LuaNbtAccessor(Supplier<CompoundTag> source, Consumer<CompoundTag> commit, List<String> path) {
        this.source = source;
        this.commit = commit;
        this.path = List.copyOf(path);
    }

    public static LuaNbtAccessor from(ItemStack stack) {
        return new LuaNbtAccessor(() -> ItemStackData.read(stack), data -> ItemStackData.set(stack, data), List.of());
    }

    public static LuaNbtAccessor from(CompoundTag nbt) {
        return new LuaNbtAccessor(nbt);
    }

    private CompoundTag resolve(CompoundTag root, boolean create) {
        CompoundTag current = root;
        for (String key : path) {
            CompoundTag child = current.getCompound(key).orElse(null);
            if (child == null) {
                child = new CompoundTag();
                if (create) current.put(key, child);
            }
            current = child;
        }
        return current;
    }

    private void mutate(Consumer<CompoundTag> edit) {
        CompoundTag root = source.get();
        edit.accept(resolve(root, true));
        commit.accept(root);
    }

    public boolean contains(String key) { return nbt().contains(key); }
    public boolean contains(String key, int type) { return ItemStackData.contains(nbt(), key, type); }
    public LuaNbtAccessor newCompoundTag() { return from(new CompoundTag()); }
    public int getInt(String key) { return nbt().getIntOr(key, 0); }
    public double getDouble(String key) { return nbt().getDoubleOr(key, 0d); }
    public float getFloat(String key) { return nbt().getFloatOr(key, 0f); }
    public long getLong(String key) { return nbt().getLongOr(key, 0L); }
    public String getString(String key) { return nbt().getStringOr(key, ""); }
    public boolean getBoolean(String key) { return nbt().getBooleanOr(key, false); }
    public boolean getBoolean(CompoundTag nbt, String key) { return nbt.getBooleanOr(key, false); }

    public LuaNbtAccessor getCompound(String key) {
        if (!contains(key, Tag.TAG_COMPOUND)) return null;
        List<String> childPath = new ArrayList<>(path);
        childPath.add(key);
        return new LuaNbtAccessor(source, commit, childPath);
    }

    public void putInt(String key, int value) { mutate(tag -> tag.putInt(key, value)); }
    public void putDouble(String key, double value) { mutate(tag -> tag.putDouble(key, value)); }
    public void putFloat(String key, float value) { mutate(tag -> tag.putFloat(key, value)); }
    public void putLong(String key, long value) { mutate(tag -> tag.putLong(key, value)); }
    public void putString(String key, String value) { mutate(tag -> tag.putString(key, value)); }
    public void putBoolean(String key, boolean value) { mutate(tag -> tag.putBoolean(key, value)); }

    public void putCompound(String key, LuaNbtAccessor value) {
        if (value != null) mutate(tag -> tag.put(key, value.nbt().copy()));
    }

    @ApiStatus.Internal
    public CompoundTag nbt() { return resolve(source.get(), false); }
}
