/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.event;
import com.tacz.guns.api.event.Event;
public final class ComputeFovModifierEvent extends Event {
    private float modifier;
    public ComputeFovModifierEvent(float modifier){this.modifier=modifier;}
    public float getNewFovModifier(){return modifier;}public void setNewFovModifier(float value){modifier=value;}
}
