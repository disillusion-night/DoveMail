package top.atdove.dovemail.mail;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.atdove.dovemail.saveddata.MailStorage;

/**
 * 玩家相关的邮件事件监听。
 */
public final class MailEvents {

    private MailEvents() {
        // 禁止实例化
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        MailStorage storage = MailStorage.get(level);
        storage.ensureInbox(serverPlayer.getUUID());

        // 上线提示未读邮件数量
        long unread = storage.getAll(serverPlayer.getUUID()).stream()
                .filter(m -> !m.isRead())
                .count();
        if (unread > 0) {
            serverPlayer.sendSystemMessage(Component.translatable("message.dovemail.inbox.unread_on_login", unread));
            // 发送客户端提示用的 payload（客户端可根据开关选择以吐司/标题栏显示）
            var hint = new top.atdove.dovemail.network.payload.client.ClientboundUnreadHintPayload((int) unread);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, hint);
        }

        // 公告邮件：优先自动弹出第一封未读公告
        var allMails = storage.getAll(serverPlayer.getUUID());
        for (var mail : allMails) {
            if (!mail.isRead() && mail.isAnnouncement()) {
                var summary = mail.toSummary();
                var detailPkt = new top.atdove.dovemail.network.payload.client.ClientboundOpenMailDetailPayload(summary, new java.util.ArrayList<>(mail.getAttachments()));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, detailPkt);
                break;
            }
        }

        // 如果开启了开关，则自动打开邮箱
        if (unread > 0 && top.atdove.dovemail.Config.isAutoOpenMailboxOnLoginWhenUnread()) {
            var summaries = storage.getSummaries(serverPlayer.getUUID());
            var pkt = new top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload(summaries);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, pkt);
        }
    }
}
