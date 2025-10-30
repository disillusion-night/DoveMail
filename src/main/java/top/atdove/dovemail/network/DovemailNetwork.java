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
        // 预留：接入 NeoForge Payload 发送（ServerboundRequestMailDetailPacket）
        LOGGER.debug("[DoveMail] requestMailDetail: {}", mailId);
    }

    public static void claimAttachments(UUID mailId) {
        // 预留：接入 NeoForge Payload 发送（ServerboundClaimAttachmentsPacket）
        LOGGER.debug("[DoveMail] claimAttachments: {}", mailId);
    }
    // endregion

    // region Server -> Client callbacks (client execution)
    public static void handleMailDetail(ClientboundMailDetailPacket packet) {
        DovemailClientHooks.onMailDetailReceived(packet.mailId(), packet.attachments());
    }
    // endregion

    // region Server utilities (placeholders)
    public static void sendSummaryTo(ServerPlayer player, ClientboundMailSummaryPacket pkt) {
        // 预留：接入 Payload（单封摘要下发）
        LOGGER.debug("[DoveMail] sendSummaryTo {} -> {}", player.getGameProfile().getName(), pkt.summary().getId());
    }

    public static void sendDetailTo(ServerPlayer player, ClientboundMailDetailPacket pkt) {
        // 预留：接入 Payload（详情下发）
        LOGGER.debug("[DoveMail] sendDetailTo {} -> {}", player.getGameProfile().getName(), pkt.mailId());
    }
    // endregion
}
