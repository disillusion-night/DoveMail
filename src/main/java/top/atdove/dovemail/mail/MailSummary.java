package top.atdove.dovemail.mail;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.UUID;

/**
 * 客户端用于渲染的精简邮件数据。
 */
public record MailSummary(UUID id, String subject, String bodyJson, String senderName, long timestamp,
                          boolean read, boolean attachmentsClaimed, boolean hasAttachments) {

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyJson() {
        return bodyJson;
    }

    public String getSenderName() {
        return senderName;
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

    public Component bodyComponent() {
        if (bodyJson == null || bodyJson.isEmpty()) {
            return Component.empty();
        }
        try {
            JsonElement json = JsonParser.parseString(bodyJson);
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(Component.literal(bodyJson));
        } catch (JsonSyntaxException | IllegalStateException ex) {
            return Component.literal(bodyJson);
        }
    }
}
