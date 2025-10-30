package top.atdove.dovemail.network.payload.server;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;

import java.util.UUID;

public record ServerboundRequestMailDetailPayload(UUID mailId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundRequestMailDetailPayload> PACKET_TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "request_mail_detail"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ServerboundRequestMailDetailPayload> STREAM_CODEC =
        StreamCodec.of((buf, payload) -> buf.writeUtf(payload.mailId.toString()),
            buf -> new ServerboundRequestMailDetailPayload(java.util.UUID.fromString(buf.readUtf())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
    return PACKET_TYPE;
    }
}
