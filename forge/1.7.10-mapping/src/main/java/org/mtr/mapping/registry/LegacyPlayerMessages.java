package org.mtr.mapping.registry;

import net.minecraft.util.IChatComponent;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Routes the modern action-bar flag through the already configured mapping channel. */
public final class LegacyPlayerMessages {
    static BiConsumer<ServerPlayerEntity, Text> serverSink;
    static Consumer<Text> clientSink;
    private LegacyPlayerMessages() {}

    static void register(Registry registry) {
        registry.registerPacket(ActionBarPacket.class, ActionBarPacket::new);
        serverSink = (player, text) -> registry.sendPacketToClient(player, new ActionBarPacket(text));
    }

    public static void sendActionBar(PlayerEntity player, Text text) {
        if (player.data.worldObj.isRemote) {
            Objects.requireNonNull(clientSink, "Client player messages are not initialized").accept(text);
        } else {
            Objects.requireNonNull(serverSink, "Player message channel is not initialized")
                .accept(ServerPlayerEntity.cast(player), text);
        }
    }

    static final class ActionBarPacket extends PacketHandler {
        private final String json;
        ActionBarPacket(Text text) { json = IChatComponent.Serializer.func_150696_a(text.data); }
        ActionBarPacket(PacketBufferReceiver receiver) { json = receiver.readString(); }
        @Override public void write(PacketBufferSender sender) { sender.writeString(json); }
        @Override public void runClient() {
            IChatComponent component = IChatComponent.Serializer.func_150699_a(json);
            if (component == null) throw new IllegalArgumentException("Missing action-bar message");
            Objects.requireNonNull(clientSink, "Client player messages are not initialized").accept(new Text(component));
        }
    }
}
