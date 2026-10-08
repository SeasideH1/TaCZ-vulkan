/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.entity.sync.core;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Author: MrCrayfish.
 * Open source at <a href="https://github.com/MrCrayfish/Framework">Github</a> under LGPL License.
 */
public record SyncedClassKey<E extends Entity>(Class<E> entityClass, Identifier id) {
    public static final SyncedClassKey<LivingEntity> LIVING_ENTITY = new SyncedClassKey<>(LivingEntity.class, Identifier.parse("living_entity"));

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncedClassKey<?> that = (SyncedClassKey<?>) o;
        return this.entityClass.getName().equals(that.entityClass.getName());
    }

    @Override
    public int hashCode() {
        return this.entityClass.getName().hashCode();
    }
}
