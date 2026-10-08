/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.other.IThirdPersonAnimation;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import java.util.function.BiFunction;
import java.util.List;

/** Immutable animation data bridge used by native humanoid and player render states. */
public final class NativeThirdPersonPose {
    public interface PlayerPose {
        Vec3 transformPosition(String bone,double x,double y,double z);
        Vec3 transformRotation(String bone,double x,double y,double z);
        boolean active();
        void applyRoot(PoseStack pose);
    }
    private record PartSnapshot(PartPose pose,boolean visible,boolean skipDraw) {
        static PartSnapshot capture(ModelPart part){return new PartSnapshot(part.storePose(),part.visible,part.skipDraw);}
        void apply(ModelPart part){part.loadPose(pose);part.visible=visible;part.skipDraw=skipDraw;}
    }
    private record LegacyPose(PartSnapshot head,PartSnapshot body,PartSnapshot leftArm,PartSnapshot rightArm) {
        static LegacyPose capture(HumanoidModel<?> model){return new LegacyPose(PartSnapshot.capture(model.head),PartSnapshot.capture(model.body),PartSnapshot.capture(model.leftArm),PartSnapshot.capture(model.rightArm));}
        void apply(HumanoidModel<?> model){head.apply(model.head);body.apply(model.body);leftArm.apply(model.leftArm);rightArm.apply(model.rightArm);}
    }
    /** The live entity is scoped to extraction and never retained in an immutable render state. */
    private static final class LegacyCapture {
        final LivingEntity entity;final HumanoidRenderState state;final IThirdPersonAnimation animation;final float aim;
        LegacyPose result;
        LegacyCapture(LivingEntity entity,HumanoidRenderState state,IThirdPersonAnimation animation,float aim){this.entity=entity;this.state=state;this.animation=animation;this.aim=aim;}
        void apply(HumanoidModel<?> model){
            if(aim<=0)animation.animateGunHold(entity,model.rightArm,model.leftArm,model.body,model.head);
            else animation.animateGunAim(entity,model.rightArm,model.leftArm,model.body,model.head,aim);
            result=LegacyPose.capture(model);
        }
    }
    private static final ThreadLocal<LegacyCapture> EXTRACTING=new ThreadLocal<>();
    private record Data(PlayerPose player,LegacyPose fallback) {}
    private static final RenderStateDataKey<Data> DATA=RenderStateDataKey.create(()->"tacz_third_person_pose");
    private static BiFunction<AbstractClientPlayer,Float,PlayerPose> playerExtractor;
    private NativeThirdPersonPose() {}
    public static void registerPlayerExtractor(BiFunction<AbstractClientPlayer,Float,PlayerPose> extractor){playerExtractor=java.util.Objects.requireNonNull(extractor);}
    public static void capture(LivingEntity entity,LivingEntityRenderState state,float partial,HumanoidModel<?> model) {
        String fallback=null;float aim=0;
        boolean paused=Minecraft.getInstance().isPaused();
        var stack=entity.getMainHandItem();
        if(stack.getItem() instanceof IGun && entity.getPose()!=Pose.SLEEPING&&!entity.onClimbable()&&!entity.isSwimming()&&entity.getPose()!=Pose.FALL_FLYING) {
            var display=TimelessAPI.getGunDisplay(stack);
            if(display.isPresent()) {
                if(PlayerAnimatorCompat.hasPlayerAnimator3rd(entity,display.get())) {
                    if(!paused)PlayerAnimatorCompat.playAnimation(entity,display.get(),state.walkAnimationSpeed);
                    if(entity instanceof AbstractClientPlayer&&playerExtractor==null)throw new IllegalStateException("Native player animation extractor is not registered");
                } else {fallback=display.get().getThirdPersonAnimation();aim=IGunOperator.fromLivingEntity(entity).getSynAimingProgress();}
            }
        } else if(!paused)PlayerAnimatorCompat.stopAllAnimation(entity);
        PlayerPose player=entity instanceof AbstractClientPlayer avatar&&playerExtractor!=null?playerExtractor.apply(avatar,partial):null;
        LegacyPose legacy=null;
        // Like the original manager, pause freezes selection and skips entity-based fallback callbacks.
        if(!paused&&fallback!=null&&model!=null&&state instanceof HumanoidRenderState humanoid) {
            legacy=captureLegacy(entity,humanoid,model,ThirdPersonManager.getAnimation(fallback),aim);
        }
        ((FabricRenderState)state).setData(DATA,new Data(player,legacy));
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static LegacyPose captureLegacy(LivingEntity entity,HumanoidRenderState state,HumanoidModel<?> model,IThirdPersonAnimation animation,float aim) {
        List<ModelPart> parts=model.allParts();
        List<PartSnapshot> saved=parts.stream().map(PartSnapshot::capture).toList();
        LegacyCapture previous=EXTRACTING.get(),capture=new LegacyCapture(entity,state,animation,aim);
        EXTRACTING.set(capture);
        try {
            // The callback runs at the same HumanoidModel.setupAnim TAIL as upstream, after the
            // actual renderer's native baseline and before any subclass-specific postprocessing.
            ((HumanoidModel)model).setupAnim(state);
            if(capture.result==null)throw new IllegalStateException("Native humanoid callback hook was not invoked");
            return capture.result;
        } finally {
            for(int i=0;i<parts.size();i++)saved.get(i).apply(parts.get(i));
            if(previous==null)EXTRACTING.remove();else EXTRACTING.set(previous);
        }
    }
    public static void applyHumanoid(HumanoidModel<?> model,HumanoidRenderState state) {
        LegacyCapture capture=EXTRACTING.get();
        if(capture!=null&&capture.state==state){capture.apply(model);return;}
        Data data=((FabricRenderState)state).getData(DATA);if(data==null)return;
        if(data.player()!=null&&data.player().active()) {
            apply(data.player(),"head",model.head);apply(data.player(),"torso",model.body);
            apply(data.player(),"leftArm",model.leftArm);apply(data.player(),"rightArm",model.rightArm);
            apply(data.player(),"leftLeg",model.leftLeg);apply(data.player(),"rightLeg",model.rightLeg);return;
        }
        if(data.fallback()!=null)data.fallback().apply(model);
    }
    private static void apply(PlayerPose snapshot,String name,ModelPart part) {
        Vec3 p=snapshot.transformPosition(name,part.x,part.y,part.z),r=snapshot.transformRotation(name,part.xRot,part.yRot,part.zRot);
        part.x=(float)p.x;part.y=(float)p.y;part.z=(float)p.z;part.xRot=(float)r.x;part.yRot=(float)r.y;part.zRot=(float)r.z;
    }
    public static void applyRoot(LivingEntityRenderState state,PoseStack pose){Data data=((FabricRenderState)state).getData(DATA);if(data!=null&&data.player()!=null&&data.player().active())data.player().applyRoot(pose);}
    public static void resetWearLayers(PlayerModel model,HumanoidRenderState state) {
        Data data=((FabricRenderState)state).getData(DATA);if(data==null||(data.fallback()==null&&(data.player()==null||!data.player().active())))return;
        // In snapshot3 sleeves/pants/jacket are children, so they inherit the parent transform once.
        model.leftSleeve.resetPose();model.rightSleeve.resetPose();model.leftPants.resetPose();model.rightPants.resetPose();model.jacket.resetPose();
    }
}
