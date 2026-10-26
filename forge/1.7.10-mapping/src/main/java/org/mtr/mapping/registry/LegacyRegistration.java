package org.mtr.mapping.registry;

import cpw.mods.fml.common.registry.GameData;
import org.mtr.mapping.holder.Identifier;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class LegacyRegistration {
    private static final Method BLOCK = method("registerBlock", net.minecraft.block.Block.class, String.class, int.class);
    private static final Method ITEM = method("registerItem", net.minecraft.item.Item.class, String.class, int.class);
    private static final Method MAIN = method("getMain");

    private LegacyRegistration() {}

    private static Method method(String name, Class<?>... types) {
        try {
            // Preserve both MTR namespaces through Forge's full allocation and delegate bookkeeping.
            Method method = GameData.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Forge GameData API", exception);
        }
    }

    static void register(Object value, Identifier id) {
        try {
            (value instanceof net.minecraft.block.Block ? BLOCK : ITEM).invoke(MAIN.invoke(null), value, id.toString(), -1);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException("Forge registration failed for " + id, cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot register " + id, exception);
        }
    }
}
