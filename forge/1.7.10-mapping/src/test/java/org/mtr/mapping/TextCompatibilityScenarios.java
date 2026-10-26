package org.mtr.mapping;

import net.minecraft.util.StringTranslate;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

public class TextCompatibilityScenarios {
    public void nativeItemAndBlockTooltipsPreserveWorldAdvancedFlagAndMultilineColor() {
        java.util.List<String> tooltip = new java.util.ArrayList<>();
        tooltip.add("existing");
        org.mtr.mapping.mapper.ItemExtension item = new org.mtr.mapping.mapper.ItemExtension(new ItemSettings()) {
            @Override public void addTooltips(ItemStack stack, World world, java.util.List<MutableText> lines, TooltipContext options) {
                assertNull(world); assertTrue(options.isAdvanced());
                lines.add(TextHelper.literal("Лифт\nВерхний").formatted(TextFormatting.GRAY));
            }
        };
        item.addInformation(new net.minecraft.item.ItemStack(item), null, tooltip, true);
        assertEquals(3, tooltip.size()); assertEquals("existing", tooltip.get(0));
        assertTrue(tooltip.get(1).startsWith("§7Лифт"));
        assertTrue(tooltip.get(2).startsWith("§7Верхний"));
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        org.mtr.mapping.mapper.BlockExtension block = new org.mtr.mapping.mapper.BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron)) {
            @Override public void addTooltips(ItemStack stack, BlockView world, java.util.List<MutableText> lines, TooltipContext options) {
                assertSame(nativeWorld, world.data); assertFalse(options.isAdvanced());
                lines.add(TextHelper.literal("Направляющая").formatted(TextFormatting.GREEN));
            }
        };
        org.mtr.mapping.mapper.BlockItemExtension blockItem = new org.mtr.mapping.mapper.BlockItemExtension(new Block(block), new ItemSettings());
        blockItem.addInformation(new net.minecraft.item.ItemStack(blockItem), MtrEscalatorScenarios.player(nativeWorld), tooltip, false);
        assertEquals(4, tooltip.size()); assertTrue(tooltip.get(3).startsWith("§aНаправляющая"));
    }
    public void translatedLiftLabelsPreserveArgumentsUnicodeSiblingsAndFormatting() {
        StringTranslate.inject(new ByteArrayInputStream("mtr.test.floor=Этаж %1$s: %2$s\n".getBytes(StandardCharsets.UTF_8)));
        MutableText label = TextHelper.translatable("mtr.test.floor", 3, TextHelper.literal("Верхний"));
        assertEquals("Этаж 3: Верхний", label.getString());
        label.formatted(TextFormatting.GRAY).formatted(TextFormatting.BOLD);
        assertEquals(net.minecraft.util.EnumChatFormatting.GRAY, label.data.getChatStyle().getColor());
        assertTrue(label.data.getChatStyle().getBold());
        MutableText combined = TextHelper.append(TextHelper.literal("Лифт: "), label).append("!");
        assertEquals("Лифт: Этаж 3: Верхний!", combined.getString());
        assertEquals("Этаж 3: Верхний", label.getString());
        assertEquals(combined.data.getFormattedText(), Text.cast(combined).asOrderedText().data.getFormattedText());
        assertEquals("mtr.test.missing", TextHelper.translatable("mtr.test.missing").getString());
        label.formatted(TextFormatting.RESET);
        assertFalse(label.data.getChatStyle().getBold());
        assertNull(label.data.getChatStyle().getColor());
    }
}
