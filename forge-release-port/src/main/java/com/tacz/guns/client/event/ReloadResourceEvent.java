/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.event;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.server.packs.resources.ResourceManager;
/** Applied before gun-pack display indexing in the ordered native reload listener. */
public final class ReloadResourceEvent {
    private ReloadResourceEvent() {}
    public static void reload(ResourceManager manager){InternalAssetLoader.onResourceReload(manager);SoundPlayManager.clearSoundResourceCache();}
}
