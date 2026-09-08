package dev.modernbundles.bundle;

public final class RemoteCapabilities {
    private static volatile boolean serverSupportsSelection;

    private RemoteCapabilities() {
    }

    public static boolean serverSupportsSelection() {
        return serverSupportsSelection;
    }

    public static void setServerSupportsSelection(boolean supported) {
        serverSupportsSelection = supported;
    }
}
