/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.event.server;

import com.tacz.guns.api.event.Event;
import com.tacz.guns.api.event.Cancelable;
import com.tacz.guns.util.block.ProjectileExplosion;
import net.minecraft.world.entity.Entity;
import java.util.List;

/** Loader-independent hooks for cancellation and affected-entity filtering. */
public abstract class ProjectileExplosionEvent extends Event {
    private final ProjectileExplosion explosion;
    protected ProjectileExplosionEvent(ProjectileExplosion explosion) { this.explosion = explosion; }
    public ProjectileExplosion getExplosion() { return explosion; }
    @Cancelable public static final class Start extends ProjectileExplosionEvent {
        public Start(ProjectileExplosion explosion) { super(explosion); }
    }
    public static final class Detonate extends ProjectileExplosionEvent {
        private final List<Entity> entities;
        private final float radius;
        public Detonate(ProjectileExplosion explosion, List<Entity> entities, float radius) { super(explosion); this.entities = entities; this.radius = radius; }
        public List<Entity> getAffectedEntities() { return entities; }
        public float getRadius() { return radius; }
    }
}
