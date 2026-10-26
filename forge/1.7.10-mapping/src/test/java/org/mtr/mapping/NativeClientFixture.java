package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.settings.GameSettings;
import java.lang.reflect.Field;

/** Supplies native client state without creating a window, audio device or login session. */
public final class NativeClientFixture implements AutoCloseable {
    public final Minecraft client;
    private final Minecraft previous;
    public NativeClientFixture() throws Exception {
        previous = Minecraft.getMinecraft();
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        client = (Minecraft) unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), Minecraft.class);
        ReflectionHelper.setPrivateValue(Minecraft.class, null, client, "theMinecraft", "field_71432_P");
        client.gameSettings = new GameSettings();
        ReflectionHelper.setPrivateValue(GameSettings.class,client.gameSettings,client,"mc","field_74317_L");
        ReflectionHelper.setPrivateValue(Minecraft.class, client, new LanguageManager(new IMetadataSerializer(), "en_US"),
            "mcLanguageManager", "field_135017_as");
        ReflectionHelper.setPrivateValue(Minecraft.class, client, true, "running", "field_71425_J");
        ReflectionHelper.setPrivateValue(Minecraft.class, client, Thread.currentThread(), "field_152352_aC");
        ReflectionHelper.setPrivateValue(Minecraft.class, client, new java.util.ArrayDeque<>(), "field_152351_aB");
    }
    public void runScheduledTasks() {
        java.util.Queue<Runnable> tasks = ReflectionHelper.getPrivateValue(Minecraft.class, client, "field_152351_aB");
        while (!tasks.isEmpty()) tasks.remove().run();
    }
    public void close() {
        ReflectionHelper.setPrivateValue(Minecraft.class, null, previous, "theMinecraft", "field_71432_P");
    }
}
