/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.animation.player;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Immutable actor-free render data, applying the release's layer priorities over vanilla poses. */
public final class PoseSnapshot {
    public static final PoseSnapshot EMPTY = new PoseSnapshot(List.of());
    private final List<AnimationSnapshot.Transform> layers;
    private PoseSnapshot(List<AnimationSnapshot.Transform> layers) { this.layers = List.copyOf(layers); }
    public static PoseSnapshot capture(List<? extends IAnimation> layers, float partialTick) {
        AnimationSnapshot snapshot = new AnimationSnapshot(partialTick);
        List<AnimationSnapshot.Transform> frozen = new ArrayList<>();
        for (IAnimation layer : layers) {
            layer.setupAnim(partialTick);
            if (layer.isActive()) frozen.add(snapshot.capture(layer));
        }
        return frozen.isEmpty() ? EMPTY : new PoseSnapshot(frozen);
    }
    public boolean isActive() { return !layers.isEmpty(); }
    public Vec3 transformPosition(String bone, float x, float y, float z) { return transform(bone, TransformType.POSITION, x, y, z); }
    public Vec3 transformRotation(String bone, float x, float y, float z) { return transform(bone, TransformType.ROTATION, x, y, z); }
    public Vec3 transformBend(String bone, float plane, float bend) { return transform(bone, TransformType.BEND, plane, bend, 0); }
    private Vec3 transform(String bone, TransformType type, float x, float y, float z) {
        Vec3f value = new Vec3f(x, y, z);
        for (var layer : layers) value = layer.apply(bone, type, value);
        return new Vec3(value.getX(), value.getY(), value.getZ());
    }
}
