package dev.modernbundles.mixin;

import java.util.Optional;

import dev.modernbundles.bundle.BundleColor;
import dev.modernbundles.bundle.BundleContentsOperations;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void modernbundles$preserveDyedBundle(
        CraftingInput input,
        HolderLookup.Provider registries,
        CallbackInfoReturnable<ItemStack> callback
    ) {
        Optional<BundleColor> outputColor = BundleColor.fromBundle(callback.getReturnValue());
        if (outputColor.isEmpty()) {
            return;
        }

        ItemStack inputBundle = ItemStack.EMPTY;
        Optional<BundleColor> dyeColor = Optional.empty();
        int nonEmptyInputs = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            nonEmptyInputs++;
            if (BundleContentsOperations.isBundle(stack)) {
                if (!inputBundle.isEmpty()) {
                    return;
                }
                inputBundle = stack;
            } else {
                Optional<BundleColor> candidate = BundleColor.fromDyeItem(stack);
                if (candidate.isEmpty() || dyeColor.isPresent()) {
                    return;
                }
                dyeColor = candidate;
            }
        }

        if (nonEmptyInputs == 2
            && !inputBundle.isEmpty()
            && dyeColor.isPresent()
            && dyeColor.get() == outputColor.get()) {
            callback.setReturnValue(BundleColor.apply(inputBundle.copyWithCount(1), dyeColor.get()));
        }
    }
}
