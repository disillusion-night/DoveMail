package top.atdove.dovemail.network.payload.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;
import top.atdove.dovemail.mail.MailSummary;

import java.util.UUID;

public record ClientboundMailSummaryPayload(MailSummary summary) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundMailSummaryPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "mail_summary"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMailSummaryPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                        MailSummary s = payload.summary();
                        buf.writeUtf(s.getId().toString());
                        buf.writeUtf(s.getSubject());
                        buf.writeUtf(s.getBodyJson());
                        buf.writeUtf(s.getSenderName() == null ? "" : s.getSenderName());
                        buf.writeLong(s.getTimestamp());
                        buf.writeBoolean(s.isRead());
                        buf.writeBoolean(s.isAttachmentsClaimed());
                        buf.writeBoolean(s.hasAttachments());
                    },
                    buf -> new ClientboundMailSummaryPayload(
                            new MailSummary(
                                    UUID.fromString(buf.readUtf(32767)),
                                    buf.readUtf(32767),
                                    buf.readUtf(32767),
                                    buf.readUtf(32767),
                                    buf.readLong(),
                                    buf.readBoolean(),
                                    buf.readBoolean(),
                                    buf.readBoolean()
                            )
                    ));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
