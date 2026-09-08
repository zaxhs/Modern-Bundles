package dev.modernbundles.mixin;

import java.util.List;
import java.util.Optional;

import dev.modernbundles.bundle.BundleContentsOperations;
import dev.modernbundles.bundle.RemoteCapabilities;
import dev.modernbundles.bundle.BundleSelection;
import dev.modernbundles.bundle.SelectionAuthority;
import dev.modernbundles.bundle.BundleTooltipData;
import dev.modernbundles.network.ModNetworking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleItem.class)
public abstract class BundleItemMixin extends Item {
    @Unique
    private static final int MODERNBUNDLES$FULL_BAR_COLOR = Mth.color(1.0F, 0.33F, 0.33F);
    @Unique
    private static final int MODERNBUNDLES$BAR_COLOR = Mth.color(0.44F, 0.53F, 1.0F);
    @Unique
    private static final int MODERNBUNDLES$TICKS_AFTER_FIRST_THROW = 10;
    @Unique
    private static final int MODERNBUNDLES$TICKS_BETWEEN_THROWS = 2;
    @Unique
    private static final int MODERNBUNDLES$USE_DURATION = 200;

    protected BundleItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void modernbundles$getTooltipImage(
        ItemStack bundle,
        CallbackInfoReturnable<Optional<TooltipComponent>> callback
    ) {
        if (bundle.has(DataComponents.HIDE_TOOLTIP) || bundle.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)) {
            callback.setReturnValue(Optional.empty());
            return;
        }

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        callback.setReturnValue(
            contents == null
                ? Optional.empty()
                : Optional.of(new BundleTooltipData(contents, BundleSelection.getSelectedItem(bundle)))
        );
    }

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void modernbundles$removeObsoleteFullnessLine(
        ItemStack bundle,
        TooltipContext context,
        List<Component> tooltipComponents,
        TooltipFlag tooltipFlag,
        CallbackInfo callback
    ) {
        callback.cancel();
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void modernbundles$getBarColor(
        ItemStack bundle,
        CallbackInfoReturnable<Integer> callback
    ) {
        BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        callback.setReturnValue(
            contents.weight().compareTo(Fraction.ONE) >= 0
                ? MODERNBUNDLES$FULL_BAR_COLOR
                : MODERNBUNDLES$BAR_COLOR
        );
    }

    @Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
    private void modernbundles$overrideStackedOnOther(
        ItemStack bundle,
        Slot slot,
        ClickAction action,
        Player player,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (!modernbundles$enhancedInteractionEnabled(player, bundle)) {
            return;
        }

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || bundle.getCount() != 1) {
            callback.setReturnValue(false);
            return;
        }

        ItemStack slottedStack = slot.getItem();
        if (action == ClickAction.PRIMARY && !slottedStack.isEmpty()) {
            if (BundleContentsOperations.tryTransfer(bundle, slot, player) > 0) {
                modernbundles$playInsertSound(player);
            } else {
                modernbundles$playInsertFailSound(player);
            }
            modernbundles$broadcastChanges(player);
            callback.setReturnValue(true);
        } else if (action == ClickAction.SECONDARY && slottedStack.isEmpty()) {
            ItemStack removedStack = BundleContentsOperations.removeSelected(bundle);
            if (!removedStack.isEmpty()) {
                ItemStack remainder = slot.safeInsert(removedStack);
                if (remainder.isEmpty()) {
                    modernbundles$playRemoveOneSound(player);
                } else {
                    BundleContentsOperations.tryInsert(bundle, remainder);
                }
            }
            modernbundles$broadcastChanges(player);
            callback.setReturnValue(true);
        } else {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void modernbundles$overrideOtherStackedOnMe(
        ItemStack bundle,
        ItemStack other,
        Slot slot,
        ClickAction action,
        Player player,
        SlotAccess access,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (!modernbundles$enhancedInteractionEnabled(player, bundle)) {
            return;
        }

        if (bundle.getCount() != 1) {
            callback.setReturnValue(false);
            return;
        }

        if (action == ClickAction.PRIMARY && other.isEmpty()) {
            BundleSelection.clear(bundle);
            callback.setReturnValue(false);
            return;
        }

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            callback.setReturnValue(false);
            return;
        }

        if (action == ClickAction.PRIMARY && !other.isEmpty()) {
            if (slot.allowModification(player) && BundleContentsOperations.tryInsert(bundle, other) > 0) {
                modernbundles$playInsertSound(player);
            } else {
                modernbundles$playInsertFailSound(player);
            }
            modernbundles$broadcastChanges(player);
            callback.setReturnValue(true);
        } else if (action == ClickAction.SECONDARY && other.isEmpty()) {
            if (slot.allowModification(player)) {
                ItemStack removedStack = BundleContentsOperations.removeSelected(bundle);
                if (!removedStack.isEmpty()) {
                    modernbundles$playRemoveOneSound(player);
                    access.set(removedStack);
                }
            }
            modernbundles$broadcastChanges(player);
            callback.setReturnValue(true);
        } else {
            BundleSelection.clear(bundle);
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void modernbundles$use(
        Level level,
        Player player,
        InteractionHand usedHand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback
    ) {
        ItemStack bundle = player.getItemInHand(usedHand);
        if (!modernbundles$enhancedInteractionEnabled(player, bundle)) {
            return;
        }
        player.startUsingItem(usedHand);
        callback.setReturnValue(InteractionResultHolder.sidedSuccess(bundle, level.isClientSide()));
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack bundle, int remainingUseDuration) {
        if (livingEntity instanceof Player player && modernbundles$enhancedInteractionEnabled(player, bundle)) {
            boolean firstThrow = remainingUseDuration == MODERNBUNDLES$USE_DURATION;
            boolean repeatedThrow = remainingUseDuration < MODERNBUNDLES$USE_DURATION - MODERNBUNDLES$TICKS_AFTER_FIRST_THROW
                && remainingUseDuration % MODERNBUNDLES$TICKS_BETWEEN_THROWS == 0;
            if (firstThrow || repeatedThrow) {
                modernbundles$dropContent(level, player, bundle);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MODERNBUNDLES$USE_DURATION;
    }

    @Unique
    private void modernbundles$dropContent(Level level, Player player, ItemStack bundle) {
        ItemStack removedStack = BundleContentsOperations.removeSelected(bundle);
        if (!removedStack.isEmpty()) {
            modernbundles$playRemoveOneSound(player);
            player.drop(removedStack, true);
            modernbundles$playDropContentsSound(level, player);
            player.awardStat(Stats.ITEM_USED.get((BundleItem)(Object)this));
        }
    }

    @Unique
    private static void modernbundles$playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    @Unique
    private static void modernbundles$playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    @Unique
    private static void modernbundles$playInsertFailSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.5F, 0.65F);
    }

    @Unique
    private static void modernbundles$playDropContentsSound(Level level, Entity entity) {
        level.playSound(
            null,
            entity.blockPosition(),
            SoundEvents.BUNDLE_DROP_CONTENTS,
            SoundSource.PLAYERS,
            0.8F,
            0.8F + entity.level().getRandom().nextFloat() * 0.4F
        );
    }

    @Unique
    private static void modernbundles$broadcastChanges(Player player) {
        if (player.containerMenu != null) {
            player.containerMenu.slotsChanged(player.getInventory());
        }
    }

    @Unique
    private static boolean modernbundles$enhancedInteractionEnabled(Player player, ItemStack bundle) {
        if (player.level().isClientSide()) {
            return RemoteCapabilities.serverSupportsSelection();
        }

        SelectionAuthority.prepareForInteraction(player, bundle);
        return player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
            && ModNetworking.supportsSelection(serverPlayer);
    }
}

