package dev.modernbundles.network;

import dev.modernbundles.bundle.BundleInteractionHooks;
import dev.modernbundles.bundle.BundleSnapshot;
import dev.modernbundles.bundle.BundleSelectionRequest;
import dev.modernbundles.bundle.SelectionAuthority;
import dev.modernbundles.bundle.BundleSlotTransferRequest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "1";

    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION).optional();
        registrar.playToServer(
            SelectBundleItemPayload.TYPE,
            SelectBundleItemPayload.STREAM_CODEC,
            ModNetworking::handleSelectBundleItem
        );
        registrar.playToServer(
            TransferBundleToSlotPayload.TYPE,
            TransferBundleToSlotPayload.STREAM_CODEC,
            ModNetworking::handleTransferBundleToSlot
        );
    }

    private static void handleSelectBundleItem(SelectBundleItemPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (!PacketRateLimiter.allowSelection(player)) {
                return;
            }

            AbstractContainerMenu menu = player.containerMenu;
            if (player.isSpectator() || !menu.stillValid(player)) {
                SelectionAuthority.clear(player);
                resyncIfAllowed(player);
                return;
            }

            boolean applied = BundleSelectionRequest.apply(
                menu,
                payload.containerId(),
                payload.slotIndex(),
                payload.selectedItemIndex(),
                payload.expectedContents()
            );
            if (applied) {
                SelectionAuthority.activate(
                    player,
                    payload.containerId(),
                    payload.slotIndex(),
                    menu.getSlot(payload.slotIndex()).getItem(),
                    payload.selectedItemIndex()
                );
            } else {
                SelectionAuthority.clear(player);
                resyncIfAllowed(player);
            }
        }
    }

    private static void handleTransferBundleToSlot(TransferBundleToSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!PacketRateLimiter.allowTransfer(player)) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (player.isSpectator() || !menu.stillValid(player)) {
            resyncIfAllowed(player);
            return;
        }

        if (!BundleSnapshot.matches(menu.getCarried(), payload.expectedContents())
            || !BundleSlotTransferRequest.isMatchingTarget(menu, payload.containerId(), payload.slotIndex())) {
            resyncIfAllowed(player);
            return;
        }

        if (BundleInteractionHooks.onStackedOnOther(menu, menu.getSlot(payload.slotIndex()), player)) {
            menu.broadcastChanges();
            return;
        }

        if (!BundleSlotTransferRequest.canApply(menu, player, payload.containerId(), payload.slotIndex())) {
            resyncIfAllowed(player);
            return;
        }

        int transferred = BundleSlotTransferRequest.apply(menu, player, payload.containerId(), payload.slotIndex());
        if (transferred > 0) {
            player.playSound(
                SoundEvents.BUNDLE_REMOVE_ONE,
                0.8F,
                0.8F + player.level().getRandom().nextFloat() * 0.4F
            );
            menu.broadcastChanges();
        } else {
            resyncIfAllowed(player);
        }
    }

    private static void resyncIfAllowed(ServerPlayer player) {
        if (PacketRateLimiter.allowResync(player)) {
            player.containerMenu.broadcastFullState();
        }
    }

    public static boolean supportsSelection(ServerPlayer player) {
        return player.connection.hasChannel(SelectBundleItemPayload.TYPE);
    }
}

