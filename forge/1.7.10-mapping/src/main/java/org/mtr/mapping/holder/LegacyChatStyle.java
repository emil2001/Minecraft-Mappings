package org.mtr.mapping.holder;

import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;

/** Keeps modern style values through the native component copy and inheritance paths. */
final class LegacyChatStyle extends ChatStyle {
    private static final Field[] NATIVE_FIELDS = Arrays.stream(ChatStyle.class.getDeclaredFields())
        .filter(field -> !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
    private static final Field PARENT = Arrays.stream(NATIVE_FIELDS).filter(field -> field.getType() == ChatStyle.class).findFirst().get();
    static { for (Field field : NATIVE_FIELDS) field.setAccessible(true); }

    // shortcut: native JSON lacks RGB/fonts and font selection needs a TTF renderer before gameplay acceptance.
    private Integer rgb;
    private ResourceLocation font;

    LegacyChatStyle() {}
    LegacyChatStyle(ChatStyle source) {
        // Native serializers read private fields directly, including nullable local overrides.
        try { for (Field field : NATIVE_FIELDS) field.set(this, field.get(source)); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot copy native chat style", exception); }
        if (source instanceof LegacyChatStyle) {
            rgb = ((LegacyChatStyle) source).rgb;
            font = ((LegacyChatStyle) source).font;
        }
    }
    static LegacyChatStyle copyOf(ChatStyle style) { return new LegacyChatStyle(style); }
    void setRgb(Integer rgb) {
        this.rgb = rgb;
        super.setColor(rgb == null ? null : TextColor.nativeColor(rgb));
    }
    Integer getRgb() { return rgb != null ? rgb : rgbOf(this); }
    static Integer rgbOf(ChatStyle style) {
        if (style == null) return null;
        if (style instanceof LegacyChatStyle && ((LegacyChatStyle) style).rgb != null) return ((LegacyChatStyle) style).rgb;
        Integer color = TextColor.rgb(style.getColor());
        return color != null ? color : rgbOf(parentOf(style));
    }
    void setFont(ResourceLocation font) { this.font = font; }
    ResourceLocation getFont() { return fontOf(this); }
    static ResourceLocation fontOf(ChatStyle style) {
        if (style == null) return null;
        if (style instanceof LegacyChatStyle && ((LegacyChatStyle) style).font != null) return ((LegacyChatStyle) style).font;
        return fontOf(parentOf(style));
    }
    private static ChatStyle parentOf(ChatStyle style) {
        try { return (ChatStyle) PARENT.get(style); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot read native chat parent", exception); }
    }
    @Override public EnumChatFormatting getColor() { return rgb == null ? super.getColor() : TextColor.nativeColor(rgb); }
    @Override public boolean isEmpty() { return rgb == null && font == null && super.isEmpty(); }
    @Override public ChatStyle setColor(EnumChatFormatting color) { rgb = null; return super.setColor(color); }
    @Override public ChatStyle createShallowCopy() { return new LegacyChatStyle(this); }
    @Override public ChatStyle createDeepCopy() {
        LegacyChatStyle copy = new LegacyChatStyle(super.createDeepCopy());
        copy.setRgb(getRgb());
        copy.font = getFont();
        return copy;
    }
    @Override public boolean equals(Object other) {
        return other instanceof ChatStyle && super.equals(other) && Objects.equals(getRgb(), rgbOf((ChatStyle) other))
            && Objects.equals(getFont(), fontOf((ChatStyle) other));
    }
    @Override public int hashCode() {
        return Objects.hash(getRgb(), getFont(), getBold(), getItalic(), getUnderlined(), getStrikethrough(), getObfuscated(), getChatClickEvent(), getChatHoverEvent());
    }
}
