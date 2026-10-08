/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.nativeapi.GeometrySnapshot;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.Optional;

public class StatueRenderer implements BlockEntityRenderer<StatueBlockEntity,StatueRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        GeometrySnapshot geometry=GeometrySnapshot.EMPTY;
        RenderType material;
        final ItemStackRenderState gun=new ItemStackRenderState();
        final Matrix4f gunTransform=new Matrix4f();
    }
    private final ItemModelResolver items;
    public StatueRenderer(BlockEntityRendererProvider.Context context){items=context.itemModelResolver();}
    public static Optional<BedrockModel> getModel(){return InternalAssetLoader.getBedrockModel(InternalAssetLoader.STATUE_MODEL_LOCATION);}
    public static Identifier getTextureLocation(){return InternalAssetLoader.STATUE_TEXTURE_LOCATION;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(StatueBlockEntity entity,State state,float partialTick,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity,state,partialTick,camera,breaking);
        state.geometry=GeometrySnapshot.EMPTY;state.material=null;state.gun.clear();
        if(entity.getLevel()==null)return;
        getModel().ifPresent(model->{
            var facing=entity.getBlockState().getValue(TargetBlock.FACING);
            PoseStack pose=new PoseStack();pose.translate(.5,1.5,.5);
            pose.rotate(Axis.YN.rotationDegrees((facing.get2DDataValue()+2)%4*90));pose.rotate(Axis.ZN.rotationDegrees(180));
            state.material=RenderConfig.BLOCK_ENTITY_TRANSLUCENT.get()?RenderTypes.entityTranslucent(getTextureLocation()):RenderTypes.entityCutoutCull(getTextureLocation());
            state.geometry=model.extractGeometry(pose,ItemDisplayContext.NONE,state.lightCoords,OverlayTexture.NO_OVERLAY);
            pose.scale(.5f,.5f,.5f);pose.translate(0,-.875,-1.2);pose.rotate(Axis.ZP.rotationDegrees(180));
            pose.translate(0,Math.sin(Util.getMillis()/500.0)*.1,0);state.gunTransform.set(pose.last().pose());
            Vec3 gunOrigin=Vec3.atLowerCornerOf(entity.getBlockPos()).add(pose.last().pose().m30(),pose.last().pose().m31(),pose.last().pose().m32());
            try(var distance=com.tacz.guns.util.RenderDistance.withDistanceSquared(gunOrigin.distanceToSqr(camera))) {
                items.updateForTopItem(state.gun,entity.getGunItem(),ItemDisplayContext.FIXED,entity.getLevel(),null,0);
            }
        });
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        if(state.material==null)return;
        collector.submitCustomGeometry(pose,state.material,state.geometry);
        pose.pushPose();pose.mulPose(state.gunTransform);
        state.gun.submit(pose,collector,LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
    @Override public int getViewDistance(){return RenderConfig.TARGET_RENDER_DISTANCE.get();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public boolean shouldRender(StatueBlockEntity entity,Vec3 camera){return Vec3.atCenterOf(entity.getBlockPos().above()).closerThan(camera,getViewDistance());}
}
