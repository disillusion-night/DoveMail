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
    registrar.playToClient(
        top.atdove.dovemail.network.payload.ClientboundUnreadHintPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ClientboundUnreadHintPayload.STREAM_CODEC,
        ModNetwork::onClientUnreadHint
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
    registrar.playToServer(
        top.atdove.dovemail.network.payload.ServerboundDeleteReadMailsPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ServerboundDeleteReadMailsPayload.STREAM_CODEC,
        ModNetwork::onServerDeleteReadMails
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

    private static void onClientUnreadHint(top.atdove.dovemail.network.payload.ClientboundUnreadHintPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailNetwork.handleUnreadHint(payload.count()));
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
            var maybeRecipient = resolveRecipient(server, storage, payload.recipientName());
            if (maybeRecipient == null) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.dovemail.compose.recipient_not_found", payload.recipientName()
                ));
                return;
            }

            var mail = new top.atdove.dovemail.mail.Mail();
            mail.setSubject(payload.subject())
                .setBodyPlain(payload.body())
                .setSenderName(sender.getGameProfile().getName())
                .setTimestamp(System.currentTimeMillis())
                .setRead(false)
                .setAttachmentsClaimed(false);

            storage.addOrUpdate(maybeRecipient.uuid(), mail);

            if (maybeRecipient.online() != null) {
                var summary = mail.toSummary();
                var summaryPkt = new top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload(summary);
                top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(maybeRecipient.online(), summaryPkt);
                sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.dovemail.compose.sent", payload.recipientName()));
            } else {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.dovemail.compose.sent_offline", payload.recipientName()));
            }
        });
    }

    private record ResolvedRecipient(java.util.UUID uuid, net.minecraft.server.level.ServerPlayer online) {}

    private static ResolvedRecipient resolveRecipient(net.minecraft.server.MinecraftServer server,
                                                      top.atdove.dovemail.saveddata.MailStorage storage,
                                                      String recipientName) {
        // 在线优先
        var online = server.getPlayerList().getPlayerByName(recipientName);
        if (online != null) {
            return new ResolvedRecipient(online.getUUID(), online);
        }
        // 离线：要求曾经登录过（profile 可解析 且 我们存储中有该玩家的收件箱）
        var cache = server.getProfileCache();
        if (cache != null) {
            var profileOpt = cache.get(recipientName);
            if (profileOpt.isPresent()) {
                var uuid = profileOpt.get().getId();
                if (storage.getKnownPlayers().contains(uuid)) {
                    return new ResolvedRecipient(uuid, null);
                }
            }
        }
        return null;
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

    private static void onServerDeleteReadMails(top.atdove.dovemail.network.payload.ServerboundDeleteReadMailsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            var result = deleteReadForPlayer(player);
            // 刷新邮箱摘要
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(player.serverLevel());
            var summaries = storage.getSummaries(player.getUUID());
            var pkt = new top.atdove.dovemail.network.payload.ClientboundOpenMailboxPayload(summaries);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
            // 反馈结果
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.dovemail.delete_read.result", result.deleted(), result.skipped()
            ));
        });
    }

    private record DeleteResult(int deleted, int skipped) {}

    private static DeleteResult deleteReadForPlayer(net.minecraft.server.level.ServerPlayer player) {
        var storage = top.atdove.dovemail.saveddata.MailStorage.get(player.serverLevel());
        var all = new java.util.ArrayList<>(storage.getAll(player.getUUID()));
        int deleted = 0;
        int skipped = 0;
        for (var mail : all) {
            boolean shouldDelete = mail.isRead() && (mail.getAttachments().isEmpty() || mail.isAttachmentsClaimed());
            if (shouldDelete) {
                if (storage.remove(player.getUUID(), mail.getId())) deleted++;
            } else if (mail.isRead()) {
                // read but has unclaimed attachments
                skipped++;
            }
        }
        return new DeleteResult(deleted, skipped);
    }
    // endregion
}
