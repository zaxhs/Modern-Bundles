package dev.modernbundles.network;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import dev.modernbundles.bundle.SelectionAuthority;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PacketRateLimiter {
    public static final int SELECTION_REQUESTS_PER_SECOND = 60;
    public static final int TRANSFER_REQUESTS_PER_SECOND = 20;
    public static final int FULL_RESYNCS_PER_SECOND = 2;
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final FixedWindowRateLimiter<UUID> SELECTION =
        new FixedWindowRateLimiter<>(SELECTION_REQUESTS_PER_SECOND, WINDOW_NANOS);
    private static final FixedWindowRateLimiter<UUID> TRANSFER =
        new FixedWindowRateLimiter<>(TRANSFER_REQUESTS_PER_SECOND, WINDOW_NANOS);
    private static final FixedWindowRateLimiter<UUID> RESYNC =
        new FixedWindowRateLimiter<>(FULL_RESYNCS_PER_SECOND, WINDOW_NANOS);

    private PacketRateLimiter() {
    }

    public static boolean allowSelection(ServerPlayer player) {
        return SELECTION.tryAcquire(player.getUUID(), System.nanoTime());
    }

    public static boolean allowTransfer(ServerPlayer player) {
        return TRANSFER.tryAcquire(player.getUUID(), System.nanoTime());
    }

    public static boolean allowResync(ServerPlayer player) {
        return RESYNC.tryAcquire(player.getUUID(), System.nanoTime());
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player.getUUID());
            SelectionAuthority.clear(player);
        }
    }

    static void clear(UUID playerId) {
        SELECTION.clear(playerId);
        TRANSFER.clear(playerId);
        RESYNC.clear(playerId);
    }
}
