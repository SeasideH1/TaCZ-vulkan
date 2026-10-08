/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.particle.BulletHoleParticle;
import com.tacz.guns.init.ModParticles;
import net.fabricmc.api.EnvType;

public class ParticleFactoryRegistry {
    public static void init() {
        net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(ModParticles.BULLET_HOLE.get(), new BulletHoleParticle.Provider());
    }
}