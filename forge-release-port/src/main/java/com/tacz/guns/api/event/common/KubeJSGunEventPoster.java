/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.event.common;

import com.tacz.guns.api.event.Event;
import com.tacz.guns.fabric.compat.ScriptIntegrationHooks;

/** Legacy event contract; engine-specific implementations are supplied by optional modules. */
public interface KubeJSGunEventPoster<E extends Event> {
    default void postEventToKubeJS(E event) { ScriptIntegrationHooks.post(ScriptIntegrationHooks.Channel.COMMON, event); }
    default void postClientEventToKubeJS(E event) { ScriptIntegrationHooks.post(ScriptIntegrationHooks.Channel.CLIENT, event); }
    default void postServerEventToKubeJS(E event) { ScriptIntegrationHooks.post(ScriptIntegrationHooks.Channel.SERVER, event); }
}
