package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.registry.*;
import org.mtr.mapping.tool.HolderBase;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class LootTableScenarios {
    public void originalCargoLootWithoutRegisteredItemsDoesNotAbortLoadingOrInventDrops() {
        Registry registry = new Registry();
        BlockRegistryObject loader = registry.registerBlock(new Identifier("mtr", "train_cargo_loader"),
            () -> new Block(new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron))));
        BlockRegistryObject unloader = registry.registerBlock(new Identifier("mtr", "train_cargo_unloader"),
            () -> new Block(new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron))));
        assertDoesNotThrow(registry::init);
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        World world = new World(nativeWorld);
        BlockPos pos = new BlockPos(4, 64, 4);
        for (BlockRegistryObject block : new BlockRegistryObject[]{loader, unloader}) {
            assertNull(net.minecraft.item.Item.getItemFromBlock(block.get().data));
            world.setBlockState(pos, block.get().getDefaultState(), 2);
            assertTrue(block.get().data.getDrops(nativeWorld, 4, 64, 4, 0, 0).isEmpty());
        }
    }

    public enum SlabType implements StringIdentifiable {
        BOTTOM, TOP, DOUBLE;
        public String asString2() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    static class Slab extends BlockExtension {
        static final EnumProperty<SlabType> TYPE = EnumProperty.of("type", SlabType.class);
        Slab() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(TYPE); }
    }

    public void originalDoubleSlabLootKeepsBothItemsWithSilkTouch() {
        Registry registry = new Registry();
        BlockRegistryObject slab = registry.registerBlockWithBlockItem(new Identifier("mtr", "station_color_metal_slab"), () -> new Block(new Slab()));
        registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        World world = new World(nativeWorld);
        BlockPos pos = new BlockPos(4, 64, 4);
        world.setBlockState(pos, slab.get().getDefaultState().with(new Property<>(Slab.TYPE.data), SlabType.DOUBLE), 2);
        assertEquals(2, slab.get().data.getDrops(nativeWorld, 4, 64, 4, 2, 0).get(0).stackSize);
        net.minecraft.entity.player.EntityPlayer player = MtrEscalatorScenarios.player(nativeWorld);
        net.minecraft.item.ItemStack pick = new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_pickaxe);
        pick.addEnchantment(net.minecraft.enchantment.Enchantment.silkTouch, 1);
        player.inventory.mainInventory[0] = pick;
        assertTrue(slab.get().data.removedByPlayer(nativeWorld, player, 4, 64, 4, true));
        slab.get().data.harvestBlock(nativeWorld, player, 4, 64, 4, 2);
        int count = 0;
        for (Object entity : nativeWorld.loadedEntityList) if (entity instanceof net.minecraft.entity.item.EntityItem) {
            net.minecraft.item.ItemStack stack = ((net.minecraft.entity.item.EntityItem) entity).getEntityItem();
            if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(slab.get().data)) count += stack.stackSize;
        }
        assertEquals(2, count, "The native silk-touch path must not bypass the modern double-slab loot table");
        BlockPlacementScenarios.MemoryWorld explosionWorld = new BlockPlacementScenarios.MemoryWorld();
        new World(explosionWorld).setBlockState(pos, slab.get().getDefaultState().with(new Property<>(Slab.TYPE.data), SlabType.DOUBLE), 2);
        explosionWorld.rand.setSeed(2);
        slab.get().data.dropBlockAsItemWithChance(explosionWorld, 4, 64, 4, 2, .5F, 0);
        int survived = 0;
        for (Object entity : explosionWorld.loadedEntityList) if (entity instanceof net.minecraft.entity.item.EntityItem) {
            survived += ((net.minecraft.entity.item.EntityItem) entity).getEntityItem().stackSize;
        }
        assertEquals(1, survived, "Explosion decay must roll independently for each item in a double slab");
    }

    public void originalSignAnyOfConditionDropsOnlyNorthAndEast() {
        Registry registry = new Registry();
        BlockRegistryObject sign = registry.registerBlockWithBlockItem(new Identifier("mtr", "railway_sign_3_even"),
            () -> new Block(new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron)) {
                @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(DirectionHelper.FACING); }
            }));
        registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        World world = new World(nativeWorld);
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            world.setBlockState(new BlockPos(4, 64, 4), sign.get().getDefaultState().with(new Property<>(DirectionHelper.FACING.data), direction.data), 2);
            assertEquals(direction == Direction.NORTH || direction == Direction.EAST ? 1 : 0,
                sign.get().data.getDrops(nativeWorld, 4, 64, 4, 0, 0).size());
        }
    }
}
