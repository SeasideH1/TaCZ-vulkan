/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.util;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.util.block.ProjectileExplosion;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

public class ExplodeUtil {
    public static void createExplosion(Entity owner, Entity exploder, float damage, float radius, boolean knockback, boolean destroy, Vec3 hitPos) {
        // 客户端不执行
        if (!(exploder.level() instanceof ServerLevel level)) {
            return;
        }
        // 依据配置文件读取方块破坏方式
        Explosion.BlockInteraction mode = Explosion.BlockInteraction.KEEP;
        if (destroy) {
            mode = Explosion.BlockInteraction.DESTROY;
        }
        // 创建爆炸
        ProjectileExplosion explosion = new ProjectileExplosion(level, owner, exploder, null, null, hitPos.x(), hitPos.y(), hitPos.z(), damage, radius, knockback, mode);
        // 监听 forge 事件
        if (com.tacz.guns.api.event.TaczEvents.BUS.post(new com.tacz.guns.api.event.server.ProjectileExplosionEvent.Start(explosion))) {
            return;
        }
        // 执行爆炸逻辑
        explosion.explode();
        explosion.finalizeExplosion(true);
        if (mode == Explosion.BlockInteraction.KEEP) {
            explosion.clearToBlow();
        }
        final Explosion.BlockInteraction finalMode = mode;
        // 客户端发包，发送爆炸相关信息
        level.players().stream().filter(player -> Mth.sqrt((float) player.distanceToSqr(hitPos)) < AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get()).forEach(player -> {
            ClientboundExplodePacket packet = new ClientboundExplodePacket(hitPos, radius, explosion.getToBlow().size(),
                    java.util.Optional.ofNullable(explosion.getHitPlayers().get(player)),
                    radius >= 2.0F && finalMode != Explosion.BlockInteraction.KEEP ? net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER : net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.util.random.WeightedList.of(), true);
            player.connection.send(packet);
        });
    }
}
