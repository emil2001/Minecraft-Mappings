package org.mtr.mapping.tool;

import org.mtr.mapping.holder.*;

public final class RandomBlockTicks {
    private RandomBlockTicks() {}
    public static void tick(net.minecraft.block.Block block,net.minecraft.world.World world,int x,int y,int z,java.util.Random random) {
        if (!(block instanceof BlockAbstractMapping)) { block.updateTick(world,x,y,z,random); return; }
        if (!(world instanceof net.minecraft.world.WorldServer) || world.isRemote) return;
        BlockAbstractMapping mapped=(BlockAbstractMapping)block; BlockPos pos=new BlockPos(x,y,z);
        BlockState state=new World(world).getBlockState(pos);
        if (state.getBlock().data==block && mapped.hasRandomTicks2(state))
            mapped.randomTick2(state,new ServerWorld((net.minecraft.world.WorldServer)world),pos,new org.mtr.mapping.holder.Random(random));
    }
}
