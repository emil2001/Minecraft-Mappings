package org.mtr.mapping.registry;

import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.tool.PacketBufferSender;

public abstract class PacketHandler {
    public abstract void write(PacketBufferSender sender);
    public void runServer(MinecraftServer server, ServerPlayerEntity player) {}
    public void runClient() {}
}
