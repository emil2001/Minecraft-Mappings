package org.mtr.mapping.registry;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class RegistryClient {
    public final EventRegistryClient eventRegistryClient = new EventRegistryClient();
    private final Registry registry;
    private boolean initialized;
    private final java.util.List<Runnable> renderers = new java.util.ArrayList<>();
    public RegistryClient(Registry registry) { this.registry = registry; }
    public void init() {
        if (initialized) return;
        GameThreadQueues.init();
        LegacyClientEvents.init();
        LegacyBlockRenderers.init();
        LegacyItemRenderers.init();
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getMinecraft();
        installPlayerMessages(client);
        installPlayerPhysics(client);
        ((net.minecraft.client.resources.IReloadableResourceManager) client.getResourceManager()).registerReloadListener(
            new org.mtr.mapping.tool.LegacyClientTranslations(client.getLanguageManager()));
        renderers.forEach(Runnable::run);
        eventRegistryClient.init();
        initialized = true;
    }
    static void installPlayerMessages(net.minecraft.client.Minecraft client) {
        LegacyPlayerMessages.clientSink = text -> client.ingameGUI.func_110326_a(text.data.getFormattedText(), false);
    }
    static void installPlayerPhysics(net.minecraft.client.Minecraft client) {
        LegacyPlayerPhysics.clientSink = packet -> org.mtr.mapping.tool.PlayerPhysics.updateClientNoGravity(
            client.thePlayer, packet.dimension, packet.entityId, packet.value);
    }
    public org.mtr.mapping.holder.KeyBinding registerKeyBinding(String translationKey, int key, String categoryKey) {
        net.minecraft.client.settings.KeyBinding binding = new net.minecraft.client.settings.KeyBinding(
            translationKey, org.mtr.mapping.tool.KeyCodes.toLegacy(key), categoryKey);
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(binding);
        return new org.mtr.mapping.holder.KeyBinding(binding);
    }
    public void registerBlockRenderType(org.mtr.mapping.holder.RenderLayer layer,BlockRegistryObject block) {
        if (initialized) throw new IllegalStateException("Client registrations are closed");
        renderers.add(() -> LegacyBlockRenderers.layer(block.get().data,layer));
    }
    public void registerBlockColors(org.mtr.mapping.holder.BlockColorProvider provider,BlockRegistryObject... blocks) {
        if (initialized) throw new IllegalStateException("Client registrations are closed");
        renderers.add(() -> { for (BlockRegistryObject block : blocks) LegacyBlockRenderers.colors(block.get().data,provider); });
    }
    public void registerItemColors(org.mtr.mapping.holder.ItemColorProvider provider,ItemRegistryObject... items) {
        if (initialized) throw new IllegalStateException("Client registrations are closed");
        renderers.add(() -> { for (ItemRegistryObject item:items) LegacyItemRenderers.color(item.get().data,provider); });
    }
    public void registerItemModelPredicate(ItemRegistryObject item,org.mtr.mapping.holder.Identifier id,ModelPredicateProvider provider) {
        if (initialized) throw new IllegalStateException("Client registrations are closed");
        renderers.add(() -> LegacyItemRenderers.predicate(item.get().data,id,provider));
    }
    @FunctionalInterface public interface ModelPredicateProvider {
        float call(org.mtr.mapping.holder.ItemStack stack,org.mtr.mapping.holder.ClientWorld world,org.mtr.mapping.holder.LivingEntity entity);
    }
    public void setupPackets(org.mtr.mapping.holder.Identifier id) {
        if (registry.network==null) registry.setupPackets(id);
        else if (!registry.network.matches(id)) throw new IllegalArgumentException("Client and common packet channels differ");
    }
    public <T extends PacketHandler> void sendPacketToServer(T packet) {
        if (registry.network == null) throw new IllegalStateException("Packet channel is not configured");
        registry.network.sendToServer(packet);
    }
    public <T extends org.mtr.mapping.mapper.BlockEntityExtension> void registerBlockEntityRenderer(BlockEntityTypeRegistryObject<T> registration,
            java.util.function.Function<org.mtr.mapping.mapper.BlockEntityRenderer.Argument,org.mtr.mapping.mapper.BlockEntityRenderer<T>> factory) {
        if (initialized) throw new IllegalStateException("Client registrations are closed");
        renderers.add(() -> LegacyBlockEntityRenderers.register(registration.get(),factory.apply(
            new org.mtr.mapping.mapper.BlockEntityRenderer.Argument(net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance))));
    }
    public <T extends org.mtr.mapping.mapper.EntityExtension> void registerEntityRenderer(EntityTypeRegistryObject<T> registration,
            java.util.function.Function<org.mtr.mapping.mapper.EntityRenderer.Argument,org.mtr.mapping.mapper.EntityRenderer<T>> factory) {
        if (initialized) throw new IllegalStateException("Client registrations are closed");
        renderers.add(() -> installRenderer(registration.get(),factory));
    }
    @SuppressWarnings("unchecked")
    private static <T extends org.mtr.mapping.mapper.EntityExtension> void installRenderer(org.mtr.mapping.holder.EntityType<T> type,
            java.util.function.Function<org.mtr.mapping.mapper.EntityRenderer.Argument,org.mtr.mapping.mapper.EntityRenderer<T>> factory) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.client.renderer.entity.RenderManager manager = net.minecraft.client.renderer.entity.RenderManager.instance;
        org.mtr.mapping.mapper.EntityRenderer<T> renderer = factory.apply(new org.mtr.mapping.mapper.EntityRenderer.Argument(manager));
        type.data.visibility = (entity,x,y,z) -> {
            net.minecraft.client.renderer.culling.Frustrum frustum = new net.minecraft.client.renderer.culling.Frustrum();
            frustum.setPosition(x,y,z);
            return renderer.shouldRender2((T)entity,new org.mtr.mapping.holder.Frustum(frustum),x,y,z);
        };
        type.data.setClientSpawner(message -> {
                T entity = type.data.create(java.util.Objects.requireNonNull(client.theWorld,"No client world for entity spawn"));
                entity.setEntityId((Integer)cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
                    cpw.mods.fml.common.network.internal.FMLMessage.EntityMessage.class,message,"entityId"));
                entity.setLocationAndAngles(spawnField(message,"scaledX").doubleValue(),spawnField(message,"scaledY").doubleValue(),
                    spawnField(message,"scaledZ").doubleValue(),spawnField(message,"scaledYaw").floatValue(),spawnField(message,"scaledPitch").floatValue());
                return entity;
        });
        type.data.whenClassKnown(entityClass -> client.func_152344_a(() -> {
            cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(entityClass,renderer);
            manager.entityRenderMap.put(entityClass,renderer);
        }));
    }
    private static Number spawnField(cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage message, String field) {
        return cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage.class,message,field);
    }
}
