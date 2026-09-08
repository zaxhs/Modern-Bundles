package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import dev.modernbundles.bundle.BundleSnapshot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BundleSnapshotTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void exactContentsMatchAndChangesAreRejected() {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStack(Items.STONE, 4))));
        List<ItemStack> snapshot = BundleSnapshot.copyContents(bundle);
        assertTrue(BundleSnapshot.matches(bundle, snapshot));

        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStack(Items.STONE, 3))));
        assertFalse(BundleSnapshot.matches(bundle, snapshot));
        assertFalse(BundleSnapshot.matches(new ItemStack(Items.CHEST), snapshot));
    }

    @Test
    void oversizedClientSnapshotIsRejected() {
        List<ItemStack> malicious = new ArrayList<>();
        for (int index = 0; index <= BundleSnapshot.MAX_CONTENT_STACKS; index++) {
            malicious.add(ItemStack.EMPTY);
        }
        assertFalse(BundleSnapshot.matches(new ItemStack(Items.BUNDLE), malicious));
    }
}
