/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.renderer.nativeapi.GeometrySnapshot;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.Optional;

/** Extracts all bullet and tracer geometry before rendering; submitted data never reads a live entity. */
public class EntityBulletRenderer extends EntityRenderer<EntityKineticBullet,EntityBulletRenderer.State> {
    public static final class State extends EntityRenderState {
        GeometrySnapshot ammo=GeometrySnapshot.EMPTY,tracer=GeometrySnapshot.EMPTY;
        RenderType ammoMaterial,tracerMaterial;
    }
    public EntityBulletRenderer(EntityRendererProvider.Context context){super(context);}
    public static Optional<BedrockModel> getModel(){return InternalAssetLoader.getBedrockModel(InternalAssetLoader.DEFAULT_BULLET_MODEL);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(EntityKineticBullet bullet,State state,float partialTicks) {
        super.extractRenderState(bullet,state,partialTicks);
        state.ammo=state.tracer=GeometrySnapshot.EMPTY;state.ammoMaterial=state.tracerMaterial=null;
        var display=TimelessAPI.getGunDisplay(bullet.getGunDisplayId(),bullet.getGunId());
        if(display.isEmpty())return;
        float[] tracerColor=bullet.getTracerColorOverride().orElse(display.get().getTracerColor());
        TimelessAPI.getClientAmmoIndex(bullet.getAmmoId()).ifPresent(index->{
            PoseStack pose=new PoseStack();
            if(index.getAmmoEntityModel()!=null&&index.getAmmoEntityTextureLocation()!=null) {
                pose.rotate(Axis.YP.rotationDegrees(Mth.lerp(partialTicks,bullet.yRotO,bullet.getYRot())-180));
                pose.rotate(Axis.XP.rotationDegrees(Mth.lerp(partialTicks,bullet.xRotO,bullet.getXRot())));
                pose.pushPose();pose.translate(0,1.5,0);pose.scale(-1,-1,1);
                state.ammoMaterial=RenderTypes.entityTranslucentCull(index.getAmmoEntityTextureLocation());
                state.ammo=index.getAmmoEntityModel().extractGeometry(pose,ItemDisplayContext.GROUND,state.lightCoords,OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            if(bullet.isTracerAmmo())extractTracer(bullet,state,tracerColor!=null?tracerColor:index.getTracerColor(),partialTicks,pose);
        });
    }
    private void extractTracer(EntityKineticBullet bullet,State state,float[] color,float partialTicks,PoseStack pose) {
        getModel().ifPresent(model->{
            var shooter=bullet.getOwner();if(shooter==null)return;
            boolean firstPerson=Minecraft.getInstance().options.getCameraType().isFirstPerson()&&shooter instanceof LocalPlayer;
            if(firstPerson&&!RenderConfig.FIRST_PERSON_BULLET_TRACER_ENABLE.get())return;
            pose.pushPose();
            Vec3 position=bullet.getPosition(partialTicks);
            double distance=position.distanceTo(shooter.getEyePosition(partialTicks));
            double trail=Math.min(.85*bullet.getDeltaMovement().length(),distance*.8);
            if(firstPerson) {
                var camera=Minecraft.getInstance().gameRenderer.mainCamera();
                Vector3f offset=bullet.getFirstPersonRenderOffset();
                if(offset==null) {
                    offset=new Vector3f(GunItemRendererWrapper.muzzleRenderOffset);
                    bullet.setCameraXRot(camera.xRot());bullet.setCameraYRot(camera.yRot());bullet.setFirstPersonRenderOffset(offset);
                }
                double reducer=Math.max(0,50-distance)/50;
                pose.rotate(Axis.YN.rotationDegrees(bullet.getCameraYRot()+180));pose.rotate(Axis.XN.rotationDegrees(bullet.getCameraXRot()));
                pose.translate(offset.x*reducer,offset.y*reducer,offset.z*reducer);
                pose.rotate(Axis.XP.rotationDegrees(bullet.getCameraXRot()));pose.rotate(Axis.YP.rotationDegrees(bullet.getCameraYRot()+180));
            }
            float width=.005f*bullet.getTracerSizeOverride()*(float)Math.max(1,distance/3.5);
            pose.rotate(Axis.YP.rotationDegrees(Mth.lerp(partialTicks,bullet.yRotO,bullet.getYRot())-180));
            pose.rotate(Axis.XP.rotationDegrees(Mth.lerp(partialTicks,bullet.xRotO,bullet.getXRot())));
            pose.translate(0,firstPerson?0:-.2,trail/2);pose.scale(width,width,(float)trail);
            if(bullet.tickCount>=5||position.distanceTo(shooter.getEyePosition())>2) {
                state.tracerMaterial=RenderTypes.energySwirl(InternalAssetLoader.DEFAULT_BULLET_TEXTURE,15,15);
                state.tracer=model.extractGeometry(pose,ItemDisplayContext.NONE,state.lightCoords,OverlayTexture.NO_OVERLAY,color[0],color[1],color[2],1);
            }
            pose.popPose();
        });
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        if(state.ammoMaterial!=null)collector.submitCustomGeometry(pose,state.ammoMaterial,state.ammo);
        if(state.tracerMaterial!=null)collector.submitCustomGeometry(pose,state.tracerMaterial,state.tracer);
    }
    @Override protected int getBlockLightLevel(EntityKineticBullet bullet,BlockPos position){return 15;}
    @Override public boolean shouldRender(EntityKineticBullet bullet,Frustum frustum,double x,double y,double z,float partialTicks) {
        AABB bounds=bullet.getBoundingBox().inflate(.5);
        if(bounds.hasNaN()||bounds.getSize()==0)bounds=new AABB(bullet.getX()-2,bullet.getY()-2,bullet.getZ()-2,bullet.getX()+2,bullet.getY()+2,bullet.getZ()+2);
        return frustum.isVisible(bounds);
    }
}
