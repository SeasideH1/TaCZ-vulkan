/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.entity.sync.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Persistent per-entity attachment replacing the release's Forge capability provider. */
public final class DataHolderCapabilityProvider {
    public static final AttachmentType<DataHolder> ATTACHMENT = AttachmentRegistry.<DataHolder>builder()
            .initializer(DataHolder::new)
            .persistent(DataHolderCodec.INSTANCE)
            // Copying is explicit: individual keys choose whether they survive player death.
            .buildAndRegister(Identifier.fromNamespaceAndPath("tacz", "synced_entity_data"));

    private DataHolderCapabilityProvider() { }

    public static void init() {
        // Force registration during common initialization, before entities deserialize.
    }

    public static DataHolder get(Entity entity) {
        return ((AttachmentTarget) entity).getAttachedOrCreate(ATTACHMENT);
    }

    public static void set(Entity entity, DataHolder holder) {
        ((AttachmentTarget) entity).setAttached(ATTACHMENT, holder);
    }
}
