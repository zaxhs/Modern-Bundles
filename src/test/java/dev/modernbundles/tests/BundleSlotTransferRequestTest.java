package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import dev.modernbundles.bundle.BundleSlotTransferRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

class BundleSlotTransferRequestTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void validRequestMovesMatchingItemsFromTheCarriedBundle() {
        TestMenu menu = new TestMenu(7, slot(new ItemStack(Items.IRON_INGOT, 2)));
        menu.setCarried(bundleWithItems(new ItemStack(Items.DIRT), new ItemStack(Items.IRON_INGOT, 10)));

        assertEquals(10, BundleSlotTransferRequest.apply(menu, null, 7, 0));
        assertEquals(12, menu.getSlot(0).getItem().getCount());
        assertEquals(1, menu.getCarried().get(DataComponents.BUNDLE_CONTENTS).size());
        assertEquals(Items.DIRT, menu.getCarried().get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getItem());
    }

    @Test
    void staleAndInvalidSlotRequestsAreRejected() {
        TestMenu menu = new TestMenu(7, slot(new ItemStack(Items.IRON_INGOT)));
        menu.setCarried(bundleWithItems(new ItemStack(Items.IRON_INGOT)));

        assertEquals(0, BundleSlotTransferRequest.apply(menu, null, 8, 0));
        assertEquals(0, BundleSlotTransferRequest.apply(menu, null, 7, -1));
        assertEquals(0, BundleSlotTransferRequest.apply(menu, null, 7, 1));
        assertEquals(1, menu.getSlot(0).getItem().getCount());
    }

    @Test
    void requestRequiresAnEligibleCarriedBundleAndOccupiedExactTarget() {
        TestMenu menu = new TestMenu(7, slot(new ItemStack(Items.IRON_INGOT)));

        menu.setCarried(new ItemStack(Items.CHEST));
        assertEquals(0, BundleSlotTransferRequest.apply(menu, null, 7, 0));

        menu.setCarried(bundleWithItems(new ItemStack(Items.IRON_INGOT)).copyWithCount(2));
        assertEquals(0, BundleSlotTransferRequest.apply(menu, null, 7, 0));

        menu.setCarried(bundleWithItems(new ItemStack(Items.GOLD_INGOT)));
        assertEquals(0, BundleSlotTransferRequest.apply(menu, null, 7, 0));

        menu.setCarried(bundleWithItems(new ItemStack(Items.IRON_INGOT)));
        menu.getSlot(0).set(ItemStack.EMPTY);
        assertEquals(0, BundleSlotTransferRequest.apply(menu, null, 7, 0));
    }

    @Test
    void fullMatchingTargetIsValidForHooksButCannotTransfer() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.IRON_INGOT, 10));
        TestMenu fullMenu = new TestMenu(7, slot(new ItemStack(Items.IRON_INGOT, 64)));
        fullMenu.setCarried(bundle.copy());

        assertTrue(BundleSlotTransferRequest.isValidTarget(fullMenu, null, 7, 0));
        assertFalse(BundleSlotTransferRequest.canApply(fullMenu, null, 7, 0));
        assertEquals(0, BundleSlotTransferRequest.apply(fullMenu, null, 7, 0));
        assertEquals(64, fullMenu.getSlot(0).getItem().getCount());
        assertEquals(10, fullMenu.getCarried().get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getCount());
    }

    @Test
    void requestRejectsInactiveFakeAndInsertionRejectingTargets() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.IRON_INGOT, 10));

        TestMenu inactiveMenu = new TestMenu(
            7,
            new TestSlot(new ItemStack(Items.IRON_INGOT), true, false, false)
        );
        inactiveMenu.setCarried(bundle.copy());
        assertEquals(0, BundleSlotTransferRequest.apply(inactiveMenu, null, 7, 0));

        TestMenu fakeMenu = new TestMenu(7, new TestSlot(new ItemStack(Items.IRON_INGOT), true, true, true));
        fakeMenu.setCarried(bundle.copy());
        assertEquals(0, BundleSlotTransferRequest.apply(fakeMenu, null, 7, 0));

        TestMenu rejectingMenu = new TestMenu(
            7,
            new TestSlot(new ItemStack(Items.IRON_INGOT), false, true, false)
        );
        rejectingMenu.setCarried(bundle.copy());
        assertEquals(0, BundleSlotTransferRequest.apply(rejectingMenu, null, 7, 0));
    }

    @Test
    void requestRejectsSlotsThatThePlayerMayNotModify() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.IRON_INGOT, 10));

        TestMenu pickupLockedMenu = new TestMenu(
            7,
            new TestSlot(new ItemStack(Items.IRON_INGOT), true, true, false) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            }
        );
        pickupLockedMenu.setCarried(bundle.copy());

        assertTrue(BundleSlotTransferRequest.isMatchingTarget(pickupLockedMenu, 7, 0));
        assertFalse(BundleSlotTransferRequest.isValidTarget(pickupLockedMenu, null, 7, 0));
        assertFalse(BundleSlotTransferRequest.canApply(pickupLockedMenu, null, 7, 0));
        assertEquals(0, BundleSlotTransferRequest.apply(pickupLockedMenu, null, 7, 0));
        assertEquals(1, pickupLockedMenu.getSlot(0).getItem().getCount());
        assertEquals(
            10,
            pickupLockedMenu.getCarried().get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getCount()
        );

        TestMenu modificationLockedMenu = new TestMenu(
            7,
            new TestSlot(new ItemStack(Items.IRON_INGOT), true, true, false) {
                @Override
                public boolean allowModification(Player player) {
                    return false;
                }
            }
        );
        modificationLockedMenu.setCarried(bundle.copy());

        assertFalse(BundleSlotTransferRequest.canApply(modificationLockedMenu, null, 7, 0));
        assertEquals(0, BundleSlotTransferRequest.apply(modificationLockedMenu, null, 7, 0));
        assertEquals(1, modificationLockedMenu.getSlot(0).getItem().getCount());
        assertEquals(
            10,
            modificationLockedMenu.getCarried().get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0).getCount()
        );
    }

    private static ItemStack bundleWithItems(ItemStack... items) {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(items)));
        return bundle;
    }

    private static Slot slot(ItemStack stack) {
        return new Slot(new SimpleContainer(stack), 0, 0, 0);
    }

    private static final class TestMenu extends AbstractContainerMenu {
        private TestMenu(int containerId, Slot... slots) {
            super(null, containerId);
            for (Slot slot : slots) {
                this.addSlot(slot);
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static class TestSlot extends Slot {
        private final boolean acceptsItems;
        private final boolean active;
        private final boolean fake;

        private TestSlot(ItemStack stack, boolean acceptsItems, boolean active, boolean fake) {
            super(new SimpleContainer(stack), 0, 0, 0);
            this.acceptsItems = acceptsItems;
            this.active = active;
            this.fake = fake;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.acceptsItems;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }

        @Override
        public boolean isFake() {
            return this.fake;
        }
    }
}

