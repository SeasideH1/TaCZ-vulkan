package com.tacz.guns.network;

import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.sync.ModSerializers;
import com.tacz.guns.entity.sync.core.IDataSerializer;
import com.tacz.guns.entity.sync.core.Serializers;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class SyncedSerializerTest {
    private static int assertions;

    public static void main(String[] arguments) {
        roundTrip(Serializers.BOOLEAN, true);
        roundTrip(Serializers.BYTE, (byte) -121);
        roundTrip(Serializers.SHORT, (short) -32_000);
        roundTrip(Serializers.INTEGER, -123_456);
        roundTrip(Serializers.LONG, Long.MIN_VALUE + 42);
        roundTrip(Serializers.FLOAT, 0.25f);
        roundTrip(Serializers.DOUBLE, Math.PI);
        roundTrip(Serializers.CHARACTER, '枪');
        roundTrip(Serializers.STRING, "TaCZ 枪械");
        roundTrip(Serializers.BLOCK_POS, new BlockPos(-15, 64, 250));
        roundTrip(Serializers.UUID, UUID.fromString("8e15d09a-a0dd-4cdc-90f3-e2f5a446f8b0"));
        roundTrip(Serializers.RESOURCE_LOCATION, Identifier.fromNamespaceAndPath("tacz", "ak47"));
        CompoundTag data = new CompoundTag();
        data.putString("GunId", "tacz:ak47");
        roundTrip(Serializers.TAG_COMPOUND, data);
        CompoundTag copied = Serializers.TAG_COMPOUND.copy(data);
        copied.putString("GunId", "tacz:m4a1");
        check(!copied.equals(data));
        for (ReloadState.StateType state : ReloadState.StateType.values()) {
            ReloadState reload = new ReloadState();
            reload.setStateType(state);
            reload.setCountDown(state == ReloadState.StateType.NOT_RELOADING ? -1 : 1400);
            roundTrip(ModSerializers.RELOAD_STATE, reload);
            ReloadState cloned = ModSerializers.RELOAD_STATE.copy(reload);
            cloned.setCountDown(500);
            check(!reload.equals(cloned));
        }
        for (int invalid : new int[] {-1, ReloadState.StateType.values().length, Integer.MAX_VALUE}) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeInt(invalid);
                buffer.writeLong(1);
                try {
                    ModSerializers.RELOAD_STATE.read(buffer);
                    throw new AssertionError("Invalid reload state accepted");
                } catch (IllegalArgumentException expected) {
                    assertions++;
                }
            } finally {
                buffer.release();
            }
        }
        System.out.println("SyncedSerializerTest: " + assertions + " assertions passed");
    }

    private static <T> void roundTrip(IDataSerializer<T> serializer, T value) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            serializer.write(buffer, value);
            check(value.equals(serializer.read(buffer)));
            check(!buffer.isReadable());
            check(value.equals(serializer.read(serializer.write(value))));
            var json = serializer.encode(value, JsonOps.INSTANCE).getOrThrow();
            check(value.equals(serializer.decode(JsonOps.INSTANCE, json).getOrThrow()));
            check(value.equals(serializer.copy(value)));
        } finally {
            buffer.release();
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("Synced serializer assertion " + (assertions + 1) + " failed");
        }
        assertions++;
    }
}
