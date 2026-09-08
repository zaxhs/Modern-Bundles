package dev.modernbundles.client.tooltip;

import dev.modernbundles.ModernBundles;
import dev.modernbundles.config.ClientConfig;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

@EventBusSubscriber(modid = ModernBundles.MODID, value = Dist.CLIENT)
public final class BundleTooltipEvents {
    private BundleTooltipEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void useAvailableTooltipWidth(RenderTooltipEvent.Pre event) {
        boolean hasBundleTooltip = event.getComponents().stream()
            .anyMatch(ClientBundleTooltip.class::isInstance);
        if (!hasBundleTooltip || !ClientConfig.EXPAND_BUNDLE_TOOLTIP.get()) {
            return;
        }

        int tooltipWidth = event.getComponents().stream()
            .filter(component -> !(component instanceof ClientBundleTooltip))
            .mapToInt(component -> component.getWidth(event.getFont()))
            .max()
            .orElse(0);
        int otherComponentsHeight = event.getComponents().stream()
            .filter(component -> !(component instanceof ClientBundleTooltip))
            .mapToInt(ClientTooltipComponent::getHeight)
            .sum();
        int vanillaHeightAdjustment = event.getComponents().size() == 1 ? -2 : 0;

        for (ClientTooltipComponent component : event.getComponents()) {
            if (component instanceof ClientBundleTooltip bundleTooltip) {
                bundleTooltip.fitToTooltipBounds(
                    tooltipWidth,
                    otherComponentsHeight,
                    vanillaHeightAdjustment,
                    event.getScreenWidth(),
                    event.getScreenHeight()
                );
            }
        }
    }
}

