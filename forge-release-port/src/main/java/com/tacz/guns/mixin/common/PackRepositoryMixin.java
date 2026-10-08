/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.common;

import com.tacz.guns.resource.GunPackLoader;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Shadow @Final @Mutable private Set<RepositorySource> sources;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void tacz$addGunPacks(RepositorySource[] originalSources, CallbackInfo ci) {
        for (RepositorySource source : originalSources) {
            PackType type = source instanceof FolderRepositorySource folder
                    ? ((FolderRepositorySourceAccessor) folder).tacz$getPackType()
                    : source instanceof ServerPacksSource ? PackType.SERVER_DATA : null;
            if (type != null) {
                Set<RepositorySource> augmented = new LinkedHashSet<>(sources);
                // Fresh-world preview repositories contain only ServerPacksSource, with no
                // world folder yet. Their initial data must already include the gunpack.
                augmented.add(GunPackLoader.forType(type));
                sources = augmented;
                return;
            }
        }
    }
}
