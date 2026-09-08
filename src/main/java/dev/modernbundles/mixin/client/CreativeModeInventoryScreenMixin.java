package dev.modernbundles.mixin.client;

import javax.annotation.Nullable;

import dev.modernbundles.bundle.BundleInteractionPolicy;
import dev.modernbundles.client.BundleMouseActions;
import dev.modernbundles.config.ClientConfig;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void modernbundles$handleBundleClick(
        @Nullable Slot slot,
        int slotId,
        int mouseButton,
        ClickType type,
        CallbackInfo callback
    ) {
        if (type == ClickType.PICKUP
            && mouseButton == 1
            && ClientConfig.BUNDLE_DRAG_ENABLED.get()
            && BundleMouseActions.handleMatchingTransfer((AbstractContainerScreen<?>)(Object)this, slot)) {
            callback.cancel();
            return;
        }

        if (BundleInteractionPolicy.shouldClearSelectionBeforeClick(type)) {
            BundleMouseActions.clearSelectionBeforeClick((AbstractContainerScreen<?>)(Object)this, slot);
        }
    }
}

