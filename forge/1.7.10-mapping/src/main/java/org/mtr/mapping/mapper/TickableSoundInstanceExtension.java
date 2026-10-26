package org.mtr.mapping.mapper;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public interface TickableSoundInstanceExtension extends net.minecraft.client.audio.ITickableSound {
    boolean isRelative();
    boolean isRepeatable();
    boolean isDone();
    void tick2();
    @Override default void update() { tick2(); }
    @Override default boolean isDonePlaying() { return isDone(); }
}
