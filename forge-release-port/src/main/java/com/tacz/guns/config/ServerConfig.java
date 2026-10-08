/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import com.tacz.guns.fabric.config.FabricConfigSpec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

public final class ServerConfig {
    public static FabricConfigSpec SERVER_CONFIG_SPEC;
    private static final Gson GSON = new Gson();
    private static Map<String, Object> localSnapshot;
    private static Map<String, Object> defaults;

    public static synchronized FabricConfigSpec init() {
        if (SERVER_CONFIG_SPEC == null) {
            FabricConfigSpec.Builder builder = new FabricConfigSpec.Builder();
            SyncConfig.init(builder);
            SERVER_CONFIG_SPEC = builder.build();
            defaults = SERVER_CONFIG_SPEC.snapshot();
        }
        return SERVER_CONFIG_SPEC;
    }
    public static void loadWorld(MinecraftServer server) {
        init();
        Path target = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/tacz-server.toml");
        Path template = FabricLoader.getInstance().getGameDir().resolve("defaultconfigs/tacz-server.toml");
        if (!Files.exists(target) && Files.isRegularFile(template)) {
            try { Files.createDirectories(target.getParent()); Files.copy(template, target); }
            catch (IOException e) { throw new IllegalStateException("Cannot create server config " + target, e); }
        }
        SERVER_CONFIG_SPEC.load(target);
        refreshDerived();
    }
    public static String snapshot() { return GSON.toJson(init().snapshot()); }
    public static synchronized void applySnapshot(String json) {
        init();
        Map<String, Object> incoming = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        Map<String, Object> previous = SERVER_CONFIG_SPEC.snapshot();
        SERVER_CONFIG_SPEC.applySnapshot(incoming);
        try {
            refreshDerived();
        } catch (RuntimeException failure) {
            SERVER_CONFIG_SPEC.applySnapshot(previous);
            try { refreshDerived(); } catch (RuntimeException restoreFailure) { failure.addSuppressed(restoreFailure); }
            throw failure;
        }
        if (localSnapshot == null) localSnapshot = previous;
    }
    public static synchronized void clearRemote() {
        if (localSnapshot != null) {
            SERVER_CONFIG_SPEC.applySnapshot(localSnapshot);
            localSnapshot = null;
            refreshDerived();
        }
    }
    public static void unloadWorld() {
        init().close();
        SERVER_CONFIG_SPEC.applySnapshot(defaults);
        refreshDerived();
    }
    public static void refreshDerived() {
        HeadShotAABBConfigRead.init();
        InteractKeyConfigRead.init();
    }
}
