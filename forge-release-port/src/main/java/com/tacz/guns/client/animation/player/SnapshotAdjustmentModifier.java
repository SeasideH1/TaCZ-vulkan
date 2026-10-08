/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.animation.player;

import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import java.util.*;
import java.util.function.Function;

/** Evaluates entity-dependent adjustments on the extraction thread, retaining only numbers. */
public final class SnapshotAdjustmentModifier extends AdjustmentModifier {
    static final List<String> BONES = List.of("body", "head", "torso", "leftArm", "rightArm", "leftLeg", "rightLeg");
    public SnapshotAdjustmentModifier(Function<String, Optional<PartModifier>> source) { super(source); }
    AnimationSnapshot.Transform capture(AnimationSnapshot snapshot) {
        var child = snapshot.capture(getAnim());
        if (!enabled) return child;
        float fade = getFadeIn(snapshot.partialTick()) * getFadeOut(snapshot.partialTick());
        Map<String, PartModifier> adjustments = new HashMap<>();
        for (String bone : BONES) source.apply(bone).ifPresent(value -> adjustments.put(bone, value));
        Map<String, PartModifier> frozen = Map.copyOf(adjustments);
        return (bone, type, baseline) -> {
            var value = child.apply(bone, type, baseline);
            PartModifier adjustment = frozen.get(bone);
            if (adjustment == null) return value;
            return switch (type) {
                case POSITION -> value.add(adjustment.offset().scale(fade));
                case ROTATION -> value.add(adjustment.rotation().scale(fade));
                case BEND -> value;
            };
        };
    }
}
