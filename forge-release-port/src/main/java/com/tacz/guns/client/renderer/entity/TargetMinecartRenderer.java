/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.nativeapi.GeometrySnapshot;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.entity.TargetMinecart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** Uses vanilla rail-state extraction, then submits the official replacement cart geometry only. */
public class TargetMinecartRenderer extends EntityRenderer<TargetMinecart,TargetMinecartRenderer.State> {
    public static final class State extends MinecartRenderState {
        GeometrySnapshot body=GeometrySnapshot.EMPTY,head=GeometrySnapshot.EMPTY;
        RenderType bodyMaterial,headMaterial;
    }
    private final MinecartRenderer railExtractor;
    public TargetMinecartRenderer(EntityRendererProvider.Context context) {
        super(context);railExtractor=new MinecartRenderer(context,ModelLayers.TNT_MINECART);shadowRadius=.25f;
    }
    public static Optional<BedrockModel> getModel(){return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MINECART_MODEL_LOCATION);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(TargetMinecart entity,State state,float partialTick) {
        railExtractor.extractRenderState(entity,state,partialTick);
        state.body=state.head=GeometrySnapshot.EMPTY;state.bodyMaterial=state.headMaterial=null;
        getModel().ifPresent(model->{
            var head=model.getNode("head");var head2=model.getNode("head2");if(head==null||head2==null)return;
            boolean visible1=head.visible,visible2=head2.visible;
            try {
                head.visible=head2.visible=false;
                PoseStack pose=new PoseStack();pose.translate(.5,1.875,.5);pose.scale(1.5f,1.5f,1.5f);
                pose.rotate(Axis.ZN.rotationDegrees(180));pose.rotate(Axis.YN.rotationDegrees(90));
                state.bodyMaterial=RenderTypes.entityTranslucent(InternalAssetLoader.TARGET_MINECART_TEXTURE_LOCATION);
                state.body=model.extractGeometry(pose,ItemDisplayContext.NONE,state.lightCoords,OverlayTexture.NO_OVERLAY);
                if(entity.getGameProfile()!=null) {
                    pose.translate(0,1,-4.5/16);
                    var profile=entity.getGameProfile();
                    var skin=Minecraft.getInstance().getSkinManager().get(profile).getNow(Optional.empty()).orElseGet(()->DefaultPlayerSkin.get(profile));
                    state.headMaterial=RenderTypes.entityTranslucentCull(skin.body().texturePath());
                    var vertices=new GeometrySnapshot.Builder();head.visible=true;
                    head.render(pose,ItemDisplayContext.NONE,vertices,state.lightCoords,OverlayTexture.NO_OVERLAY);
                    head2.visible=true;pose.translate(0,0,.01);
                    head2.render(pose,ItemDisplayContext.NONE,vertices,state.lightCoords,OverlayTexture.NO_OVERLAY);
                    state.head=vertices.build();
                }
            } finally {head.visible=visible1;head2.visible=visible2;}
        });
    }
    @Override public Vec3 getRenderOffset(State state){return railExtractor.getRenderOffset(state);}
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        super.submit(state,pose,collector,camera);
        if(state.bodyMaterial==null)return;
        pose.pushPose();
        long seed=state.offsetSeed;
        pose.translate(((((seed>>16)&7)+.5f)/8f-.5f)*.004f,((((seed>>20)&7)+.5f)/8f-.5f)*.004f,((((seed>>24)&7)+.5f)/8f-.5f)*.004f);
        applyRailTransform(state,pose);
        if(state.hurtTime>0)pose.rotateDegrees(Axis.XP,Mth.sin(state.hurtTime)*state.hurtTime*state.damageTime/10*state.hurtDir);
        pose.scale(.75f,.75f,.75f);pose.translate(-.5f,(state.displayOffset-8)/16f,.5f);pose.rotateDegrees(Axis.YP,90);
        collector.submitCustomGeometry(pose,state.bodyMaterial,state.body);
        if(state.headMaterial!=null)collector.submitCustomGeometry(pose,state.headMaterial,state.head);
        pose.popPose();
    }
    /** Snapshot-3 AbstractMinecartRenderer's new/legacy rail transforms, without its vanilla cart draw. */
    private static void applyRailTransform(State state,PoseStack pose) {
        if(state.isNewRender) {
            pose.rotateDegrees(Axis.YP,state.yRot);pose.rotateDegrees(Axis.ZP,-state.xRot);pose.translate(0,.375f,0);return;
        }
        float xRot=state.xRot,yRot=state.yRot;
        if(state.posOnRail!=null&&state.frontPos!=null&&state.backPos!=null) {
            pose.translate(state.posOnRail.x-state.x,(state.frontPos.y+state.backPos.y)/2-state.y,state.posOnRail.z-state.z);
            Vec3 direction=state.backPos.subtract(state.frontPos);
            if(direction.length()!=0) {
                direction=direction.normalize();yRot=(float)(Math.atan2(direction.z,direction.x)*180/Math.PI);xRot=(float)(Math.atan(direction.y)*73);
            }
        }
        pose.translate(0,.375f,0);pose.rotateDegrees(Axis.YP,180-yRot);pose.rotateDegrees(Axis.ZP,-xRot);
    }
}
