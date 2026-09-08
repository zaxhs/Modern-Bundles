package dev.modernbundles.recipe;

import java.util.ArrayList;
import java.util.List;

import dev.modernbundles.ModernBundles;
import dev.modernbundles.bundle.BundleColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class BundleRecipeUnlocks {
    private static final List<ResourceLocation> RECIPE_IDS = createRecipeIds();

    private BundleRecipeUnlocks() {
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.awardRecipesByKey(RECIPE_IDS);
        }
    }

    public static List<ResourceLocation> recipeIds() {
        return RECIPE_IDS;
    }

    private static List<ResourceLocation> createRecipeIds() {
        List<ResourceLocation> ids = new ArrayList<>(BundleColor.values().length + 1);
        ids.add(ResourceLocation.withDefaultNamespace("bundle"));
        for (BundleColor color : BundleColor.values()) {
            ids.add(ResourceLocation.fromNamespaceAndPath(ModernBundles.MODID, color.modelName()));
        }
        return List.copyOf(ids);
    }
}
