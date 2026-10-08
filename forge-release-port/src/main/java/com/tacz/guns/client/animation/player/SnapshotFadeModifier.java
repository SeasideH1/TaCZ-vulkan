/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.animation.player;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;

/** Same easing and blending as the release's standardFadeIn, with a frozen render view. */
public final class SnapshotFadeModifier extends AbstractFadeModifier {
    private final Ease ease;
    private SnapshotFadeModifier(int length, Ease ease) { super(length); this.ease = ease; }
    public static SnapshotFadeModifier standardFadeIn(int length, Ease ease) { return new SnapshotFadeModifier(length, ease); }
    @Override protected float getAlpha(String bone, TransformType type, float progress) { return ease.invoke(progress); }
    AnimationSnapshot.Transform capture(AnimationSnapshot snapshot) {
        var target = snapshot.capture(getAnim());
        float progress = calculateProgress(snapshot.partialTick());
        if (progress > 1) return target;
        var source = snapshot.capture(beginAnimation);
        float alpha = ease.invoke(progress);
        return (bone, type, baseline) -> target.apply(bone, type, baseline).scale(alpha)
                .add(source.apply(bone, type, baseline).scale(1 - alpha));
    }
}
