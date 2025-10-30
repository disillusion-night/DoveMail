package top.atdove.dovemail.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;

public record ServerboundComposeMailPayload(String recipientName, String subject, String body, boolean asSystem, boolean asAnnouncement) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundComposeMailPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "compose_mail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundComposeMailPayload> STREAM_CODEC =
        StreamCodec.of((buf, payload) -> {
            buf.writeUtf(payload.recipientName(), 32767);
            buf.writeUtf(payload.subject(), 32767);
            buf.writeUtf(payload.body(), 32767);
            buf.writeBoolean(payload.asSystem());
            buf.writeBoolean(payload.asAnnouncement());
            },
            buf -> new ServerboundComposeMailPayload(
                buf.readUtf(32767),
                buf.readUtf(32767),
                buf.readUtf(32767),
                buf.readBoolean(),
                buf.readBoolean()
            ));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
