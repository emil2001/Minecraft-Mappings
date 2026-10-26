package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ChunkEvent;
import org.mtr.mapping.holder.ClientWorld;
import org.mtr.mapping.holder.WorldChunk;
import org.mtr.mapping.mapper.GraphicsHolder;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class EventRegistryClient {
    private Runnable startTick=() -> {},endTick=() -> {},join=() -> {},disconnect=() -> {},reload=() -> {};
    private BiConsumer<ClientWorld,WorldChunk> chunkLoad=(world,chunk) -> {};
    private Consumer<GraphicsHolder> gui;
    private NetHandlerPlayClient connection;
    private boolean initialized;

    EventRegistryClient() {}
    void init() {
        if (initialized) return;
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        initialized=true;
    }
    public void registerStartClientTick(Runnable callback) { startTick=Objects.requireNonNull(callback); }
    public void registerEndClientTick(Runnable callback) { endTick=Objects.requireNonNull(callback); }
    public void registerClientJoin(Runnable callback) { join=Objects.requireNonNull(callback); }
    public void registerClientDisconnect(Runnable callback) { disconnect=Objects.requireNonNull(callback); }
    public void registerChunkLoad(BiConsumer<ClientWorld,WorldChunk> callback) { chunkLoad=Objects.requireNonNull(callback); }
    public void registerResourceReloadEvent(Runnable callback) { reload=Objects.requireNonNull(callback); }
    public void registerGuiRendering(Consumer<GraphicsHolder> callback) { gui=Objects.requireNonNull(callback); }

    @SubscribeEvent public void clientTick(TickEvent.ClientTickEvent event) {
        // GameThreadQueues drains at HIGHEST, before MTR's START callback.
        if (event.phase==TickEvent.Phase.START) startTick.run(); else endTick.run();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void playerJoined(EntityJoinWorldEvent event) {
        Minecraft client=Minecraft.getMinecraft();
        if (!event.world.isRemote || event.entity!=client.thePlayer) return;
        // The network-connected notification precedes player creation. A local player spawn also
        // happens on dimension changes and respawns; the play connection identifies the actual join.
        NetHandlerPlayClient current=client.getNetHandler();
        if (current==null || connection==current) return;
        if (connection!=null) { connection=null; disconnect.run(); }
        connection=current;
        join.run();
    }
    @SubscribeEvent public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft client=Minecraft.getMinecraft();
        Runnable cleanup=() -> {
            if (connection!=null && connection.getNetworkManager()==event.manager) {
                connection=null;
                disconnect.run();
            }
        };
        // FML may post from Netty. The native client queue survives WorldEvent.Unload,
        // whereas the packet queue deliberately discards pending work on world unload.
        if (client.func_152345_ab()) cleanup.run(); else client.func_152344_a(cleanup);
    }
    @SubscribeEvent public void chunkLoaded(ChunkEvent.Load event) {
        if (event.world.isRemote) chunkLoad.accept(new ClientWorld(event.world),new WorldChunk(event.getChunk()));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void resourcesReloaded(TextureStitchEvent.Post event) {
        // Resource consumers must see the completed block atlas, not the separate item-atlas event.
        if (event.map.getTextureType()==0) reload.run();
    }
    @SubscribeEvent public void guiRendered(RenderGameOverlayEvent.Post event) {
        // Forge posts once per element as well as ALL. MTR's complete HUD is drawn once.
        // The native render-tick bridge supplies frame timing; this keeps the GUI matrix intact.
        if (event.type==RenderGameOverlayEvent.ElementType.ALL && gui!=null) GraphicsHolder.createInstanceSafe(gui);
    }
}
