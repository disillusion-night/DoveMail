package top.atdove.dovemail.mail;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 邮件数据模型与 NBT 序列化/反序列化工具。
 *
 * NBT 结构（根为 CompoundTag）：
 * - id: UUID（唯一标识邮件）
 * - subject: String（主题）
 * - body: String（正文 JSON 文本组件）
 * - sender: Compound
 *     - uuid: UUID（发件人 UUID，选填）
 *     - name: String（发件人名称，选填）
 * - attachments: List<Compound>（物品附件；每个元素为 ItemStack 的 NBT）
 * - scoreboard: Compound（记分板分数；key 为目标名，value 为 int 分数）
 * - timestamp: long（毫秒时间戳，Epoch Milli）
 */
public class Mail {
    // NBT key 常量
    public static final String KEY_ID = "id";
    public static final String KEY_SUBJECT = "subject";
    public static final String KEY_BODY = "body";
    public static final String KEY_SENDER = "sender";
    public static final String KEY_SENDER_UUID = "uuid";
    public static final String KEY_SENDER_NAME = "name";
    public static final String KEY_ATTACHMENTS = "attachments";
    public static final String KEY_SCOREBOARD = "scoreboard";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_READ = "read";
    public static final String KEY_ATTACHMENTS_CLAIMED = "attachments_claimed";

    private UUID id;
    private String subject;
    private String bodyJson;
    private UUID senderUuid;
    private String senderName;
    private List<ItemStack> attachments = new ArrayList<>();
    private Map<String, Integer> scoreboard = new LinkedHashMap<>();
    private long timestamp;
    private boolean read;
    private boolean attachmentsClaimed;

    public Mail() {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.bodyJson = componentToJson(Component.empty());
        this.read = false;
        this.attachmentsClaimed = false;
    }

    public Mail(UUID id, String subject, String body, UUID senderUuid, String senderName,
                List<ItemStack> attachments, Map<String, Integer> scoreboard, long timestamp) {
        this.id = id != null ? id : UUID.randomUUID();
        this.subject = subject != null ? subject : "";
        this.bodyJson = normalizeToJsonComponent(body);
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        if (attachments != null) this.attachments = new ArrayList<>(attachments);
        if (scoreboard != null) this.scoreboard = new LinkedHashMap<>(scoreboard);
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.read = false;
        this.attachmentsClaimed = false;
    }

    // 序列化为 NBT（需要 HolderLookup.Provider）
    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(KEY_ID, id);
        if (subject != null) {
            tag.putString(KEY_SUBJECT, subject);
        }
        if (bodyJson != null) {
            tag.putString(KEY_BODY, bodyJson);
        }
        tag.putBoolean(KEY_READ, read);
        tag.putBoolean(KEY_ATTACHMENTS_CLAIMED, attachmentsClaimed);

        CompoundTag sender = new CompoundTag();
        if (senderUuid != null) sender.putUUID(KEY_SENDER_UUID, senderUuid);
        if (senderName != null && !senderName.isEmpty()) sender.putString(KEY_SENDER_NAME, senderName);
        if (!sender.isEmpty()) tag.put(KEY_SENDER, sender);

        ListTag list = new ListTag();
        if (attachments != null) {
            for (ItemStack stack : attachments) {
                if (stack == null || stack.isEmpty()) continue;
                list.add(stack.save(provider));
            }
        }
        tag.put(KEY_ATTACHMENTS, list);

        CompoundTag scoreboardTag = new CompoundTag();
        if (scoreboard != null) {
            for (Map.Entry<String, Integer> e : scoreboard.entrySet()) {
                if (e.getKey() == null) continue;
                scoreboardTag.putInt(e.getKey(), e.getValue() != null ? e.getValue() : 0);
            }
        }
        tag.put(KEY_SCOREBOARD, scoreboardTag);

