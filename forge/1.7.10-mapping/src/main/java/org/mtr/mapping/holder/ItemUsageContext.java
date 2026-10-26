package org.mtr.mapping.holder;

public class ItemUsageContext {
    private final World world;
    private final PlayerEntity player;
    private final ItemStack stack;
    private final BlockPos pos;
    private final Direction side;
    private final Vector3d hit;

    public ItemUsageContext(net.minecraft.world.World world, net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.item.ItemStack stack, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        this.world = new World(world);
        this.player = player == null ? null : new PlayerEntity(player);
        this.stack = new ItemStack(stack);
        pos = new BlockPos(x, y, z);
        this.side = Direction.convert(net.minecraftforge.common.util.ForgeDirection.getOrientation(side));
        hit = new Vector3d(x + (double) hitX, y + (double) hitY, z + (double) hitZ);
    }
    public World getWorld() { return world; }
    public PlayerEntity getPlayer() { return player; }
    public ItemStack getStack() { return stack; }
    public BlockPos getBlockPos() { return pos; }
    public Direction getSide() { return side; }
    public Vector3d getHitPos() { return hit; }
    public Direction getPlayerFacing() { return Direction.fromRotation(getPlayerYaw()); }
    public float getPlayerYaw() { return player == null ? 0 : player.data.rotationYaw; }
    public Hand getHand() { return Hand.MAIN_HAND; }
}
