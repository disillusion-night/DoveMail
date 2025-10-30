package top.atdove.dovemail.network;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * 网络层门面：后续接入 NeoForge 新版 Payload 注册与分发。
 * 当前提供客户端调用入口与服务端下发回调，便于先接好 GUI 侧逻辑。
 */
public final class DovemailNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DovemailNetwork() {}

    // region Client -> Server calls (client side)
    public static void requestMailDetail(UUID mailId) {
        // 客户端 -> 服务端：请求邮件详情
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new top.atdove.dovemail.network.payload.ServerboundRequestMailDetailPayload(mailId)
        );
        LOGGER.debug("[DoveMail] requestMailDetail: {}", mailId);
    }

    public static void claimAttachments(UUID mailId) {
        // 客户端 -> 服务端：领取附件
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new top.atdove.dovemail.network.payload.ServerboundClaimAttachmentsPayload(mailId)
        );
        LOGGER.debug("[DoveMail] claimAttachments: {}", mailId);
    }

    public static void composeMail(String recipientName, String subject, String body, boolean asSystem, boolean asAnnouncement) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new top.atdove.dovemail.network.payload.ServerboundComposeMailPayload(recipientName, subject, body, asSystem, asAnnouncement)
        );
        LOGGER.debug("[DoveMail] composeMail to={} subject={} asSystem={} announce={}", recipientName, subject, asSystem, asAnnouncement);
    }

    public static void openMailbox() {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new top.atdove.dovemail.network.payload.ServerboundOpenMailboxPayload()
        );
        LOGGER.debug("[DoveMail] openMailbox request sent");
    }

    public static void deleteReadMails() {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new top.atdove.dovemail.network.payload.ServerboundDeleteReadMailsPayload()
        );
        LOGGER.debug("[DoveMail] deleteReadMails request sent");
    }

    public static void openAttachments() {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new top.atdove.dovemail.network.payload.ServerboundOpenAttachmentsPayload()
        );
        LOGGER.debug("[DoveMail] openAttachments request sent");
    }
    // endregion

    // region Server -> Client callbacks (client execution)
    public static void handleMailDetail(top.atdove.dovemail.network.payload.ClientboundMailDetailPayload payload) {
        DovemailClientHooks.onMailDetailReceived(payload.mailId(), payload.attachments());
    }

    public static void handleMailSummary(top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload payload) {
        top.atdove.dovemail.network.DovemailClientHooks.onMailSummaryReceived(payload.summary());
    }

    public static void handleOpenMailbox(top.atdove.dovemail.network.payload.ClientboundOpenMailboxPayload payload) {
        DovemailClientHooks.onOpenMailbox(payload.summaries());
    }

    public static void handleUnreadHint(int count) {
        DovemailClientHooks.onUnreadHint(count);
    }
    // endregion

    // region Server utilities (placeholders)
    public static void sendSummaryTo(ServerPlayer player, top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload pkt) {
        // 服务端 -> 客户端：下发单封摘要
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
        LOGGER.debug("[DoveMail] sendSummaryTo {} -> {}", player.getGameProfile().getName(), pkt.summary().getId());
    }

    public static void sendDetailTo(ServerPlayer player, top.atdove.dovemail.network.payload.ClientboundMailDetailPayload pkt) {
        // 服务端 -> 客户端：下发详情
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
        LOGGER.debug("[DoveMail] sendDetailTo {} -> {}", player.getGameProfile().getName(), pkt.mailId());
    }
    // endregion
}
