package org.mtr.mapping;

import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;
import static org.junit.jupiter.api.Assertions.*;

public class StyleCompatibilityScenarios {
    public void paletteStylesAreImmutableAndRetainNativeChatInheritance() {
        Style empty = Style.getEmptyMapped();
        Style white = empty.withColor(TextFormatting.WHITE);
        Style green = empty.withColor(TextFormatting.GREEN);
        assertNull(empty.data.getColor());
        assertEquals(EnumChatFormatting.WHITE, white.data.getColor());
        MutableText marker = TextHelper.setStyle(TextHelper.literal("-"), green);
        MutableText route = TextHelper.setStyle(TextHelper.literal(" Линия"), white);
        MutableText message = TextHelper.append(marker, route).append("!");
        assertEquals("- Линия!", message.getString());
        assertTrue(message.data.getFormattedText().startsWith("§a-§r§f Линия§r§a!"));
        marker.formatted(TextFormatting.BOLD);
        assertFalse(green.data.getBold(), "Styles applied to components must not share mutable native ChatStyle");
        assertTrue(route.data.getChatStyle().getBold(), "An explicit sibling color must not suppress inherited bold");
        String json = net.minecraft.util.IChatComponent.Serializer.func_150696_a(message.data);
        net.minecraft.util.IChatComponent restored = net.minecraft.util.IChatComponent.Serializer.func_150699_a(json);
        assertEquals(message.data.getFormattedText(), restored.getFormattedText(), "Native JSON must retain palette colors and nullable local overrides");
        for (TextFormatting color : TextFormatting.values()) if (color.data.isColor()) {
            Style mapped = empty.withColor(color);
            assertEquals(color.data, mapped.data.getColor());
            assertEquals(color.data, empty.withColor(TextColor.fromRgb(mapped.getColor().getRgb())).data.getColor());
        }
    }

    public void rgbAndFontMetadataSurviveStyleCopiesInheritanceAndNativeComponents() {
        Identifier font = new Identifier("mtr", "mtr");
        Style style = Style.getEmptyMapped().withColor(TextColor.fromRgb(0x1278AB)).withFont(font);
        MutableText marker = TextHelper.setStyle(TextHelper.literal("-"), style).append("*");
        ChatStyle inherited = ((net.minecraft.util.IChatComponent) marker.data.getSiblings().get(0)).getChatStyle();
        assertEquals(0x1278AB, new Style(inherited).getColor().getRgb());
        assertEquals(font, new Style(inherited).getFont());
        ChatStyle shallow = marker.data.getChatStyle().createShallowCopy();
        ChatStyle deep = inherited.createDeepCopy();
        net.minecraft.util.IChatComponent copied = marker.data.createCopy();
        marker.formatted(TextFormatting.RED);
        assertEquals(0x1278AB, new Style(shallow).getColor().getRgb());
        assertEquals(0x1278AB, new Style(deep).getColor().getRgb());
        assertEquals(font, new Style(deep).getFont());
        assertEquals(0x1278AB, new Style(copied.getChatStyle()).getColor().getRgb());
        assertEquals(TextFormatting.RED.data, inherited.getColor());
        assertEquals(font, new Style(inherited).getFont());
        assertEquals(0x1278AB, style.getColor().getRgb());
        assertEquals(font, style.getFont());
        assertNotEquals(style.data, style.withColor(TextColor.fromRgb(0x1278AC)).data);
        assertNotEquals(style.data, style.withFont(new Identifier("mtr", "other")).data);
        assertEquals(style.data, shallow);
        assertEquals(style.data.hashCode(), shallow.hashCode());
        marker.formatted(TextFormatting.RESET);
        assertNull(new Style(marker.data.getChatStyle()).getColor());
        assertEquals(new Identifier("minecraft", "default"), new Style(marker.data.getChatStyle()).getFont());
    }
}
