/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.entity.sync;

import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.sync.core.IDataSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public class ModSerializers {
    private static ReloadState.StateType readState(FriendlyByteBuf buffer) {
        int ordinal = buffer.readInt();
        ReloadState.StateType[] values = ReloadState.StateType.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Invalid TaCZ reload state: " + ordinal);
        }
        return values[ordinal];
    }

    public static final IDataSerializer<ReloadState> RELOAD_STATE = new IDataSerializer<>() {
        @Override
        public ReloadState copy(ReloadState value) {
            return new ReloadState(value);
        }

        @Override
        public void write(FriendlyByteBuf buf, ReloadState value) {
            buf.writeInt(value.getStateType().ordinal());
            buf.writeLong(value.getCountDown());
        }

        @Override
        public ReloadState read(FriendlyByteBuf buf) {
            ReloadState reloadState = new ReloadState();
            reloadState.setStateType(readState(buf));
            reloadState.setCountDown(buf.readLong());
            return reloadState;
        }

        @Override
        public Tag write(ReloadState value) {
            CompoundTag compound = new CompoundTag();
            compound.putString("StateType", value.getStateType().toString());
            compound.putLong("CountDown", value.getCountDown());
            return compound;
        }

        @Override
        public ReloadState read(Tag nbt) {
            CompoundTag compound = (CompoundTag) nbt;
            try {
                ReloadState.StateType stateType = ReloadState.StateType.valueOf(compound.getStringOr("StateType", "NOT_RELOADING"));
                long countDown = compound.getLongOr("CountDown", ReloadState.NOT_RELOADING_COUNTDOWN);
                ReloadState reloadState = new ReloadState();
                reloadState.setStateType(stateType);
                reloadState.setCountDown(countDown);
                return reloadState;
            } catch (IllegalArgumentException ignore) {
            }
            return new ReloadState();
        }
    };
}
