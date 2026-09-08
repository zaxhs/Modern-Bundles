package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import dev.modernbundles.bundle.BundleContentsOperations;
import dev.modernbundles.bundle.BundleColor;
import dev.modernbundles.bundle.BundleSelection;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;

class BundleContentsOperationsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void insertionUsesVanillaCapacityAndMovesACombinedTypeToTheFront() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.DIRT, 2), new ItemStack(Items.STONE, 3));
        BundleSelection.setSelectedItem(bundle, 1);
        ItemStack insertedStack = new ItemStack(Items.STONE, 64);

        assertEquals(59, BundleContentsOperations.tryInsert(bundle, insertedStack));
        assertEquals(5, insertedStack.getCount());

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        assertEquals(2, contents.size());
        assertTrue(contents.getItemUnsafe(0).is(Items.STONE));
        assertEquals(62, contents.getItemUnsafe(0).getCount());
        assertTrue(contents.getItemUnsafe(1).is(Items.DIRT));
        assertEquals(Fraction.ONE, contents.weight());
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void failedInsertionDoesNotChangeContentsOrSelection() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE, 64));
        BundleSelection.setSelectedItem(bundle, 0);
        ItemStack insertedStack = new ItemStack(Items.DIRT);

        assertEquals(0, BundleContentsOperations.tryInsert(bundle, insertedStack));
        assertEquals(1, insertedStack.getCount());
        assertEquals(0, BundleSelection.getSelectedItem(bundle));
        assertEquals(64, bundle.get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getCount());
    }

    @Test
    void transferRejectsContainerItemsBeforeTakingThemFromTheSlot() {
        ItemStack bundle = bundleWithItems();
        SimpleContainer container = new SimpleContainer(new ItemStack(Items.SHULKER_BOX));
        Slot slot = new Slot(container, 0, 0, 0);

        assertEquals(0, BundleContentsOperations.tryTransfer(bundle, slot, null));
        assertTrue(slot.getItem().is(Items.SHULKER_BOX));
        assertTrue(bundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty());
    }

    @Test
    void matchingTransferFindsEveryExactEntryAndPreservesUnrelatedOrder() {
        ItemStack matchingIron = new ItemStack(Items.IRON_INGOT, 4);
        matchingIron.set(DataComponents.CUSTOM_NAME, Component.literal("Matching iron"));
        ItemStack otherMatchingIron = matchingIron.copyWithCount(3);
        ItemStack bundle = bundleWithItems(
            new ItemStack(Items.DIRT, 2),
            matchingIron,
            new ItemStack(Items.DIAMOND),
            otherMatchingIron
        );
        BundleSelection.setSelectedItem(bundle, 0);
        ItemStack slottedIron = matchingIron.copyWithCount(2);
        Slot slot = new Slot(new SimpleContainer(slottedIron), 0, 0, 0);

        assertEquals(7, BundleContentsOperations.tryTransferMatchingToSlot(bundle, slot));
        assertEquals(9, slot.getItem().getCount());
        assertEquals(Component.literal("Matching iron"), slot.getItem().get(DataComponents.CUSTOM_NAME));

        BundleContents remaining = bundle.get(DataComponents.BUNDLE_CONTENTS);
        assertEquals(2, remaining.size());
        assertTrue(remaining.getItemUnsafe(0).is(Items.DIRT));
        assertTrue(remaining.getItemUnsafe(1).is(Items.DIAMOND));
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void matchingTransferRespectsCustomSlotCapacityAndLeavesTheRemainder() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.IRON_INGOT, 10));
        BundleSelection.setSelectedItem(bundle, 0);
        Slot slot = new LimitedSlot(new SimpleContainer(new ItemStack(Items.IRON_INGOT, 14)), 16, true);

        assertEquals(2, BundleContentsOperations.tryTransferMatchingToSlot(bundle, slot));
        assertEquals(16, slot.getItem().getCount());
        assertEquals(8, bundle.get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getCount());
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void matchingTransferStillRespectsTheItemsOwnStackLimit() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.ENDER_PEARL, 5));
        Slot slot = new LimitedSlot(new SimpleContainer(new ItemStack(Items.ENDER_PEARL, 15)), 64, true);

        assertEquals(1, BundleContentsOperations.tryTransferMatchingToSlot(bundle, slot));
        assertEquals(16, slot.getItem().getCount());
        assertEquals(4, bundle.get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getCount());
    }

    @Test
    void matchingTransferFailuresDoNotMutateContentsSlotsOrSelection() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.IRON_INGOT, 10));
        BundleSelection.setSelectedItem(bundle, 0);
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT, 2);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("Different iron"));
        Slot mismatchedSlot = new Slot(new SimpleContainer(namedIron), 0, 0, 0);

        assertEquals(0, BundleContentsOperations.tryTransferMatchingToSlot(bundle, mismatchedSlot));
        assertEquals(2, mismatchedSlot.getItem().getCount());
        assertEquals(10, bundle.get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getCount());
        assertEquals(0, BundleSelection.getSelectedItem(bundle));

        Slot rejectingSlot = new LimitedSlot(new SimpleContainer(new ItemStack(Items.IRON_INGOT, 2)), 64, false);
        assertEquals(0, BundleContentsOperations.tryTransferMatchingToSlot(bundle, rejectingSlot));
        assertEquals(2, rejectingSlot.getItem().getCount());
        assertEquals(10, bundle.get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getCount());
        assertEquals(0, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void selectedRemovalPreservesStackComponentsAndRemainingOrder() {
        ItemStack namedDirt = new ItemStack(Items.DIRT, 2);
        namedDirt.set(DataComponents.CUSTOM_NAME, Component.literal("Selected dirt"));
        ItemStack bundle = bundleWithItems(
            new ItemStack(Items.STONE, 8),
            namedDirt,
            new ItemStack(Items.DIAMOND)
        );
        BundleSelection.setSelectedItem(bundle, 1);

        ItemStack removedStack = BundleContentsOperations.removeSelected(bundle);

        assertTrue(removedStack.is(Items.DIRT));
        assertEquals(2, removedStack.getCount());
        assertEquals(Component.literal("Selected dirt"), removedStack.get(DataComponents.CUSTOM_NAME));
        BundleContents remaining = bundle.get(DataComponents.BUNDLE_CONTENTS);
        assertEquals(2, remaining.size());
        assertTrue(remaining.getItemUnsafe(0).is(Items.STONE));
        assertTrue(remaining.getItemUnsafe(1).is(Items.DIAMOND));
        assertEquals(Fraction.getFraction(9, 64), remaining.weight());
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void removalWithoutASelectionFallsBackToTheFirstType() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE, 3), new ItemStack(Items.DIRT, 2));

        ItemStack removedStack = BundleContentsOperations.removeSelected(bundle);

        assertTrue(removedStack.is(Items.STONE));
        assertEquals(3, removedStack.getCount());
        assertEquals(1, bundle.get(DataComponents.BUNDLE_CONTENTS).size());
        assertTrue(bundle.get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).is(Items.DIRT));
    }

    @Test
    void mutationsClearOnlyTheSelectionMarker() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE));
        CompoundTag root = new CompoundTag();
        root.putString("unrelated", "preserved");
        bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        BundleSelection.setSelectedItem(bundle, 0);

        BundleContentsOperations.clearContents(bundle);

        assertTrue(bundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty());
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
        assertEquals("preserved", bundle.get(DataComponents.CUSTOM_DATA).copyTag().getString("unrelated"));
    }

    @Test
    void recognitionKeepsDyedBundlesAsTheVanillaItem() {
        assertTrue(BundleContentsOperations.isBundle(new ItemStack(Items.BUNDLE)));
        ItemStack dyed = BundleColor.apply(new ItemStack(Items.BUNDLE), BundleColor.WHITE);
        assertTrue(dyed.is(Items.BUNDLE));
        assertTrue(BundleContentsOperations.isBundle(dyed));
        assertFalse(BundleContentsOperations.isBundle(new ItemStack(Items.CHEST)));
    }

    @Test
    void mixinAppliesHeldUseDurationToBaseAndMetadataDyedBundles() {
        ItemStack baseBundle = new ItemStack(Items.BUNDLE);
        ItemStack dyedBundle = BundleColor.apply(new ItemStack(Items.BUNDLE), BundleColor.WHITE);

        assertEquals(200, baseBundle.getUseDuration(null));
        assertEquals(200, dyedBundle.getUseDuration(null));
    }

    private static ItemStack bundleWithItems(ItemStack... items) {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(items)));
        return bundle;
    }

    private static final class LimitedSlot extends Slot {
        private final int maximum;
        private final boolean acceptsItems;

        private LimitedSlot(SimpleContainer container, int maximum, boolean acceptsItems) {
            super(container, 0, 0, 0);
            this.maximum = maximum;
            this.acceptsItems = acceptsItems;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return this.maximum;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.acceptsItems;
        }
    }
}