        tag.putLong(KEY_TIMESTAMP, timestamp);
        return tag;
    }

    // 从 NBT 反序列化（需要 HolderLookup.Provider）
    public static Mail load(CompoundTag tag, HolderLookup.Provider provider) {
        Mail mail = new Mail();
        if (tag == null) return mail;

        if (tag.hasUUID(KEY_ID)) {
            mail.id = tag.getUUID(KEY_ID);
        }
        mail.subject = tag.contains(KEY_SUBJECT, Tag.TAG_STRING) ? tag.getString(KEY_SUBJECT) : "";
        String rawBody = tag.contains(KEY_BODY, Tag.TAG_STRING) ? tag.getString(KEY_BODY) : "";
        mail.bodyJson = normalizeToJsonComponent(rawBody);
    mail.read = tag.contains(KEY_READ, Tag.TAG_BYTE) && tag.getBoolean(KEY_READ);
    mail.attachmentsClaimed = tag.contains(KEY_ATTACHMENTS_CLAIMED, Tag.TAG_BYTE) && tag.getBoolean(KEY_ATTACHMENTS_CLAIMED);

        if (tag.contains(KEY_SENDER, Tag.TAG_COMPOUND)) {
            CompoundTag s = tag.getCompound(KEY_SENDER);
            if (s.hasUUID(KEY_SENDER_UUID)) mail.senderUuid = s.getUUID(KEY_SENDER_UUID);
            if (s.contains(KEY_SENDER_NAME, Tag.TAG_STRING)) mail.senderName = s.getString(KEY_SENDER_NAME);
        }

        mail.attachments.clear();
        if (tag.contains(KEY_ATTACHMENTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(KEY_ATTACHMENTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag st = list.getCompound(i);
                // 1.21+ parseOptional 返回 ItemStack
                ItemStack stack = ItemStack.parseOptional(provider, st);
                if (!stack.isEmpty()) mail.attachments.add(stack);
            }
        }

        mail.scoreboard.clear();
        if (tag.contains(KEY_SCOREBOARD, Tag.TAG_COMPOUND)) {
            CompoundTag sc = tag.getCompound(KEY_SCOREBOARD);
            for (String key : sc.getAllKeys()) {
                mail.scoreboard.put(key, sc.getInt(key));
            }
        }

        if (tag.contains(KEY_TIMESTAMP, Tag.TAG_LONG)) {
            mail.timestamp = tag.getLong(KEY_TIMESTAMP);
        }
        return mail;
    }

    // Getters/Setters
    public UUID getId() { return id; }
    public String getSubject() { return subject; }
    public String getBody() { return bodyJson; }
    public Component getBodyComponent() { return jsonToComponent(bodyJson); }
    public UUID getSenderUuid() { return senderUuid; }
    public String getSenderName() { return senderName; }
    public List<ItemStack> getAttachments() { return attachments; }
    public Map<String, Integer> getScoreboard() { return scoreboard; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public boolean isAttachmentsClaimed() { return attachmentsClaimed; }

    public Mail setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public Mail setBody(String body) {
        this.bodyJson = normalizeToJsonComponent(body);
        return this;
    }

    public Mail setBodyComponent(Component component) {
        this.bodyJson = componentToJson(component);
        return this;
    }

    public Mail setBodyPlain(String body) {
        this.bodyJson = plainTextToJsonComponent(body);
        return this;
    }

    public Mail setSenderUuid(UUID senderUuid) {
        this.senderUuid = senderUuid;
        return this;
    }

    public Mail setSenderName(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public Mail setAttachments(List<ItemStack> attachments) {
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        return this;
    }

    public Mail setScoreboard(Map<String, Integer> scoreboard) {
        this.scoreboard = scoreboard != null ? new LinkedHashMap<>(scoreboard) : new LinkedHashMap<>();
        return this;
    }

    public Mail setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Mail setRead(boolean read) {
        this.read = read;
        return this;
    }

    public Mail setAttachmentsClaimed(boolean attachmentsClaimed) {
        this.attachmentsClaimed = attachmentsClaimed;
        return this;
    }

    // 便捷方法
    public Mail addAttachment(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            this.attachments.add(stack);
        }
        return this;
    }

    public Mail putScore(String objective, int value) {
        if (objective != null && !objective.isEmpty()) {
            this.scoreboard.put(objective, value);
        }
        return this;
    }

    public Mail markRead() {
        this.read = true;
        return this;
    }

    public Mail markAttachmentsClaimed() {
        this.attachmentsClaimed = true;
        return this;
    }

    public static String plainTextToJsonComponent(String text) {
        return componentToJson(Component.literal(text != null ? text : ""));
    }

    private static String normalizeToJsonComponent(String value) {
        return componentToJson(jsonToComponent(value));
    }

    private static String componentToJson(Component component) {
        Component safeComponent = component != null ? component : Component.empty();
        JsonElement json = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, safeComponent).getOrThrow();
        return json.toString();
    }

    private static Component jsonToComponent(String value) {
        if (value == null || value.isEmpty()) {
            return Component.empty();
        }
        try {
            JsonElement json = JsonParser.parseString(value);
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (JsonSyntaxException | IllegalStateException ex) {
            return Component.literal(value);
        }
    }
}
