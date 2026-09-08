package dev.modernbundles.bundle;

public final class BundleSelectionScroll {
    private BundleSelectionScroll() {
    }

    public static int getNextSelection(double scrollAmount, int selectedItem, int selectionSize) {
        if (selectionSize <= 0) {
            return BundleSelection.NO_SELECTED_ITEM;
        }

        int direction = (int)Math.signum(scrollAmount);
        int next = selectedItem - direction;
        next = Math.max(BundleSelection.NO_SELECTED_ITEM, next);
        while (next < 0) {
            next += selectionSize;
        }
        while (next >= selectionSize) {
            next -= selectionSize;
        }
        return next;
    }
}

