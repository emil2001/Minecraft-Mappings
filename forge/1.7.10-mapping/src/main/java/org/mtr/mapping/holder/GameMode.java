package org.mtr.mapping.holder;

import net.minecraft.world.WorldSettings.GameType;

public enum GameMode {
    SURVIVAL(GameType.SURVIVAL),CREATIVE(GameType.CREATIVE),ADVENTURE(GameType.ADVENTURE),NOT_SET(GameType.NOT_SET);
    public final GameType data;
    GameMode(GameType data) { this.data=data; }
    public static GameMode getCreativeMapped() { return CREATIVE; }
    public static GameMode getSurvivalMapped() { return SURVIVAL; }
    public static GameMode convert(GameType data) {
        for (GameMode mode : values()) if (mode.data==data) return mode;
        throw new IllegalArgumentException("Unknown native game mode: "+data);
    }
}
