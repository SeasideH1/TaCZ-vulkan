/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.animation.player;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import java.util.ArrayList;
import java.util.List;

/** Tracks public modifier operations without reflective access to the pinned core library. */
public final class SnapshotModifierLayer<T extends IAnimation> extends ModifierLayer<T> {
    private final List<AbstractModifier> captureModifiers = new ArrayList<>();
    public SnapshotModifierLayer() { super(); }
    public SnapshotModifierLayer(T animation, AbstractModifier... modifiers) {
        super();
        setAnimation(animation);
        for (AbstractModifier modifier : modifiers) addModifierLast(modifier);
    }
    @Override public void addModifier(AbstractModifier modifier, int index) {
        super.addModifier(modifier, index);
        captureModifiers.add(index, modifier);
    }
    @Override public void removeModifier(int index) {
        super.removeModifier(index);
        captureModifiers.remove(index);
    }
    AnimationSnapshot.Transform capture(AnimationSnapshot snapshot) {
        return snapshot.capture(captureModifiers.isEmpty() ? getAnimation() : captureModifiers.getFirst());
    }
}
