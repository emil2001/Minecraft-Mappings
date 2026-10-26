package org.mtr.mapping.holder;

public abstract class EntityAbstractMapping extends net.minecraft.entity.Entity {
    protected EntityAbstractMapping(net.minecraft.world.World world) { super(world); }
    public World getEntityWorld2() { return new World(worldObj); }
    public Vector3d getPos2() { return new Vector3d(posX,boundingBox.minY,posZ); }
    public Vector3d getCameraPosVec2(float partialTick) {
        return new Vector3d(lastTickPosX+(posX-lastTickPosX)*partialTick,
            lastTickPosY+(posY-lastTickPosY)*partialTick-yOffset+getEyeHeight(),
            lastTickPosZ+(posZ-lastTickPosZ)*partialTick);
    }
    public void kill2() { setDead(); }
}
