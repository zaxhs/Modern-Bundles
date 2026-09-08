package dev.modernbundles.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec.BooleanValue EXPAND_BUNDLE_TOOLTIP;
    public static final ModConfigSpec.BooleanValue BUNDLE_DRAG_ENABLED;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        EXPAND_BUNDLE_TOOLTIP = builder
            .comment("Show every bundle entry in the tooltip and allow scrolling through all of them")
            .translation("modernbundles.configuration.expandBundleTooltip")
            .define("expandBundleTooltip", true);
        BUNDLE_DRAG_ENABLED = builder
            .comment("While holding a bundle in an inventory, drag over slots with left click to insert items or right click to remove items")
            .translation("modernbundles.configuration.bundleDragEnabled")
            .define("bundleDragEnabled", true);
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}

