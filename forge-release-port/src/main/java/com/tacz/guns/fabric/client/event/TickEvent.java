/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.event;

import com.tacz.guns.api.event.Event;
import com.tacz.guns.api.event.LogicalSide;
import net.minecraft.world.entity.player.Player;

/** TACZ client callbacks emitted by the Fabric tick/extraction bridges. */
public abstract class TickEvent extends Event {
    public enum Phase { START, END }
    public final Phase phase;
    public final LogicalSide side = LogicalSide.CLIENT;

    protected TickEvent(Phase phase) { this.phase = phase; }

    public static final class ClientTickEvent extends TickEvent {
        public ClientTickEvent(Phase phase) { super(phase); }
    }

    public static final class PlayerTickEvent extends TickEvent {
        public final Player player;
        public PlayerTickEvent(Phase phase, Player player) { super(phase); this.player = player; }
    }

    public static final class RenderTickEvent extends TickEvent {
        public final float renderTickTime;
        public RenderTickEvent(Phase phase, float partialTick) { super(phase); renderTickTime = partialTick; }
    }
}
