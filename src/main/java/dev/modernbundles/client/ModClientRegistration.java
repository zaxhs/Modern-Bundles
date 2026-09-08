package dev.modernbundles.client;

import dev.modernbundles.ModernBundles;
import dev.modernbundles.bundle.BundleTooltipData;
import dev.modernbundles.client.tooltip.ClientBundleTooltip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = ModernBundles.MODID, value = Dist.CLIENT)
public final class ModClientRegistration {
    private ModClientRegistration() {
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(BundleTooltipData.class, ClientBundleTooltip::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        BundleGuiItemRenderer.openModelLocations().forEach(event::register);
    }

    @SubscribeEvent
    public static void addCreativeBundleVariants(BuildCreativeModeTabContentsEvent event) {
        CreativeBundleVariants.addTo(event);
    }
}

