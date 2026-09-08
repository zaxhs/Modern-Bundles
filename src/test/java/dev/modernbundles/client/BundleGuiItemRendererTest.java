package dev.modernbundles.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import dev.modernbundles.ModernBundles;
import dev.modernbundles.bundle.BundleColor;
import dev.modernbundles.bundle.BundleSelection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

class BundleGuiItemRendererTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void openRenderingRequiresGuiContextAndAValidSelection() {
        ItemStack bundle = bundleWithStone();
        assertFalse(BundleGuiItemRenderer.shouldRenderOpen(bundle, ItemDisplayContext.GUI));

        BundleSelection.setSelectedItem(bundle, 0);
        assertTrue(BundleGuiItemRenderer.shouldRenderOpen(bundle, ItemDisplayContext.GUI));
        for (ItemDisplayContext context : ItemDisplayContext.values()) {
            if (context != ItemDisplayContext.GUI) {
                assertFalse(BundleGuiItemRenderer.shouldRenderOpen(bundle, context));
            }
        }

        BundleSelection.setSelectedItem(bundle, 1);
        assertFalse(BundleGuiItemRenderer.shouldRenderOpen(bundle, ItemDisplayContext.GUI));

        ItemStack unsupported = new ItemStack(Items.STONE);
        unsupported.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStack(Items.DIRT))));
        BundleSelection.setSelectedItem(unsupported, 0);
        assertFalse(BundleGuiItemRenderer.shouldRenderOpen(unsupported, ItemDisplayContext.GUI));
    }

    @Test
    void everySupportedBundleRegistersStandaloneBackAndFrontModels() {
        List<ModelResourceLocation> models = BundleGuiItemRenderer.openModelLocations();
        assertEquals(34, models.size());

        for (String modelName : BundleColor.values().length == 16
            ? java.util.stream.Stream.concat(
                java.util.stream.Stream.of("bundle"),
                java.util.stream.Stream.of(BundleColor.values()).map(BundleColor::modelName)
            ).toList()
            : List.<String>of()) {
            ModelResourceLocation back = BundleGuiItemRenderer.openBackModel(modelName);
            ModelResourceLocation front = BundleGuiItemRenderer.openFrontModel(modelName);
            assertTrue(models.contains(back));
            assertTrue(models.contains(front));
            assertEquals(ModelResourceLocation.STANDALONE_VARIANT, back.getVariant());
            assertEquals(ModelResourceLocation.STANDALONE_VARIANT, front.getVariant());
            assertEquals(
                ResourceLocation.fromNamespaceAndPath(ModernBundles.MODID, "item/" + modelName + "_open_back"),
                back.id()
            );
            assertEquals(
                ResourceLocation.fromNamespaceAndPath(ModernBundles.MODID, "item/" + modelName + "_open_front"),
                front.id()
            );
        }
    }

    private static ItemStack bundleWithStone() {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStack(Items.STONE))));
        return bundle;
    }
}

