package dev.modernbundles.client;

import java.util.List;
import javax.annotation.Nullable;

import dev.modernbundles.ModernBundles;
import dev.modernbundles.bundle.BundleContentsOperations;
import dev.modernbundles.bundle.BundleInteractionHooks;
import dev.modernbundles.bundle.BundleInteractionPolicy;
import dev.modernbundles.bundle.BundleSelection;
import dev.modernbundles.bundle.BundleSelectionScroll;
import dev.modernbundles.bundle.BundleSnapshot;
import dev.modernbundles.config.ClientConfig;
import dev.modernbundles.network.SelectBundleItemPayload;
import dev.modernbundles.network.TransferBundleToSlotPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ModernBundles.MODID, value = Dist.CLIENT)
public final class BundleMouseActions {
    private static final BundleScrollAccumulator SCROLL_ACCUMULATOR = new BundleScrollAccumulator();

    private static AbstractContainerScreen<?> selectedScreen;
    private static Slot selectedSlot;
    private static ItemStack selectedBundle = ItemStack.EMPTY;
    private static ServerSlotTarget selectedServerTarget = ServerSlotTarget.NONE;

    private BundleMouseActions() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!supportsTransfer()
            || !ClientConfig.BUNDLE_DRAG_ENABLED.get()
            || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
            || !(screen instanceof BundleDragScreenAccess dragScreen)) {
            return;
        }

        if (dragScreen.modernbundles$handleBundleDrag(
            event.getMouseX(),
            event.getMouseY(),
            event.getMouseButton(),
            event.getDragX(),
            event.getDragY()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen
            && selectFromScroll(screen, event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenRendered(ScreenEvent.Render.Post event) {
        if (selectedScreen == null) {
            return;
        }

        if (event.getScreen() != selectedScreen) {
            clearTrackedSelection();
            return;
        }

        Slot hoveredSlot = selectedScreen.getSlotUnderMouse();
        if (hoveredSlot == null
            || hoveredSlot != selectedSlot
            || !BundleContentsOperations.isBundle(hoveredSlot.getItem())) {
            clearTrackedSelection();
        } else if (BundleSelection.getSelectedItem(hoveredSlot.getItem()) == BundleSelection.NO_SELECTED_ITEM) {
            BundleSelection.clear(selectedBundle);
            forgetTrackedSelection();
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() == selectedScreen) {
            clearTrackedSelection();
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            SCROLL_ACCUMULATOR.reset();
        }
    }

    public static void clearSelectionBeforeClick(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot == null || !BundleContentsOperations.isBundle(slot.getItem())) {
            return;
        }

        if (screen == selectedScreen && slot == selectedSlot) {
            clearTrackedSelection();
        } else if (BundleSelection.getSelectedItem(slot.getItem()) != BundleSelection.NO_SELECTED_ITEM) {
            BundleSelection.clear(slot.getItem());
            ItemStack bundle = slot.getItem();
            sendSelection(resolveServerSlot(screen, slot), bundle, BundleSelection.NO_SELECTED_ITEM);
        }
    }

    public static boolean canDragIntoBundle(AbstractContainerScreen<?> screen, @Nullable Slot slot) {
        if (!supportsTransfer()) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || slot == null || !BundleInteractionPolicy.canDragIntoBundle(
            screen.getMenu().getCarried(),
            slot.getItem(),
            slot.mayPickup(player)
        )) {
            return false;
        }

        return !(screen instanceof CreativeModeInventoryScreen)
            || CreativeInventorySlotResolver.findInventoryMenuSlot(player.inventoryMenu, slot)
                != CreativeInventorySlotResolver.NO_SERVER_SLOT;
    }

    public static boolean canStartDragIntoBundle(AbstractContainerScreen<?> screen, @Nullable Slot slot) {
        if (!supportsTransfer()) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || slot == null || !BundleInteractionPolicy.canStartDragIntoBundle(
            screen.getMenu().getCarried(),
            slot.mayPickup(player)
        )) {
            return false;
        }

        return !(screen instanceof CreativeModeInventoryScreen)
            || CreativeInventorySlotResolver.findInventoryMenuSlot(player.inventoryMenu, slot)
                != CreativeInventorySlotResolver.NO_SERVER_SLOT;
    }

    public static boolean canDragOutOfBundle(AbstractContainerScreen<?> screen, @Nullable Slot slot) {
        if (!supportsTransfer()) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null
            || slot == null
            || !BundleInteractionPolicy.canDragOutOfBundle(screen.getMenu().getCarried(), slot)) {
            return false;
        }

        return !(screen instanceof CreativeModeInventoryScreen)
            || CreativeInventorySlotResolver.findInventoryMenuSlot(player.inventoryMenu, slot)
                != CreativeInventorySlotResolver.NO_SERVER_SLOT;
    }

    public static boolean handleMatchingTransfer(AbstractContainerScreen<?> screen, @Nullable Slot slot) {
        if (!supportsTransfer() || slot == null) {
            return false;
        }

        ItemStack bundle = screen.getMenu().getCarried();
        if (!BundleInteractionPolicy.shouldMergeBundleIntoSlot(bundle, slot)) {
            return false;
        }

        Minecraft minecraft = screen.getMinecraft();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }

        ServerSlotTarget target = resolveServerSlot(screen, slot);
        if (!target.isValid()) {
            return false;
        }

        boolean creativeInventory = screen instanceof CreativeModeInventoryScreen;
        if (!creativeInventory && minecraft.getConnection() == null) {
            return false;
        }

        ItemStack currentBundle = screen.getMenu().getCarried();
        List<ItemStack> expectedContents = BundleSnapshot.copyContents(currentBundle);
        boolean eventHandled = BundleInteractionHooks.onStackedOnOther(screen.getMenu(), slot, player);
        int transferred = !eventHandled
            && slot.allowModification(player)
            && BundleInteractionPolicy.canTransferMatchingToSlot(currentBundle, slot)
            ? BundleContentsOperations.tryTransferMatchingToSlot(currentBundle, slot)
            : 0;

        if (!creativeInventory) {
            PacketDistributor.sendToServer(
                new TransferBundleToSlotPayload(target.containerId(), target.slotIndex(), expectedContents)
            );
        }

        if (creativeInventory) {
            player.inventoryMenu.broadcastChanges();
            if (transferred > 0) {
                player.playSound(
                    SoundEvents.BUNDLE_REMOVE_ONE,
                    0.8F,
                    0.8F + player.level().getRandom().nextFloat() * 0.4F
                );
            }
        }
        return true;
    }

    private static boolean selectFromScroll(AbstractContainerScreen<?> screen, double scrollX, double scrollY) {
        if (!supportsSelection()) {
            return false;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null) {
            return false;
        }

        ItemStack bundle = slot.getItem();
        if (!BundleContentsOperations.isBundle(bundle)) {
            return false;
        }

        BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        int selectionSize = BundleTooltipLayout.configuredItemsToShow(contents);
        if (selectionSize == 0) {
            return false;
        }

        BundleScrollAccumulator.Step step = SCROLL_ACCUMULATOR.add(scrollX, scrollY);
        int scrollAmount = step.y() == 0 ? -step.x() : step.y();
        if (scrollAmount != 0) {
            int selectedItem = BundleSelection.getSelectedItem(bundle);
            int nextSelection = BundleSelectionScroll.getNextSelection(scrollAmount, selectedItem, selectionSize);
            if (selectedItem != nextSelection) {
                setSelection(screen, slot, nextSelection);
            }
        }

        return true;
    }

    private static void setSelection(AbstractContainerScreen<?> screen, Slot slot, int selectedItem) {
        if (selectedScreen != null && (screen != selectedScreen || slot != selectedSlot)) {
            clearTrackedSelection();
        }

        ItemStack bundle = slot.getItem();
        BundleSelection.setSelectedItem(bundle, selectedItem);
        selectedScreen = screen;
        selectedSlot = slot;
        selectedBundle = bundle;
        selectedServerTarget = resolveServerSlot(screen, slot);
        sendSelection(selectedServerTarget, bundle, selectedItem);
    }

    private static void clearTrackedSelection() {
        Slot slot = selectedSlot;
        ItemStack bundle = selectedBundle;
        ServerSlotTarget serverTarget = selectedServerTarget;
        forgetTrackedSelection();

        if (BundleContentsOperations.isBundle(bundle)) {
            BundleSelection.clear(bundle);
        }
        if (slot != null) {
            ItemStack currentBundle = slot.getItem();
            if (currentBundle != bundle && BundleContentsOperations.isBundle(currentBundle)) {
                BundleSelection.clear(currentBundle);
            }
        }
        sendSelection(serverTarget, bundle, BundleSelection.NO_SELECTED_ITEM);
    }

    private static void forgetTrackedSelection() {
        selectedScreen = null;
        selectedSlot = null;
        selectedBundle = ItemStack.EMPTY;
        selectedServerTarget = ServerSlotTarget.NONE;
    }

    private static ServerSlotTarget resolveServerSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen instanceof CreativeModeInventoryScreen) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return ServerSlotTarget.NONE;
            }

            int slotIndex = CreativeInventorySlotResolver.findInventoryMenuSlot(player.inventoryMenu, slot);
            return slotIndex == CreativeInventorySlotResolver.NO_SERVER_SLOT
                ? ServerSlotTarget.NONE
                : new ServerSlotTarget(player.inventoryMenu.containerId, slotIndex);
        }

        int slotIndex = screen.getMenu().slots.indexOf(slot);
        return slotIndex < 0
            ? ServerSlotTarget.NONE
            : new ServerSlotTarget(screen.getMenu().containerId, slotIndex);
    }

    private static void sendSelection(ServerSlotTarget target, ItemStack bundle, int selectedItem) {
        if (target.isValid() && supportsSelection()) {
            PacketDistributor.sendToServer(
                new SelectBundleItemPayload(
                    target.containerId(),
                    target.slotIndex(),
                    selectedItem,
                    BundleSnapshot.copyContents(bundle)
                )
            );
        }
    }

    private static boolean supportsSelection() {
        return Minecraft.getInstance().getConnection() != null
            && Minecraft.getInstance().getConnection().hasChannel(SelectBundleItemPayload.TYPE);
    }

    private static boolean supportsTransfer() {
        return Minecraft.getInstance().getConnection() != null
            && Minecraft.getInstance().getConnection().hasChannel(TransferBundleToSlotPayload.TYPE);
    }

    private record ServerSlotTarget(int containerId, int slotIndex) {
        private static final ServerSlotTarget NONE = new ServerSlotTarget(-1, -1);

        private boolean isValid() {
            return this.containerId >= 0 && this.slotIndex >= 0;
        }
    }
}

