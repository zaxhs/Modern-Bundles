package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modernbundles.bundle.RemoteCapabilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RemoteCapabilitiesTest {
    @AfterEach
    void reset() {
        RemoteCapabilities.setServerSupportsSelection(false);
    }

    @Test
    void unsupportedAndSupportedServerPathsAreExplicit() {
        RemoteCapabilities.setServerSupportsSelection(false);
        assertFalse(RemoteCapabilities.serverSupportsSelection());
        RemoteCapabilities.setServerSupportsSelection(true);
        assertTrue(RemoteCapabilities.serverSupportsSelection());
    }
}
