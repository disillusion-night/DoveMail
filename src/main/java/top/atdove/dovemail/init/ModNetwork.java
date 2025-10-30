package top.atdove.dovemail.init;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import top.atdove.dovemail.Dovemail;

/**
 * 集中管理网络包注册。
 * 后续在 onRegisterPayloadHandlers 中注册具体的 C2S/S2C Payload。
 */
public final class ModNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ModNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetwork::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        // 版本化注册器，后续在此处添加 playToClient / playToServer 注册
        var registrar = event.registrar(Dovemail.MODID).versioned("1");

        // Clientbound
    registrar.playToClient(
        top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload.STREAM_CODEC,
        ModNetwork::onClientMailSummary
    );
    registrar.playToClient(
        top.atdove.dovemail.network.payload.ClientboundMailDetailPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ClientboundMailDetailPayload.STREAM_CODEC,
        ModNetwork::onClientMailDetail
    );
    registrar.playToClient(
        top.atdove.dovemail.network.payload.ClientboundOpenMailboxPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ClientboundOpenMailboxPayload.STREAM_CODEC,
        ModNetwork::onClientOpenMailbox
    );

        // Serverbound
    registrar.playToServer(
        top.atdove.dovemail.network.payload.ServerboundRequestMailDetailPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ServerboundRequestMailDetailPayload.STREAM_CODEC,
        ModNetwork::onServerRequestMailDetail
    );
    registrar.playToServer(
        top.atdove.dovemail.network.payload.ServerboundClaimAttachmentsPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ServerboundClaimAttachmentsPayload.STREAM_CODEC,
        ModNetwork::onServerClaimAttachments
    );
    registrar.playToServer(
        top.atdove.dovemail.network.payload.ServerboundComposeMailPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ServerboundComposeMailPayload.STREAM_CODEC,
        ModNetwork::onServerComposeMail
    );
    registrar.playToServer(
        top.atdove.dovemail.network.payload.ServerboundOpenMailboxPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ServerboundOpenMailboxPayload.STREAM_CODEC,
        ModNetwork::onServerOpenMailbox
    );

        LOGGER.debug("[DoveMail] Payload registrar initialized and payloads registered");
    }

    // region Handlers
    private static void onClientMailSummary(top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailNetwork.handleMailSummary(payload));
    }

    private static void onClientMailDetail(top.atdove.dovemail.network.payload.ClientboundMailDetailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailNetwork.handleMailDetail(payload));
    }

    private static void onClientOpenMailbox(top.atdove.dovemail.network.payload.ClientboundOpenMailboxPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailNetwork.handleOpenMailbox(payload));
    }

    private static void onServerRequestMailDetail(top.atdove.dovemail.network.payload.ServerboundRequestMailDetailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            // 服务端：读取存储，收集附件并下发详情
            var level = player.serverLevel();
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(level);
            storage.get(player.getUUID(), payload.mailId()).ifPresent(mail -> {
                if (!mail.isRead()) {
                    mail.markRead();
                    storage.setDirty();
                    // 推送摘要更新
                    var sumPkt = new top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload(mail.toSummary());
                    top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(player, sumPkt);
                }
                var pkt = new top.atdove.dovemail.network.payload.ClientboundMailDetailPayload(mail.getId(), mail.getAttachments());
                top.atdove.dovemail.network.DovemailNetwork.sendDetailTo(player, pkt);
            });
        });
    }

    private static void onServerClaimAttachments(top.atdove.dovemail.network.payload.ServerboundClaimAttachmentsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            var level = player.serverLevel();
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(level);
            storage.get(player.getUUID(), payload.mailId()).ifPresent(mail -> {
                // 发放附件
                var list = new java.util.ArrayList<>(mail.getAttachments());
                for (var stack : list) {
                    if (stack == null || stack.isEmpty()) continue;
                    if (!player.getInventory().add(stack.copy())) {
                        player.drop(stack.copy(), false);
                    }
                }
                mail.markAttachmentsClaimed().markRead();
                storage.setDirty();
                // 可以选择下发摘要更新，也可以仅下发详情刷新
                var detail = new top.atdove.dovemail.network.payload.ClientboundMailDetailPayload(mail.getId(), java.util.List.of());
                top.atdove.dovemail.network.DovemailNetwork.sendDetailTo(player, detail);
                var sumPkt = new top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload(mail.toSummary());
                top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(player, sumPkt);
            });
        });
    }

    private static void onServerComposeMail(top.atdove.dovemail.network.payload.ServerboundComposeMailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer sender)) return;
            var level = sender.serverLevel();
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(level);

            var server = sender.server;
            var recipient = server.getPlayerList().getPlayerByName(payload.recipientName());
            if (recipient == null) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.dovemail.compose.target_offline", payload.recipientName()));
                return;
            }

        var mail = new top.atdove.dovemail.mail.Mail();
        mail.setSubject(payload.subject())
            .setBodyPlain(payload.body())
                    .setSenderName(sender.getGameProfile().getName())
                    .setTimestamp(System.currentTimeMillis())
                    .setRead(false)
                    .setAttachmentsClaimed(false);

            storage.addOrUpdate(recipient.getUUID(), mail);

            // 可选：下发一条摘要给收件人，便于其已打开邮箱时刷新
            var summary = mail.toSummary();
            var summaryPkt = new top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload(summary);
            top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(recipient, summaryPkt);

            sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.dovemail.compose.sent", payload.recipientName()));
        });
    }

    @SuppressWarnings("unused")
    private static void onServerOpenMailbox(top.atdove.dovemail.network.payload.ServerboundOpenMailboxPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(player.serverLevel());
            var summaries = storage.getSummaries(player.getUUID());
            var pkt = new top.atdove.dovemail.network.payload.ClientboundOpenMailboxPayload(summaries);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
        });
    }
    // endregion
}
