package com.tacz.guns.network;

import com.tacz.guns.network.message.BulletSpawnData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** Full release field order and fast-projectile precision against real target buffers. */
public final class BulletSpawnCodecTest {
    public static void main(String[] arguments) {
        int assertions = 0;
        for (boolean enabled : new boolean[] {false, true}) {
            BulletSpawnData original = new BulletSpawnData(14.25f, -167.5f, 80.125, -0.03125, -92.75,
                    42, Identifier.parse("tacz:556x45"), 0.0625f, enabled, !enabled, enabled,
                    3.5f, 11.25f, 200, 7.25f, 0.015625f, 3, !enabled,
                    Identifier.parse("tacz:m4a1"), Identifier.parse("tacz:m4a1_display"));
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                BulletSpawnData.encode(original, buffer);
                BulletSpawnData decoded = BulletSpawnData.decode(buffer);
                if (!original.equals(decoded)) throw new AssertionError("Spawn fields or precision changed");
                assertions++;
                if (buffer.isReadable()) throw new AssertionError("Spawn codec left trailing data");
                assertions++;
                BulletSpawnData.encode(original, buffer);
                if (buffer.readFloat() != 14.25f || buffer.readFloat() != -167.5f) throw new AssertionError("Rotation wire order changed");
                assertions++;
                if (buffer.readDouble() != 80.125 || buffer.readDouble() != -0.03125 || buffer.readDouble() != -92.75) throw new AssertionError("Velocity must remain double precision");
                assertions++;
                if (buffer.readInt() != 42 || !buffer.readIdentifier().equals(original.ammoId())) throw new AssertionError("Owner/ammo wire order changed");
                assertions++;
                if (buffer.readFloat() != original.gravity() || buffer.readBoolean() != enabled
                        || buffer.readBoolean() == enabled || buffer.readBoolean() != enabled) throw new AssertionError("Projectile flags changed");
                assertions++;
                if (buffer.readFloat() != original.explosionRadius() || buffer.readFloat() != original.explosionDamage()
                        || buffer.readInt() != original.life() || buffer.readFloat() != original.speed()
                        || buffer.readFloat() != original.friction() || buffer.readInt() != original.pierce()
                        || buffer.readBoolean() == enabled) throw new AssertionError("Ballistic fields changed");
                assertions++;
                if (!buffer.readIdentifier().equals(original.gunId()) || !buffer.readIdentifier().equals(original.gunDisplayId())
                        || buffer.isReadable()) throw new AssertionError("Gun/display wire order changed");
                assertions++;
            } finally {
                buffer.release();
            }
        }
        System.out.println("BulletSpawnCodecTest: " + assertions + " assertions passed");
    }
}
