package org.mtr.mapping.registry;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.EntityHelper;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityUtilityScenarios {
    public void entityAnglesAndDispenserItemsUseNativeStateAndServerWorld() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            Entity player=new Entity(fixture.player); fixture.player.rotationYaw=17; fixture.player.rotationPitch=-23;
            assertEquals(17,EntityHelper.getYaw(player)); assertEquals(-23,EntityHelper.getPitch(player));
            EntityHelper.setYaw(player,275); EntityHelper.setPitch(player,42);
            assertEquals(275,fixture.player.rotationYaw); assertEquals(42,fixture.player.rotationPitch);
            ItemStack item=new ItemStack(new ItemConvertible(net.minecraft.init.Items.iron_ingot),3);
            item.getOrCreateTag().putString("driver_key","test"); BlockPos pos=new BlockPos(-17,65,18);
            fixture.world.getChunkFromBlockCoords(pos.getX(),pos.getZ());
            int before=fixture.world.loadedEntityList.size(); EntityHelper.spawnItem(new ServerWorld(fixture.world),pos,item);
            assertEquals(before+1,fixture.world.loadedEntityList.size());
            net.minecraft.entity.item.EntityItem dropped=(net.minecraft.entity.item.EntityItem)fixture.world.loadedEntityList.get(before);
            assertSame(fixture.world,dropped.worldObj); assertEquals(-16.5,dropped.posX); assertEquals(65.5,dropped.posY); assertEquals(18.5,dropped.posZ);
            assertSame(item.data,dropped.getEntityItem()); assertEquals("test",dropped.getEntityItem().getTagCompound().getString("driver_key"));
            EntityHelper.spawnItem(new ServerWorld(fixture.world),pos,ItemStack.getEmptyMapped()); assertEquals(before+1,fixture.world.loadedEntityList.size());
        }
    }
}
