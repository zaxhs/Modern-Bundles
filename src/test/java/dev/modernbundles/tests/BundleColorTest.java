package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.mojang.serialization.JsonOps;
import dev.modernbundles.bundle.BundleColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BundleColorTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void allSixteenColorsStayMinecraftBundleAndUseStableModelData() {
        assertEquals(16, BundleColor.values().length);
        for (BundleColor color : BundleColor.values()) {
            ItemStack result = BundleColor.apply(new ItemStack(Items.BUNDLE), color);
            assertTrue(result.is(Items.BUNDLE));
            assertEquals(color, BundleColor.fromBundle(result).orElseThrow());
            assertEquals(color.modelData(), result.get(DataComponents.CUSTOM_MODEL_DATA).value());
            assertEquals(color.generatedName(), result.getHoverName().getString());
        }
    }

    @Test
    void dyeingAndRedyeingPreserveEmptyAndFullContents() {
        ItemStack empty = BundleColor.apply(new ItemStack(Items.BUNDLE), BundleColor.WHITE);
        assertTrue(empty.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).isEmpty());

        ItemStack full = new ItemStack(Items.BUNDLE);
        full.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStack(Items.STONE, 64))));
        BundleColor.apply(full, BundleColor.RED);
        BundleColor.apply(full, BundleColor.BLUE);

        BundleContents contents = full.get(DataComponents.BUNDLE_CONTENTS);
        assertEquals(1, contents.size());
        assertTrue(contents.getItemUnsafe(0).is(Items.STONE));
        assertEquals(64, contents.getItemUnsafe(0).getCount());
        assertEquals(BundleColor.BLUE, BundleColor.fromBundle(full).orElseThrow());
    }

    @Test
    void unrelatedCustomDataAndPlayerChosenNameArePreserved() {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        CompoundTag root = new CompoundTag();
        root.putString("other_mod", "keep");
        CompoundTag namespace = new CompoundTag();
        namespace.putInt("other_value", 42);
        root.put("modernbundles", namespace);
        bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        bundle.set(DataComponents.CUSTOM_NAME, Component.literal("Travel kit"));

        BundleColor.apply(bundle, BundleColor.GREEN);

        CompoundTag result = bundle.get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals("keep", result.getString("other_mod"));
        assertEquals(42, result.getCompound("modernbundles").getInt("other_value"));
        assertEquals("green", result.getCompound("modernbundles").getString("color"));
        assertEquals("Travel kit", bundle.get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void generatedNameUpdatesOnRedyeAndMetadataSurvivesSerialization() {
        ItemStack bundle = BundleColor.apply(new ItemStack(Items.BUNDLE), BundleColor.RED);
        BundleColor.apply(bundle, BundleColor.BLUE);
        assertEquals("Blue Bundle", bundle.get(DataComponents.CUSTOM_NAME).getString());

        var encoded = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, bundle).getOrThrow();
        ItemStack decoded = ItemStack.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertTrue(decoded.is(Items.BUNDLE));
        assertEquals(BundleColor.BLUE, BundleColor.fromBundle(decoded).orElseThrow());
        assertEquals(12, decoded.get(DataComponents.CUSTOM_MODEL_DATA).value());
    }
}
