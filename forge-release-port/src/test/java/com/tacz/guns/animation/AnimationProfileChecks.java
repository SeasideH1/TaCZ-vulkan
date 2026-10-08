package com.tacz.guns.animation;

import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.client.ThirdPersonAnimationMode;
import com.tacz.guns.fabric.config.FabricConfigSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

/** Verifies profile configuration selection; rendered fallback/clip parity is a separate gate. */
public final class AnimationProfileChecks {
    private static int assertions;
    private static void check(boolean condition) {
        assertions++;
        if (!condition) throw new AssertionError("Check " + assertions);
    }
    public static void main(String[] args) throws Exception {
        check(!RenderConfig.isAuthoredThirdPersonEnabled());
        var builder = new FabricConfigSpec.Builder();
        RenderConfig.init(builder);
        Path root = Files.createTempDirectory(Path.of(args[0]), "animation-profile-");
        Path file = root.resolve("client.toml");
        try (var spec = builder.build()) {
            check(RenderConfig.THIRD_PERSON_ANIMATION_MODE.getDefault() == ThirdPersonAnimationMode.RELEASE_FALLBACK);
            check(!RenderConfig.isAuthoredThirdPersonEnabled());
            check(RenderConfig.THIRD_PERSON_ANIMATION_MODE.getPath().equals(List.of("render", "ThirdPersonAnimationMode")));
            check(RenderConfig.THIRD_PERSON_ANIMATION_MODE.getChoices().equals(List.of("RELEASE_FALLBACK", "AUTHORED_CLIPS")));
            spec.load(file);
            check(!RenderConfig.isAuthoredThirdPersonEnabled());
            check(Files.readString(file).contains("RELEASE_FALLBACK"));
            var authored = new LinkedHashMap<>(spec.snapshot());
            authored.put("render.ThirdPersonAnimationMode", "AUTHORED_CLIPS");
            spec.validateSnapshot(authored);
            check(!RenderConfig.isAuthoredThirdPersonEnabled());
            spec.saveSnapshot(authored);
            check(RenderConfig.isAuthoredThirdPersonEnabled());
            spec.reload();
            check(RenderConfig.isAuthoredThirdPersonEnabled());
            var invalid = new LinkedHashMap<>(spec.snapshot());
            invalid.put("render.ThirdPersonAnimationMode", "ALWAYS_ON");
            try { spec.saveSnapshot(invalid); throw new AssertionError("Unknown profile accepted"); }
            catch (IllegalArgumentException expected) { assertions++; }
            check(RenderConfig.isAuthoredThirdPersonEnabled());
            authored.put("render.ThirdPersonAnimationMode", "RELEASE_FALLBACK");
            spec.saveSnapshot(authored);
            check(!RenderConfig.isAuthoredThirdPersonEnabled());
            spec.reload();
            check(!RenderConfig.isAuthoredThirdPersonEnabled());
            // Existing unmodified release configuration has no added profile key.
            Files.writeString(file, "[render]\nEnableLaserFadeOut = false\n");
            spec.reload();
            check(!RenderConfig.isAuthoredThirdPersonEnabled());
            check(!RenderConfig.ENABLE_LASER_FADE_OUT.get());
        }
        System.out.println("PASS " + assertions + " minimal-release/authored animation profile assertions");
    }
}
