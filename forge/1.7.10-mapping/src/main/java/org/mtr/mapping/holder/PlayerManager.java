package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;
import java.util.UUID;

public final class PlayerManager extends NativeHolder<net.minecraft.server.management.ServerConfigurationManager> {
    public PlayerManager(net.minecraft.server.management.ServerConfigurationManager data) { super(data); }
    public ServerPlayerEntity getPlayer(UUID id) {
        for (Object entry : data.playerEntityList) {
            net.minecraft.entity.player.EntityPlayerMP player = (net.minecraft.entity.player.EntityPlayerMP) entry;
            if (player.getUniqueID().equals(id)) return new ServerPlayerEntity(player);
        }
        return null;
    }
}
