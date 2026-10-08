package com.tacz.guns.network;

import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.world.phys.Vec3;

/** Checks the exact native method chosen by GunAnimationStateContext against release arithmetic. */
public final class WalkDistanceFormulaTest {
    public static void main(String[] arguments) {
        ClientAvatarState avatar = new ClientAvatarState();
        float previous = 0, current = 0;
        int checks = 0;
        for (float movement : new float[] {0.6f, 0.15f, 0f, 1.2f}) {
            avatar.tick(Vec3.ZERO, Vec3.ZERO);
            previous = current;
            current += movement;
            avatar.addWalkDistance(movement);
            for (float partial : new float[] {0f, 0.25f, 0.5f, 1f}) {
                float expected = current + (current - previous) * partial;
                float actual = -avatar.getBackwardsInterpolatedWalkDistance(partial);
                if (Math.abs(expected - actual) > 0.00001f) throw new AssertionError("Walk animation phase changed");
                checks++;
            }
        }
        System.out.println("WalkDistanceFormulaTest: " + checks + " assertions passed");
    }
}
