package dev.modernbundles.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityFoundationTest {
    private static final Path MAIN = Path.of(System.getProperty("modernbundles.projectDir"), "src", "main");

    @Test
    void allDyeRecipesUseOnlyVanillaRecipeAndItemTypes() throws IOException {
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
            assertTrue(json.contains("\"minecraft:custom_name\":\"\\\""));
            assertFalse(json.contains("modernbundles:" + file.getFileName().toString().replace(".json", "")));
        }
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
}
