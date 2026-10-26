package org.mtr.mapping.holder;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.world.WorldSettings.GameType;
import org.mtr.mapping.tool.NativeHolder;
import java.lang.reflect.Field;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class PlayerListEntry extends NativeHolder<PlayerControllerMP> {
    private static final Field MODE=ReflectionHelper.findField(PlayerControllerMP.class,"currentGameType","field_78779_k");
    public PlayerListEntry(PlayerControllerMP data) { super(data); }
    public GameMode getGameMode() {
        try { return GameMode.convert((GameType)MODE.get(data)); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot read local game mode",exception); }
    }
}
