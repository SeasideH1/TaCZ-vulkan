/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.animation.player;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.compat.playeranimator.animation.AdjustmentYRotModifier;
import com.tacz.guns.compat.playeranimator.animation.AnimationManager;
import com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorAssetManager;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Native per-entity state, with no entity-ID keyed caches or external mod lifecycle. */
public final class NativePlayerAnimationState {
    private static final AttachmentType<NativePlayerAnimationState> TYPE = AttachmentRegistry.create(Identifier.parse("tacz:player_animation_state"));
    private static boolean initialized;
    private final Map<Identifier, ModifierLayer<IAnimation>> layers;
    private final List<SnapshotModifierLayer<IAnimation>> ordered;
    private final long generation;
    private int lastTick;

    private NativePlayerAnimationState(AbstractClientPlayer player) {
        generation = PlayerAnimatorAssetManager.get().generation();
        lastTick = player.tickCount;
        SnapshotModifierLayer<IAnimation> lower = new SnapshotModifierLayer<>();
        SnapshotModifierLayer<IAnimation> upper = new SnapshotModifierLayer<>();
        SnapshotModifierLayer<IAnimation> once = new SnapshotModifierLayer<>();
        SnapshotModifierLayer<IAnimation> rotation = new SnapshotModifierLayer<>(null, AdjustmentYRotModifier.getModifier(player));
        ordered = List.of(lower, upper, once, rotation); // Original priorities 93, 94, 95, 96.
        Map<Identifier, ModifierLayer<IAnimation>> byName = new LinkedHashMap<>();
        byName.put(PlayerAnimatorCompat.LOWER_ANIMATION, lower);
        byName.put(PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, upper);
        byName.put(PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, once);
        byName.put(PlayerAnimatorCompat.ROTATION_ANIMATION, rotation);
        layers = Collections.unmodifiableMap(byName);
    }
    public static synchronized void init() {
        if (initialized) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) return;
            for (AbstractClientPlayer clientPlayer : client.level.players()) {
                AttachmentTarget target = (AttachmentTarget) clientPlayer;
                if (!PlayerAnimatorCompat.isAuthoredAnimationEnabled()) {
                    target.removeAttached(TYPE);
                    continue;
                }
                NativePlayerAnimationState state = target.getAttached(TYPE);
                if (state != null && state.lastTick != clientPlayer.tickCount) {
                    state.ordered.forEach(layer -> { if (layer.isActive()) layer.tick(); });
                    state.lastTick = clientPlayer.tickCount;
                }
            }
        });
        initialized = true;
    }
    private static NativePlayerAnimationState get(AbstractClientPlayer player) {
        AttachmentTarget target = (AttachmentTarget) player;
        NativePlayerAnimationState state = target.getAttached(TYPE);
        if (state == null || state.generation != PlayerAnimatorAssetManager.get().generation()) {
            state = new NativePlayerAnimationState(player);
            target.setAttached(TYPE, state);
        }
        return state;
    }
    public static Map<Identifier, ModifierLayer<IAnimation>> getPlayerAssociatedData(AbstractClientPlayer player) { return get(player).layers; }

    /** Called after the renderer has selected clips with its exact extracted walk speed. */
    public static PoseSnapshot captureSelected(AbstractClientPlayer player, float partialTick) {
        if (!PlayerAnimatorCompat.isAuthoredAnimationEnabled()) {
            ((AttachmentTarget) player).removeAttached(TYPE);
            return PoseSnapshot.EMPTY;
        }
        return PoseSnapshot.capture(get(player).ordered, partialTick);
    }

    /** Performs original clip selection once for this extraction, then freezes all transforms. */
    public static PoseSnapshot capture(AbstractClientPlayer player, float partialTick) {
        if (!PlayerAnimatorCompat.isAuthoredAnimationEnabled()) return captureSelected(player, partialTick);
        NativePlayerAnimationState state = get(player);
        var display = TimelessAPI.getGunDisplay(player.getMainHandItem()).orElse(null);
        if (display != null && AnimationManager.hasPlayerAnimator3rd(display)) {
            float speed = player.walkAnimation.speed(partialTick);
            AnimationManager.playLowerAnimation(player, display, speed);
            AnimationManager.playLoopUpperAnimation(player, display, speed);
            AnimationManager.playRotationAnimation(player, display);
        } else AnimationManager.stopAllAnimation(player);
        return PoseSnapshot.capture(state.ordered, partialTick);
    }
}
