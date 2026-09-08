package dev.modernbundles.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BundleFeatureGateTest {
    private static final Path PROJECT = Path.of(System.getProperty("modernbundles.projectDir"));
    private static final Path MAIN = PROJECT.resolve(Path.of("src", "main"));

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void vanillaBundleIsEnabledByDefaultWithoutExperimentalFlags() {
        assertFalse(FeatureFlags.DEFAULT_FLAGS.contains(FeatureFlags.BUNDLE));
        assertFalse(FeatureFlags.isExperimental(FeatureFlags.DEFAULT_FLAGS));
        assertTrue(Items.BUNDLE.requiredFeatures().isSubsetOf(FeatureFlags.DEFAULT_FLAGS));
        assertTrue(Items.BUNDLE.isEnabled(FeatureFlags.DEFAULT_FLAGS));
        assertTrue(new ItemStack(Items.BUNDLE).isItemEnabled(FeatureFlags.DEFAULT_FLAGS));
        assertSame(
            Items.BUNDLE,
            BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("bundle"))
        );
    }

    @Test
    void projectDoesNotEnableFeatureFlagsOrRegisterReplacementContent() throws IOException {
        String allJava = readAll(MAIN.resolve("java"));
        assertFalse(allJava.contains("FeatureFlags.BUNDLE"));
        assertFalse(allJava.contains("DeferredRegister"));
        assertFalse(allJava.contains("DataComponentType.builder"));
        assertFalse(allJava.contains("RecipeSerializer.register"));
    }

    @Test
    void recipesAndCreativeVariantsAreIndependentOfTheExperiment() throws IOException {
        Path normalRecipe = MAIN.resolve(Path.of("resources", "data", "minecraft", "recipe", "bundle.json"));
        String normalJson = Files.readString(normalRecipe);
        assertTrue(normalJson.contains("\"id\": \"minecraft:bundle\""));
        assertFalse(normalJson.contains("neoforge:conditions"));
        assertFalse(normalRecipe.toString().contains("datapacks"));

        Path dyeRecipes = MAIN.resolve(Path.of("resources", "data", "modernbundles", "recipe"));
        try (var stream = Files.list(dyeRecipes)) {
            assertTrue(stream.filter(path -> path.toString().endsWith("_bundle.json"))
                .allMatch(path -> {
                    try {
                        return !Files.readString(path).contains("neoforge:conditions");
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }));
        }

        String creative = Files.readString(MAIN.resolve(Path.of(
            "java", "dev", "modernbundles", "client", "CreativeBundleVariants.java"
        )));
        assertFalse(creative.contains("FeatureFlags.BUNDLE"));
    }

    private static String readAll(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(path -> path.toString().endsWith(".java"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .reduce("", String::concat);
        }
    }
}
