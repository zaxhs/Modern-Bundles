package dev.modernbundles.network;

import java.util.List;

import dev.modernbundles.ModernBundles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SelectBundleItemPayload(
    int containerId,
    int slotIndex,
    int selectedItemIndex,
    List<ItemStack> expectedContents
)
    implements CustomPacketPayload {
    public static final Type<SelectBundleItemPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ModernBundles.MODID, "select_bundle_item")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectBundleItemPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SelectBundleItemPayload::containerId,
        ByteBufCodecs.VAR_INT,
        SelectBundleItemPayload::slotIndex,
        ByteBufCodecs.VAR_INT,
        SelectBundleItemPayload::selectedItemIndex,
        ItemStack.OPTIONAL_LIST_STREAM_CODEC,
        SelectBundleItemPayload::expectedContents,
        SelectBundleItemPayload::new
    );

    @Override
    public Type<SelectBundleItemPayload> type() {
        return TYPE;
    }
}

