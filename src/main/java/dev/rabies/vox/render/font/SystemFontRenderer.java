package dev.rabies.vox.render.font;

import dev.rabies.vox.events.EventTiming;
import dev.rabies.vox.events.render.RenderTextEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;

public class SystemFontRenderer extends SystemFont {
    private final FontRenderer fontRenderer;
    private final int[] colorCodes = new int[32];

    public SystemFontRenderer(Font font) {
        super(font);
        fontRenderer = Minecraft.getMinecraft().fontRenderer;
        setUpColorCodes();
    }

    public void drawCenteredStringWithShadow(String text, double x, double y, int color) {
        drawStringWithShadow(text, x - (this.getStringWidth(text) / 2F), y, color);
    }

    public void drawCenteredString(String text, double x, double y, int color) {
        drawString(text, x - (this.getStringWidth(text) / 2F), y, new Color(color), false);
    }

    public final void drawStringWithShadow(String text, double x, double y, int color) {
        drawString(text, x + 0.6F, y + 0.6F, new Color(color), true);
        drawString(text, x, y, new Color(color), false);
    }

    public final void drawString(String text, double x, double y, int color) {
        drawString(text, x, y, new Color(color), false);
    }

    public void drawString(String text, double x, double y, Color color, boolean shadow) {
        if (text == null) return;
        RenderTextEvent renderTextEvent = new RenderTextEvent(EventTiming.POST, text);
        MinecraftForge.EVENT_BUS.post(renderTextEvent);
        text = renderTextEvent.getString();

        x -= 1;
        y -= 2;

        if (color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255 && color.getAlpha() == 32)
            color = new Color(255, 255, 255);
        if (color.getAlpha() < 4)
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        if (shadow)
            color = new Color(color.getRed() / 4, color.getGreen() / 4, color.getBlue() / 4, color.getAlpha());

        boolean strikethrough = false;
        boolean underline = false;
        x *= 2.0D;
        y *= 2.0D;
        GlStateManager.pushMatrix();
        GlStateManager.resetColor();
        GlStateManager.scale(0.5D, 0.5D, 0.5D);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
        GlStateManager.enableTexture2D();
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GlStateManager.bindTexture(getTexture().getGlTextureId());
        ArrayList<VanillaChar> vanillaChars = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            CharData charData = getCharData(c);
            if (c == '\u00A7') {
                int colorIndex = 21;
                try {
                    colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
                } catch (IndexOutOfBoundsException e) {
                    // invalid color code
                }
                if (colorIndex < 16) {
                    underline = false;
                    strikethrough = false;
                    GlStateManager.bindTexture(getTexture().getGlTextureId());
                    if (colorIndex < 0) colorIndex = 15;
                    if (shadow) colorIndex += 16;
                    int colorCode = this.colorCodes[colorIndex];
                    color = new Color((colorCode >> 16 & 0xFF), (colorCode >> 8 & 0xFF), (colorCode & 0xFF), color.getAlpha());
                    GlStateManager.color(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
                } else if (colorIndex == 18) {
                    strikethrough = true;
                } else if (colorIndex == 19) {
                    underline = true;
                } else if (colorIndex == 21) {
                    underline = false;
                    strikethrough = false;
                    GlStateManager.color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                    GlStateManager.bindTexture(getTexture().getGlTextureId());
                }
                i++;
            } else if (charData != null) {
                GlStateManager.glBegin(GL11.GL_TRIANGLES);
                drawChar(charData, (float) x, (float) y);
                GlStateManager.glEnd();
                if (strikethrough)
                    drawLine(x, y + (double) charData.height / 2, x + charData.width - getPaddingWidth(), y + (double) charData.height / 2, 1.0F);
                if (underline)
                    drawLine(x, y + charData.height - 2.0D, x + charData.width - getPaddingWidth(), y + charData.height - 2.0D, 1.0F);
                x += charData.width - getPaddingWidth();
            } else {
                vanillaChars.add(new VanillaChar(x, y, c, color.getRGB()));
                x += (fontRenderer.getStringWidth(Character.toString(c)) + getPaddingWidth()) * getFont().getSize() / 18D;
            }
        }
        GlStateManager.popMatrix();
        for (VanillaChar vc : vanillaChars) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(vc.x / 2, vc.y / 2, 0);
            GlStateManager.scale(getFont().getSize() / 18D, getFont().getSize() / 18D, 0);
            fontRenderer.drawString(Character.toString(vc.c), 1, 1, vc.color, shadow);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public double getStringWidth(String text) {
        if (text == null) return 0;
        RenderTextEvent renderTextEvent = new RenderTextEvent(EventTiming.POST, text);
        MinecraftForge.EVENT_BUS.post(renderTextEvent);
        text = renderTextEvent.getString();

        double width = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            CharData charData = getCharData(c);
            if (c == '\u00A7') {
                i++;
            } else if (charData != null) {
                width += charData.width - getPaddingWidth();
            } else {
                width += (fontRenderer.getStringWidth(Character.toString(c)) + getPaddingWidth()) * getFont().getSize() / 18D;
            }
        }

        return (int) (width / 2);
    }

    private void drawLine(double x, double y, double x1, double y1, float width) {
        GlStateManager.disableTexture2D();
        GlStateManager.glLineWidth(width);
        GlStateManager.glBegin(GL11.GL_LINES);
        GlStateManager.glVertex3f((float) x, (float) y, 0);
        GlStateManager.glVertex3f((float) x1, (float) y1, 0);
        GlStateManager.glEnd();
        GlStateManager.enableTexture2D();
    }

    private void setUpColorCodes() {
        for (int index = 0; index < 32; index++) {
            int noClue = (index >> 3 & 0x1) * 85;
            int red = (index >> 2 & 0x1) * 170 + noClue;
            int green = (index >> 1 & 0x1) * 170 + noClue;
            int blue = (index & 0x1) * 170 + noClue;

            if (index == 6) {
                red += 85;
            }
            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            this.colorCodes[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
        }
    }

    private static class VanillaChar {
        double x;
        double y;
        char c;
        int color;

        VanillaChar(double x, double y, char c, int color) {
            this.x = x;
            this.y = y;
            this.c = c;
            this.color = color;
        }
    }
}
