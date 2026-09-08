package dev.modernbundles;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.modernbundles.network.ModNetworking;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(ModernBundles.MODID)
public class ModernBundles {
    public static final String MODID = "modernbundles";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModernBundles(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Modern Bundles loading");
        modEventBus.addListener(ModNetworking::register);
    }
}

