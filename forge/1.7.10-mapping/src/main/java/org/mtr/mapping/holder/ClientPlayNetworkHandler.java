package org.mtr.mapping.holder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.mtr.mapping.tool.NativeHolder;
import java.util.UUID;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class ClientPlayNetworkHandler extends NativeHolder<NetHandlerPlayClient> {
    public ClientPlayNetworkHandler(NetHandlerPlayClient data) { super(data); }
    public PlayerListEntry getPlayerListEntry(UUID uuid) {
        Minecraft client=Minecraft.getMinecraft();
        // Legacy tab packets contain names/ping only; MTR asks solely for the local player's authoritative mode.
        if (client.getNetHandler()!=data || client.thePlayer==null || client.playerController==null || !client.thePlayer.getUniqueID().equals(uuid)) return null;
        return new PlayerListEntry(client.playerController);
    }
}
