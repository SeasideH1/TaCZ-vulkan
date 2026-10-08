/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.gui.overlay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
/** Fractional legacy HUD placement expressed through the native two-dimensional extraction pose. */
final class NativeHudDrawing {
    private NativeHudDrawing() {}
    static void text(GuiGraphicsExtractor graphics,Font font,String text,float x,float y,int color,boolean shadow) {
        graphics.pose().pushMatrix();graphics.pose().translate(x,y);graphics.text(font,text,0,0,color,shadow);graphics.pose().popMatrix();
    }
    static void text(GuiGraphicsExtractor graphics,Font font,String text,float x,float y,int color) {text(graphics,font,text,x,y,color,true);}
    static void text(GuiGraphicsExtractor graphics,Font font,Component text,float x,float y,int color,boolean shadow) {
        graphics.pose().pushMatrix();graphics.pose().translate(x,y);graphics.text(font,text,0,0,color,shadow);graphics.pose().popMatrix();
    }
}
