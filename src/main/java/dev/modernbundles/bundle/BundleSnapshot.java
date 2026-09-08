package dev.modernbundles.bundle;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

public final class BundleSnapshot {
    public static final int MAX_CONTENT_STACKS = 64;

    private BundleSnapshot() {
    }

    public static List<ItemStack> copyContents(ItemStack bundle) {
        BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return contents.itemCopyStream().toList();
    }

    public static boolean matches(ItemStack bundle, List<ItemStack> expectedContents) {
        if (!BundleContentsOperations.isBundle(bundle)
            || expectedContents.size() > MAX_CONTENT_STACKS) {
            return false;
        }

        BundleContents actual = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (actual.size() != expectedContents.size()) {
            return false;
        }

        for (int index = 0; index < expectedContents.size(); index++) {
            if (!ItemStack.matches(actual.getItemUnsafe(index), expectedContents.get(index))) {
                return false;
            }
        }
        return true;
    }
}
