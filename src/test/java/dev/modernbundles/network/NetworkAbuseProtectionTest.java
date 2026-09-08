package dev.modernbundles.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NetworkAbuseProtectionTest {
    @Test
    void rateLimitsRunBeforeHandlingAndFullResyncIsCentralized() throws IOException {
        Path source = Path.of(
            System.getProperty("modernbundles.projectDir"),
            "src", "main", "java", "dev", "modernbundles", "network", "ModNetworking.java"
        );
        String networking = Files.readString(source);
        assertTrue(networking.indexOf("PacketRateLimiter.allowSelection(player)")
            < networking.indexOf("BundleSelectionRequest.apply"));
        assertTrue(networking.indexOf("PacketRateLimiter.allowTransfer(player)")
            < networking.indexOf("BundleSnapshot.matches"));
        assertEquals(1, count(networking, "broadcastFullState()"));
        assertTrue(networking.contains("PacketRateLimiter.allowResync(player)"));
    }

    private static int count(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
