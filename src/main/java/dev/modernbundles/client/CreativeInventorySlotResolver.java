package dev.modernbundles.client;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

final class CreativeInventorySlotResolver {
    static final int NO_SERVER_SLOT = -1;

    private CreativeInventorySlotResolver() {
    }

    static int findInventoryMenuSlot(AbstractContainerMenu inventoryMenu, Slot creativeSlot) {
        for (int index = 0; index < inventoryMenu.slots.size(); index++) {
            Slot inventorySlot = inventoryMenu.getSlot(index);
            if (creativeSlot.isSameInventory(inventorySlot)
                && creativeSlot.getSlotIndex() == inventorySlot.getSlotIndex()) {
                return index;
            }
        }

        return NO_SERVER_SLOT;
    }
}

