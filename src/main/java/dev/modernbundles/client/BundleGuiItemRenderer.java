package dev.modernbundles.client;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.modernbundles.ModernBundles;
import dev.modernbundles.bundle.BundleColor;
import dev.modernbundles.bundle.BundleSelection;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BundleGuiItemRenderer {
    private static final String OPEN_BACK_SUFFIX = "_open_back";
    private static final String OPEN_FRONT_SUFFIX = "_open_front";

    private static final List<String> MODEL_NAMES = Stream.concat(
        Stream.of("bundle"),
        Stream.of(BundleColor.values()).map(BundleColor::modelName)
    ).toList();
    private static final List<ModelResourceLocation> OPEN_MODEL_LOCATIONS = MODEL_NAMES.stream()
        .flatMap(name -> Stream.of(openBackModel(name), openFrontModel(name)))
        .toList();

    private BundleGuiItemRenderer() {
    }

    public static boolean renderOpenBundle(
        ItemRenderer itemRenderer,
        ItemStack bundle,
        ItemDisplayContext displayContext,
        boolean leftHand,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int combinedLight,
        int combinedOverlay,
        @Nullable Level level,
        @Nullable LivingEntity entity,
        int seed
    ) {
        if (!shouldRenderOpen(bundle, displayContext)) {
            return false;
        }

        ItemStack selectedStack = BundleSelection.getSelectedItemStack(bundle);
        ModelManager modelManager = itemRenderer.getItemModelShaper().getModelManager();
        String modelName = BundleColor.fromBundle(bundle).map(BundleColor::modelName).orElse("bundle");
        BakedModel openBack = modelManager.getModel(openBackModel(modelName));
        BakedModel openFront = modelManager.getModel(openFrontModel(modelName));
        BakedModel missingModel = modelManager.getMissingModel();
        if (openBack == missingModel || openFront == missingModel) {
            return false;
        }

        itemRenderer.render(
            bundle,
            displayContext,
            leftHand,
            poseStack,
            bufferSource,
            combinedLight,
            combinedOverlay,
            openBack
        );
        BakedModel selectedModel = itemRenderer.getModel(selectedStack, level, entity, seed);
        itemRenderer.render(
            selectedStack,
            displayContext,
            leftHand,
            poseStack,
            bufferSource,
            combinedLight,
            combinedOverlay,
            selectedModel
        );
        itemRenderer.render(
            bundle,
            displayContext,
            leftHand,
            poseStack,
            bufferSource,
            combinedLight,
            combinedOverlay,
            openFront
        );
        return true;
    }

    static boolean shouldRenderOpen(ItemStack stack, ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.GUI
            && dev.modernbundles.bundle.BundleContentsOperations.isBundle(stack)
            && !BundleSelection.getSelectedItemStack(stack).isEmpty();
    }

    static List<ModelResourceLocation> openModelLocations() {
        return OPEN_MODEL_LOCATIONS;
    }

    static ModelResourceLocation openBackModel(String modelName) {
        return openModel(modelName, OPEN_BACK_SUFFIX);
    }

    static ModelResourceLocation openFrontModel(String modelName) {
        return openModel(modelName, OPEN_FRONT_SUFFIX);
    }

    private static ModelResourceLocation openModel(String modelName, String suffix) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(
            ModernBundles.MODID,
            "item/" + modelName + suffix
        );
        return ModelResourceLocation.standalone(modelId);
    }
}

