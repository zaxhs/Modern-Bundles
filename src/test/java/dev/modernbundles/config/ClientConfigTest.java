package dev.modernbundles.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClientConfigTest {
    @Test
    void clientOptionsDefaultOn() {
        assertFalse(ClientConfig.SPEC.isEmpty());
        assertEquals(List.of("expandBundleTooltip"), ClientConfig.EXPAND_BUNDLE_TOOLTIP.getPath());
        assertTrue(ClientConfig.EXPAND_BUNDLE_TOOLTIP.getDefault());
        assertEquals(List.of("bundleDragEnabled"), ClientConfig.BUNDLE_DRAG_ENABLED.getPath());
        assertTrue(ClientConfig.BUNDLE_DRAG_ENABLED.getDefault());
        assertEquals(2, ClientConfig.SPEC.getSpec().size());
    }
}

