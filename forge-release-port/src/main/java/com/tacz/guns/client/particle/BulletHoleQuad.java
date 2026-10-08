/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.particle;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Extracts the original six-face decal into the native immutable particle state. */
final class BulletHoleQuad {
    private BulletHoleQuad() {}

    static void submit(QuadParticleRenderState state, SingleQuadParticle.Layer layer,
                       Direction direction, float x, float y, float z, float scale,
                       float u0, float u1, float v0, float v1, int color, int light) {
        Quaternionf face = direction.getRotation();
        // The original local quad is XZ at y=0.01 before the scale is applied.
        Vector3f offset = new Vector3f(0, 0.01F * scale, 0).rotate(face);
        // Native vertices are (1,-1), (1,1), (-1,1), (-1,-1) in XY.
        // This basis maps them one-for-one to the original XZ winding/UV order:
        // (-1,-1), (-1,1), (1,1), (1,-1). Normal stays along the block face.
        Quaternionf rotation = new Quaternionf(face).rotateX(-Mth.HALF_PI).rotateZ(Mth.PI);
        state.add(layer, x + offset.x, y + offset.y, z + offset.z,
                rotation.x, rotation.y, rotation.z, rotation.w, scale,
                u0, u1, v0, v1, color, light);
    }

    static int glow(int age) {
        return Math.max(15 - age / 2, 0);
    }

    static float fade(int age, int lifetime, double thresholdFraction) {
        if (lifetime <= 0 || age >= lifetime) {
            return 0;
        }
        double threshold = Math.clamp(thresholdFraction, 0, 1) * lifetime;
        // 1 is a valid configured threshold: remain opaque until removal rather
        // than dividing zero by zero and emitting a NaN alpha every frame.
        if (age <= threshold || threshold >= lifetime) {
            return 1;
        }
        return Math.clamp(1.0F - (float) ((age - threshold) / (lifetime - threshold)), 0, 1);
    }
}
