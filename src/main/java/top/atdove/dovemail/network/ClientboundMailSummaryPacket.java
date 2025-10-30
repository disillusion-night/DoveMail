package top.atdove.dovemail.network;

import net.minecraft.network.FriendlyByteBuf;
import top.atdove.dovemail.mail.MailSummary;

/**
 * 单封邮件摘要（服务端->客户端）。
 */
public record ClientboundMailSummaryPacket(MailSummary summary) {
    public static ClientboundMailSummaryPacket decode(FriendlyByteBuf buf) {
        // 复用现有字段顺序：id, subject, bodyJson, senderName, timestamp, read, attachmentsClaimed, hasAttachments
        var id = buf.readUUID();
        var subject = buf.readUtf(32767);
        var bodyJson = buf.readUtf(32767);
        var senderName = buf.readUtf(32767);
        long timestamp = buf.readLong();
        boolean read = buf.readBoolean();
        boolean attachmentsClaimed = buf.readBoolean();
        boolean hasAttachments = buf.readBoolean();
        return new ClientboundMailSummaryPacket(new MailSummary(id, subject, bodyJson, senderName, timestamp, read, attachmentsClaimed, hasAttachments));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(summary.id());
        buf.writeUtf(summary.subject());
        buf.writeUtf(summary.bodyJson());
        buf.writeUtf(summary.getSenderName() == null ? "" : summary.getSenderName());
        buf.writeLong(summary.timestamp());
        buf.writeBoolean(summary.read());
        buf.writeBoolean(summary.attachmentsClaimed());
        buf.writeBoolean(summary.hasAttachments());
    }
}
