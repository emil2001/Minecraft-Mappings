package org.mtr.mapping.holder;

import cpw.mods.fml.relauncher.ReflectionHelper;
import org.mtr.mapping.registry.LegacyClientEvents;
import org.mtr.mapping.tool.NativeHolder;
import java.io.File;
import java.lang.reflect.Field;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class MinecraftClient extends NativeHolder<net.minecraft.client.Minecraft> {
    private static final Field RUNNING = ReflectionHelper.findField(net.minecraft.client.Minecraft.class, "running", "field_71425_J");
    public MinecraftClient(net.minecraft.client.Minecraft data) { super(data); }
    public static MinecraftClient getInstance() { return new MinecraftClient(net.minecraft.client.Minecraft.getMinecraft()); }
    public ClientWorld getWorldMapped() { return data.theWorld == null ? null : new ClientWorld(data.theWorld); }
    public ClientPlayerEntity getPlayerMapped() { return data.thePlayer == null ? null : new ClientPlayerEntity(data.thePlayer); }
    public ClientPlayNetworkHandler getNetworkHandler() { return data.getNetHandler()==null?null:new ClientPlayNetworkHandler(data.getNetHandler()); }
    public GameOptions getOptionsMapped() { return new GameOptions(data.gameSettings); }
    public Screen getCurrentScreenMapped() { return data.currentScreen == null ? null : new Screen(data.currentScreen); }
    public HitResult getCrosshairTargetMapped() { return data.objectMouseOver == null ? null : new HitResult(data.objectMouseOver); }
    public TextureManager getTextureManager() { return new TextureManager(data.getTextureManager()); }
    public SoundManager getSoundManager() { return new SoundManager(data.getSoundHandler()); }
    public Window getWindow() { return new Window(data); }
    public GameRenderer getGameRendererMapped() { return new GameRenderer(data.entityRenderer); }
    public File getRunDirectoryMapped() { return data.mcDataDir; }
    public boolean isPaused() { return data.isGamePaused(); }
    public boolean isRunning() {
        try { return RUNNING.getBoolean(data); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot read Minecraft lifecycle", exception); }
    }
    public void execute(Runnable task) { data.func_152344_a(task); }
    public float getLastFrameDuration() { return LegacyClientEvents.FRAME.getDurationTicks(); }
    public void openScreen(Screen screen) { data.displayGuiScreen(screen == null ? null : screen.data); }
}
