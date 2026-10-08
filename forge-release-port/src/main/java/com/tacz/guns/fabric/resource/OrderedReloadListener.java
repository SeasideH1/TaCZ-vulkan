/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.resource;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Prepare in parallel, cross the outer barrier once, then apply in dependency order. */
public final class OrderedReloadListener implements PreparableReloadListener {
    private final List<PreparableReloadListener> listeners;
    private final Runnable beforeApply;
    public OrderedReloadListener(List<PreparableReloadListener> listeners, Runnable beforeApply) {
        this.listeners = List.copyOf(listeners);
        this.beforeApply = beforeApply;
    }
    @Override public void prepareSharedState(SharedState state) {
        listeners.forEach(listener -> listener.prepareSharedState(state));
    }
    @Override public CompletableFuture<Void> reload(SharedState state, Executor preparationExecutor,
                                                   PreparationBarrier outerBarrier, Executor applyExecutor) {
        int count = listeners.size();
        if (count == 0) return outerBarrier.wait(null).thenRunAsync(beforeApply, applyExecutor);
        @SuppressWarnings("unchecked") CompletableFuture<Void>[] ready = new CompletableFuture[count];
        @SuppressWarnings("unchecked") CompletableFuture<Void>[] applyGate = new CompletableFuture[count];
        @SuppressWarnings("unchecked") CompletableFuture<Void>[] complete = new CompletableFuture[count];
        for (int i = 0; i < count; i++) { ready[i] = new CompletableFuture<>(); applyGate[i] = new CompletableFuture<>(); }
        for (int i = 0; i < count; i++) {
            final int index = i;
            PreparationBarrier childBarrier = new PreparationBarrier() {
                @Override public <T> CompletableFuture<T> wait(T prepared) {
                    ready[index].complete(null);
                    return applyGate[index].thenApply(ignored -> prepared);
                }
            };
            try { complete[i] = listeners.get(i).reload(state, preparationExecutor, childBarrier, applyExecutor); }
            catch (Throwable failure) { complete[i] = CompletableFuture.failedFuture(failure); }
            complete[i].whenComplete((ignored, failure) -> {
                if (failure != null) {
                    for (CompletableFuture<Void> gate : applyGate) gate.completeExceptionally(failure);
                    for (CompletableFuture<Void> preparation : ready) preparation.completeExceptionally(failure);
                } else if (index + 1 < count) applyGate[index + 1].complete(null);
            });
        }
        CompletableFuture.allOf(ready).thenCompose(ignored -> outerBarrier.wait(null))
                .thenRunAsync(beforeApply, applyExecutor).whenComplete((ignored, failure) -> {
                    if (failure == null) applyGate[0].complete(null);
                    else for (CompletableFuture<Void> gate : applyGate) gate.completeExceptionally(failure);
                });
        return CompletableFuture.allOf(complete);
    }
}
