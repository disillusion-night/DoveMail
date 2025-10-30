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
    // endregion

    // region Server -> Client callbacks (client execution)
    public static void handleMailDetail(top.atdove.dovemail.network.payload.ClientboundMailDetailPayload payload) {
        DovemailClientHooks.onMailDetailReceived(payload.mailId(), payload.attachments());
    }

    public static void handleMailSummary(top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload payload) {
        // 目前暂无收件箱即时刷新逻辑，这里先留日志/占位（将来可用于增量更新列表）
        LOGGER.debug("[DoveMail] handleMailSummary client recv id={} subject={}",
                payload.summary().getId(), payload.summary().getSubject());
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
