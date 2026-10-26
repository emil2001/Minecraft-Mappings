package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;
import java.util.function.Predicate;

public final class PlayerHelper {
    private PlayerHelper() {}
    public static PlayerInventory getPlayerInventory(PlayerEntity player) { return player == null ? null : new PlayerInventory(player.data.inventory); }
    public static boolean isHolding(PlayerEntity player, Predicate<Item> predicate) {
        return player != null && !player.getMainHandStack().isEmpty() && predicate.test(player.getMainHandStack().getItem());
    }
}
