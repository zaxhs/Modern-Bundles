package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.JsonOps;
import dev.modernbundles.bundle.BundleSelection;
import dev.modernbundles.bundle.BundleTooltipData;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.connection.ConnectionType;

class BundleSelectionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void selectionLivesInBundleContentsWithoutTouchingCustomData() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));
        CompoundTag root = new CompoundTag();
        root.putString("unrelated", "preserved");
        bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        BundleContents originalContents = bundle.get(DataComponents.BUNDLE_CONTENTS);

        BundleSelection.setSelectedItem(bundle, 1);

        assertEquals(1, BundleSelection.getSelectedItem(bundle));
        assertNotSame(originalContents, bundle.get(DataComponents.BUNDLE_CONTENTS));
        assertEquals("preserved", bundle.get(DataComponents.CUSTOM_DATA).copyTag().getString("unrelated"));

        BundleSelection.clear(bundle);

        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
        assertEquals("preserved", bundle.get(DataComponents.CUSTOM_DATA).copyTag().getString("unrelated"));
    }

    @Test
    void copiedStacksDoNotShareLaterSelectionChanges() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));
        BundleSelection.setSelectedItem(bundle, 1);
        ItemStack copy = bundle.copy();

        BundleSelection.setSelectedItem(bundle, 0);

        assertEquals(0, BundleSelection.getSelectedItem(bundle));
        assertEquals(1, BundleSelection.getSelectedItem(copy));

        BundleSelection.clear(copy);
        assertEquals(0, BundleSelection.getSelectedItem(bundle));
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(copy));
    }

    @Test
    void selectionDoesNotAffectContentsEqualityOrHashing() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));
        BundleContents unselected = bundle.get(DataComponents.BUNDLE_CONTENTS);

        BundleSelection.setSelectedItem(bundle, 1);
        BundleContents selected = bundle.get(DataComponents.BUNDLE_CONTENTS);

        assertEquals(unselected, selected);
        assertEquals(unselected.hashCode(), selected.hashCode());
    }

    @Test
    void selectionIsExcludedFromPersistentSerialization() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));
        BundleSelection.setSelectedItem(bundle, 1);

        var encoded = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, bundle).getOrThrow();
        ItemStack decoded = ItemStack.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertFalse(encoded.toString().contains("selected_item"));
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(decoded));
        assertEquals(2, decoded.get(DataComponents.BUNDLE_CONTENTS).size());
    }

    @Test
    void selectionIsExcludedFromNetworkSerialization() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));
        BundleSelection.setSelectedItem(bundle, 1);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),
            ConnectionType.OTHER
        );

        try {
            ItemStack.STREAM_CODEC.encode(buffer, bundle);
            ItemStack decoded = ItemStack.STREAM_CODEC.decode(buffer);

            assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(decoded));
            assertEquals(2, decoded.get(DataComponents.BUNDLE_CONTENTS).size());
        } finally {
            buffer.release();
        }
    }

    @Test
    void hiddenSelectionsAreAcceptedAndInvalidSelectionsAreRejected() {
        ItemStack bundle = bundleWithItemTypes(13);
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundle));

        BundleSelection.setSelectedItem(bundle, 7);
        assertEquals(7, BundleSelection.getSelectedItem(bundle));
        BundleSelection.setSelectedItem(bundle, 12);
        assertEquals(12, BundleSelection.getSelectedItem(bundle));
        BundleSelection.setSelectedItem(bundle, 13);
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
        BundleSelection.setSelectedItem(bundle, -2);
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void selectingTheCurrentEntryTogglesItOff() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));

        BundleSelection.toggleSelectedItem(bundle, 1);
        assertEquals(1, BundleSelection.getSelectedItem(bundle));
        BundleSelection.toggleSelectedItem(bundle, 1);
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void visibleCountMatchesTheReleasedFourColumnLayout() {
        assertEquals(0, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(0)));
        assertEquals(1, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(1)));
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(8)));
        assertEquals(11, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(11)));
        assertEquals(12, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(12)));
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(13)));
        assertEquals(9, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(14)));
        assertEquals(10, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(15)));
        assertEquals(11, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(16)));
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(17)));
    }

    @Test
    void bundleTooltipCarriesContentsAndTransientSelection() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));
        BundleSelection.setSelectedItem(bundle, 1);

        BundleTooltipData tooltip = (BundleTooltipData)bundle.getItem().getTooltipImage(bundle).orElseThrow();

        assertEquals(bundle.get(DataComponents.BUNDLE_CONTENTS), tooltip.contents());
        assertEquals(1, tooltip.selectedItem());
    }

    private static ItemStack bundleWithItemTypes(int count) {
        List<ItemStack> items = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ItemStack stack = new ItemStack(Items.STONE);
            CompoundTag marker = new CompoundTag();
            marker.putInt("type", index);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
            items.add(stack);
        }
        return bundleWithItems(items.toArray(ItemStack[]::new));
    }

    private static ItemStack bundleWithItems(ItemStack... items) {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(items)));
        return bundle;
    }
}

