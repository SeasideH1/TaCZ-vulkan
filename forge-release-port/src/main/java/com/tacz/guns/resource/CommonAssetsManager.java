/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.resource;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tacz.guns.api.vmlib.LuaGunLogicConstant;
import com.tacz.guns.api.vmlib.LuaLibrary;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.manager.*;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.Ignite;
import com.tacz.guns.resource.pojo.data.loot.LootTableInjection;
import com.tacz.guns.resource.serialize.*;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.phys.Vec3;
import com.tacz.guns.api.event.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;

import java.util.*;
import java.util.function.Consumer;

public class CommonAssetsManager implements ICommonResourceProvider {
    private static volatile CommonAssetsManager INSTANCE;
    private static final net.fabricmc.fabric.api.resource.v1.DataResourceStore.Key<CommonAssetsManager> RESOURCE_KEY =
            new net.fabricmc.fabric.api.resource.v1.DataResourceStore.Key<>();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new com.tacz.guns.resource.serialize.IdentifierSerializer())
            .registerTypeAdapter(Pair.class, new PairSerializer())
            .registerTypeAdapter(GunSmithTableIngredient.class, new GunSmithTableIngredientSerializer())
            .registerTypeAdapter(GunSmithTableResult.class, new GunSmithTableResultSerializer())
            .registerTypeAdapter(ExtraDamage.DistanceDamagePair.class, new DistanceDamagePairSerializer())
            .registerTypeAdapter(Vec3.class, new Vec3Serializer())
            .registerTypeAdapter(Ignite.class, new IgniteSerializer())
            .registerTypeAdapter(RecipeFilter.class, new RecipeFilter.Deserializer())
            .registerTypeAdapter(CommonGunIndex.class, new CommonGunIndexSerializer())
            .registerTypeAdapter(CommonAmmoIndex.class, new CommonAmmoIndexSerializer())
            .registerTypeAdapter(CommonAttachmentIndex.class, new CommonAttachmentIndexSerializer())
            .registerTypeAdapter(CommonBlockIndex.class, new CommonBlockIndexSerializer())
            .registerTypeAdapter(TabConfig.class, new TabConfig.Deserializer())
            .create();

    private final net.minecraft.core.HolderLookup.Provider registries;
    public CommonAssetsManager() { this(net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY)); }
    public CommonAssetsManager(net.minecraft.core.HolderLookup.Provider registries) { this.registries = registries; }

    private final List<INetworkCacheReloadListener> listeners = new ArrayList<>();
    private CommonDataManager<GunData> gunData;
    private CommonDataManager<AttachmentData> attachmentData;
    private CommonDataManager<BlockData> blockData;
    private CommonDataManager<CommonAmmoIndex> ammoIndex;
    private CommonDataManager<CommonGunIndex> gunIndex;
    private CommonDataManager<CommonAttachmentIndex> attachmentIndex;
    private CommonDataManager<CommonBlockIndex> blockIndex;
    private RecipeFilterManager recipeFilterManager;
    private LootInjectionManager lootInjectionManager;

    private AttachmentsTagManager attachmentsTagManager;
    List<LuaLibrary> libList = List.of(new LuaGunLogicConstant());
    private final ScriptManager scriptManager = new ScriptManager(new FileToIdConverter("scripts", ".lua"), libList);

    public void reloadAndRegister(Consumer<PreparableReloadListener> register) {
        // 这里会顺序重载，所以需要把index这种依赖data的放在后面
        gunData = register(new CommonDataManager<>(DataType.GUN_DATA, GunData.class, GSON, "data/guns", "GunDataLoader"));
        attachmentData = register(new AttachmentDataManager());
        attachmentsTagManager = register(new AttachmentsTagManager());
        recipeFilterManager = register(new RecipeFilterManager());
        lootInjectionManager = new LootInjectionManager(registries);
        register.accept(lootInjectionManager);
        blockData = register(new CommonDataManager<>(DataType.BLOCK_DATA, BlockData.class, GSON, "data/blocks", "BlockDataLoader"));
        register.accept(scriptManager);

        ammoIndex = register(new CommonDataManager<>(DataType.AMMO_INDEX, CommonAmmoIndex.class, GSON, "index/ammo", "AmmoIndexLoader"));
        gunIndex = register(new CommonDataManager<>(DataType.GUN_INDEX, CommonGunIndex.class, GSON, "index/guns", "GunIndexLoader"));
        attachmentIndex = register(new CommonDataManager<>(DataType.ATTACHMENT_INDEX, CommonAttachmentIndex.class, GSON, "index/attachments", "AttachmentIndexLoader"));
        blockIndex = register(new CommonDataManager<>(DataType.BLOCK_INDEX, CommonBlockIndex.class, GSON, "index/blocks", "BlockIndexLoader"));

        listeners.forEach(register);
        register.accept((sharedState, backgroundExecutor, barrier, gameExecutor) -> {
            return barrier
                    .wait(Void.TYPE)
                    .thenRunAsync(AllowAttachmentTagMatcher::resetCache, gameExecutor);
        });
    }

    private <T extends INetworkCacheReloadListener> T register(T listener) {
        listeners.add(listener);
        return listener;
    }

    public Map<DataType, Map<Identifier, String>> getNetworkCache() {
        ImmutableMap.Builder<DataType, Map<Identifier, String>> builder = ImmutableMap.builder();
        for (INetworkCacheReloadListener listener : listeners) {
            builder.put(listener.getType(), listener.getNetworkCache());
        }
        return builder.build();
    }

    @Nullable
    @Override
    public GunData getGunData(Identifier id) {
        return gunData.getData(id);
    }

    @Nullable
    @Override
    public AttachmentData getAttachmentData(Identifier id) {
        return attachmentData.getData(id);
    }

    @Nullable
    @Override
    public BlockData getBlockData(Identifier id) {
        return blockData.getData(id);
    }

    @Override
    @Nullable
    public RecipeFilter getRecipeFilter(Identifier id) {
        return recipeFilterManager.getFilter(id);
    }

    public List<LootTableInjection> getLootTableInjections(Identifier lootTable) {
        if (lootInjectionManager == null) {
            return List.of();
        }
        return lootInjectionManager.getInjections(lootTable);
    }

    @Nullable
    @Override
    public CommonGunIndex getGunIndex(Identifier gunId) {
        return gunIndex.getData(gunId);
    }

    @Override
    public Set<Map.Entry<Identifier, CommonGunIndex>> getAllGuns() {
        return gunIndex.getAllData().entrySet();
    }

    @Nullable
    @Override
    public CommonAmmoIndex getAmmoIndex(Identifier ammoId) {
        return ammoIndex.getData(ammoId);
    }

    @Override
    public Set<Map.Entry<Identifier, CommonAmmoIndex>> getAllAmmos() {
        return ammoIndex.getAllData().entrySet();
    }

    @Nullable
    @Override
    public CommonAttachmentIndex getAttachmentIndex(Identifier attachmentId) {
        return attachmentIndex.getData(attachmentId);
    }

    @Override
    public Set<Map.Entry<Identifier, CommonAttachmentIndex>> getAllAttachments() {
        return attachmentIndex.getAllData().entrySet();
    }

    @Override
    public LuaTable getScript(Identifier scriptId) {
        return scriptManager.getScript(scriptId);
    }

    @Nullable
    @Override
    public CommonBlockIndex getBlockIndex(Identifier blockId) {
        return blockIndex.getData(blockId);
    }

    @Override
    public Set<Map.Entry<Identifier, CommonBlockIndex>> getAllBlocks() {
        return blockIndex.getAllData().entrySet();
    }

    @Override
    public Set<String> getAttachmentTags(Identifier registryName) {
        return attachmentsTagManager.getAttachmentTags(registryName);
    }

    @Override
    public Set<String> getAllowAttachmentTags(Identifier registryName) {
        return attachmentsTagManager.getAllowAttachmentTags(registryName);
    }

    /**
     * 获取实例<br/>
     * 实例仅当内置服务器/专用服务器启动时才会被创建<br/>
     * 当客户端正连接到多人游戏时，该方法将返回 null
     * @return CommonAssetsManger实例
     */
    @Nullable
    public static CommonAssetsManager getInstance() {
        return INSTANCE;
    }

    public static void clearInstance() {
        INSTANCE = null;
    }

    /**
     * 根据当前环境选择合适的缓存<br/>
     * 当前环境为单人游戏或多人游戏的服务端时，返回CommonAssetsManger实例<br/>
     * 当前环境为多人游戏的客户端时，返回CommonNetworkCache实例
     * @return ICommonResourceProvider实例
     */
    public static ICommonResourceProvider get() {
        return INSTANCE == null ? CommonNetworkCache.INSTANCE : INSTANCE;
    }

    public RecipeManager recipeManager;
    private static MinecraftServer currentServer;

    /** Registers one ordered factory per server resource reload, including initial startup. */
    public static void registerFabricReloaders() {
        net.fabricmc.fabric.api.resource.v1.DataResourceLoader.get().registerReloadListener(
                Identifier.fromNamespaceAndPath("tacz", "common_assets"), registries -> {
                    CommonAssetsManager manager = new CommonAssetsManager(registries);
                    List<PreparableReloadListener> reloaders = new ArrayList<>();
                    manager.reloadAndRegister(reloaders::add);
                    java.util.concurrent.atomic.AtomicReference<CommonAssetsManager> previous = new java.util.concurrent.atomic.AtomicReference<>();
                    var ordered = new com.tacz.guns.fabric.resource.OrderedReloadListener(reloaders, () -> {
                        previous.set(INSTANCE);
                        INSTANCE = manager;
                    });
                    return new PreparableReloadListener() {
                        @Override public void prepareSharedState(SharedState state) { ordered.prepareSharedState(state); }
                        @Override public java.util.concurrent.CompletableFuture<Void> reload(SharedState state, java.util.concurrent.Executor background,
                                PreparationBarrier barrier, java.util.concurrent.Executor game) {
                            return ordered.reload(state, background, barrier, game)
                                    .thenRunAsync(() -> state.get(net.fabricmc.fabric.api.resource.v1.DataResourceLoader.DATA_RESOURCE_STORE_KEY)
                                            .put(RESOURCE_KEY, manager), game)
                                    .whenCompleteAsync((ignored, failure) -> {
                                if (failure != null && INSTANCE == manager) {
                                    INSTANCE = previous.get();
                                    AllowAttachmentTagMatcher.resetCache();
                                }
                            }, game);
                        }
                    };
                });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            reconcileCommittedResources(server);
            initializeRecipes(server);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            // A different vanilla listener can fail after our apply phase completed. Follow
            // the data store actually retained by the server, including overlapping reloads.
            reconcileCommittedResources(server);
            if (success) initializeRecipes(server);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            reconcileCommittedResources(player.level().getServer());
            if (INSTANCE != null) {
                // During /reload Fabric sends this callback inside PlayerList.reloadResources,
                // before END_DATA_PACK_RELOAD. Bind and initialize the replacement recipes here
                // rather than synchronizing an empty cache from the newly applied manager.
                initializeRecipes(player.level().getServer());
                NetworkHandler.sendToClientPlayer(new ServerMessageSyncGunPack(INSTANCE.getNetworkCache()), player);
                List<GunSmithTableRecipe> recipes = INSTANCE.recipeManager == null ? List.of() : INSTANCE.recipeManager.getRecipes().stream()
                        .map(holder -> holder.value()).filter(GunSmithTableRecipe.class::isInstance).map(GunSmithTableRecipe.class::cast).toList();
                NetworkHandler.sendToClientPlayer(new com.tacz.guns.network.message.ServerMessageSyncTableRecipes(recipes), player);
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            clearInstance();
            currentServer = null;
        });
    }

    private static void reconcileCommittedResources(MinecraftServer server) {
        // Fresh-world creation opens a new ResourceManager while reusing the preview's
        // ReloadableServerResources. Fabric's public store follows that data-manager lifetime,
        // unlike an association keyed by the replaceable resource-reader object.
        CommonAssetsManager committed = ((net.fabricmc.fabric.api.resource.v1.DataResourceStore) server).getOrThrow(RESOURCE_KEY);
        if (INSTANCE != committed) {
            INSTANCE = committed;
            AllowAttachmentTagMatcher.resetCache();
        }
    }

    private static void initializeRecipes(MinecraftServer server) {
        if (INSTANCE == null) return;
        RecipeManager recipes = server.getRecipeManager();
        if (INSTANCE.recipeManager == recipes) return;
        int tableRecipes = 0;
        for (var holder : recipes.getRecipes()) {
            if (holder.value() instanceof GunSmithTableRecipe recipe) {
                recipe.setId(holder.id().identifier());
                recipe.init();
                tableRecipes++;
            }
        }
        INSTANCE.recipeManager = recipes;
        com.tacz.guns.GunMod.LOGGER.info("TACZ server assets ready: {} guns, {} attachments, {} ammo types, {} gunsmith recipes; selected packs={}",
                INSTANCE.getAllGuns().size(), INSTANCE.getAllAttachments().size(), INSTANCE.getAllAmmos().size(),
                tableRecipes, server.getPackRepository().getSelectedIds());
    }

    public static java.util.concurrent.CompletableFuture<Void> reloadAllPack() {
        MinecraftServer server = currentServer;
        if (server == null) return java.util.concurrent.CompletableFuture.completedFuture(null);
        java.util.concurrent.CompletableFuture<Void> result = new java.util.concurrent.CompletableFuture<>();
        server.execute(() -> {
            try {
                PackRepository repository = server.getPackRepository();
                repository.reload();
                server.reloadResources(repository.getSelectedIds()).whenComplete((ignored, failure) -> {
                    if (failure == null) result.complete(null);
                    else result.completeExceptionally(failure);
                });
            } catch (Throwable failure) { result.completeExceptionally(failure); }
        });
        return result;
    }
}
