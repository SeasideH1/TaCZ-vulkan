/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.client.event;

import com.tacz.guns.api.event.common.KubeJSGunEventPoster;
import com.tacz.guns.api.event.Event;

/**
 * 玩家交换主副手物品时触发该事件
 */
public class SwapItemWithOffHand extends Event implements KubeJSGunEventPoster<SwapItemWithOffHand> {
    public SwapItemWithOffHand() {
        postClientEventToKubeJS(this);
    }
}
