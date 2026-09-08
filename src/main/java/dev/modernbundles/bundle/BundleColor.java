package dev.modernbundles.bundle;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

public enum BundleColor {
    WHITE(DyeColor.WHITE, 1, "White"),
    ORANGE(DyeColor.ORANGE, 2, "Orange"),
    MAGENTA(DyeColor.MAGENTA, 3, "Magenta"),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE, 4, "Light Blue"),
    YELLOW(DyeColor.YELLOW, 5, "Yellow"),
    LIME(DyeColor.LIME, 6, "Lime"),
    PINK(DyeColor.PINK, 7, "Pink"),
    GRAY(DyeColor.GRAY, 8, "Gray"),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY, 9, "Light Gray"),
    CYAN(DyeColor.CYAN, 10, "Cyan"),
    PURPLE(DyeColor.PURPLE, 11, "Purple"),
    BLUE(DyeColor.BLUE, 12, "Blue"),
    BROWN(DyeColor.BROWN, 13, "Brown"),
    GREEN(DyeColor.GREEN, 14, "Green"),
    RED(DyeColor.RED, 15, "Red"),
    BLACK(DyeColor.BLACK, 16, "Black");

    private static final String NAMESPACE_KEY = "modernbundles";
    private static final String COLOR_KEY = "color";

    private final DyeColor dyeColor;
    private final int modelData;
    private final String displayName;

    BundleColor(DyeColor dyeColor, int modelData, String displayName) {
        this.dyeColor = dyeColor;
        this.modelData = modelData;
        this.displayName = displayName;
    }

    public DyeColor dyeColor() {
        return this.dyeColor;
    }

    public int modelData() {
        return this.modelData;
    }

    public String serializedName() {
        return this.dyeColor.getName();
    }

    public String modelName() {
        return this.serializedName() + "_bundle";
    }

    public String generatedName() {
        return this.displayName + " Bundle";
    }

    public static Optional<BundleColor> fromDyeItem(ItemStack stack) {
        if (!(stack.getItem() instanceof DyeItem dyeItem)) {
            return Optional.empty();
        }
        return fromDyeColor(dyeItem.getDyeColor());
    }

    public static Optional<BundleColor> fromDyeColor(DyeColor color) {
        for (BundleColor candidate : values()) {
            if (candidate.dyeColor == color) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static Optional<BundleColor> fromBundle(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(NAMESPACE_KEY)) {
            return Optional.empty();
        }
        String name = root.getCompound(NAMESPACE_KEY).getString(COLOR_KEY).toLowerCase(Locale.ROOT);
        for (BundleColor candidate : values()) {
            if (candidate.serializedName().equals(name)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static ItemStack apply(ItemStack source, BundleColor color) {
        Optional<BundleColor> previousColor = fromBundle(source);
        Component currentName = source.get(DataComponents.CUSTOM_NAME);
        boolean replaceName = currentName == null
            || previousColor.map(previous -> previous.generatedName().equals(currentName.getString())).orElse(false);

        CustomData.update(DataComponents.CUSTOM_DATA, source, root -> {
            CompoundTag namespace = root.getCompound(NAMESPACE_KEY);
            namespace.putString(COLOR_KEY, color.serializedName());
            root.put(NAMESPACE_KEY, namespace);
        });
        source.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(color.modelData()));
        if (replaceName) {
            source.set(DataComponents.CUSTOM_NAME, Component.literal(color.generatedName()));
        }
        return source;
    }
}
