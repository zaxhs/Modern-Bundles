package dev.modernbundles.bundle;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

public final class BundleContentsOperations {
    private BundleContentsOperations() {
    }

    public static boolean isBundle(ItemStack stack) {
        return stack.getItem() instanceof BundleItem;
    }

    public static int tryInsert(ItemStack bundle, ItemStack insertedStack) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }

        BundleContents.Mutable mutableContents = new BundleContents.Mutable(contents);
        int inserted = mutableContents.tryInsert(insertedStack);
        if (inserted > 0) {
            replaceContents(bundle, mutableContents.toImmutable());
        }
        return inserted;
    }

    public static int tryTransfer(ItemStack bundle, Slot slot, Player player) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        ItemStack slottedStack = slot.getItem();
        if (contents == null || slottedStack.isEmpty() || !slottedStack.canFitInsideContainerItems()) {
            return 0;
        }

        BundleContents.Mutable mutableContents = new BundleContents.Mutable(contents);
        int inserted = mutableContents.tryTransfer(slot, player);
        if (inserted > 0) {
            replaceContents(bundle, mutableContents.toImmutable());
        }
        return inserted;
    }

    public static int tryTransferMatchingToSlot(ItemStack bundle, Slot slot) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        ItemStack slottedStack = slot.getItem();
        if (contents == null || contents.isEmpty() || slottedStack.isEmpty()) {
            return 0;
        }

        List<ItemStack> remainingItems = contents.itemCopyStream()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        int transferred = 0;
        for (int index = 0; index < remainingItems.size(); index++) {
            ItemStack bundledStack = remainingItems.get(index);
            int maximum = Math.min(slot.getMaxStackSize(bundledStack), bundledStack.getMaxStackSize());
            if (!ItemStack.isSameItemSameComponents(bundledStack, slottedStack)
                || !slot.mayPlace(bundledStack)
                || slot.getItem().getCount() >= maximum) {
                continue;
            }

            int previousCount = bundledStack.getCount();
            int availableSpace = maximum - slot.getItem().getCount();
            ItemStack remainder = slot.safeInsert(bundledStack, availableSpace);
            int moved = previousCount - remainder.getCount();
            if (moved > 0) {
                transferred += moved;
                if (remainder.isEmpty()) {
                    remainingItems.remove(index--);
                } else {
                    remainingItems.set(index, remainder);
                }
            }
        }

        if (transferred > 0) {
            replaceContents(bundle, new BundleContents(List.copyOf(remainingItems)));
        }
        return transferred;
    }

    public static ItemStack findMatchingItem(ItemStack bundle, ItemStack target) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || target.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (ItemStack bundledStack : contents.items()) {
            if (ItemStack.isSameItemSameComponents(bundledStack, target)) {
                return bundledStack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getSelectedOrFirstItem(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int selectedItem = BundleSelection.getSelectedItem(bundle);
        return contents.getItemUnsafe(selectedItem == BundleSelection.NO_SELECTED_ITEM ? 0 : selectedItem);
    }

    public static ItemStack removeSelected(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            BundleSelection.clear(bundle);
            return ItemStack.EMPTY;
        }

        int selectedItem = BundleSelection.getSelectedItem(bundle);
        int removedIndex = selectedItem == BundleSelection.NO_SELECTED_ITEM ? 0 : selectedItem;
        List<ItemStack> remainingItems = contents.itemCopyStream()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ItemStack removedStack = remainingItems.remove(removedIndex);
        replaceContents(bundle, new BundleContents(List.copyOf(remainingItems)));
        return removedStack;
    }

    public static void clearContents(ItemStack bundle) {
        if (bundle.has(DataComponents.BUNDLE_CONTENTS)) {
            replaceContents(bundle, BundleContents.EMPTY);
        } else {
            BundleSelection.clear(bundle);
        }
    }

    public static void replaceContents(ItemStack bundle, BundleContents contents) {
        bundle.set(DataComponents.BUNDLE_CONTENTS, contents);
        BundleSelection.clear(bundle);
    }
}

