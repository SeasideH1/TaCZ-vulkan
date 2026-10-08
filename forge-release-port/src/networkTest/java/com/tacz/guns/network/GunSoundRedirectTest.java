package com.tacz.guns.network;

import com.tacz.guns.client.sound.GunSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.ConstantFloat;

/** Verifies redirection metadata with real target Sound objects; this is not an audio playback test. */
public final class GunSoundRedirectTest {
    public static void main(String[] arguments) throws Exception {
        Sound template = new Sound(Identifier.parse("tacz:template"), ConstantFloat.of(0.75f),
                ConstantFloat.of(1.1f), 3, Sound.Type.FILE, true, false, 64);
        Class<?> type = Class.forName(GunSoundInstance.class.getName() + "$TaczSound");
        var constructor = type.getDeclaredConstructor(Identifier.class, Identifier.class, Sound.class);
        constructor.setAccessible(true);
        Identifier logical = Identifier.parse("tacz:ak47/shoot");
        Identifier actual = Identifier.parse("tacz:tacz_sounds/ak47/shoot.ogg");
        Sound sound = (Sound) constructor.newInstance(logical, actual, template);
        int checks = 0;
        if (!sound.getLocation().equals(logical)) throw new AssertionError("Sound logical ID changed"); checks++;
        if (!sound.getPath().equals(actual)) throw new AssertionError("Gun-pack sound path lost"); checks++;
        if (sound.getVolume() != template.getVolume()) throw new AssertionError("Volume distribution lost"); checks++;
        if (sound.getPitch() != template.getPitch()) throw new AssertionError("Pitch distribution lost"); checks++;
        if (sound.getWeight() != 3) throw new AssertionError("Sound weight lost"); checks++;
        if (!sound.shouldStream()) throw new AssertionError("Streaming flag lost"); checks++;
        if (sound.shouldPreload()) throw new AssertionError("Unrequested preloading"); checks++;
        if (sound.getAttenuationDistance() != 64) throw new AssertionError("Attenuation distance lost"); checks++;
        if (sound.getType() != Sound.Type.FILE) throw new AssertionError("Redirect must resolve an actual file"); checks++;
        System.out.println("GunSoundRedirectTest: " + checks + " assertions passed");
    }
}
