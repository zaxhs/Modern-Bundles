package dev.modernbundles.mixin.client;

import javax.annotation.Nullable;

import java.util.Set;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.modernbundles.bundle.BundleInteractionPolicy;
import dev.modernbundles.client.BundleDragScreenAccess;
import dev.modernbundles.client.BundleMouseActions;
import dev.modernbundles.config.ClientConfig;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements BundleDragScreenAccess {
    @Unique
    private static final int MODERNBUNDLES$NO_DRAG_BUTTON = -1;

    @Shadow
    @Final
    protected Set<Slot> quickCraftSlots;

    @Shadow
    protected boolean isQuickCrafting;

    @Shadow
    private boolean skipNextRelease;

    @Shadow
    private boolean doubleclick;

    @Shadow
    private long lastClickTime;

    @Unique
    private int modernbundles$bundleDragButton = MODERNBUNDLES$NO_DRAG_BUTTON;

    @Unique
    private int modernbundles$bundleDragCandidateButton = MODERNBUNDLES$NO_DRAG_BUTTON;

    @Unique
    @Nullable
    private Slot modernbundles$bundleDragCandidateSlot;

    @Unique
    private boolean modernbundles$bundleDragCandidateStartedOnEmptySlot;

    @Unique
    @Nullable
    private Slot modernbundles$lastDragSlot;

    @Shadow
    @Nullable
    private Slot findSlot(double mouseX, double mouseY) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type);

    @ModifyExpressionValue(
        method = "renderTooltip",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z")
    )
    private boolean modernbundles$showBundleTooltipWhileCarrying(boolean carriedStackEmpty) {
        Slot hoveredSlot = ((AbstractContainerScreen<?>)(Object)this).getSlotUnderMouse();
        ItemStack hoveredStack = hoveredSlot == null ? ItemStack.EMPTY : hoveredSlot.getItem();
        return BundleInteractionPolicy.shouldRenderTooltip(carriedStackEmpty, hoveredStack);
    }

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

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void modernbundles$prepareBundleDrag(
        double mouseX,
        double mouseY,
        int mouseButton,
        CallbackInfoReturnable<Boolean> callback
    ) {
        this.modernbundles$clearBundleDragCandidate();
        if (mouseButton == this.modernbundles$bundleDragButton) {
            this.modernbundles$clearBundleDragState();
            this.skipNextRelease = false;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        boolean bundleDragEnabled = ClientConfig.BUNDLE_DRAG_ENABLED.get();
        boolean touchscreen = screen.getMinecraft().options.touchscreen().get();
        if (!bundleDragEnabled || touchscreen || (mouseButton != 0 && mouseButton != 1)) {
            return;
        }

        Slot slot = this.findSlot(mouseX, mouseY);
        boolean eligibleSlot = mouseButton == 0
            ? BundleMouseActions.canStartDragIntoBundle(screen, slot)
            : BundleMouseActions.canDragOutOfBundle(screen, slot);
        if (BundleInteractionPolicy.shouldBeginBundleDrag(
            screen.getMenu().getCarried(),
            mouseButton,
            bundleDragEnabled,
            touchscreen,
            eligibleSlot
        )) {
            this.modernbundles$bundleDragCandidateButton = mouseButton;
            this.modernbundles$bundleDragCandidateSlot = slot;
            this.modernbundles$bundleDragCandidateStartedOnEmptySlot = slot != null && slot.getItem().isEmpty();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void modernbundles$dragBundleAcrossSlots(
        double mouseX,
        double mouseY,
        int mouseButton,
        double dragX,
        double dragY,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.modernbundles$handleBundleDrag(mouseX, mouseY, mouseButton, dragX, dragY)) {
            callback.setReturnValue(true);
        }
    }

    @Override
    public boolean modernbundles$handleBundleDrag(
        double mouseX,
        double mouseY,
        int mouseButton,
        double dragX,
        double dragY
    ) {
        boolean bundleDragEnabled = ClientConfig.BUNDLE_DRAG_ENABLED.get();
        if (!bundleDragEnabled) {
            this.modernbundles$clearBundleDragCandidate();
            if (this.modernbundles$bundleDragButton != MODERNBUNDLES$NO_DRAG_BUTTON) {
                this.modernbundles$clearBundleDragState();
                this.skipNextRelease = false;
            }
            return false;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        Slot slot = this.findSlot(mouseX, mouseY);
        if (this.modernbundles$bundleDragButton == MODERNBUNDLES$NO_DRAG_BUTTON) {
            if (mouseButton != this.modernbundles$bundleDragCandidateButton
                || !BundleInteractionPolicy.shouldOwnBundleDrag(
                screen.getMenu().getCarried(),
                mouseButton,
                bundleDragEnabled,
                screen.getMinecraft().options.touchscreen().get()
            )) {
                return false;
            }

            Slot startingSlot = this.modernbundles$bundleDragCandidateSlot;
            boolean enteredDifferentSlot = slot != null && slot != startingSlot;
            if (BundleInteractionPolicy.shouldKeepBundleDragPending(
                mouseButton,
                this.modernbundles$bundleDragCandidateStartedOnEmptySlot,
                enteredDifferentSlot
            )) {
                // consume motion without suppressing release so vanilla still performs a normal pickup click
                return true;
            }

            this.modernbundles$bundleDragButton = mouseButton;
            this.modernbundles$clearBundleDragCandidate();
            this.modernbundles$suppressVanillaDragRelease();
            this.modernbundles$handleBundleDragSlot(screen, startingSlot, mouseButton);
            this.modernbundles$lastDragSlot = startingSlot;
        } else if (this.modernbundles$bundleDragButton != mouseButton) {
            return false;
        }

        this.skipNextRelease = true;
        if (slot != this.modernbundles$lastDragSlot) {
            this.modernbundles$lastDragSlot = slot;
            this.modernbundles$handleBundleDragSlot(screen, slot, mouseButton);
        }

        return true;
    }

    @Unique
    private void modernbundles$handleBundleDragSlot(
        AbstractContainerScreen<?> screen,
        @Nullable Slot slot,
        int mouseButton
    ) {
        if (mouseButton == 0 && BundleMouseActions.canDragIntoBundle(screen, slot)) {
            this.slotClicked(slot, slot.index, mouseButton, ClickType.PICKUP);
        } else if (mouseButton == 1 && BundleMouseActions.canDragOutOfBundle(screen, slot)) {
            if (slot.getItem().isEmpty()) {
                this.slotClicked(slot, slot.index, mouseButton, ClickType.PICKUP);
            } else {
                BundleMouseActions.handleMatchingTransfer(screen, slot);
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void modernbundles$prepareBundleDragRelease(
        double mouseX,
        double mouseY,
        int mouseButton,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (mouseButton == this.modernbundles$bundleDragButton) {
            this.modernbundles$suppressVanillaDragRelease();
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void modernbundles$finishBundleDrag(
        double mouseX,
        double mouseY,
        int mouseButton,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (mouseButton == this.modernbundles$bundleDragButton) {
            this.modernbundles$clearBundleDragState();
        }
        if (mouseButton == this.modernbundles$bundleDragCandidateButton) {
            this.modernbundles$clearBundleDragCandidate();
        }
    }

    @Unique
    private void modernbundles$suppressVanillaDragRelease() {
        this.isQuickCrafting = false;
        this.quickCraftSlots.clear();
        this.skipNextRelease = true;
        this.doubleclick = false;
        this.lastClickTime = 0L;
    }

    @Unique
    private void modernbundles$clearBundleDragState() {
        this.modernbundles$bundleDragButton = MODERNBUNDLES$NO_DRAG_BUTTON;
        this.modernbundles$lastDragSlot = null;
    }

    @Unique
    private void modernbundles$clearBundleDragCandidate() {
        this.modernbundles$bundleDragCandidateButton = MODERNBUNDLES$NO_DRAG_BUTTON;
        this.modernbundles$bundleDragCandidateSlot = null;
        this.modernbundles$bundleDragCandidateStartedOnEmptySlot = false;
    }
}

