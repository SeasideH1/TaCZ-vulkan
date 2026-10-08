/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.item.nativeapi;
import com.mojang.serialization.MapCodec;
import com.tacz.guns.item.AmmoBoxItem;
import com.tacz.guns.mixin.client.ItemTintSourcesAccessor;
import com.tacz.guns.mixin.client.RangeSelectPropertiesAccessor;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Native item-model range selection and per-stack tint, retaining all nine official ammo-box variants. */
public final class NativeItemModelTypes {
    private static boolean registered;
    private NativeItemModelTypes() {}
    public static void register() {
        if(registered)return;
        RangeSelectPropertiesAccessor.tacz$idMapper().put(AmmoBoxItem.PROPERTY_NAME,AmmoBoxState.CODEC);
        ItemTintSourcesAccessor.tacz$idMapper().put(Identifier.fromNamespaceAndPath("tacz","ammo_box"),AmmoBoxTint.CODEC);
        registered=true;
    }
    private enum AmmoBoxState implements RangeSelectItemModelProperty {
        INSTANCE;
        static final MapCodec<AmmoBoxState> CODEC=MapCodec.unit(INSTANCE);
        @Override public float get(ItemStack stack,ClientLevel level,ItemOwner owner,int seed) {
            return AmmoBoxItem.getStatue(stack,level,owner instanceof LivingEntity living?living:null,seed);
        }
        @Override public MapCodec<? extends RangeSelectItemModelProperty> type(){return CODEC;}
    }
    private enum AmmoBoxTint implements ItemTintSource {
        INSTANCE;
        static final MapCodec<AmmoBoxTint> CODEC=MapCodec.unit(INSTANCE);
        @Override public int calculate(ItemStack stack,ClientLevel level,LivingEntity entity){return 0xFF000000|AmmoBoxItem.getColor(stack,0);}
        @Override public MapCodec<? extends ItemTintSource> type(){return CODEC;}
    }
}
