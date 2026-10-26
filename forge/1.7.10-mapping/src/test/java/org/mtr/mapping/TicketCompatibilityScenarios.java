package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import static org.junit.jupiter.api.Assertions.*;

public class TicketCompatibilityScenarios {
    public void ticketCurrencyCountsAndRemovesMatchingStacksWithoutLosingOtherItems() {
        net.minecraft.entity.player.EntityPlayer nativePlayer = MtrEscalatorScenarios.player(new BlockPlacementScenarios.MemoryWorld());
        nativePlayer.inventory.mainInventory[0] = new net.minecraft.item.ItemStack(net.minecraft.init.Items.emerald, 2);
        nativePlayer.inventory.mainInventory[1] = new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot, 7);
        nativePlayer.inventory.mainInventory[2] = new net.minecraft.item.ItemStack(net.minecraft.init.Items.emerald, 5);
        PlayerEntity player = new PlayerEntity(nativePlayer);
        PlayerInventory inventory = PlayerHelper.getPlayerInventory(player);
        assertEquals(7, inventory.count(Items.getEmeraldMapped()));
        assertTrue(PlayerHelper.isHolding(player, item -> item.equals(Items.getEmeraldMapped())));
        assertFalse(PlayerHelper.isHolding(null, item -> { fail("absent player must not evaluate held-item predicate"); return true; }));
        java.util.function.Predicate<ItemStack> currency = stack -> stack.getItem().equals(Items.getEmeraldMapped());
        assertEquals(7, InventoryHelper.remove(new Inventory(inventory.data), currency, 1, true));
        assertEquals(7, inventory.count(Items.getEmeraldMapped()));
        assertEquals(4, InventoryHelper.remove(new Inventory(inventory.data), currency, 4, false));
        assertNull(nativePlayer.inventory.mainInventory[0]);
        assertEquals(3, nativePlayer.inventory.mainInventory[2].stackSize);
        assertEquals(7, nativePlayer.inventory.mainInventory[1].stackSize);
        assertEquals(3, InventoryHelper.remove(new Inventory(inventory.data), currency, -1, false));
        assertEquals(0, inventory.count(Items.getEmeraldMapped()));
        assertEquals(0, InventoryHelper.remove(new Inventory(inventory.data), currency, 100, false));
        assertNull(PlayerHelper.getPlayerInventory(null));
    }
    public void ticketBalancesAndEntryZonesSurviveNativeScoreboardNbtRoundTrip() {
        Scoreboard board = new Scoreboard(new net.minecraft.scoreboard.Scoreboard());
        ScoreboardObjective balance = ScoreboardHelper.addObjective(board, "mtr_balance", ScoreboardCriteria.DUMMY,
            new Text(TextHelper.literal("Баланс").data), ScoreboardCriterionRenderType.INTEGER);
        assertEquals(0, ScoreboardHelper.getPlayerScore(board, "MtrRider", balance));
        ScoreboardHelper.setPlayerScore(board, "MtrRider", balance, 500);
        ScoreboardHelper.incrementPlayerScore(board, "MtrRider", balance, -37);
        ScoreboardObjective zone = ScoreboardHelper.addObjective(board, "mtr_entry_zone_1", ScoreboardCriteria.DUMMY,
            new Text(TextHelper.literal("Entry Zone 1").data), ScoreboardCriterionRenderType.INTEGER);
        ScoreboardHelper.setPlayerScore(board, "MtrRider", zone, -1);
        assertThrows(IllegalArgumentException.class, () -> ScoreboardHelper.addObjective(board, "mtr_balance", ScoreboardCriteria.DUMMY,
            new Text(TextHelper.literal("replace").data), ScoreboardCriterionRenderType.INTEGER));
        net.minecraft.scoreboard.ScoreboardSaveData save = new net.minecraft.scoreboard.ScoreboardSaveData();
        save.func_96499_a(board.data);
        net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound(); save.writeToNBT(nbt);
        Scoreboard restored = new Scoreboard(new net.minecraft.scoreboard.Scoreboard());
        net.minecraft.scoreboard.ScoreboardSaveData load = new net.minecraft.scoreboard.ScoreboardSaveData();
        load.func_96499_a(restored.data); load.readFromNBT(nbt);
        ScoreboardObjective restoredBalance = ScoreboardHelper.getScoreboardObjective(restored, "mtr_balance");
        assertEquals("Баланс", restoredBalance.data.getDisplayName());
        assertEquals(463, ScoreboardHelper.getPlayerScore(restored, "MtrRider", restoredBalance));
        assertEquals(-1, ScoreboardHelper.getPlayerScore(restored, "MtrRider", ScoreboardHelper.getScoreboardObjective(restored, "mtr_entry_zone_1")));
        assertNull(ScoreboardHelper.getScoreboardObjective(restored, "missing"));
    }
}
