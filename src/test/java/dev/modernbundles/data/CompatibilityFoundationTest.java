package dev.modernbundles.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.modernbundles.bundle.BundleColor;
import dev.modernbundles.recipe.BundleRecipeUnlocks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CompatibilityFoundationTest {
    private static final Path PROJECT = Path.of(System.getProperty("modernbundles.projectDir"));
    private static final Path MAIN = PROJECT.resolve(Path.of("src", "main"));

    @Test
    void modernBundleRecipeOverridesVanillaIdWithReferenceLayout() throws IOException {
        JsonObject recipe = readJson(MAIN.resolve(Path.of("resources", "data", "minecraft", "recipe", "bundle.json")));
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("equipment", recipe.get("category").getAsString());
        assertEquals("-", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("#", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("minecraft:string", recipe.getAsJsonObject("key").getAsJsonObject("-").get("item").getAsString());
        assertEquals("minecraft:leather", recipe.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        assertEquals("minecraft:bundle", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void allDyeRecipesUseOnlyVanillaRecipeAndItemTypesAndItemNames() throws IOException {
        Path recipes = MAIN.resolve(Path.of("resources", "data", "modernbundles", "recipe"));
        List<Path> files;
        try (var stream = Files.list(recipes)) {
            files = stream.filter(path -> path.toString().endsWith("_bundle.json")).toList();
        }
        assertEquals(16, files.size());
        for (Path file : files) {
            String json = Files.readString(file);
            assertTrue(json.contains("\"type\":\"minecraft:crafting_shapeless\""));
            assertTrue(json.contains("\"id\":\"minecraft:bundle\""));
            assertTrue(json.contains("\"minecraft:custom_data\""));
            assertTrue(json.contains("\"minecraft:custom_model_data\""));
            assertTrue(json.contains("\"minecraft:item_name\":\"\\\""));
            assertFalse(json.contains("\"minecraft:custom_name\""));
            assertFalse(json.contains("modernbundles:" + file.getFileName().toString().replace(".json", "")));
        }
    }

    @Test
    void everyBundleRecipeIsGrantedOnLoginAndUsesVanillaCrafting() throws IOException {
        List<ResourceLocation> ids = BundleRecipeUnlocks.recipeIds();
        assertEquals(17, ids.size());
        assertEquals(ResourceLocation.withDefaultNamespace("bundle"), ids.getFirst());
        Set<String> paths = new HashSet<>();
        for (BundleColor color : BundleColor.values()) {
            assertTrue(ids.contains(ResourceLocation.fromNamespaceAndPath("modernbundles", color.modelName())));
            paths.add(color.modelName());
        }
        assertEquals(16, paths.size());

        String mainClass = Files.readString(MAIN.resolve(Path.of("java", "dev", "modernbundles", "ModernBundles.java")));
        String unlocks = Files.readString(MAIN.resolve(Path.of(
            "java", "dev", "modernbundles", "recipe", "BundleRecipeUnlocks.java"
        )));
        assertTrue(mainClass.contains("NeoForge.EVENT_BUS.addListener(BundleRecipeUnlocks::onPlayerLogin)"));
        assertTrue(unlocks.contains("player.awardRecipesByKey(RECIPE_IDS)"));
        assertFalse(unlocks.contains("TickEvent"));

        JsonObject normal = readJson(MAIN.resolve(Path.of("resources", "data", "minecraft", "recipe", "bundle.json")));
        assertEquals("minecraft:crafting_shaped", normal.get("type").getAsString());
        for (String path : paths) {
            JsonObject dyed = readJson(MAIN.resolve(Path.of(
                "resources", "data", "modernbundles", "recipe", path + ".json"
            )));
            assertEquals("minecraft:crafting_shapeless", dyed.get("type").getAsString());
        }
    }

    @Test
    void tooltipTranslationKeysExistAndUseVanillaNameStyling() throws IOException {
        Path tooltipPath = MAIN.resolve(Path.of(
            "java", "dev", "modernbundles", "client", "tooltip", "ClientBundleTooltip.java"
        ));
        String tooltip = Files.readString(tooltipPath);
        JsonObject language = readJson(MAIN.resolve(Path.of(
            "resources", "assets", "modernbundles", "lang", "en_us.json"
        )));
        Matcher matcher = Pattern.compile("Component\\.translatable\\(\"([^\"]+)\"\\)").matcher(tooltip);
        int checked = 0;
        while (matcher.find()) {
            assertTrue(language.has(matcher.group(1)), "Missing en_us key " + matcher.group(1));
            checked++;
        }
        assertEquals(3, checked);
        assertFalse(tooltip.contains("item.minecraft.bundle."));
        assertTrue(tooltip.contains("stack.getTooltipLines"));
        assertFalse(tooltip.contains("ChatFormatting.ITALIC"));
    }

    @Test
    void jeiRemainsOptionalBecauseRecipesAreOrdinaryVanillaJson() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));
        String activeBuild = build.lines().filter(line -> !line.stripLeading().startsWith("//")).reduce("", String::concat);
        assertFalse(Pattern.compile("(?i)(implementation|api|runtimeOnly).*(jei|mezz\\.jei)").matcher(activeBuild).find());
    }

    @Test
    void sourceRegistersNoGameplayContentAndPayloadsAreOptional() throws IOException {
        String allJava;
        try (var stream = Files.walk(MAIN.resolve("java"))) {
            allJava = stream.filter(path -> path.toString().endsWith(".java"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .reduce("", String::concat);
        }
        assertFalse(allJava.contains("DeferredRegister"));
        assertFalse(allJava.contains("DataComponentType.builder"));
        assertFalse(allJava.contains("RecipeSerializer.register"));

        String networking = Files.readString(MAIN.resolve(Path.of(
            "java", "dev", "modernbundles", "network", "ModNetworking.java"
        )));
        String mouse = Files.readString(MAIN.resolve(Path.of(
            "java", "dev", "modernbundles", "client", "BundleMouseActions.java"
        )));
        assertTrue(networking.contains("registrar(NETWORK_VERSION).optional()"));
        assertTrue(mouse.contains("hasChannel(SelectBundleItemPayload.TYPE)"));
        assertTrue(mouse.contains("hasChannel(TransferBundleToSlotPayload.TYPE)"));
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
