package dev.modernbundles.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dev.modernbundles.bundle.BundleColor;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class CreativeBundleVariants {
    private CreativeBundleVariants() {
    }

    public static List<ItemStack> createStacks() {
        List<ItemStack> stacks = new ArrayList<>(BundleColor.values().length + 1);
        stacks.add(new ItemStack(Items.BUNDLE));
        for (BundleColor color : BundleColor.values()) {
            stacks.add(BundleColor.apply(new ItemStack(Items.BUNDLE), color));
        }
        return List.copyOf(stacks);
    }

    public static void addTo(BuildCreativeModeTabContentsEvent event) {
        if (!CreativeModeTabs.TOOLS_AND_UTILITIES.equals(event.getTabKey())) {
            return;
        }
        addStacks(event, event.getParentEntries(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        addStacks(event, event.getSearchEntries(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }

    private static void addStacks(
        BuildCreativeModeTabContentsEvent event,
        Set<ItemStack> entries,
        CreativeModeTab.TabVisibility visibility
    ) {
        ItemStack previous = new ItemStack(Items.LEAD);
        for (ItemStack stack : createStacks()) {
            if (entries.contains(stack)) {
                previous = stack;
            } else if (entries.contains(previous)) {
                event.insertAfter(previous, stack, visibility);
                previous = stack;
            } else {
                event.accept(stack, visibility);
                previous = stack;
            }
        }
    }
}
