/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

/**
 * CPU reference for TACZ release 1.1.8-hotfix2's eight-bit scope stencil operations.
 * Source baseline: MCModderAnchor/TACZ b482eff8c94a733ac8d0910193fca3893954027c,
 * BedrockAttachmentModel and BedrockGunModel. GPL-3.0.
 *
 * This describes fragment selection, not rasterization. A passing CPU test does
 * not establish Vulkan rendering, depth coverage, or screenshot parity.
 */
public final class ScopeStencilReference {
    public static final int MASK = 0xff;
    public static final int APERTURE_SEGMENTS = 90;
    public static final float APERTURE_Z = -90.0f;
    public static final float APERTURE_RADIUS = 80.0f;
    public static final float APERTURE_CENTER_SCALE = 16.0f * 90.0f;

    private static final float[] SIN = new float[65536];
    static {
        for (int i = 0; i < SIN.length; i++) SIN[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
    }

    private ScopeStencilReference() {
    }

    /** GL_GREATER compares the reference on the left with the stored value. */
    public static int writeOcular(int stored, int ocularId, boolean depthPassed) {
        checkByte(stored);
        checkOcular(ocularId);
        return depthPassed && ocularId > stored ? ocularId : stored;
    }

    /** KEEP/KEEP/INVERT only changes fragments passing both tests. */
    public static int invertAperture(int stored, int ocularId, boolean covered, boolean depthPassed) {
        checkByte(stored);
        checkOcular(ocularId);
        return covered && depthPassed && stored == ocularId ? (~stored & MASK) : stored;
    }

    public static boolean scopeBodyVisible(int beforeAperture) {
        checkByte(beforeAperture);
        return beforeAperture == 0;
    }

    public static boolean blackoutVisible(int resolved, int ocularId) {
        checkByte(resolved);
        checkOcular(ocularId);
        return resolved == ocularId;
    }

    public static boolean scopeReticleVisible(int resolved, int ocularId) {
        checkByte(resolved);
        checkOcular(ocularId);
        return resolved == (~ocularId & MASK);
    }

    public static boolean sightReticleVisible(int resolved, int ocularId) {
        checkByte(resolved);
        checkOcular(ocularId);
        return resolved == ocularId;
    }

    public static boolean gunVisible(int resolved, boolean scope, boolean sight) {
        checkByte(resolved);
        if (scope && sight) {
            return 127 > resolved;
        }
        return !scope || resolved == 0;
    }

    /** The upstream fan, deliberately not a smooth-circle or erosion replacement. */
    public static float[] apertureFan(float ocularX, float ocularY, float radiusModifier, float aimingProgress) {
        if (!Float.isFinite(ocularX) || !Float.isFinite(ocularY)
                || !Float.isFinite(radiusModifier) || !Float.isFinite(aimingProgress)) {
            throw new IllegalArgumentException("Aperture parameters must be finite");
        }
        float centerX = ocularX * APERTURE_CENTER_SCALE;
        float centerY = ocularY * APERTURE_CENTER_SCALE;
        float radius = APERTURE_RADIUS * radiusModifier * aimingProgress;
        float[] vertices = new float[(APERTURE_SEGMENTS + 2) * 3];
        vertices[0] = centerX;
        vertices[1] = centerY;
        vertices[2] = APERTURE_Z;
        for (int i = 0; i <= APERTURE_SEGMENTS; i++) {
            // Forge 1.20.1 Mth lookup table and indexing, preserved bit-for-bit.
            float angle = i * ((float) Math.PI * 2.0f) / APERTURE_SEGMENTS;
            int offset = (i + 1) * 3;
            vertices[offset] = centerX + SIN[(int) (angle * 10430.378f + 16384.0f) & 65535] * radius;
            vertices[offset + 1] = centerY + SIN[(int) (angle * 10430.378f) & 65535] * radius;
            vertices[offset + 2] = APERTURE_Z;
        }
        return vertices;
    }

    private static void checkByte(int value) {
        if (value < 0 || value > MASK) {
            throw new IllegalArgumentException("Stencil value must be in [0,255]");
        }
    }

    private static void checkOcular(int ocularId) {
        // Upstream checks zero-based i > Byte.MAX_VALUE, so id 128 is accepted.
        if (ocularId < 1 || ocularId > 128) {
            throw new IllegalArgumentException("Ocular ID must be in [1,128]");
        }
    }
}
