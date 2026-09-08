package dev.modernbundles.bundle;

import java.util.List;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class BundleSelectionRequest {
    private BundleSelectionRequest() {
    }

    public static boolean apply(
        AbstractContainerMenu menu,
        int expectedContainerId,
        int slotIndex,
        int selectedItemIndex,
        List<ItemStack> expectedContents
    ) {
        if (menu.containerId != expectedContainerId || slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }

        ItemStack bundle = menu.getSlot(slotIndex).getItem();
        if (!BundleSnapshot.matches(bundle, expectedContents)) {
            return false;
        }

        if (selectedItemIndex == BundleSelection.NO_SELECTED_ITEM) {
            BundleSelection.clear(bundle);
        } else if (BundleSelection.isSelectable(bundle, selectedItemIndex)) {
            BundleSelection.setSelectedItem(bundle, selectedItemIndex);
        } else {
            return false;
        }

        return true;
    }
}

