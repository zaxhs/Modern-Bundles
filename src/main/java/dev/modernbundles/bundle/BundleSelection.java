package dev.modernbundles.bundle;

import java.util.stream.StreamSupport;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

public final class BundleSelection {
    public static final int NO_SELECTED_ITEM = -1;

    private BundleSelection() {
    }

    public static int getSelectedItem(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return NO_SELECTED_ITEM;
        }

        int selectedItem = selectionAccess(contents).modernbundles$getSelectedItem();
        return isSelectable(contents, selectedItem) ? selectedItem : NO_SELECTED_ITEM;
    }

    public static ItemStack getSelectedItemStack(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        int selectedItem = getSelectedItem(bundle);
        return contents != null && selectedItem != NO_SELECTED_ITEM
            ? contents.getItemUnsafe(selectedItem)
            : ItemStack.EMPTY;
    }

    public static int getNumberOfItemsToShow(ItemStack bundle) {
        return getNumberOfItemsToShow(bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
    }

    public static int getNumberOfItemsToShow(BundleContents contents) {
        int size = contents.size();
        int maximum = size > 12 ? 11 : 12;
        int remainder = size % 4;
        int emptyGridSlots = remainder == 0 ? 0 : 4 - remainder;
        return Math.min(size, maximum - emptyGridSlots);
    }

    public static void setSelectedItem(ItemStack bundle, int selectedItem) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        int nextSelection = contents != null && isSelectable(contents, selectedItem)
            ? selectedItem
            : NO_SELECTED_ITEM;
        replaceSelection(bundle, contents, nextSelection);
    }

    public static void toggleSelectedItem(ItemStack bundle, int selectedItem) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (selectedItem == getSelectedItem(bundle) || contents == null || !isSelectable(contents, selectedItem)) {
            clear(bundle);
        } else {
            setSelectedItem(bundle, selectedItem);
        }
    }

    public static void clear(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        replaceSelection(bundle, contents, NO_SELECTED_ITEM);
    }

    static boolean isSelectable(ItemStack bundle, int selectedItem) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        return contents != null && isSelectable(contents, selectedItem);
    }

    private static boolean isSelectable(BundleContents contents, int selectedItem) {
        return selectedItem >= 0 && selectedItem < contents.size();
    }

    private static void replaceSelection(ItemStack bundle, BundleContents contents, int selectedItem) {
        if (contents == null || selectionAccess(contents).modernbundles$getSelectedItem() == selectedItem) {
            return;
        }

        BundleContents replacement = new BundleContents(
            StreamSupport.stream(contents.items().spliterator(), false).toList()
        );
        selectionAccess(replacement).modernbundles$setSelectedItem(selectedItem);
        bundle.set(DataComponents.BUNDLE_CONTENTS, replacement);
    }

    private static BundleContentsSelectionAccess selectionAccess(BundleContents contents) {
        return (BundleContentsSelectionAccess)(Object)contents;
    }
}

