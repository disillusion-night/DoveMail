package top.atdove.dovemail.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.neoforge.network.NetworkEvent;
import top.atdove.dovemail.mail.MailSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 从服务器同步收件箱数据到客户端。
 */
public class ClientboundMailSyncPacket {
    private final List<MailSummary> summaries;

    public ClientboundMailSyncPacket(List<MailSummary> summaries) {
        this.summaries = summaries;
    }

    public List<MailSummary> getSummaries() {
        return summaries;
    }

    public static ClientboundMailSyncPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<MailSummary> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID id = buf.readUUID();
            String subject = buf.readUtf(32767);
            String bodyJson = buf.readUtf(32767);
            long timestamp = buf.readLong();
            boolean read = buf.readBoolean();
            boolean attachmentsClaimed = buf.readBoolean();
            list.add(new MailSummary(id, subject, bodyJson, timestamp, read, attachmentsClaimed));
        }
        return new ClientboundMailSyncPacket(list);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(summaries.size());
        for (MailSummary summary : summaries) {
            buf.writeUUID(summary.getId());
            buf.writeUtf(summary.getSubject());
            buf.writeUtf(summary.getBodyJson());
            buf.writeLong(summary.getTimestamp());
            buf.writeBoolean(summary.isRead());
            buf.writeBoolean(summary.isAttachmentsClaimed());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientInboxCache.update(summaries));
        context.setPacketHandled(true);
    }
}
