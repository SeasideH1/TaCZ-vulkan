/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.event.server;

import com.tacz.guns.api.event.common.KubeJSGunEventPoster;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.tacz.guns.api.event.Event;

/**
 * 子弹击中方块时触发的事件，目前仅在服务端触发
 */
public class AmmoHitBlockEvent extends Event implements KubeJSGunEventPoster<AmmoHitBlockEvent> {
    private final Level level;
    private final BlockHitResult hitResult;
    private final BlockState state;
    private final EntityKineticBullet ammo;

    public AmmoHitBlockEvent(Level level, BlockHitResult hitResult, BlockState state, EntityKineticBullet ammo) {
        this.level = level;
        this.hitResult = hitResult;
        this.state = state;
        this.ammo = ammo;
        postServerEventToKubeJS(this);
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    public Level getLevel() {
        return level;
    }

    public BlockHitResult getHitResult() {
        return hitResult;
    }

    public BlockState getState() {
        return state;
    }

    public EntityKineticBullet getAmmo() {
        return ammo;
    }
}
