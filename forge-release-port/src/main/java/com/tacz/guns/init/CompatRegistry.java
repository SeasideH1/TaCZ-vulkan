/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.fabricmc.loader.api.FabricLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Optional integrations are separate modules using explicit Fabric entrypoints. */
public final class CompatRegistry {
    public static final String CLOTH_CONFIG = "cloth_config";
    public static final String OCULUS = "oculus";
    public static final String CARRY_ON_ID = "carryon";
    public static final String COMMON_ENTRYPOINT = "tacz:common_integration";
    public static final String CLIENT_ENTRYPOINT = "tacz:client_integration";
    public interface Integration {
        Set<String> supportedMods();
        void initialize();
    }
    private static boolean commonInitialized, clientInitialized;
    private CompatRegistry() { }
    public static synchronized void registerCommon() {
        if (commonInitialized) return;
        initialize(COMMON_ENTRYPOINT, List.of("kubejs", CARRY_ON_ID));
        commonInitialized = true;
    }
    public static synchronized void registerClient() {
        if (clientInitialized) return;
        initialize(CLIENT_ENTRYPOINT, List.of("acceleratedrendering", CLOTH_CONFIG, "controllable", "jei", OCULUS, "iris", "optifine", "shouldersurfing"));
        clientInitialized = true;
    }
    private static void initialize(String entrypoint, List<String> knownMods) {
        FabricLoader loader = FabricLoader.getInstance();
        Set<String> supported = new HashSet<>();
        for (Integration integration : loader.getEntrypoints(entrypoint, Integration.class)) {
            integration.initialize();
            supported.addAll(integration.supportedMods());
        }
        for (String mod : knownMods) {
            if (loader.isModLoaded(mod) && !supported.contains(mod)) {
                GunMod.LOGGER.warn("Detected {} but no compatible TACZ integration module is registered. Its preserved integration source is not part of the core artifact.", mod);
            }
        }
    }
    public static void checkModLoad(String modId, Runnable action) {
        if (FabricLoader.getInstance().isModLoaded(modId)) action.run();
    }
}
