package dev.modernbundles.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import dev.modernbundles.bundle.BundleSnapshot;
import dev.modernbundles.network.TransferBundleToSlotPayload;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

class TransferBundleToSlotPayloadTest {
    @Test
    void payloadCodecRoundTripsTheMenuTarget() {
        TransferBundleToSlotPayload payload = new TransferBundleToSlotPayload(12, 37, List.of());
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            TransferBundleToSlotPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, TransferBundleToSlotPayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void codecRejectsOversizedContentsBeforeStackDecodingAndOnEncoding() {
        RegistryFriendlyByteBuf decodeBuffer = buffer();
        try {
            decodeBuffer.writeVarInt(12);
            decodeBuffer.writeVarInt(37);
            decodeBuffer.writeVarInt(BundleSnapshot.MAX_CONTENT_STACKS + 1);
            assertThrows(DecoderException.class, () -> TransferBundleToSlotPayload.STREAM_CODEC.decode(decodeBuffer));
        } finally {
            decodeBuffer.release();
        }

        RegistryFriendlyByteBuf encodeBuffer = buffer();
        try {
            List<ItemStack> oversized = Collections.nCopies(BundleSnapshot.MAX_CONTENT_STACKS + 1, ItemStack.EMPTY);
            assertThrows(
                EncoderException.class,
                () -> TransferBundleToSlotPayload.STREAM_CODEC.encode(
                    encodeBuffer,
                    new TransferBundleToSlotPayload(12, 37, oversized)
                )
            );
        } finally {
            encodeBuffer.release();
        }
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.OTHER);
    }
}
