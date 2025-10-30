package top.atdove.dovemail.saveddata;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import top.atdove.dovemail.mail.Mail;
import top.atdove.dovemail.mail.MailSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 负责持久化与管理所有邮件实例的存储器。
 */
public class MailStorage extends SavedData {
    private static final String DATA_NAME = "dovemail_mail_storage";
    private static final String TAG_PLAYERS = "players";
    private static final String TAG_PLAYER_ID = "player";
    private static final String TAG_MAILS = "mails";
    private static final SavedData.Factory<MailStorage> FACTORY = new SavedData.Factory<>(MailStorage::new, MailStorage::load, null);

    private final Map<UUID, LinkedHashMap<UUID, Mail>> inboxes = new LinkedHashMap<>();

    public MailStorage() {
        // 默认构造函数用于加载或初始化空存储。
    }

    public static MailStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static MailStorage load(CompoundTag tag, HolderLookup.Provider provider) {
        MailStorage storage = new MailStorage();
        if (tag.contains(TAG_PLAYERS, Tag.TAG_LIST)) {
            ListTag players = tag.getList(TAG_PLAYERS, Tag.TAG_COMPOUND);
            for (int i = 0; i < players.size(); i++) {
                CompoundTag playerTag = players.getCompound(i);
                if (!playerTag.hasUUID(TAG_PLAYER_ID)) {
                    continue;
                }
                UUID playerId = playerTag.getUUID(TAG_PLAYER_ID);
                LinkedHashMap<UUID, Mail> inbox = storage.inbox(playerId);
                if (playerTag.contains(TAG_MAILS, Tag.TAG_LIST)) {
                    ListTag mailList = playerTag.getList(TAG_MAILS, Tag.TAG_COMPOUND);
                    for (int j = 0; j < mailList.size(); j++) {
                        CompoundTag mailTag = mailList.getCompound(j);
                        Mail mail = Mail.load(mailTag, provider);
                        inbox.put(mail.getId(), mail);
                    }
                }
            }
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, LinkedHashMap<UUID, Mail>> entry : inboxes.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(TAG_PLAYER_ID, entry.getKey());

            ListTag mailList = new ListTag();
            for (Mail mail : entry.getValue().values()) {
                mailList.add(mail.save(provider));
            }
            playerTag.put(TAG_MAILS, mailList);
            players.add(playerTag);
        }
        tag.put(TAG_PLAYERS, players);
        return tag;
    }

    public Collection<Mail> getAll(UUID playerId) {
        LinkedHashMap<UUID, Mail> inbox = inboxes.get(playerId);
        if (inbox == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(inbox.values());
    }

    public List<MailSummary> getSummaries(UUID playerId) {
        LinkedHashMap<UUID, Mail> inbox = inboxes.get(playerId);
        if (inbox == null || inbox.isEmpty()) {
            return Collections.emptyList();
        }
        List<MailSummary> summaries = new ArrayList<>(inbox.size());
        for (Mail mail : inbox.values()) {
            summaries.add(mail.toSummary());
        }
        return summaries;
    }

    public Collection<Mail> getAll() {
        if (inboxes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Mail> all = new ArrayList<>();
        for (LinkedHashMap<UUID, Mail> inbox : inboxes.values()) {
            all.addAll(inbox.values());
        }
        return Collections.unmodifiableList(all);
    }

    public Optional<Mail> get(UUID playerId, UUID id) {
        LinkedHashMap<UUID, Mail> inbox = inboxes.get(playerId);
        if (inbox == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inbox.get(id));
    }

    public void addOrUpdate(UUID playerId, Mail mail) {
        LinkedHashMap<UUID, Mail> inbox = inbox(playerId);
        UUID id = mail.getId();
        if (id == null) {
            id = UUID.randomUUID();
            mail.setRead(false).setAttachmentsClaimed(false);
        }
        inbox.put(id, mail);
        setDirty();
    }

    public boolean remove(UUID playerId, UUID id) {
        LinkedHashMap<UUID, Mail> inbox = inboxes.get(playerId);
        if (inbox == null) {
            return false;
        }
        if (inbox.remove(id) != null) {
            pruneIfEmpty(playerId, inbox);
            setDirty();
            return true;
        }
        return false;
    }

    public boolean markRead(UUID playerId, UUID id) {
        Mail mail = get(playerId, id).orElse(null);
        if (mail == null) {
            return false;
        }
        if (!mail.isRead()) {
            mail.setRead(true);
            setDirty();
        }
        return true;
    }

    public boolean markAttachmentsClaimed(UUID playerId, UUID id) {
        Mail mail = get(playerId, id).orElse(null);
        if (mail == null) {
            return false;
        }
        if (!mail.isAttachmentsClaimed()) {
            mail.setAttachmentsClaimed(true);
            setDirty();
        }
        return true;
    }

    public void clear(UUID playerId) {
        LinkedHashMap<UUID, Mail> inbox = inboxes.remove(playerId);
        if (inbox != null && !inbox.isEmpty()) {
            setDirty();
        }
    }

    public void clearAll() {
        if (!inboxes.isEmpty()) {
            inboxes.clear();
            setDirty();
        }
    }

    public boolean hasMail(UUID playerId) {
        LinkedHashMap<UUID, Mail> inbox = inboxes.get(playerId);
        return inbox != null && !inbox.isEmpty();
    }

    public Set<UUID> getKnownPlayers() {
        return Collections.unmodifiableSet(inboxes.keySet());
    }

    public void ensureInbox(UUID playerId) {
        inboxes.computeIfAbsent(playerId, key -> {
            setDirty();
            return new LinkedHashMap<>();
        });
    }

    private LinkedHashMap<UUID, Mail> inbox(UUID playerId) {
        return inboxes.computeIfAbsent(playerId, key -> new LinkedHashMap<>());
    }

    private void pruneIfEmpty(UUID playerId, LinkedHashMap<UUID, Mail> inbox) {
        if (inbox.isEmpty()) {
            inboxes.remove(playerId);
        }
    }
}
