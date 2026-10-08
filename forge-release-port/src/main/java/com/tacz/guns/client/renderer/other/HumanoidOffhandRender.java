/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.other;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.pojo.display.gun.LayerGunShow;
import com.tacz.guns.util.math.MathUtil;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** Holstered/offhand guns are resolved from inventories during extraction, never read during submission. */
public final class HumanoidOffhandRender {
    public record DisplayItem(ItemStackRenderState item,Matrix4fc transform) {}
    public static final RenderStateDataKey<List<DisplayItem>> ITEMS=RenderStateDataKey.create(()->"tacz_holstered_guns");
    private HumanoidOffhandRender() {}
    public static void extract(LivingEntity entity,LivingEntityRenderState state) {
        List<DisplayItem> display=new ArrayList<>();
        ItemStack offhand=entity.getOffhandItem();
        if(!offhand.isEmpty()&&offhand.getItem() instanceof IGun)TimelessAPI.getGunDisplay(offhand).ifPresent(data->add(entity,offhand,data.getOffhandShow(),display));
        if(entity instanceof Player player)for(int slot=0;slot<9;slot++) {
            if(slot==player.getInventory().getSelectedSlot())continue;
            ItemStack stack=player.getInventory().getItem(slot);final int index=slot;
            if(!stack.isEmpty()&&stack.getItem() instanceof IGun)TimelessAPI.getGunDisplay(stack).ifPresent(data->{
                var slots=data.getHotbarShow();if(slots!=null&&slots.containsKey(index))add(entity,stack,slots.get(index),display);
            });
        }
        ((FabricRenderState)state).setData(ITEMS,List.copyOf(display));
    }
    private static void add(LivingEntity entity,ItemStack stack,LayerGunShow show,List<DisplayItem> target) {
        if(show==null)return;
        Vector3f p=show.getPos(),r=show.getRotate(),s=show.getScale();Quaternionf rotation=new Quaternionf();
        MathUtil.toQuaternion((float)Math.toRadians(r.x),(float)Math.toRadians(r.y),(float)Math.toRadians(r.z),rotation);
        Matrix4fc transform=new Matrix4f().translation(-p.x/16f,1.5f-p.y/16f,p.z/16f).scale(-s.x,-s.y,s.z).rotate(rotation);
        ItemStackRenderState item=new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(item,stack,ItemDisplayContext.FIXED,entity.level(),entity,entity.getId());
        target.add(new DisplayItem(item,transform));
    }
    public static void submit(ArmedEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,int light) {
        for(DisplayItem item:((FabricRenderState)state).getDataOrDefault(ITEMS,List.of())) {
            pose.pushPose();pose.mulPose(item.transform());item.item().submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
        }
    }
}
