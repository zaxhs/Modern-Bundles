package dev.modernbundles.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.modernbundles.bundle.BundleContentsOperations;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyExpressionValue(
        method = "renderArmWithItem",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;applyForgeHandTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;FFF)Z"
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"
        )
    )
    private boolean modernbundles$useStandardSwingForBundles(
        boolean isUsingItem,
        @Local(argsOnly = true) ItemStack stack
    ) {
        boolean useStandardSwing = BundleContentsOperations.isBundle(stack)
            && stack.getUseAnimation() == UseAnim.NONE;
        return isUsingItem && !useStandardSwing;
    }
}

