package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.modernbundles.bundle.BundleSelection;
import dev.modernbundles.bundle.BundleSelectionScroll;
import org.junit.jupiter.api.Test;

class BundleSelectionScrollTest {
    @Test
    void scrollingDownStartsAtTheLastVisibleItem() {
        assertEquals(7, BundleSelectionScroll.getNextSelection(1.0, BundleSelection.NO_SELECTED_ITEM, 8));
    }

    @Test
    void scrollingUpStartsAtTheFirstVisibleItem() {
        assertEquals(0, BundleSelectionScroll.getNextSelection(-1.0, BundleSelection.NO_SELECTED_ITEM, 8));
    }

    @Test
    void selectionWrapsInBothDirections() {
        assertEquals(7, BundleSelectionScroll.getNextSelection(1.0, 0, 8));
        assertEquals(0, BundleSelectionScroll.getNextSelection(-1.0, 7, 8));
    }

    @Test
    void anEmptyVisibleRangeHasNoSelection() {
        assertEquals(
            BundleSelection.NO_SELECTED_ITEM,
            BundleSelectionScroll.getNextSelection(1.0, BundleSelection.NO_SELECTED_ITEM, 0)
        );
    }
}

