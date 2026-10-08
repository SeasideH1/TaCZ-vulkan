package com.tacz.guns.fabric;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Exact native constructor boundary; real damage/explosion-world behavior has a separate server probe. */
public final class ExplosionContextChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        Vec3 from=new Vec3(2,4,6),to=new Vec3(3,5,7);
        boolean oldRejected=false;
        try{new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,(Entity)null);}catch(NullPointerException expected){oldRejected=true;}
        check(oldRejected,"Reproduce removed nullable-entity constructor contract");
        ClipContext context=new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,CollisionContext.empty());
        check(context.getFrom().equals(from),"Ray origin retained");check(context.getTo().equals(to),"Ray endpoint retained");
        check(!context.getBlockShape(Blocks.STONE.defaultBlockState(),EmptyBlockGetter.INSTANCE,BlockPos.ZERO).isEmpty(),"Collider rays still see solid stone");
        check(context.getBlockShape(Blocks.AIR.defaultBlockState(),EmptyBlockGetter.INSTANCE,BlockPos.ZERO).isEmpty(),"Air does not occlude");
        check(context.getFluidShape(Fluids.WATER.defaultFluidState(),EmptyBlockGetter.INSTANCE,BlockPos.ZERO).isEmpty(),"Fluid.NONE still ignores water");
        System.out.println("EXPLOSION_CONTEXT_PASS assertions="+checks+" actual_blast_world_damage=SEPARATE_PROBE");
    }
}
