package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

class BundleItemBarColorTest {
    private static final int BAR_COLOR = 0x007087FF;
    private static final int FULL_BAR_COLOR = 0x00FF5454;
    private static final int OPAQUE_ALPHA = 0xFF000000;

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void emptyAndPartialBundlesUseTheModernBlue() {
        assertEquals(BAR_COLOR, bundleWithStone(Items.BUNDLE, 0).getBarColor());
        assertEquals(BAR_COLOR, bundleWithStone(Items.BUNDLE, 1).getBarColor());
    }

    @Test
    void fullAndOverCapacityBundlesUseTheModernRed() {
        assertEquals(FULL_BAR_COLOR, bundleWithStone(Items.BUNDLE, 64).getBarColor());
        assertEquals(FULL_BAR_COLOR, bundleWithStone(Items.BUNDLE, 65).getBarColor());
    }

    @Test
    void colorsRenderAsTheExactOpaqueValuesFrom1215() {
        assertEquals(0xFF7087FF, bundleWithStone(Items.BUNDLE, 1).getBarColor() | OPAQUE_ALPHA);
        assertEquals(0xFFFF5454, bundleWithStone(Items.BUNDLE, 64).getBarColor() | OPAQUE_ALPHA);
    }

    private static ItemStack bundleWithStone(Item bundleItem, int stoneCount) {
        ItemStack bundle = new ItemStack(bundleItem);
        if (stoneCount > 0) {
            bundle.set(
                DataComponents.BUNDLE_CONTENTS,
                new BundleContents(List.of(new ItemStack(Items.STONE, stoneCount)))
            );
        }
        return bundle;
    }
}

