package org.mtr.mapping;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.BlockItemExtension;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.registry.*;
import static org.junit.jupiter.api.Assertions.*;

public class ItemAndRegistryScenarios {
    public void newlyWrappedWorldsAndEntitiesRetainIdentityInMaps() {
        BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld();
        java.util.Map<World, String> modules = new java.util.HashMap<>();
        modules.put(new World(world), "rail actions");
        assertEquals("rail actions", modules.get(new World(world)));
        assertEquals(new World(world), new BlockView(world));
        assertEquals(new BlockView(world), new World(world));
        assertEquals(new World(world).hashCode(), new BlockView(world).hashCode());
        net.minecraft.entity.player.EntityPlayer player = MtrEscalatorScenarios.player(world);
        assertEquals(new PlayerEntity(player), new Entity(player));
        assertEquals(new Entity(player), new LivingEntity(player));
        assertEquals(new PlayerEntity(player).hashCode(), new LivingEntity(player).hashCode());
        assertNotEquals(new World(world), new World(new BlockPlacementScenarios.MemoryWorld()));
    }

    public void realFmlTicksDrainClientTasksAndWorldUnloadCancelsOldWork() {
        int[] count = {0};
        GameThreadQueues.client(() -> count[0]++);
        assertEquals(0, count[0]);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().post(new cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent(cpw.mods.fml.common.gameevent.TickEvent.Phase.START));
        assertEquals(1, count[0]);
        GameThreadQueues.client(() -> fail("Old world task survived unload"));
        BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld();
        world.isRemote = true;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(world));
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().post(new cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent(cpw.mods.fml.common.gameevent.TickEvent.Phase.START));
        assertEquals(1, count[0]);
    }

    public void fragmentedMtrPayloadFitsTheNativeLegacyCustomPacket() {
        String text = String.join("", java.util.Collections.nCopies(16373, "x"));
        org.mtr.mapping.tool.PacketBufferSender sender = new org.mtr.mapping.tool.PacketBufferSender(io.netty.buffer.Unpooled::buffer);
        sender.writeString(text);
        sender.writeBoolean(true);
        java.util.List<io.netty.buffer.ByteBuf> frames = new java.util.ArrayList<>();
        sender.send(frames::add, Runnable::run);
        try {
            for (io.netty.buffer.ByteBuf frame : frames) {
                io.netty.buffer.ByteBuf exact = frame.copy();
                try { assertDoesNotThrow(() -> new cpw.mods.fml.common.network.internal.FMLProxyPacket(exact, "mtr:packet").toC17Packet()); }
                finally { exact.release(); }
            }
        } finally { frames.forEach(io.netty.buffer.ByteBuf::release); }
    }

    public void nativeBlockItemPlacesExtendedStateAndRejectsWithoutConsumption() {
        Registry registry = new Registry();
        PlacementBlock block = new PlacementBlock();
        BlockRegistryObject registered = registry.registerBlockWithBlockItem(new Identifier("mtr", "placement_item"), () -> new Block(block));
        registry.init();
        BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld();
        net.minecraft.entity.player.EntityPlayer player = new net.minecraft.entity.player.EntityPlayer(world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MtrPlacer")) {
            public void addChatMessage(net.minecraft.util.IChatComponent text) {}
            public boolean canCommandSenderUseCommand(int level, String command) { return true; }
            public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() { return new net.minecraft.util.ChunkCoordinates(0, 64, 0); }
        };
        player.rotationYaw = 90;
        world.setBlock(4, 63, 5, net.minecraft.init.Blocks.stone);
        net.minecraft.item.Item nativeItem = net.minecraft.item.Item.getItemFromBlock(registered.get().data);
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(nativeItem, 2);
        nativeItem.onItemUse(stack, player, world, 4, 63, 5, 1, .25F, 1, .75F);
        BlockState placed = new World(world).getBlockState(new BlockPos(4, 64, 5));
        assertTrue(placed.isOf(registered.get()));
        assertEquals(Direction.WEST.data, placed.get(new Property<>(org.mtr.mapping.mapper.DirectionHelper.FACING.data)));
        assertEquals(5, placed.get(new Property<>(PlacementBlock.VARIANT.data)));
        assertEquals(placed, block.seenOnPlaced);
        assertEquals(1, stack.stackSize);
        block.reject = true;
        world.setBlock(6, 63, 5, net.minecraft.init.Blocks.stone);
        nativeItem.onItemUse(stack, player, world, 6, 63, 5, 1, .25F, 1, .75F);
        assertEquals(net.minecraft.init.Blocks.air, world.getBlock(6, 64, 5));
        assertEquals(1, stack.stackSize);
    }

    static class PlacementBlock extends BlockExtension {
        static final IntegerProperty VARIANT = IntegerProperty.of("variant", 0, 5);
        BlockState seenOnPlaced;
        boolean reject;
        PlacementBlock() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public void addBlockProperties(java.util.List<org.mtr.mapping.tool.HolderBase<?>> properties) {
            properties.add(org.mtr.mapping.mapper.DirectionHelper.FACING);
            properties.add(VARIANT);
        }
        @Override public BlockState getPlacementState2(ItemPlacementContext context) {
            if (reject) return null;
            assertEquals(new BlockPos(4, 64, 5), context.getBlockPos());
            assertEquals(4.25, context.getHitPos().getXMapped());
            assertEquals(64, context.getHitPos().getYMapped());
            return getDefaultState2().with(new Property<>(org.mtr.mapping.mapper.DirectionHelper.FACING.data), context.getPlayerFacing().data)
                .with(new Property<>(VARIANT.data), 5);
        }
        @Override public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            seenOnPlaced = world.getBlockState(pos);
        }
    }

    public void nativeToolUseReceivesClickedBlockHandAndAbsoluteHit() {
        BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld();
        net.minecraft.entity.player.EntityPlayer player = new net.minecraft.entity.player.EntityPlayer(world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MtrTester")) {
            public void addChatMessage(net.minecraft.util.IChatComponent text) {}
            public boolean canCommandSenderUseCommand(int level, String command) { return true; }
            public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() { return new net.minecraft.util.ChunkCoordinates(0, 64, 0); }
        };
        player.rotationYaw = 90;
        ItemExtension tool = new ItemExtension(new ItemSettings()) {
            @Override public ActionResult useOnBlock2(ItemUsageContext context) {
                assertSame(world, context.getWorld().data);
                assertSame(player, context.getPlayer().data);
                assertEquals(new BlockPos(4, 64, 5), context.getBlockPos());
                assertEquals(Direction.UP, context.getSide());
                assertEquals(Direction.WEST, context.getPlayerFacing());
                assertEquals(4.25, context.getHitPos().getXMapped());
                assertEquals(65, context.getHitPos().getYMapped());
                assertEquals(5.75, context.getHitPos().getZMapped());
                context.getStack().decrement(1);
                return ActionResult.SUCCESS;
            }
        };
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(tool, 2);
        assertTrue(tool.onItemUse(stack, player, world, 4, 64, 5, 1, .25F, 1, .75F));
        assertEquals(1, stack.stackSize);
        ItemExtension passive = new ItemExtension(new ItemSettings());
        assertFalse(passive.onItemUse(new net.minecraft.item.ItemStack(passive), player, world, 4, 64, 5, 1, 0, 0, 0));
    }

    static BlockEntityTypeRegistryObject<RegistryTile> registeredType;

    public void registeredBlockEntityFactorySurvivesNativeReload() {
        Registry registry = new Registry();
        BlockRegistryObject block = registry.registerBlock(new Identifier("mtr", "registered_tile_block"),
            () -> new Block(new RegistryBlock()));
        registeredType = registry.registerBlockEntityType(new Identifier("mtr", "registered_tile"), RegistryTile::new, block::get);
        assertFalse(registeredType.isPresent());
        registry.init();
        BlockPos pos = new BlockPos(2, 64, 2);
        RegistryTile tile = new RegistryTile(pos, block.get().getDefaultState());
        tile.floor = "Платформа";
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        RegistryTile loaded = (RegistryTile) net.minecraft.tileentity.TileEntity.createAndLoadEntity(tag);
        assertNotNull(loaded);
        assertEquals("Платформа", loaded.floor);
        assertEquals("mtr:registered_tile", tag.getString("mtr_type"));
        assertEquals(pos, loaded.getPos2());
    }

    static class RegistryBlock extends BlockExtension implements BlockWithEntity {
        RegistryBlock() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        public BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state) { return new RegistryTile(pos, state); }
    }
    static class RegistryTile extends BlockEntityExtension {
        String floor;
        RegistryTile(BlockPos pos, BlockState state) { super(registeredType.get(), pos, state); }
        @Override public void writeCompoundTag(CompoundTag tag) { tag.putString("floor", floor); }
        @Override public void readCompoundTag(CompoundTag tag) { floor = tag.getString("floor"); }
    }

    public void deferredRegistrationsKeepNamesFactoriesAndCreativeTabs() {
        Registry registry = new Registry();
        ItemRegistryObject[] tool = new ItemRegistryObject[1];
        CreativeModeTabHolder tab = registry.createCreativeModeTabHolder(new Identifier("mtr", "test_tools"),
            () -> tool[0].get().getDefaultStack());
        BlockRegistryObject block = registry.registerBlockWithBlockItem(new Identifier("mtr", "registry_block"),
            () -> new Block(new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron))),
            (value, settings) -> new BlockItemExtension(value, settings.maxCount(16)), tab);
        tool[0] = registry.registerItem(new Identifier("mtr", "registry_tool"),
            settings -> new Item(new ItemExtension(settings.maxCount(1))), tab);
        BlockRegistryObject foreign = registry.registerBlockWithBlockItem(new Identifier("mtrsteamloco", "registry_eye_candy"),
            () -> new Block(new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron))));
        assertFalse(block.isPresent());
        assertThrows(IllegalStateException.class, block::get);
        assertFalse(tool[0].isPresent());
        assertThrows(IllegalArgumentException.class, () -> registry.registerBlock(new Identifier("mtr", "registry_block"),
            () -> { throw new AssertionError("Duplicate supplier must never run"); }));
        registry.init();
        assertTrue(block.isPresent());
        assertSame(block.get().data, cpw.mods.fml.common.registry.GameRegistry.findBlock("mtr", "registry_block"));
        net.minecraft.item.Item nativeBlockItem = net.minecraft.item.Item.getItemFromBlock(block.get().data);
        assertTrue(nativeBlockItem instanceof BlockItemExtension);
        assertEquals(16, nativeBlockItem.getItemStackLimit());
        assertSame(tab.creativeModeTab, nativeBlockItem.getCreativeTab());
        assertSame(tool[0].get().data, cpw.mods.fml.common.registry.GameRegistry.findItem("mtr", "registry_tool"));
        assertSame(tool[0].get().data, tab.creativeModeTab.getIconItemStack().getItem());
        assertEquals("item.mtr.registry_tool", tool[0].get().getTranslationKey());
        assertEquals(1, tool[0].get().data.getItemStackLimit());
        assertSame(foreign.get().data, cpw.mods.fml.common.registry.GameRegistry.findBlock("mtrsteamloco", "registry_eye_candy"));
        assertEquals("mtrsteamloco:registry_eye_candy", foreign.get().data.delegate.name());
        assertSame(net.minecraft.item.Item.getItemFromBlock(foreign.get().data),
            cpw.mods.fml.common.registry.GameRegistry.findItem("mtrsteamloco", "registry_eye_candy"));
        registry.init();
        assertThrows(IllegalStateException.class, () -> registry.registerItem(new Identifier("mtr", "late"),
            settings -> new Item(new ItemExtension(settings))));
    }

    public void toolLinksAndDisplayNameSurviveLegacyItemNbt() {
        ItemExtension nativeTool = new ItemExtension(new ItemSettings().maxCount(1));
        net.minecraft.item.Item.itemRegistry.addObject(4000, "mtr:test_linking_tool", nativeTool);
        ItemStack stack = new ItemStack(new ItemConvertible(nativeTool));
        long position = new BlockPos(-30000000, 255, 30000000).asLong();
        stack.getOrCreateTag().putLong("pos", position);
        stack.setCustomName(new Text(new net.minecraft.util.ChatComponentText("Связи лифта")));
        assertEquals(1, nativeTool.getItemStackLimit());
        NBTTagCompound saved = new NBTTagCompound();
        stack.data.writeToNBT(saved);
        ItemStack loaded = new ItemStack(net.minecraft.item.ItemStack.loadItemStackFromNBT(saved));
        assertEquals(position, loaded.getOrCreateTag().getLong("pos"));
        assertEquals("Связи лифта", loaded.data.getDisplayName());
        assertEquals(nativeTool, loaded.getItem().data);
        ItemStack copy = loaded.copy();
        copy.getOrCreateTag().putLong("pos", 1);
        assertEquals(position, loaded.getOrCreateTag().getLong("pos"));
        assertFalse(copy.isEmpty());
    }

    public void consumedAndMissingLegacyStacksStayEmpty() {
        ItemStack empty = ItemStack.getEmptyMapped();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.getCount());
        empty.decrement(1);
        assertTrue(empty.isEmpty());
        assertTrue(new ItemStack((net.minecraft.item.ItemStack) null).isEmpty());
        ItemStack stack = new ItemStack(new ItemConvertible(net.minecraft.init.Items.stick), 2);
        stack.decrement(1);
        assertEquals(1, stack.getCount());
        stack.decrement(1);
        assertTrue(stack.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> stack.decrement(-1));
        assertThrows(IllegalArgumentException.class, () -> new ItemSettings().maxCount(0));
    }
}
