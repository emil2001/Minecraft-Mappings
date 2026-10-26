package org.mtr.mapping.registry;

import net.minecraft.entity.player.EntityPlayerMP;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Mirrors the modern tracked NoGravity field to the owning legacy client. */
public final class LegacyPlayerPhysics {
    static BiConsumer<EntityPlayerMP, Boolean> serverSink;
    static Consumer<NoGravityPacket> clientSink;
    private LegacyPlayerPhysics() {}

    static void register(Registry registry) {
        registry.registerPacket(NoGravityPacket.class, NoGravityPacket::new);
        serverSink = (player, value) -> {
            if (player.playerNetServerHandler != null && player.playerNetServerHandler.netManager.isChannelOpen())
                registry.sendPacketToClient(new ServerPlayerEntity(player),
                    new NoGravityPacket(player.dimension, player.getEntityId(), value));
        };
    }

    public static void syncNoGravity(EntityPlayerMP player, boolean value) {
        if (serverSink != null) serverSink.accept(player, value);
    }

    static final class NoGravityPacket extends PacketHandler {
        final int dimension, entityId;
        final boolean value;
        NoGravityPacket(int dimension, int entityId, boolean value) {
            this.dimension = dimension; this.entityId = entityId; this.value = value;
        }
        NoGravityPacket(PacketBufferReceiver receiver) {
            dimension = receiver.readInt(); entityId = receiver.readInt(); value = receiver.readBoolean();
        }
        @Override public void write(PacketBufferSender sender) {
            sender.writeInt(dimension); sender.writeInt(entityId); sender.writeBoolean(value);
        }
        @Override public void runClient() {
            if (clientSink != null) clientSink.accept(this);
        }
        // PacketHandler.runServer deliberately grants no authority to a client copy.
    }
}
