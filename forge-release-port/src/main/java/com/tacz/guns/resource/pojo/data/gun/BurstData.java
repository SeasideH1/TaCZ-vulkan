/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class BurstData {
    @SerializedName("continuous_shoot")
    private boolean continuousShoot = false;

    @SerializedName("count")
    private int count = 3;

    @SerializedName("bpm")
    private double bpm = 200;

    @SerializedName("min_interval")
    private double minInterval = 1;

    public int getCount() {
        return count;
    }

    public double getBpm() {
        return bpm;
    }

    public double getMinInterval() {
        return minInterval;
    }

    public boolean isContinuousShoot() {
        return continuousShoot;
    }
}
