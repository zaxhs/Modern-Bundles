package dev.modernbundles.bundle;

import java.util.Map;
import java.util.WeakHashMap;

import dev.modernbundles.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class SelectionAuthority {
    private static final Map<ServerPlayer, ActiveSelection> ACTIVE = new WeakHashMap<>();

    private SelectionAuthority() {
    }

    public static void activate(ServerPlayer player, int containerId, int slotIndex, ItemStack bundle, int selectedIndex) {
        if (selectedIndex == BundleSelection.NO_SELECTED_ITEM) {
            ACTIVE.remove(player);
        } else {
            ACTIVE.put(player, new ActiveSelection(containerId, slotIndex, bundle, selectedIndex));
        }
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player);
    }

    public static void prepareForInteraction(Player player, ItemStack bundle) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ActiveSelection selection = ACTIVE.get(serverPlayer);
        if (selection == null || !selection.matches(serverPlayer, bundle)) {
            ACTIVE.remove(serverPlayer);
            BundleSelection.clear(bundle);
        }
    }

    private record ActiveSelection(int containerId, int slotIndex, ItemStack bundle, int selectedIndex) {
        private boolean matches(ServerPlayer player, ItemStack interactedBundle) {
            AbstractContainerMenu menu = player.containerMenu;
            return ModNetworking.supportsSelection(player)
                && !player.isSpectator()
                && menu.containerId == this.containerId
                && menu.stillValid(player)
                && this.slotIndex >= 0
                && this.slotIndex < menu.slots.size()
                && menu.getSlot(this.slotIndex).getItem() == this.bundle
                && interactedBundle == this.bundle
                && BundleSelection.getSelectedItem(this.bundle) == this.selectedIndex;
        }
    }
}
