/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.animation.player;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import java.util.IdentityHashMap;
import java.util.Map;

/** Frozen evaluation graph. Rotation wrapping is evaluated exactly rather than approximated. */
public final class AnimationSnapshot {
    @FunctionalInterface public interface Transform {
        Vec3f apply(String bone, TransformType type, Vec3f baseline);
    }
    static final Transform IDENTITY = (bone, type, baseline) -> baseline;
    private final float partialTick;
    private final Map<IAnimation, Transform> captured = new IdentityHashMap<>();
    public AnimationSnapshot(float partialTick) { this.partialTick = partialTick; }
    public float partialTick() { return partialTick; }

    public Transform capture(IAnimation animation) {
        if (animation == null || !animation.isActive()) return IDENTITY;
        Transform existing = captured.get(animation);
        if (existing != null) return existing;
        Transform frozen;
        if (animation instanceof KeyframeAnimationPlayer player) {
            // Recreate the evaluator's loop phase with its public API. No live player/state is retained.
            KeyframeAnimationPlayer copy;
            if (player.isLoopStarted()) {
                copy = new KeyframeAnimationPlayer(player.getData(), player.getData().endTick);
                copy.tick();
                int limit = player.getData().endTick - player.getData().returnToTick + 1;
                for (int i = 0; copy.getCurrentTick() != player.getCurrentTick() && i <= limit; i++) copy.tick();
                if (copy.getCurrentTick() != player.getCurrentTick()) throw new IllegalStateException("Cannot capture animation loop phase");
            } else copy = new KeyframeAnimationPlayer(player.getData(), player.getCurrentTick());
            copy.setupAnim(partialTick);
            frozen = (bone, type, baseline) -> copy.get3DTransform(bone, type, partialTick, baseline);
        } else if (animation instanceof SnapshotModifierLayer<?> layer) frozen = layer.capture(this);
        else if (animation instanceof SnapshotFadeModifier fade) frozen = fade.capture(this);
        else if (animation instanceof SnapshotAdjustmentModifier adjustment) frozen = adjustment.capture(this);
        else throw new IllegalArgumentException("Animation type has no immutable capture adapter: " + animation.getClass().getName());
        captured.put(animation, frozen);
        return frozen;
    }
}
