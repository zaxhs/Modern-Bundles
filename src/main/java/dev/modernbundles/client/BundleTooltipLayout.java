package dev.modernbundles.client;

import dev.modernbundles.bundle.BundleSelection;
import dev.modernbundles.config.ClientConfig;

import net.minecraft.world.item.component.BundleContents;

public record BundleTooltipLayout(int columns, int rows, int itemsToShow, boolean hasHiddenItems) {
    public static final int SLOT_SIZE = 24;

    private static final int COMPACT_COLUMNS = 4;
    private static final int COMPACT_MAX_SLOTS = 12;
    private static final int SCREEN_HORIZONTAL_RESERVE = 16;
    private static final int TOOLTIP_FRAME_ALLOWANCE = 7;

    public static BundleTooltipLayout forCurrentConfig(BundleContents contents) {
        return create(contents, ClientConfig.EXPAND_BUNDLE_TOOLTIP.get(), Integer.MAX_VALUE);
    }

    public static BundleTooltipLayout create(BundleContents contents, boolean expanded, int maximumRows) {
        return create(contents, expanded, maximumRows, COMPACT_COLUMNS);
    }

    public static BundleTooltipLayout create(
        BundleContents contents,
        boolean expanded,
        int maximumRows,
        int preferredColumns
    ) {
        return create(contents, expanded, maximumRows, preferredColumns, Integer.MAX_VALUE);
    }

    public static BundleTooltipLayout create(
        BundleContents contents,
        boolean expanded,
        int maximumRows,
        int preferredColumns,
        int maximumColumns
    ) {
        int itemCount = contents.size();
        int itemsToShow = itemsToShow(contents, expanded);
        if (!expanded) {
            int slotCount = Math.min(COMPACT_MAX_SLOTS, itemCount);
            return new BundleTooltipLayout(
                COMPACT_COLUMNS,
                divideRoundUp(slotCount, COMPACT_COLUMNS),
                itemsToShow,
                itemCount > COMPACT_MAX_SLOTS
            );
        }

        int columns = Math.max(COMPACT_COLUMNS, (int)Math.ceil(Math.sqrt(itemCount)));
        int availableRows = Math.max(1, maximumRows);
        columns = Math.max(columns, divideRoundUp(itemCount, availableRows));
        columns = Math.max(columns, preferredColumns);
        int usefulColumns = Math.max(COMPACT_COLUMNS, itemCount);
        int availableColumns = Math.max(COMPACT_COLUMNS, maximumColumns);
        columns = Math.min(columns, Math.min(usefulColumns, availableColumns));
        int rows = divideRoundUp(itemCount, columns);

        return new BundleTooltipLayout(columns, rows, itemsToShow, false);
    }

    public static int configuredItemsToShow(BundleContents contents) {
        return itemsToShow(contents, ClientConfig.EXPAND_BUNDLE_TOOLTIP.get());
    }

    public static int itemsToShow(BundleContents contents, boolean expanded) {
        return expanded
            ? contents.size()
            : BundleSelection.getNumberOfItemsToShow(contents);
    }

    public static int maximumRowsForTooltip(
        int scaledScreenHeight,
        int otherComponentsHeight,
        int bundleNonGridHeight,
        int vanillaHeightAdjustment
    ) {
        int availableGridHeight = scaledScreenHeight
            - TOOLTIP_FRAME_ALLOWANCE
            - otherComponentsHeight
            - bundleNonGridHeight
            - vanillaHeightAdjustment;
        return Math.max(1, availableGridHeight / SLOT_SIZE);
    }

    public static int columnsForTooltipWidth(int tooltipWidth, int scaledScreenWidth) {
        return Math.min(
            Math.max(COMPACT_COLUMNS, Math.max(0, tooltipWidth) / SLOT_SIZE),
            maximumColumnsForScreen(scaledScreenWidth)
        );
    }

    public static int maximumColumnsForScreen(int scaledScreenWidth) {
        int availableWidth = Math.max(COMPACT_COLUMNS * SLOT_SIZE, scaledScreenWidth - SCREEN_HORIZONTAL_RESERVE);
        return Math.max(COMPACT_COLUMNS, availableWidth / SLOT_SIZE);
    }

    public int gridWidth() {
        return this.columns * SLOT_SIZE;
    }

    public int gridHeight() {
        return this.rows * SLOT_SIZE;
    }

    private static int divideRoundUp(int value, int divisor) {
        return value == 0 ? 0 : 1 + (value - 1) / divisor;
    }
}

