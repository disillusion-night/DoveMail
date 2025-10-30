package top.atdove.dovemail.mail;

import java.util.UUID;

/**
 * 客户端用于渲染的精简邮件数据。
 */
public class MailSummary {
    private final UUID id;
    private final String subject;
    private final String bodyJson;
    private final long timestamp;
    private final boolean read;
    private final boolean attachmentsClaimed;

    public MailSummary(UUID id, String subject, String bodyJson, long timestamp, boolean read, boolean attachmentsClaimed) {
        this.id = id;
        this.subject = subject;
        this.bodyJson = bodyJson;
        this.timestamp = timestamp;
        this.read = read;
        this.attachmentsClaimed = attachmentsClaimed;
    }

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyJson() {
        return bodyJson;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isAttachmentsClaimed() {
        return attachmentsClaimed;
    }
}
