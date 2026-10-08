/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.fabric.registry.FabricRegistry;
import com.tacz.guns.fabric.registry.RegistryHandle;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import com.mojang.serialization.MapCodec;

public final class ModParticles {
    public static final FabricRegistry<ParticleType<?>> PARTICLE_TYPES = FabricRegistry.create(BuiltInRegistries.PARTICLE_TYPE, "tacz");
    public static final RegistryHandle<ParticleType<BulletHoleOption>> BULLET_HOLE = PARTICLE_TYPES.register("bullet_hole", () -> new ParticleType<>(false) {
        @Override public MapCodec<BulletHoleOption> codec() { return BulletHoleOption.CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BulletHoleOption> streamCodec() { return BulletHoleOption.STREAM_CODEC; }
    });
}
