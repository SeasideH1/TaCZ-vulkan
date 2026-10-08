package com.tacz.guns.compat.shouldersurfing;


public final class ShoulderSurfingCompat {
    private static final String MOD_ID = "shouldersurfing";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean showCrosshair() {
        if (INSTALLED) {
            return ShoulderSurfingCompatInner.showCrosshair();
        }
        return false;
    }

    /** Applies signed camera deltas; false lets the caller update the vanilla camera. */
    public static boolean applyCameraRecoil(float pitchDelta, float yawDelta) {
        return INSTALLED && ShoulderSurfingCompatInner.applyCameraRecoil(pitchDelta, yawDelta);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }
}
