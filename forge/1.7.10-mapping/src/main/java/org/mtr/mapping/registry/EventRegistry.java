package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.ServerWorld;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class EventRegistry {
    private final java.util.List<LegacyCommand> commands = new java.util.ArrayList<>();
    private Consumer<MinecraftServer> started = server -> {};
    private Consumer<MinecraftServer> stopping = server -> {};
    private Runnable startTick = () -> {};
    private Consumer<ServerWorld> endWorldTick = world -> {};
    private BiConsumer<MinecraftServer, ServerPlayerEntity> join = (server, player) -> {};
    private BiConsumer<MinecraftServer, ServerPlayerEntity> disconnect = (server, player) -> {};

    void init() { FMLCommonHandler.instance().bus().register(this); }
    void addCommand(LegacyCommand command) { commands.add(command); }
    public void registerServerStarted(Consumer<MinecraftServer> consumer) { started = consumer; }
    public void registerServerStopping(Consumer<MinecraftServer> consumer) { stopping = consumer; }
    public void registerStartServerTick(Runnable runnable) { startTick = runnable; }
    public void registerEndWorldTick(Consumer<ServerWorld> consumer) { endWorldTick = consumer; }
    public void registerPlayerJoin(BiConsumer<MinecraftServer, ServerPlayerEntity> consumer) { join = consumer; }
    public void registerPlayerDisconnect(BiConsumer<MinecraftServer, ServerPlayerEntity> consumer) { disconnect = consumer; }

    // FML lifecycle events go to the @Mod entrypoint, not to the ordinary event bus.
    public void serverStarted(net.minecraft.server.MinecraftServer server) {
        commands.forEach(command -> command.install(server));
        started.accept(new MinecraftServer(server));
    }
    public void serverStopping(net.minecraft.server.MinecraftServer server) {
        GameThreadQueues.stopServer(server);
        try { stopping.accept(new MinecraftServer(server)); }
        finally { commands.forEach(command -> command.detach(server)); }
    }
    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) startTick.run();
    }
    @SubscribeEvent public void worldTick(TickEvent.WorldTickEvent event) {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.END && event.world instanceof WorldServer) {
            endWorldTick.accept(new ServerWorld((WorldServer) event.world));
        }
    }
    @SubscribeEvent public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) { player(event.player, join); }
    @SubscribeEvent public void playerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) { player(event.player, disconnect); }
    private static void player(net.minecraft.entity.player.EntityPlayer player, BiConsumer<MinecraftServer, ServerPlayerEntity> callback) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            callback.accept(new MinecraftServer(serverPlayer.mcServer), new ServerPlayerEntity(serverPlayer));
        }
    }
}
