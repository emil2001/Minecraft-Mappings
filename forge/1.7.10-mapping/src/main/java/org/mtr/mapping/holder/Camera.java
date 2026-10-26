package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.LegacyCamera;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class Camera extends HolderBase<net.minecraft.client.Minecraft> {
    public Camera(net.minecraft.client.Minecraft data) { super(data); }
    public Vector3d getPos() { LegacyCamera.State state = LegacyCamera.getState(data); return new Vector3d(state.x, state.y, state.z); }
    public BlockPos getBlockPos() {
        LegacyCamera.State state = LegacyCamera.getState(data);
        return new BlockPos(MathHelper.floor(state.x), MathHelper.floor(state.y), MathHelper.floor(state.z));
    }
    public float getYaw() { return LegacyCamera.getState(data).yaw; }
    public float getPitch() { return LegacyCamera.getState(data).pitch; }
    public boolean isThirdPerson() { return data.gameSettings.thirdPersonView > 0; }
}
