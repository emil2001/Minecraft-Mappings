package org.mtr.mapping.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockItemExtension;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.EntityExtension;
import java.util.*;
import java.util.function.*;

public final class Registry {
    public final EventRegistry eventRegistry = new EventRegistry();
    LegacyNetwork network;
    private final Map<String, Function<org.mtr.mapping.tool.PacketBufferReceiver, ? extends PacketHandler>> packets = new LinkedHashMap<>();
    private final List<Runnable> blocks = new ArrayList<>();
    private final List<Runnable> items = new ArrayList<>();
    private final List<Runnable> blockEntities = new ArrayList<>();
    private final List<Runnable> entityTypes = new ArrayList<>();
    private final List<Runnable> soundEvents = new ArrayList<>();
    private final Set<Identifier> blockIds = new HashSet<>();
    private final Set<Identifier> itemIds = new HashSet<>();
    private final Set<Identifier> blockEntityIds = new HashSet<>();
    private final Set<Identifier> entityIds = new HashSet<>();
    private final Set<Identifier> soundIds = new HashSet<>();
    private static boolean stateTypeRegistered;
    private boolean started;
    private boolean initialized;

    public void init() {
        if (initialized) return;
        if (started) throw new IllegalStateException("Registration is already running or failed");
        started = true;
        org.mtr.mapping.tool.LegacyTranslations.loadDefaults();
        org.mtr.mapping.tool.Waterlogging.init();
        GameThreadQueues.init();
        eventRegistry.init();
        if (!stateTypeRegistered) {
            GameRegistry.registerTileEntity(org.mtr.mapping.tool.BlockStateTileEntity.class, "mtr:block_state");
            stateTypeRegistered = true;
        }
        blocks.forEach(Runnable::run);
        items.forEach(Runnable::run);
        blockEntities.forEach(Runnable::run);
        entityTypes.forEach(Runnable::run);
        soundEvents.forEach(Runnable::run);
        for (Identifier id : blockIds) {
            org.mtr.mapping.tool.BlockLootTable table = org.mtr.mapping.tool.BlockLootTable.forBlock(
                new Block((net.minecraft.block.Block) net.minecraft.block.Block.blockRegistry.getObject(id.toString())));
            if (table != null) table.validateItems();
        }
        initialized = true;
    }

    public void registerRecipes() {
        if (!initialized) throw new IllegalStateException("Legacy items must be registered before crafting recipes");
        org.mtr.mapping.tool.CraftingRecipes.register();
    }

    public void setupPackets(Identifier id) {
        if (network != null || started) throw new IllegalStateException("Packet channel is already configured or closed");
        network = new LegacyNetwork(id, packets);
        LegacyPlayerMessages.register(this);
        LegacyPlayerPhysics.register(this);
    }
    public void registerCommand(String name, Consumer<CommandBuilder<?>> consumer, String... aliases) {
        if (started) throw new IllegalStateException("Registrations are closed");
        eventRegistry.addCommand(new LegacyCommand(name, consumer, aliases));
    }
    public <T extends PacketHandler> void registerPacket(Class<T> type, Function<org.mtr.mapping.tool.PacketBufferReceiver, T> factory) {
        if (started) throw new IllegalStateException("Registrations are closed");
        if (packets.putIfAbsent(type.getName(), factory) != null) throw new IllegalArgumentException("Duplicate packet " + type.getName());
    }
    public <T extends PacketHandler> void sendPacketToClient(ServerPlayerEntity player, T packet) {
        if (network == null) throw new IllegalStateException("Packet channel is not configured");
        network.sendToClient(player, packet);
    }

    public BlockRegistryObject registerBlock(Identifier id, Supplier<Block> supplier) {
        reserve(blockIds, id);
        BlockRegistryObject result = new BlockRegistryObject();
        blocks.add(() -> {
            Block block = Objects.requireNonNull(supplier.get());
            block.data.setBlockName(id.getNamespace() + "." + id.getPath());
            LegacyRegistration.register(block.data, id);
            result.value = block;
        });
        return result;
    }

    public BlockRegistryObject registerBlockWithBlockItem(Identifier id, Supplier<Block> supplier, CreativeModeTabHolder... tabs) {
        return registerBlockWithBlockItem(id, supplier, BlockItemExtension::new, tabs);
    }

    public BlockRegistryObject registerBlockWithBlockItem(Identifier id, Supplier<Block> supplier,
            BiFunction<Block, ItemSettings, BlockItemExtension> factory, CreativeModeTabHolder... tabs) {
        reserve(itemIds, id);
        BlockRegistryObject result = registerBlock(id, supplier);
        items.add(() -> {
            net.minecraft.item.Item item = factory.apply(result.get(), new ItemSettings());
            if (tabs.length > 0) result.get().data.setCreativeTab(tabs[0].creativeModeTab);
            registerNativeItem(id, item, tabs);
        });
        return result;
    }

    public ItemRegistryObject registerItem(Identifier id, Function<ItemSettings, Item> factory, CreativeModeTabHolder... tabs) {
        reserve(itemIds, id);
        ItemRegistryObject result = new ItemRegistryObject();
        items.add(() -> {
            Item item = Objects.requireNonNull(factory.apply(new ItemSettings()));
            registerNativeItem(id, item.data, tabs);
            result.value = item;
        });
        return result;
    }

    public CreativeModeTabHolder createCreativeModeTabHolder(Identifier id, Supplier<ItemStack> icon) {
        return new CreativeModeTabHolder(id, icon);
    }

    @SafeVarargs
    public final <T extends BlockEntityExtension> BlockEntityTypeRegistryObject<T> registerBlockEntityType(Identifier id,
            BiFunction<BlockPos, BlockState, T> factory, Supplier<Block>... blocks) {
        reserve(blockEntityIds, id);
        BlockEntityTypeRegistryObject<T> result = new BlockEntityTypeRegistryObject<>();
        Supplier<Block>[] copy = blocks.clone();
        blockEntities.add(() -> result.value = BlockEntityFactoryRegistry.register(id, factory, copy));
        return result;
    }

    public <T extends EntityExtension> EntityTypeRegistryObject<T> registerEntityType(Identifier id,
            BiFunction<EntityType<?>,World,T> factory, float width, float height) {
        reserve(entityIds,id);
        EntityTypeRegistryObject<T> result = new EntityTypeRegistryObject<>();
        entityTypes.add(() -> result.value = EntityFactoryRegistry.register(id,factory,width,height));
        return result;
    }

    public SoundEventRegistryObject registerSoundEvent(Identifier id) {
        reserve(soundIds,id);
        SoundEventRegistryObject result = new SoundEventRegistryObject();
        soundEvents.add(() -> result.value = new SoundEvent(id.data)); return result;
    }

    private void reserve(Set<Identifier> ids, Identifier id) {
        if (started) throw new IllegalStateException("Registrations are closed");
        if (!ids.add(id)) throw new IllegalArgumentException("Duplicate registration " + id);
    }

    private static void registerNativeItem(Identifier id, net.minecraft.item.Item item, CreativeModeTabHolder[] tabs) {
        item.setUnlocalizedName(id.getNamespace() + "." + id.getPath());
        if (tabs.length > 0) item.setCreativeTab(tabs[0].creativeModeTab);
        LegacyRegistration.register(item, id);
    }
}
