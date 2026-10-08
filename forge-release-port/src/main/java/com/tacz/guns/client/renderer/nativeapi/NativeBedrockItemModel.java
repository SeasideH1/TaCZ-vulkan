/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.mixin.client.ItemModelsAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.*;
import java.lang.Math;
import java.util.function.Consumer;

/** Custom native ItemModel: capture legacy-authored Bedrock geometry during item extraction. */
public record NativeBedrockItemModel(ItemTransforms transforms,Material.Baked particle,boolean blockLight,Matrix4fc localTransform) implements ItemModel {
    public static final Identifier TYPE=Identifier.fromNamespaceAndPath("tacz","bedrock");
    private static boolean registered;
    public static void register(){if(!registered){ItemModelsAccessor.tacz$idMapper().put(TYPE,Unbaked.CODEC);registered=true;}}
    @Override public void update(ItemStackRenderState state,ItemStack stack,ItemModelResolver resolver,ItemDisplayContext context,ClientLevel level,ItemOwner owner,int seed) {
        NativeItemRenderers.get(stack).ifPresent(renderer->{
            boolean oldFlash=com.tacz.guns.client.model.functional.MuzzleFlashRender.isSelf;
            boolean oldShell=com.tacz.guns.client.model.functional.ShellRender.isSelf;
            boolean heldByLocal=owner==net.minecraft.client.Minecraft.getInstance().player
                    && (context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
            NativeRenderQueue.Frame frame;
            try {
                com.tacz.guns.client.model.functional.MuzzleFlashRender.isSelf=heldByLocal;
                com.tacz.guns.client.model.functional.ShellRender.isSelf=heldByLocal;
                {
                    var camera=net.minecraft.client.Minecraft.getInstance().gameRenderer.mainCamera();
                    var scale=transforms.getTransform(context).scale();
                    double itemScale=Math.max(Math.abs(scale.x()),Math.max(Math.abs(scale.y()),Math.abs(scale.z())));
                    double localScale=Math.sqrt(localTransform.m00()*localTransform.m00()+localTransform.m01()*localTransform.m01()+localTransform.m02()*localTransform.m02()
                            +localTransform.m10()*localTransform.m10()+localTransform.m11()*localTransform.m11()+localTransform.m12()*localTransform.m12()
                            +localTransform.m20()*localTransform.m20()+localTransform.m21()*localTransform.m21()+localTransform.m22()*localTransform.m22());
                    if(owner instanceof net.minecraft.world.entity.LivingEntity living)itemScale*=living.getScale();
                    double offset=transforms.getTransform(context).translation().length()
                            +itemScale*Math.sqrt(localTransform.m30()*localTransform.m30()+localTransform.m31()*localTransform.m31()+localTransform.m32()*localTransform.m32());
                    try(var distance=com.tacz.guns.util.RenderDistance.withDistanceSquared(owner==null?0:owner.position().distanceToSqr(camera.position()));
                        var detail=com.tacz.guns.util.RenderDistance.withDetailLod(context,itemScale*localScale,offset)) {
                        frame=renderer.extract(stack,context,-1,-1);
                    }
                }
            } finally {
                com.tacz.guns.client.model.functional.MuzzleFlashRender.isSelf=oldFlash;
                com.tacz.guns.client.model.functional.ShellRender.isSelf=oldShell;
            }
            if(frame.commands().isEmpty())return;
            // GUI caches key on explicit model identity, not on the special renderer's fields.
            // Match vanilla SpecialModelWrapper and include per-stack captured geometry/materials.
            state.appendModelIdentityElement(this);
            state.appendModelIdentityElement(frame);
            var layer=state.newLayer();layer.setUsesBlockLight(blockLight);layer.setParticleMaterial(particle);
            layer.setItemTransform(transforms.getTransform(context));layer.setLocalTransform(localTransform);
            layer.setupSpecialModel(FrameRenderer.INSTANCE,frame);
            float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
            for(var command:frame.commands())if(command instanceof NativeRenderQueue.Draw draw)draw.geometry().includeBounds(bounds);
            // Eight conservative corners replace one allocated Vector3f per extracted vertex.
            Vector3fc[] snapshot=Float.isFinite(bounds[0])?new Vector3fc[8]:new Vector3fc[0];
            for(int i=0;i<snapshot.length;i++)snapshot[i]=new Vector3f(bounds[(i&1)==0?0:3],bounds[(i&2)==0?1:4],bounds[(i&4)==0?2:5]);
            layer.setExtents(()->snapshot);
        });
    }
    public record Unbaked(Identifier base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base)).apply(i,Unbaked::new));
        @Override public MapCodec<? extends ItemModel.Unbaked> type(){return CODEC;}
        @Override public void resolveDependencies(ResolvableModel.Resolver resolver){resolver.markDependency(base);}
        @Override public ItemModel bake(ItemModel.BakingContext context,Matrix4fc transform) {
            var baseModel=context.blockModelBaker().getModel(base);
            return new NativeBedrockItemModel(baseModel.getTopTransforms(),baseModel.resolveParticleMaterial(baseModel.getTopTextureSlots(),context.blockModelBaker()),
                    baseModel.getTopGuiLight().lightLikeBlock(),new Matrix4f(transform));
        }
    }
    private enum FrameRenderer implements SpecialModelRenderer<NativeRenderQueue.Frame> {
        INSTANCE;
        @Override public NativeRenderQueue.Frame extractArgument(ItemStack stack){return NativeItemRenderers.get(stack).orElseThrow().extract(stack,ItemDisplayContext.GUI,-1,-1);}
        @Override public void submit(NativeRenderQueue.Frame frame,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline) {
            // ItemTransform.apply already performs native block-to-item centering.
            frame.submit(pose,collector,light,overlay);
        }
        @Override public void getExtents(Consumer<Vector3fc> consumer){consumer.accept(new Vector3f(0));consumer.accept(new Vector3f(1));}
    }
}
