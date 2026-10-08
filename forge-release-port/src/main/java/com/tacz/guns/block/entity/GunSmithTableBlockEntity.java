/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.block.entity;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

public class GunSmithTableBlockEntity extends BlockEntity implements net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<Identifier> {
    public static final BlockEntityType<GunSmithTableBlockEntity> TYPE = net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(GunSmithTableBlockEntity::new,
            ModBlocks.GUN_SMITH_TABLE.get(),
            ModBlocks.WORKBENCH_111.get(),
            ModBlocks.WORKBENCH_121.get(),
            ModBlocks.WORKBENCH_211.get()
    ).build();

    private static final String ID_TAG = "BlockId";

    @Nullable
    private Identifier id = null;

    public GunSmithTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }

    public void setId(Identifier id) {
        this.id = id;
    }

    @Nullable
    public Identifier getId() {
        return id;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Environment(EnvType.CLIENT)
    public AABB getRenderBoundingBox() {
        return new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(worldPosition.offset(-2, 0, -2)), net.minecraft.world.phys.Vec3.atLowerCornerOf(worldPosition.offset(2, 1, 2)));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Gun Smith Table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GunSmithTableMenu(id, inventory, getId());
    }

    @Override
    public Identifier getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
        return id == null ? DefaultAssets.DEFAULT_BLOCK_ID : id;
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        this.id = Identifier.tryParse(input.getStringOr(ID_TAG, DefaultAssets.DEFAULT_BLOCK_ID.toString()));
        if (this.id == null) this.id = DefaultAssets.DEFAULT_BLOCK_ID;
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        if (id != null) output.putString(ID_TAG, id.toString());
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
