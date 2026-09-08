package dev.modernbundles.client.tooltip;

import java.util.List;

import javax.annotation.Nullable;

import dev.modernbundles.bundle.BundleTooltipData;
import dev.modernbundles.client.BundleTooltipLayout;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.math.Fraction;

@OnlyIn(Dist.CLIENT)
public final class ClientBundleTooltip implements ClientTooltipComponent {
    private static final ResourceLocation PROGRESSBAR_BORDER_SPRITE = ResourceLocation.withDefaultNamespace(
        "container/bundle/bundle_progressbar_border"
    );
    private static final ResourceLocation PROGRESSBAR_FILL_SPRITE = ResourceLocation.withDefaultNamespace(
        "container/bundle/bundle_progressbar_fill"
    );
    private static final ResourceLocation PROGRESSBAR_FULL_SPRITE = ResourceLocation.withDefaultNamespace(
        "container/bundle/bundle_progressbar_full"
    );
    private static final ResourceLocation SLOT_HIGHLIGHT_BACK_SPRITE = ResourceLocation.withDefaultNamespace(
        "container/bundle/slot_highlight_back"
    );
    private static final ResourceLocation SLOT_HIGHLIGHT_FRONT_SPRITE = ResourceLocation.withDefaultNamespace(
        "container/bundle/slot_highlight_front"
    );
    private static final ResourceLocation SLOT_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace(
        "container/bundle/slot_background"
    );

    private static final int PROGRESSBAR_HEIGHT = 13;
    private static final int PROGRESSBAR_MARGIN_Y = 4;
    private static final int TOOLTIP_BOTTOM_MARGIN = 4;

    private static final Component BUNDLE_FULL_TEXT = Component.translatable("item.minecraft.bundle.full");
    private static final Component BUNDLE_EMPTY_TEXT = Component.translatable("item.minecraft.bundle.empty");
    private static final Component BUNDLE_EMPTY_DESCRIPTION = Component.translatable("item.minecraft.bundle.empty.description");

    private final BundleContents contents;
    private final int selectedItem;
    private BundleTooltipLayout layout;

    public ClientBundleTooltip(BundleTooltipData tooltip) {
        this.contents = tooltip.contents();
        this.selectedItem = tooltip.selectedItem();
        this.layout = BundleTooltipLayout.forCurrentConfig(this.contents);
    }

    void fitToTooltipBounds(
        int tooltipWidth,
        int otherComponentsHeight,
        int vanillaHeightAdjustment,
        int scaledScreenWidth,
        int scaledScreenHeight
    ) {
        int preferredColumns = BundleTooltipLayout.columnsForTooltipWidth(tooltipWidth, scaledScreenWidth);
        int maximumRows = BundleTooltipLayout.maximumRowsForTooltip(
            scaledScreenHeight,
            otherComponentsHeight,
            this.getNonGridHeight(),
            vanillaHeightAdjustment
        );
        this.layout = BundleTooltipLayout.create(
            this.contents,
            true,
            maximumRows,
            preferredColumns,
            BundleTooltipLayout.maximumColumnsForScreen(scaledScreenWidth)
        );
    }

    int getNonGridHeight() {
        return this.getHeight() - this.layout.gridHeight();
    }

    @Override
    public int getHeight() {
        return this.contents.isEmpty()
            ? this.getEmptyBundleBackgroundHeight(Minecraft.getInstance().font)
            : this.backgroundHeight();
    }

    @Override
    public int getWidth(Font font) {
        return this.layout.gridWidth();
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        if (this.contents.isEmpty()) {
            this.renderEmptyBundleTooltip(font, x, y, guiGraphics);
        } else {
            this.renderBundleWithItemsTooltip(font, x, y, guiGraphics);
        }
    }

    private int getEmptyBundleBackgroundHeight(Font font) {
        return this.getEmptyBundleDescriptionTextHeight(font)
            + PROGRESSBAR_MARGIN_Y
            + PROGRESSBAR_HEIGHT
            + TOOLTIP_BOTTOM_MARGIN;
    }

    private int backgroundHeight() {
        return this.itemGridHeight() + PROGRESSBAR_MARGIN_Y + PROGRESSBAR_HEIGHT + TOOLTIP_BOTTOM_MARGIN;
    }

    private int itemGridHeight() {
        return this.layout.gridHeight();
    }

    private void renderEmptyBundleTooltip(Font font, int x, int y, GuiGraphics guiGraphics) {
        this.drawEmptyBundleDescriptionText(x, y, font, guiGraphics);
        this.drawProgressbar(
            x,
            y + this.getEmptyBundleDescriptionTextHeight(font) + PROGRESSBAR_MARGIN_Y,
            font,
            guiGraphics
        );
    }

