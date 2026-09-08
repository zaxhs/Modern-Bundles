package dev.modernbundles.bundle;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;

public record BundleTooltipData(BundleContents contents, int selectedItem) implements TooltipComponent {
}

