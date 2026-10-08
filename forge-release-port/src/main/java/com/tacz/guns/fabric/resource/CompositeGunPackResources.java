/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.resource;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;

import java.io.InputStream;
import java.util.*;

/** One logical gunpack, retaining first-pack-wins resolution across directories and ZIPs. */
public final class CompositeGunPackResources implements PackResources {
    private final PackLocationInfo location;
    private final List<PackResources> packs;
    private final IoSupplier<InputStream> icon;

    public CompositeGunPackResources(PackLocationInfo location, List<PackResources> packs, IoSupplier<InputStream> icon) {
        this.location = location;
        this.packs = List.copyOf(packs);
        this.icon = icon;
    }
    private static final Map<String, String> LEGACY_DIRECTORIES = Map.ofEntries(
            Map.entry("recipe", "recipes"), Map.entry("loot_table", "loot_tables"),
            Map.entry("advancement", "advancements"), Map.entry("predicate", "predicates"),
            Map.entry("item_modifier", "item_modifiers"), Map.entry("function", "functions"),
            Map.entry("structure", "structures"), Map.entry("tags/block", "tags/blocks"),
            Map.entry("tags/item", "tags/items"), Map.entry("tags/entity_type", "tags/entity_types"),
            Map.entry("tags/fluid", "tags/fluids"), Map.entry("tags/game_event", "tags/game_events"));

    private static String legacyPath(String path) {
        for (var entry : LEGACY_DIRECTORIES.entrySet()) {
            if (path.equals(entry.getKey()) || path.startsWith(entry.getKey() + "/")) {
                return entry.getValue() + path.substring(entry.getKey().length());
            }
        }
        return path;
    }
    private static String modernPath(String path) {
        for (var entry : LEGACY_DIRECTORIES.entrySet()) {
            if (path.equals(entry.getValue()) || path.startsWith(entry.getValue() + "/")) {
                return entry.getKey() + path.substring(entry.getValue().length());
            }
        }
        return path;
    }
    @Override public PackLocationInfo location() { return location; }
    @Override public IoSupplier<InputStream> getRootResource(String... paths) {
        return paths.length == 1 && paths[0].equals("pack.png") ? icon : null;
    }
    @Override public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        for (PackResources pack : packs) {
            IoSupplier<InputStream> resource = pack.getResource(type, id);
            if (resource != null) return LegacyGunPackRecipes.wrap(type, id, resource);
            if (type == PackType.SERVER_DATA) {
                String legacy = legacyPath(id.getPath());
                if (!legacy.equals(id.getPath())) {
                    resource = pack.getResource(type, id.withPath(legacy));
                    if (resource != null) return LegacyGunPackRecipes.wrap(type, id, resource);
                }
            }
        }
        return null;
    }
    @Override public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        Map<Identifier, IoSupplier<InputStream>> resources = new LinkedHashMap<>();
        for (PackResources pack : packs) {
            pack.listResources(type, namespace, path, resources::putIfAbsent);
            if (type == PackType.SERVER_DATA) {
                String legacy = legacyPath(path);
                if (!legacy.equals(path)) pack.listResources(type, namespace, legacy,
                        (id, supplier) -> resources.putIfAbsent(id.withPath(modernPath(id.getPath())), supplier));
            }
        }
        resources.forEach((id, supplier) -> output.accept(id, LegacyGunPackRecipes.wrap(type, id, supplier)));
    }
    @Override public Set<String> getNamespaces(PackType type) {
        Set<String> namespaces = new HashSet<>();
        for (PackResources pack : packs) namespaces.addAll(pack.getNamespaces(type));
        return namespaces;
    }
    @Override @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionType<T> section) {
        PackType type;
        if (section.equals(PackMetadataSection.CLIENT_TYPE)) type = PackType.CLIENT_RESOURCES;
        else if (section.equals(PackMetadataSection.SERVER_TYPE)) type = PackType.SERVER_DATA;
        else if (section.equals(PackMetadataSection.FALLBACK_TYPE)) type = PackType.CLIENT_RESOURCES;
        else return null;
        var version = SharedConstants.getCurrentVersion().packVersion(type);
        return (T) new PackMetadataSection(Component.translatable("tacz.resources.modresources"), new InclusiveRange<>(version, version));
    }
    @Override public void close() {
        RuntimeException failure = null;
        for (PackResources pack : packs) {
            try { pack.close(); }
            catch (RuntimeException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        }
        if (failure != null) throw failure;
    }
}
