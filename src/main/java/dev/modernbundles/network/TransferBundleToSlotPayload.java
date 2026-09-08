package dev.modernbundles.network;

import java.util.List;

import dev.modernbundles.ModernBundles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record TransferBundleToSlotPayload(int containerId, int slotIndex, List<ItemStack> expectedContents)
    implements CustomPacketPayload {
    public static final Type<TransferBundleToSlotPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ModernBundles.MODID, "transfer_bundle_to_slot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TransferBundleToSlotPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TransferBundleToSlotPayload::containerId,
            ByteBufCodecs.VAR_INT,
            TransferBundleToSlotPayload::slotIndex,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC,
            TransferBundleToSlotPayload::expectedContents,
            TransferBundleToSlotPayload::new
        );

    @Override
    public Type<TransferBundleToSlotPayload> type() {
        return TYPE;
    }
}

