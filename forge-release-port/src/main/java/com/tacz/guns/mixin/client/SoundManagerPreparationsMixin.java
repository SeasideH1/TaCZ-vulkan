/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.client;

import com.google.common.collect.Maps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public class SoundManagerPreparationsMixin {
    private static final FileToIdConverter TACZ_SOUND_LISTER = new FileToIdConverter("tacz_sounds", ".ogg");

    @Shadow
    private Map<Identifier, Resource> soundCache;

    @Inject(method = "listResources", at = @At("TAIL"))
    private void tacz$includeTaczSoundResources(ResourceManager resourceManager, CallbackInfo ci) {
        Map<Identifier, Resource> merged = Maps.newHashMap(this.soundCache);
        merged.putAll(TACZ_SOUND_LISTER.listMatchingResources(resourceManager));
        this.soundCache = merged;
    }
}
