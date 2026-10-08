/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.event;

/** TACZ events are dispatched on the game thread that owns the action. */
public class Event {
    private boolean canceled;
    private Result result = Result.DEFAULT;

    public boolean isCancelable() { return getClass().isAnnotationPresent(Cancelable.class); }
    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean canceled) {
        if (!isCancelable()) throw new IllegalArgumentException("Event cannot be canceled: " + getClass().getName());
        this.canceled = canceled;
    }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = java.util.Objects.requireNonNull(result); }
    public enum Result { DENY, DEFAULT, ALLOW }
}
