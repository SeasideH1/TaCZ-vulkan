/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IBlock;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.renderer.nativeapi.GeometrySnapshot;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** Modern extraction/submission path; mutable gun-pack models never cross the frame boundary. */
public class GunSmithTableRenderer implements BlockEntityRenderer<GunSmithTableBlockEntity,GunSmithTableRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        GeometrySnapshot geometry=GeometrySnapshot.EMPTY;
        RenderType material;
    }
    public GunSmithTableRenderer(BlockEntityRendererProvider.Context context) {}
    public Optional<ClientBlockIndex> getIndex(GunSmithTableBlockEntity entity) {
        Identifier id=entity.getId();
        return id==null||id.equals(DefaultAssets.EMPTY_BLOCK_ID)?Optional.empty():TimelessAPI.getClientBlockIndex(id);
    }
    public static Optional<ClientBlockIndex> getIndex(ItemStack stack) {
        if(stack.getItem() instanceof IBlock block) {
            Identifier id=block.getBlockId(stack);
            if(!id.equals(DefaultAssets.EMPTY_BLOCK_ID))return TimelessAPI.getClientBlockIndex(id);
        }
        return Optional.empty();
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(GunSmithTableBlockEntity entity,State state,float partialTick,Vec3 camera,
                                              ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity,state,partialTick,camera,breaking);
        state.geometry=GeometrySnapshot.EMPTY;state.material=null;
        getIndex(entity).ifPresent(index->{
            if(index.getModel()==null)return;
            var blockState=entity.getBlockState();
            if(!(blockState.getBlock() instanceof AbstractGunSmithTableBlock block)||!block.isRoot(blockState))return;
            PoseStack pose=new PoseStack();pose.translate(.5,1.5,.5);pose.rotate(Axis.ZN.rotationDegrees(180));
            pose.rotate(Axis.YN.rotationDegrees(block.parseRotation(blockState.getValue(AbstractGunSmithTableBlock.FACING))));
            state.material=RenderConfig.BLOCK_ENTITY_TRANSLUCENT.get()?RenderTypes.entityTranslucent(index.getTexture()):RenderTypes.entityCutoutCull(index.getTexture());
            state.geometry=index.getModel().extractGeometry(pose,ItemDisplayContext.NONE,state.lightCoords,OverlayTexture.NO_OVERLAY);
        });
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        if(state.material!=null&&!state.geometry.vertices().isEmpty())collector.submitCustomGeometry(pose,state.material,state.geometry);
    }
    @Override public boolean shouldRenderOffScreen(){return true;}
}
