package top.atdove.dovemail.network.payload.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;
import top.atdove.dovemail.mail.MailSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ClientboundOpenMailboxPayload(List<MailSummary> summaries) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundOpenMailboxPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "open_mailbox"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenMailboxPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                        List<MailSummary> list = payload.summaries();
                        buf.writeVarInt(list.size());
                        for (MailSummary s : list) {
                            buf.writeUtf(s.getId().toString());
                            buf.writeUtf(s.getSubject());
                            buf.writeUtf(s.getBodyJson());
                            buf.writeUtf(s.getSenderName() == null ? "" : s.getSenderName());
                            buf.writeLong(s.getTimestamp());
                            buf.writeBoolean(s.isRead());
                            buf.writeBoolean(s.isAttachmentsClaimed());
                            buf.writeBoolean(s.hasAttachments());
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<MailSummary> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            UUID id = UUID.fromString(buf.readUtf(32767));
                            String subject = buf.readUtf(32767);
                            String bodyJson = buf.readUtf(32767);
                            String sender = buf.readUtf(32767);
                            long ts = buf.readLong();
                            boolean read = buf.readBoolean();
                            boolean claimed = buf.readBoolean();
                            boolean hasAtt = buf.readBoolean();
                            list.add(new MailSummary(id, subject, bodyJson, sender, ts, read, claimed, hasAtt));
                        }
                        return new ClientboundOpenMailboxPayload(list);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
