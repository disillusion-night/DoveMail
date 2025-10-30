package top.atdove.dovemail.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;

public record ServerboundOpenAttachmentsPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundOpenAttachmentsPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "open_attachments"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOpenAttachmentsPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {}, buf -> new ServerboundOpenAttachmentsPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
