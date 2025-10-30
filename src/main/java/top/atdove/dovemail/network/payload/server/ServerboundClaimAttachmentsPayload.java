package top.atdove.dovemail.network.payload.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;

import java.util.UUID;

public record ServerboundClaimAttachmentsPayload(UUID mailId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundClaimAttachmentsPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "claim_attachments"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundClaimAttachmentsPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> buf.writeUtf(payload.mailId.toString()),
                    buf -> new ServerboundClaimAttachmentsPayload(java.util.UUID.fromString(buf.readUtf(32767))));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
