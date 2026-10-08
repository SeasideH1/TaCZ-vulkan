package com.tacz.guns.compat.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.model.Perspective;
import com.github.exopandora.shouldersurfing.client.InputHandler;

public class ShoulderSurfingCompatInner {
    public static boolean applyCameraRecoil(float pitchDelta, float yawDelta) {
        var instance = com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing.getInstance();
        if (!instance.isShoulderSurfing()) return false;
        var camera = instance.getCamera();
        camera.setXRot(camera.getXRot() + pitchDelta);
        camera.setYRot(camera.getYRot() + yawDelta);
        return true;
    }

    public static boolean showCrosshair() {
        Perspective current = Perspective.current();
        return current == Perspective.SHOULDER_SURFING && !InputHandler.FREE_LOOK.isDown();
    }
}
