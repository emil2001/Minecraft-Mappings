package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;
import org.mtr.mapping.registry.GameThreadQueues;

public final class MinecraftServer extends NativeHolder<net.minecraft.server.MinecraftServer> {
    public MinecraftServer(net.minecraft.server.MinecraftServer data) { super(data); }
    public void execute(Runnable task) { GameThreadQueues.server(data, task); }
    public ServerWorld getOverworld() { return new ServerWorld(data.worldServerForDimension(0)); }
    public PlayerManager getPlayerManager() { return new PlayerManager(data.getConfigurationManager()); }
    public java.nio.file.Path getSavePath(WorldSavePath path) {
        return getOverworld().data.getSaveHandler().getWorldDirectory().toPath().resolve(path.getRelativePath());
    }
    public java.io.File getRunDirectory() { return data.getFile("."); }
    public boolean isRunning() { return data.isServerRunning(); }
    public void stop(boolean wait) {
        data.initiateShutdown();
        if (wait) {
            try { while (!data.isServerStopped()) Thread.sleep(10); }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for server shutdown",exception);
            }
        }
    }
}
