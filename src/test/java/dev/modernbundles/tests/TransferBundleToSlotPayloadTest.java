package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import dev.modernbundles.network.TransferBundleToSlotPayload;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

class TransferBundleToSlotPayloadTest {
    @Test
    void payloadCodecRoundTripsTheMenuTarget() {
        TransferBundleToSlotPayload payload = new TransferBundleToSlotPayload(12, 37, List.of());
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.OTHER
        );
        try {
            TransferBundleToSlotPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, TransferBundleToSlotPayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}

