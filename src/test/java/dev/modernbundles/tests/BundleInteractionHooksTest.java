package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import dev.modernbundles.bundle.BundleInteractionHooks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;

class BundleInteractionHooksTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void stackedOnOtherEventCanHandleAFullMatchingTargetAndReplaceTheCarriedStack() {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(
            DataComponents.BUNDLE_CONTENTS,
            new BundleContents(List.of(new ItemStack(Items.IRON_INGOT)))
        );
        Slot slot = new Slot(new SimpleContainer(new ItemStack(Items.IRON_INGOT, 64)), 0, 0, 0);
        TestMenu menu = new TestMenu(7, slot);
        menu.setCarried(bundle);
        CancellingListener listener = new CancellingListener(bundle, slot);

        NeoForge.EVENT_BUS.register(listener);
        try {
            assertTrue(BundleInteractionHooks.onStackedOnOther(menu, slot, null));
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }

        assertTrue(listener.called);
        assertTrue(menu.getCarried().is(Items.CHEST));
    }

    private static final class CancellingListener {
        private final ItemStack expectedBundle;
        private final Slot expectedSlot;
        private boolean called;

        private CancellingListener(ItemStack expectedBundle, Slot expectedSlot) {
            this.expectedBundle = expectedBundle;
            this.expectedSlot = expectedSlot;
        }

        @SubscribeEvent
        public void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
            this.called = true;
            assertSame(this.expectedBundle, event.getCarriedItem());
            assertSame(this.expectedSlot, event.getSlot());
            assertSame(this.expectedSlot.getItem(), event.getStackedOnItem());
            assertEquals(ClickAction.SECONDARY, event.getClickAction());
            event.getCarriedSlotAccess().set(new ItemStack(Items.CHEST));
            event.setCanceled(true);
        }
    }

    private static final class TestMenu extends AbstractContainerMenu {
        private TestMenu(int containerId, Slot slot) {
            super(null, containerId);
            this.addSlot(slot);
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
}

