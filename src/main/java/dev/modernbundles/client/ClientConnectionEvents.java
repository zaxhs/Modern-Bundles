package dev.modernbundles.client;

import dev.modernbundles.ModernBundles;
import dev.modernbundles.bundle.RemoteCapabilities;
import dev.modernbundles.network.SelectBundleItemPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = ModernBundles.MODID, value = Dist.CLIENT)
public final class ClientConnectionEvents {
    private ClientConnectionEvents() {
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        RemoteCapabilities.setServerSupportsSelection(
            event.getPlayer().connection.hasChannel(SelectBundleItemPayload.TYPE)
        );
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RemoteCapabilities.setServerSupportsSelection(false);
    }
}
