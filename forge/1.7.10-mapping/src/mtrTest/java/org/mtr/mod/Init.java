package org.mtr.mod;

import org.mtr.core.serializer.SerializedDataBase;
import org.mtr.core.servlet.QueueObject;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.Registry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Test-only connection from the original packet to the real core; production Init is compiled separately. */
public final class Init {
    public static final String MOD_ID = "mtr";
    public static final Registry REGISTRY = new Registry();
    public static BiConsumer<World, QueueObject> forward;
    // The real client class registers its webserver callback during class initialization.
    public static void createWebserverSetup(Consumer<?> setup) {}
    public static org.mtr.mapping.holder.BlockPos newBlockPos(double x,double y,double z) {
        return new org.mtr.mapping.holder.BlockPos((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z));
    }
    public static <T extends SerializedDataBase> void sendMessageC2S(String key, MinecraftServer server, World world,
            SerializedDataBase data, Consumer<T> consumer, Class<T> responseType) {
        forward.accept(world, new QueueObject(key, data, consumer == null ? null : response -> server.execute(() -> consumer.accept(response)), responseType));
    }
}
