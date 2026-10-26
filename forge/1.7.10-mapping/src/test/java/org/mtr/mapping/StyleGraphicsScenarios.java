package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.LegacyStyledText;
import org.mtr.mapping.mapper.TextHelper;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

public final class StyleGraphicsScenarios {
    public static void run(Minecraft client) throws Exception {
        FontRenderer font = client.fontRenderer;
        int[] palette = ReflectionHelper.getPrivateValue(FontRenderer.class, font, "colorCode", "field_78285_g");
        int[] original = palette.clone();
        int rgb = 0x1278AB;
        MutableText text = TextHelper.setStyle(TextHelper.literal("MTR"), Style.getEmptyMapped().withColor(TextColor.fromRgb(rgb)))
            .formatted(TextFormatting.BOLD).formatted(TextFormatting.ITALIC).formatted(TextFormatting.UNDERLINE)
            .formatted(TextFormatting.OBFUSCATED).formatted(TextFormatting.STRIKETHROUGH);
        font.fontRandom.setSeed(1349);
        clear(); font.drawString("§l§o§n§k§mMTR§r", 4, 4, 0xFF000000 | rgb, true);
        byte[] expected = pixels();
        font.fontRandom.setSeed(1349);
        clear(); GraphicsHolder.createInstanceSafe(graphics -> graphics.drawText(text, 4, 4, -1, true, GraphicsHolder.getDefaultLight()));
        assertArrayEquals(expected, pixels(), "Exact RGB, shadow and all native decorations must reach the native glyph pixels");
        assertArrayEquals(original, palette, "Styled text must not modify the shared native palette");

        boolean previousBidi = font.getBidiFlag();
        try {
            font.setBidiFlag(true);
            MutableText rtl = TextHelper.setStyle(TextHelper.literal("אבג abc"), Style.getEmptyMapped().withColor(TextColor.fromRgb(rgb)));
            try {
                palette[15] = rgb; palette[31] = (rgb & 0xFCFCFC) >> 2;
                clear(); font.drawString("§fאבג abc§r", 4, 4, -1, true); expected = pixels();
            } finally { System.arraycopy(original, 0, palette, 0, original.length); }
            clear(); LegacyStyledText.draw(font, rtl.data, rtl.data.getFormattedText(), 4, 4, -1, true);
            assertArrayEquals(expected, pixels(), "RGB sidecars must follow the actual native whole-string bidi permutation");
        } finally { font.setBidiFlag(previousBidi); }

        MutableText marker = TextHelper.setStyle(TextHelper.literal("-"), Style.getEmptyMapped().withColor(TextColor.fromRgb(rgb)));
        MutableText route = TextHelper.literal(" MTR").formatted(TextFormatting.WHITE);
        MutableText combined = TextHelper.append(marker, route);
        clear(); font.drawString("-", 4, 4, 0xFF000000 | rgb, true);
        font.drawString(" MTR", 4 + font.getStringWidth("-"), 4, -1, true); expected = pixels();
        clear(); LegacyStyledText.draw(font, combined.data, combined.data.getFormattedText(), 4, 4, -1, true);
        assertArrayEquals(expected, pixels(), "The white route label must not inherit the marker RGB");
        assertArrayEquals(original, palette);

        clear(); font.drawString("§aMTR§r", 4, 4, -1, true); expected = pixels();
        clear(); MutableText ordinary = TextHelper.literal("MTR").formatted(TextFormatting.GREEN);
        LegacyStyledText.draw(font, ordinary.data, ordinary.data.getFormattedText(), 4, 4, -1, true);
        assertArrayEquals(expected, pixels(), "Ordinary native text must retain its original renderer path");
        boolean colors = client.gameSettings.chatColours;
        float width = client.gameSettings.chatWidth;
        try {
            client.gameSettings.chatColours = true; client.gameSettings.chatWidth = 0;
            MutableText nativeMarker = TextHelper.literal("-").formatted(TextFormatting.GREEN);
            MutableText label = TextHelper.literal(" MTR MTR MTR").formatted(TextFormatting.WHITE);
            try {
                palette[10] = rgb; palette[26] = (rgb & 0xFCFCFC) >> 2;
                clear(); nativeChat(client, TextHelper.append(nativeMarker, label).data).drawChat(0); expected = pixels();
            } finally { System.arraycopy(original, 0, palette, 0, original.length); }
            MutableText rgbMarker = TextHelper.setStyle(TextHelper.literal("-"), Style.getEmptyMapped().withColor(TextColor.fromRgb(rgb)));
            clear(); nativeChat(client, TextHelper.append(rgbMarker, TextHelper.literal(" MTR MTR MTR").formatted(TextFormatting.WHITE)).data).drawChat(0);
            assertArrayEquals(expected, pixels(), "Actual GuiNewChat splitting and draw hooks must retain the RGB marker");
            assertArrayEquals(original, palette);
            client.gameSettings.chatColours = false;
            clear(); nativeChat(client, TextHelper.literal("- MTR MTR MTR").data).drawChat(0); expected = pixels();
            clear(); nativeChat(client, rgbMarker.data).drawChat(0);
            assertArrayEquals(expected, pixels(), "Disabling chat colors must not apply an RGB sidecar");
        } finally { client.gameSettings.chatColours = colors; client.gameSettings.chatWidth = width; }
        System.out.println("Styled text: exact RGB/decorations/shadow, native bidi, white siblings and untouched palette passed");
    }
    private static net.minecraft.client.gui.GuiNewChat nativeChat(Minecraft client, net.minecraft.util.IChatComponent text) throws Exception {
        net.minecraft.client.gui.GuiNewChat chat = new net.minecraft.client.gui.GuiNewChat(client);
        java.lang.reflect.Method add = net.minecraft.client.gui.GuiNewChat.class.getDeclaredMethod("func_146237_a",
            net.minecraft.util.IChatComponent.class, int.class, int.class, boolean.class);
        add.setAccessible(true); add.invoke(chat, text, 0, 0, false);
        return chat;
    }
    private static void clear() {
        GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glEnable(GL11.GL_ALPHA_TEST);
        net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glClearColor(0, 0, 0, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }
    private static byte[] pixels() {
        GL11.glFinish(); ByteBuffer result = BufferUtils.createByteBuffer(64 * 64 * 4);
        GL11.glReadPixels(0, 0, 64, 64, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, result);
        byte[] bytes = new byte[result.remaining()]; result.get(bytes); return bytes;
    }
}
