package dev.modernbundles.bundle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BundleInteractionPolicy {
    private BundleInteractionPolicy() {
    }

    public static boolean shouldRenderTooltip(boolean carriedStackEmpty, ItemStack hoveredStack) {
        return carriedStackEmpty
            || hoveredStack.getTooltipImage().filter(BundleTooltipData.class::isInstance).isPresent();
    }

    public static boolean shouldClearSelectionBeforeClick(ClickType type) {
        return type == ClickType.QUICK_MOVE
            || type == ClickType.SWAP;
    }

    public static boolean shouldOwnBundleDrag(
        ItemStack carriedStack,
        int mouseButton,
        boolean bundleDragEnabled,
        boolean touchscreen
    ) {
        return bundleDragEnabled
            && !touchscreen
            && (mouseButton == 0 || mouseButton == 1)
            && BundleContentsOperations.isBundle(carriedStack)
            && carriedStack.getCount() == 1;
    }

    public static boolean shouldBeginBundleDrag(
        ItemStack carriedStack,
        int mouseButton,
        boolean bundleDragEnabled,
        boolean touchscreen,
        boolean eligibleStartSlot
    ) {
        return eligibleStartSlot
            && shouldOwnBundleDrag(carriedStack, mouseButton, bundleDragEnabled, touchscreen);
    }

    public static boolean shouldKeepBundleDragPending(
        int mouseButton,
        boolean startedOnEmptySlot,
        boolean enteredDifferentSlot
    ) {
        return mouseButton == 0
            && startedOnEmptySlot
            && !enteredDifferentSlot;
    }

    public static boolean canDragIntoBundle(
        ItemStack carriedStack,
        ItemStack slottedStack,
        boolean canTakeFromSlot
    ) {
        return canStartDragIntoBundle(carriedStack, canTakeFromSlot)
            && !slottedStack.isEmpty()
            && slottedStack.canFitInsideContainerItems();
    }

    public static boolean canStartDragIntoBundle(ItemStack carriedStack, boolean canTakeFromSlot) {
        return BundleContentsOperations.isBundle(carriedStack)
            && carriedStack.getCount() == 1
            && carriedStack.has(DataComponents.BUNDLE_CONTENTS)
            && canTakeFromSlot;
    }

    public static boolean canDragOutOfBundle(ItemStack carriedStack, Slot slot) {
        if (!isSingleBundleWithContents(carriedStack) || !slot.isActive() || slot.isFake()) {
            return false;
        }

        ItemStack slottedStack = slot.getItem();
        ItemStack bundledStack = slottedStack.isEmpty()
            ? BundleContentsOperations.getSelectedOrFirstItem(carriedStack)
            : BundleContentsOperations.findMatchingItem(carriedStack, slottedStack);
        return !bundledStack.isEmpty()
            && slot.mayPlace(bundledStack)
            && (!slottedStack.isEmpty() || maximumStackSize(slot, bundledStack) > 0);
    }

    public static boolean shouldMergeBundleIntoSlot(ItemStack carriedStack, Slot slot) {
        return !slot.getItem().isEmpty() && canDragOutOfBundle(carriedStack, slot);
    }

    public static boolean canTransferMatchingToSlot(ItemStack carriedStack, Slot slot) {
        if (!shouldMergeBundleIntoSlot(carriedStack, slot)) {
            return false;
        }

        ItemStack bundledStack = BundleContentsOperations.findMatchingItem(carriedStack, slot.getItem());
        return slot.getItem().getCount() < maximumStackSize(slot, bundledStack);
    }

    private static boolean isSingleBundleWithContents(ItemStack stack) {
        return BundleContentsOperations.isBundle(stack)
            && stack.getCount() == 1
            && stack.has(DataComponents.BUNDLE_CONTENTS);
    }

    private static int maximumStackSize(Slot slot, ItemStack stack) {
        return Math.min(slot.getMaxStackSize(stack), stack.getMaxStackSize());
    }
}