    private void renderBundleWithItemsTooltip(Font font, int x, int y, GuiGraphics guiGraphics) {
        List<ItemStack> shownItems = this.getShownItems(this.layout.itemsToShow());
        int right = x + this.layout.gridWidth();
        int bottom = y + this.itemGridHeight();
        int slotIndex = 1;

        for (int row = 1; row <= this.layout.rows(); row++) {
            for (int column = 1; column <= this.layout.columns(); column++) {
                int slotX = right - column * BundleTooltipLayout.SLOT_SIZE;
                int slotY = bottom - row * BundleTooltipLayout.SLOT_SIZE;
                if (shouldRenderSurplusText(this.layout.hasHiddenItems(), column, row)) {
                    renderCount(slotX, slotY, this.getAmountOfHiddenItems(shownItems), font, guiGraphics);
                } else if (shownItems.size() >= slotIndex) {
                    this.renderSlot(slotIndex, slotX, slotY, shownItems, font, guiGraphics);
                    slotIndex++;
                }
            }
        }

        this.drawSelectedItemTooltip(font, guiGraphics, x, y);
        this.drawProgressbar(x, y + this.itemGridHeight() + PROGRESSBAR_MARGIN_Y, font, guiGraphics);
    }

    private List<ItemStack> getShownItems(int itemsToShow) {
        int shownCount = Math.min(this.contents.size(), itemsToShow);
        return this.contents.itemCopyStream().toList().subList(0, shownCount);
    }

    private static boolean shouldRenderSurplusText(boolean hasHiddenItems, int column, int row) {
        return hasHiddenItems && column * row == 1;
    }

    private int getAmountOfHiddenItems(List<ItemStack> shownItems) {
        return this.contents.itemCopyStream().skip(shownItems.size()).mapToInt(ItemStack::getCount).sum();
    }

    private void renderSlot(
        int slotIndex,
        int x,
        int y,
        List<ItemStack> shownItems,
        Font font,
        GuiGraphics guiGraphics
    ) {
        int contentsIndex = shownItems.size() - slotIndex;
        boolean selected = contentsIndex == this.selectedItem;
        ItemStack stack = shownItems.get(contentsIndex);
        guiGraphics.blitSprite(
            selected ? SLOT_HIGHLIGHT_BACK_SPRITE : SLOT_BACKGROUND_SPRITE,
            x,
            y,
            BundleTooltipLayout.SLOT_SIZE,
            BundleTooltipLayout.SLOT_SIZE
        );
        guiGraphics.renderItem(stack, x + 4, y + 4, slotIndex);
        guiGraphics.renderItemDecorations(font, stack, x + 4, y + 4);
        if (selected) {
            guiGraphics.blitSprite(
                SLOT_HIGHLIGHT_FRONT_SPRITE,
                x,
                y,
                BundleTooltipLayout.SLOT_SIZE,
                BundleTooltipLayout.SLOT_SIZE
            );
        }
    }

    private static void renderCount(int x, int y, int count, Font font, GuiGraphics guiGraphics) {
        guiGraphics.drawCenteredString(
            font,
            "+" + count,
            x + BundleTooltipLayout.SLOT_SIZE / 2,
            y + 10,
            0xFFFFFF
        );
    }

    private void drawSelectedItemTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        if (this.selectedItem >= 0 && this.selectedItem < this.contents.size()) {
            ItemStack selectedStack = this.contents.getItemUnsafe(this.selectedItem);
            MutableComponent name = Component.empty()
                .append(selectedStack.getHoverName())
                .withStyle(selectedStack.getRarity().getStyleModifier());
            if (selectedStack.has(DataComponents.CUSTOM_NAME)) {
                name.withStyle(ChatFormatting.ITALIC);
            }
            int nameWidth = font.width(name.getVisualOrderText());
            int center = x + this.layout.gridWidth() / 2 - BundleTooltipLayout.SLOT_SIZE / 2;
            guiGraphics.renderTooltip(font, name, center - nameWidth / 2, y - 15);
        }
    }

    private void drawProgressbar(int x, int y, Font font, GuiGraphics guiGraphics) {
        guiGraphics.blitSprite(this.getProgressBarTexture(), x + 1, y, this.getProgressBarFill(), PROGRESSBAR_HEIGHT);
        guiGraphics.blitSprite(PROGRESSBAR_BORDER_SPRITE, x, y, this.layout.gridWidth(), PROGRESSBAR_HEIGHT);
        Component text = this.getProgressBarFillText();
        if (text != null) {
            guiGraphics.drawCenteredString(font, text, x + this.layout.gridWidth() / 2, y + 3, 0xFFFFFF);
        }
    }

    private void drawEmptyBundleDescriptionText(int x, int y, Font font, GuiGraphics guiGraphics) {
        guiGraphics.drawWordWrap(font, BUNDLE_EMPTY_DESCRIPTION, x, y, this.layout.gridWidth(), 0xAAAAAA);
    }

    private int getEmptyBundleDescriptionTextHeight(Font font) {
        return font.split(BUNDLE_EMPTY_DESCRIPTION, this.layout.gridWidth()).size() * 9;
    }

    private int getProgressBarFill() {
        int maximumFill = this.layout.gridWidth() - 2;
        return Mth.clamp(Mth.mulAndTruncate(this.contents.weight(), maximumFill), 0, maximumFill);
    }

    private ResourceLocation getProgressBarTexture() {
        return this.contents.weight().compareTo(Fraction.ONE) >= 0
            ? PROGRESSBAR_FULL_SPRITE
            : PROGRESSBAR_FILL_SPRITE;
    }

    @Nullable
    private Component getProgressBarFillText() {
        if (this.contents.isEmpty()) {
            return BUNDLE_EMPTY_TEXT;
        }
        return this.contents.weight().compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL_TEXT : null;
    }
}

