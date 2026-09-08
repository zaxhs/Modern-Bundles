package dev.modernbundles.bundle;

import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;

public final class BundleInteractionHooks {
    private BundleInteractionHooks() {
    }

    public static boolean onStackedOnOther(AbstractContainerMenu menu, Slot slot, Player player) {
        return CommonHooks.onItemStackedOn(
            menu.getCarried(),
            slot.getItem(),
            slot,
            ClickAction.SECONDARY,
            player,
            carriedSlotAccess(menu)
        );
    }

    private static SlotAccess carriedSlotAccess(AbstractContainerMenu menu) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return menu.getCarried();
            }

            @Override
            public boolean set(ItemStack stack) {
                menu.setCarried(stack);
                return true;
            }
        };
    }
}

