package dev.modernbundles.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

class BundleTooltipLayoutTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void expandedLayoutsGrowInBothDirectionsAndShowEveryEntry() {
        BundleTooltipLayout thirteenItems = BundleTooltipLayout.create(contents(13), true, 8);
        assertEquals(4, thirteenItems.columns());
        assertEquals(4, thirteenItems.rows());
        assertEquals(13, thirteenItems.itemsToShow());
        assertFalse(thirteenItems.hasHiddenItems());

        BundleTooltipLayout fullBundle = BundleTooltipLayout.create(contents(64), true, 8);
        assertEquals(8, fullBundle.columns());
        assertEquals(8, fullBundle.rows());
        assertEquals(64, fullBundle.itemsToShow());
        assertEquals(192, fullBundle.gridWidth());
        assertEquals(192, fullBundle.gridHeight());
    }

    @Test
    void expandedLayoutAddsColumnsWhenScreenHeightLimitsRows() {
        BundleTooltipLayout layout = BundleTooltipLayout.create(contents(64), true, 5);
        assertEquals(13, layout.columns());
        assertEquals(5, layout.rows());
        assertEquals(64, layout.itemsToShow());
    }

    @Test
    void rowBudgetUsesActualTooltipComponentHeights() {
        int bundleNonGridHeight = 21;

        assertEquals(8, BundleTooltipLayout.maximumRowsForTooltip(240, 10, bundleNonGridHeight, 0));
        assertEquals(7, BundleTooltipLayout.maximumRowsForTooltip(240, 30, bundleNonGridHeight, 0));
        assertEquals(7, BundleTooltipLayout.maximumRowsForTooltip(216, 10, bundleNonGridHeight, 0));
        assertEquals(8, BundleTooltipLayout.maximumRowsForTooltip(240, 0, bundleNonGridHeight, -2));
    }

    @Test
    void expandedLayoutUsesCompleteColumnsFromTheExistingTooltipWidth() {
        assertEquals(12, BundleTooltipLayout.columnsForTooltipWidth(300, 480));

        BundleTooltipLayout layout = BundleTooltipLayout.create(contents(64), true, 7, 12, 19);
        assertEquals(12, layout.columns());
        assertEquals(6, layout.rows());
        assertEquals(288, layout.gridWidth());
        assertEquals(144, layout.gridHeight());
    }

    @Test
    void tooltipWidthColumnsAreBoundedByTheScreenAndUsefulItemCount() {
        assertEquals(4, BundleTooltipLayout.columnsForTooltipWidth(95, 320));
        assertEquals(12, BundleTooltipLayout.columnsForTooltipWidth(1_000, 320));
        assertEquals(12, BundleTooltipLayout.maximumColumnsForScreen(320));

        BundleTooltipLayout layout = BundleTooltipLayout.create(contents(5), true, 8, 12);
        assertEquals(5, layout.columns());
        assertEquals(1, layout.rows());
    }

    @Test
    void impossibleFitUsesTheScreenWidthAndMinimizesVerticalOverflow() {
        BundleTooltipLayout layout = BundleTooltipLayout.create(contents(64), true, 4, 4, 12);

        assertEquals(12, layout.columns());
        assertEquals(6, layout.rows());
        assertEquals(64, layout.itemsToShow());
        assertFalse(layout.hasHiddenItems());
    }

    @Test
    void compactLayoutRetainsTheReleasedPreviewAndSurplusCell() {
        BundleTooltipLayout layout = BundleTooltipLayout.create(contents(13), false, 8, 12);

        assertEquals(4, layout.columns());
        assertEquals(3, layout.rows());
        assertEquals(8, layout.itemsToShow());
        assertTrue(layout.hasHiddenItems());
        assertEquals(96, layout.gridWidth());
        assertEquals(72, layout.gridHeight());
    }

    @Test
    void emptyLayoutKeepsTheStandardTooltipWidth() {
        BundleTooltipLayout layout = BundleTooltipLayout.create(BundleContents.EMPTY, true, 8);

        assertEquals(4, layout.columns());
        assertEquals(0, layout.rows());
        assertEquals(0, layout.itemsToShow());
        assertEquals(96, layout.gridWidth());
    }

    private static BundleContents contents(int itemCount) {
        List<ItemStack> items = IntStream.range(0, itemCount)
            .mapToObj(index -> new ItemStack(Items.STONE))
            .toList();
        return new BundleContents(items);
    }
}

