package com.tacz.guns.animation;

import com.tacz.guns.client.animation.player.*;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.*;
import dev.kosmx.playerAnim.api.layered.modifier.*;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.core.util.*;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Actual official clip/evaluator comparisons, not an in-game player rendering verdict. */
public final class PlayerPoseSnapshotChecks {
    private static final List<String> BONES=List.of("body","head","torso","leftArm","rightArm","leftLeg","rightLeg");
    private static int assertions;
    private static void equal(Vec3f expected, Vec3 actual, String description) {
        assertions++;
        if(Math.abs(expected.getX()-actual.x)>0.00002 || Math.abs(expected.getY()-actual.y)>0.00002 || Math.abs(expected.getZ()-actual.z)>0.00002)
            throw new AssertionError(description+" expected="+expected+" actual="+actual);
    }
    private static Vec3 actual(PoseSnapshot snapshot,String bone,TransformType type,Vec3f base) {
        return switch(type) {
            case POSITION -> snapshot.transformPosition(bone,base.getX(),base.getY(),base.getZ());
            case ROTATION -> snapshot.transformRotation(bone,base.getX(),base.getY(),base.getZ());
            case BEND -> snapshot.transformBend(bone,base.getX(),base.getY());
        };
    }
    private static void compare(IAnimation oracle,PoseSnapshot captured,float partial,String description) {
        oracle.setupAnim(partial);
        for(String bone:BONES)for(TransformType type:TransformType.values())for(int i=0;i<5;i++) {
            float v=(i-2)*3.4f;
            Vec3f base=new Vec3f(v,v+0.31f,type==TransformType.BEND?0:v-0.47f);
            Vec3f expected=oracle.isActive()?oracle.get3DTransform(bone,type,partial,base):base;
            equal(expected,actual(captured,bone,type,base),description+" "+bone+" "+type+" "+i);
        }
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);List<KeyframeAnimation> clips=new ArrayList<>();int files=0;
        try(var paths=Files.list(root)) {
            for(Path path:paths.filter(p->p.toString().endsWith(".json")).sorted().toList()) {
                try(var in=Files.newInputStream(path)) {clips.addAll(AnimationSerializing.deserializeAnimation(in));files++;}
            }
        }
        for(KeyframeAnimation clip:clips) {
            var checkpoints=new TreeSet<Integer>(List.of(0,1,Math.max(0,clip.beginTick-1),clip.beginTick,clip.endTick,
                    Math.min(1000,Math.max(0,clip.stopTick-1)),Math.min(1000,clip.stopTick),Math.min(1000,clip.endTick*2+7)));
            for(int tick:checkpoints) {
                KeyframeAnimationPlayer player=new KeyframeAnimationPlayer(clip);
                for(int i=0;i<tick;i++)player.tick();
                for(float partial:new float[]{0,0.25f,0.9f}) {
                    SnapshotModifierLayer<IAnimation> layer=new SnapshotModifierLayer<>(player);
                    PoseSnapshot snapshot=PoseSnapshot.capture(List.of(layer),partial);
                    compare(player,snapshot,partial,"clip="+clip.extraData.get("name")+" tick="+tick);
                }
            }
        }
        if(clips.size()<3)throw new AssertionError("Official clips missing");
        AtomicReference<Vec3f> rotation=new AtomicReference<>(new Vec3f(.2f,-.5f,.7f));
        var adjustSource=(java.util.function.Function<String,Optional<AdjustmentModifier.PartModifier>>)bone ->
                Optional.of(new AdjustmentModifier.PartModifier(rotation.get(),new Vec3f(1,2,3)));
        ModifierLayer<IAnimation> oracle=new ModifierLayer<>();oracle.addModifierLast(new AdjustmentModifier(adjustSource));
        SnapshotModifierLayer<IAnimation> nativeLayer=new SnapshotModifierLayer<>(null,new SnapshotAdjustmentModifier(adjustSource));
        for(int tick=0;tick<45;tick++) {
            if(tick==0||tick==2||tick==5||tick==17) {
                KeyframeAnimation data=clips.get(tick%clips.size());
                oracle.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(8,Ease.INOUTSINE),new KeyframeAnimationPlayer(data));
                nativeLayer.replaceAnimationWithFade(SnapshotFadeModifier.standardFadeIn(8,Ease.INOUTSINE),new KeyframeAnimationPlayer(data));
            }
            if(tick==10) {
                oracle.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(8,Ease.INOUTSINE),null);
                nativeLayer.replaceAnimationWithFade(SnapshotFadeModifier.standardFadeIn(8,Ease.INOUTSINE),null);
            }
            for(float partial:new float[]{0,.35f,.8f}) {
                PoseSnapshot snapshot=PoseSnapshot.capture(List.of(nativeLayer),partial);
                compare(oracle,snapshot,partial,"fade/adjust tick="+tick);
                Vec3 before=snapshot.transformRotation("rightArm",4,-4,7);
                rotation.set(new Vec3f(9,8,7));
                Vec3 after=snapshot.transformRotation("rightArm",4,-4,7);
                assertions++;if(!before.equals(after))throw new AssertionError("Snapshot retained mutable adjustment source");
                rotation.set(new Vec3f(.2f,-.5f,.7f));
            }
            oracle.tick();nativeLayer.tick();
        }
        var layer=new SnapshotModifierLayer<IAnimation>(new KeyframeAnimationPlayer(clips.getFirst()));
        PoseSnapshot saved=PoseSnapshot.capture(List.of(layer),.5f);Vec3 before=saved.transformRotation("rightArm",.7f,-.6f,8);
        for(int i=0;i<100;i++)layer.tick();
        assertions++;if(!before.equals(saved.transformRotation("rightArm",.7f,-.6f,8)))throw new AssertionError("Snapshot retained mutable tick state");
        System.out.println("PASS "+assertions+" immutable pose comparisons across "+clips.size()+" clips / "+files+" official files");
    }
}
