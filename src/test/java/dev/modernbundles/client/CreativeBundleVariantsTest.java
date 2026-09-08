package dev.modernbundles.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modernbundles.bundle.BundleColor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreativeBundleVariantsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void createsNormalBundleAndAllSixteenVanillaItemVariants() {
        List<ItemStack> stacks = CreativeBundleVariants.createStacks();
        assertEquals(17, stacks.size());
        assertTrue(stacks.getFirst().is(Items.BUNDLE));

        Set<BundleColor> colors = new HashSet<>();
        for (ItemStack stack : stacks) {
            assertTrue(stack.is(Items.BUNDLE));
            BundleColor.fromBundle(stack).ifPresent(colors::add);
        }
        assertEquals(Set.of(BundleColor.values()), colors);
    }
}
