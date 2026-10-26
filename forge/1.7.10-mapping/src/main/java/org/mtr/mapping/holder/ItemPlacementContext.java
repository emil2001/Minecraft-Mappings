package org.mtr.mapping.holder;

public final class ItemPlacementContext {
    private final ItemUsageContext usage;
    private final BlockPos target;
    public ItemPlacementContext(ItemUsageContext usage, BlockPos target) { this.usage = usage; this.target = target; }
    public BlockPos getBlockPos() { return target; }
    public BlockPos getClickedBlockPos() { return usage.getBlockPos(); }
    public World getWorld() { return usage.getWorld(); }
    public PlayerEntity getPlayer() { return usage.getPlayer(); }
    public ItemStack getStack() { return usage.getStack(); }
    public Vector3d getHitPos() { return usage.getHitPos(); }
    public Direction getSide() { return usage.getSide(); }
    public Direction getPlayerFacing() { return usage.getPlayerFacing(); }
    public float getPlayerYaw() { return usage.getPlayerYaw(); }
}
