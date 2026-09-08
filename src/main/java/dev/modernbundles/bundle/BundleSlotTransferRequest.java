package dev.modernbundles.bundle;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BundleSlotTransferRequest {
    private BundleSlotTransferRequest() {
    }

    public static boolean isMatchingTarget(AbstractContainerMenu menu, int expectedContainerId, int slotIndex) {
        if (menu.containerId != expectedContainerId || slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }

        ItemStack bundle = menu.getCarried();
        Slot slot = menu.getSlot(slotIndex);
        return BundleInteractionPolicy.shouldMergeBundleIntoSlot(bundle, slot);
    }

    public static boolean isValidTarget(
        AbstractContainerMenu menu,
        Player player,
        int expectedContainerId,
        int slotIndex
    ) {
        return isMatchingTarget(menu, expectedContainerId, slotIndex)
            && menu.getSlot(slotIndex).allowModification(player);
    }

    public static boolean canApply(
        AbstractContainerMenu menu,
        Player player,
        int expectedContainerId,
        int slotIndex
    ) {
        return isValidTarget(menu, player, expectedContainerId, slotIndex)
            && BundleInteractionPolicy.canTransferMatchingToSlot(menu.getCarried(), menu.getSlot(slotIndex));
    }

    public static int apply(AbstractContainerMenu menu, Player player, int expectedContainerId, int slotIndex) {
        if (!canApply(menu, player, expectedContainerId, slotIndex)) {
            return 0;
        }

        return BundleContentsOperations.tryTransferMatchingToSlot(menu.getCarried(), menu.getSlot(slotIndex));
    }
}

