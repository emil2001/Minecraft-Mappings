package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.MinecraftServer;

public enum GameRule {
    DO_DAYLIGHT_CYCLE;
    public boolean getBooleanGameRule(MinecraftServer server) {
        return server.getOverworld().data.getGameRules().getGameRuleBooleanValue("doDaylightCycle");
    }
}
