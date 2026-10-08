/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.nativeapi.GeometrySnapshot;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

public class TargetRenderer implements BlockEntityRenderer<TargetBlockEntity,TargetRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        GeometrySnapshot body=GeometrySnapshot.EMPTY,head=GeometrySnapshot.EMPTY;
        RenderType bodyMaterial,headMaterial;
    }
    public TargetRenderer(BlockEntityRendererProvider.Context context) {}
    public static Optional<BedrockModel> getModel(){return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MODEL_LOCATION);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(TargetBlockEntity entity,State state,float partialTick,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity,state,partialTick,camera,breaking);
        state.body=state.head=GeometrySnapshot.EMPTY;state.bodyMaterial=state.headMaterial=null;
        getModel().ifPresent(model->{
            var head=model.getNode("head");var upper=model.getNode("target_upper");
            if(head==null||upper==null)return;
            float oldRotation=upper.xRot;boolean oldVisible=head.visible;
            try {
                float degrees=-Mth.lerp(partialTick,entity.oRot,entity.rot);
                upper.xRot=(float)Math.toRadians(degrees);head.visible=false;
                PoseStack pose=new PoseStack();pose.translate(.5,.225,.5);
                pose.rotate(Axis.YN.rotationDegrees(entity.getBlockState().getValue(TargetBlock.FACING).get2DDataValue()*90));
                pose.rotate(Axis.ZN.rotationDegrees(180));pose.translate(0,-1.275,.0125);
                state.bodyMaterial=RenderTypes.entityTranslucent(InternalAssetLoader.TARGET_TEXTURE_LOCATION);
                state.body=model.extractGeometry(pose,ItemDisplayContext.NONE,state.lightCoords,OverlayTexture.NO_OVERLAY);
                if(entity.getOwner()!=null) {
                    pose.translate(0,1.25,0);pose.rotate(Axis.XP.rotationDegrees(degrees));
                    var profile=entity.getOwner();
                    var skin=Minecraft.getInstance().getSkinManager().get(profile).getNow(Optional.empty()).orElseGet(()->DefaultPlayerSkin.get(profile));
                    state.headMaterial=RenderTypes.entityCutoutCull(skin.body().texturePath());
                    head.visible=true;
                    var vertices=new GeometrySnapshot.Builder();
                    head.render(pose,ItemDisplayContext.NONE,vertices,state.lightCoords,OverlayTexture.NO_OVERLAY);
                    state.head=vertices.build();
                }
            } finally {upper.xRot=oldRotation;head.visible=oldVisible;}
        });
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        if(state.bodyMaterial!=null)collector.submitCustomGeometry(pose,state.bodyMaterial,state.body);
        if(state.headMaterial!=null)collector.submitCustomGeometry(pose,state.headMaterial,state.head);
    }
    @Override public int getViewDistance(){return RenderConfig.TARGET_RENDER_DISTANCE.get();}
    @Override public boolean shouldRenderOffScreen(){return true;}
}
