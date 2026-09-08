package dev.modernbundles.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

class CreativeInventorySlotResolverTest {
    @Test
    void creativeHotbarSlotsMapToInventoryMenuSlots() {
        SimpleContainer filler = new SimpleContainer(36);
        SimpleContainer playerInventory = new SimpleContainer(41);
        TestMenu inventoryMenu = new TestMenu();
        for (int index = 0; index < 36; index++) {
            inventoryMenu.add(new Slot(filler, index, 0, 0));
        }
        for (int index = 0; index < 9; index++) {
            inventoryMenu.add(new Slot(playerInventory, index, 0, 0));
        }
        inventoryMenu.add(new Slot(playerInventory, 40, 0, 0));

        assertEquals(36, CreativeInventorySlotResolver.findInventoryMenuSlot(
            inventoryMenu,
            new Slot(playerInventory, 0, 0, 0)
        ));
        assertEquals(44, CreativeInventorySlotResolver.findInventoryMenuSlot(
            inventoryMenu,
            new Slot(playerInventory, 8, 0, 0)
        ));
        assertEquals(45, CreativeInventorySlotResolver.findInventoryMenuSlot(
            inventoryMenu,
            new Slot(playerInventory, 40, 0, 0)
        ));
    }

    @Test
    void inventoryTabWrappersMapToTheirTargetSlots() {
        SimpleContainer playerInventory = new SimpleContainer(9);
        TestMenu inventoryMenu = new TestMenu();
        for (int index = 0; index < 9; index++) {
            inventoryMenu.add(new Slot(playerInventory, index, 0, 0));
        }

        Slot target = inventoryMenu.getSlot(4);
        assertEquals(4, CreativeInventorySlotResolver.findInventoryMenuSlot(
            inventoryMenu,
            new TestSlotWrapper(target)
        ));
    }

    @Test
    void clientOnlyCreativeSlotsHaveNoServerSlot() {
        TestMenu inventoryMenu = new TestMenu();
        inventoryMenu.add(new Slot(new SimpleContainer(1), 0, 0, 0));

        Slot catalogSlot = new Slot(new SimpleContainer(45), 3, 0, 0);
        assertEquals(
            CreativeInventorySlotResolver.NO_SERVER_SLOT,
            CreativeInventorySlotResolver.findInventoryMenuSlot(inventoryMenu, catalogSlot)
        );
    }

    private static final class TestMenu extends AbstractContainerMenu {
        private TestMenu() {
            super(null, 0);
        }

        private void add(Slot slot) {
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

    private static final class TestSlotWrapper extends Slot {
        private final Slot target;

        private TestSlotWrapper(Slot target) {
            super(target.container, target.index, 0, 0);
            this.target = target;
        }

        @Override
        public int getSlotIndex() {
            return this.target.getSlotIndex();
        }

        @Override
        public boolean isSameInventory(Slot other) {
            return this.target.isSameInventory(other);
        }
    }
}

