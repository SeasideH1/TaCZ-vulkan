/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.renderer.nativeapi;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.mixin.client.GuiGraphicsStateAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** Native offscreen GUI model preview only. Weapon optics never use this GUI facility. */
public final class NativeItemPreview {
    private static boolean registered;
    private NativeItemPreview() {}
    public static void register(){if(!registered){PictureInPictureRendererRegistry.register(context->new Renderer());registered=true;}}
    public static void extract(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y,int width,int height,
                               float centerX,float centerY,float yaw,float pitch,float scale) {
        if(width<=0||height<=0||scale<=0)return;
        Minecraft mc=Minecraft.getInstance();
        ItemStackRenderState item=new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(item,stack,ItemDisplayContext.FIXED,mc.level,null,0);
        Matrix3x2fc pose=new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor=new ScreenRectangle(x,y,width,height).transformMaxBounds(pose);
        State state=new State(item,x,y,x+width,y+height,scale,centerX,centerY,yaw,pitch,pose,scissor,
                PictureInPictureRenderState.getBounds(x,y,x+width,y+height,pose,scissor));
        ((GuiGraphicsStateAccessor)graphics).tacz$guiRenderState().addPicturesInPictureState(state);
    }
    public record State(ItemStackRenderState item,int x0,int y0,int x1,int y1,float scale,
                        float centerX,float centerY,float yaw,float pitch,Matrix3x2fc pose,
                        ScreenRectangle scissorArea,ScreenRectangle bounds) implements PictureInPictureRenderState {}
    public static final class Renderer extends PictureInPictureRenderer<State> {
        @Override public Class<State> getRenderStateClass(){return State.class;}
        @Override protected String getTextureLabel(){return "tacz_gui_rotating_item_preview";}
        @Override protected float getTranslateY(int height,int guiScale){return height/2f;}
        @Override protected void renderToTexture(State state,PoseStack pose,SubmitNodeCollector collector) {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
            pose.scale(1,-1,-1);
            pose.translate((state.centerX()-(state.x0()+state.x1())/2f)/state.scale(),
                    -(state.centerY()-(state.y0()+state.y1())/2f)/state.scale(),0);
            pose.rotateDegrees(Axis.XP,state.pitch());pose.rotateDegrees(Axis.YP,state.yaw());
            state.item().submit(pose,collector,LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);
        }
    }
}
