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
        top.atdove.dovemail.network.payload.client.ClientboundMailSummaryPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.client.ClientboundMailSummaryPayload.STREAM_CODEC,
        ModNetwork::onClientMailSummary
    );
    registrar.playToClient(
        top.atdove.dovemail.network.payload.client.ClientboundMailDetailPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.client.ClientboundMailDetailPayload.STREAM_CODEC,
        ModNetwork::onClientMailDetail
    );
    registrar.playToClient(
        top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload.STREAM_CODEC,
        ModNetwork::onClientOpenMailbox
    );
    registrar.playToClient(
        top.atdove.dovemail.network.payload.client.ClientboundUnreadHintPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.client.ClientboundUnreadHintPayload.STREAM_CODEC,
        ModNetwork::onClientUnreadHint
    );
    registrar.playToClient(
        top.atdove.dovemail.network.payload.client.ClientboundUiAlertPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.client.ClientboundUiAlertPayload.STREAM_CODEC,
        ModNetwork::onClientUiAlert
    );
    registrar.playToClient(
        top.atdove.dovemail.network.payload.client.ClientboundOpenMailDetailPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.client.ClientboundOpenMailDetailPayload.STREAM_CODEC,
        ModNetwork::onClientOpenMailDetail
    );

        // Serverbound
    registrar.playToServer(
        top.atdove.dovemail.network.payload.server.ServerboundRequestMailDetailPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.server.ServerboundRequestMailDetailPayload.STREAM_CODEC,
        ModNetwork::onServerRequestMailDetail
    );
    registrar.playToServer(
        top.atdove.dovemail.network.payload.server.ServerboundClaimAttachmentsPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.server.ServerboundClaimAttachmentsPayload.STREAM_CODEC,
        ModNetwork::onServerClaimAttachments
    );
    registrar.playToServer(
        top.atdove.dovemail.network.payload.server.ServerboundComposeMailPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.server.ServerboundComposeMailPayload.STREAM_CODEC,
        ModNetwork::onServerComposeMail
    );
    registrar.playToServer(
        top.atdove.dovemail.network.payload.server.ServerboundOpenMailboxPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.server.ServerboundOpenMailboxPayload.STREAM_CODEC,
        ModNetwork::onServerOpenMailbox
    );
    registrar.playToServer(
        top.atdove.dovemail.network.payload.server.ServerboundDeleteReadMailsPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.server.ServerboundDeleteReadMailsPayload.STREAM_CODEC,
        ModNetwork::onServerDeleteReadMails
    );
    registrar.playToServer(
        top.atdove.dovemail.network.payload.server.ServerboundOpenAttachmentsPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.server.ServerboundOpenAttachmentsPayload.STREAM_CODEC,
        ModNetwork::onServerOpenAttachments
    );

        LOGGER.debug("[DoveMail] Payload registrar initialized and payloads registered");
    }

    // region Handlers
    private static void onClientMailSummary(top.atdove.dovemail.network.payload.client.ClientboundMailSummaryPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailNetwork.handleMailSummary(payload));
    }

    private static void onClientMailDetail(top.atdove.dovemail.network.payload.client.ClientboundMailDetailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailNetwork.handleMailDetail(payload));
    }

    private static void onClientOpenMailbox(top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailNetwork.handleOpenMailbox(payload));
    }

    private static void onClientUnreadHint(top.atdove.dovemail.network.payload.client.ClientboundUnreadHintPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailNetwork.handleUnreadHint(payload.count()));
    }

    private static void onClientOpenMailDetail(top.atdove.dovemail.network.payload.client.ClientboundOpenMailDetailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailClientHooks.onOpenMailDetail(payload.summary(), payload.attachments()));
    }

    private static void onClientUiAlert(top.atdove.dovemail.network.payload.client.ClientboundUiAlertPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailClientHooks.onUiAlert(payload.key(), payload.args()));
    }

    private static void onServerRequestMailDetail(top.atdove.dovemail.network.payload.server.ServerboundRequestMailDetailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            var level = player.serverLevel();
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(level);
            storage.get(player.getUUID(), payload.mailId()).ifPresent(mail -> {
                if (!mail.isRead()) {
                    mail.markRead();
                    storage.setDirty();
                    var sumPkt = new top.atdove.dovemail.network.payload.client.ClientboundMailSummaryPayload(mail.toSummary());
                    top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(player, sumPkt);
                }
                var pkt = new top.atdove.dovemail.network.payload.client.ClientboundMailDetailPayload(mail.getId(), mail.getAttachments());
                top.atdove.dovemail.network.DovemailNetwork.sendDetailTo(player, pkt);
            });
        });
    }

    private static void onServerClaimAttachments(top.atdove.dovemail.network.payload.server.ServerboundClaimAttachmentsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(player.serverLevel());
            storage.get(player.getUUID(), payload.mailId()).ifPresent(mail ->
                handleClaimForMail(player, storage, mail)
            );
        });
    }

    private static void handleClaimForMail(net.minecraft.server.level.ServerPlayer player,
                                           top.atdove.dovemail.saveddata.MailStorage storage,
                                           top.atdove.dovemail.mail.Mail mail) {
        if (alreadyClaimedOrNone(mail)) {
            sendAttachmentFeedback(player, mail);
            sendMailDetail(player, mail.getId(), java.util.List.of());
            return;
        }
        giveAllAttachmentsToPlayer(player, mail.getAttachments());
        finalizeClaim(storage, mail);
        sendMailDetail(player, mail.getId(), java.util.List.of());
        sendMailSummary(player, mail);
    }

    private static boolean alreadyClaimedOrNone(top.atdove.dovemail.mail.Mail mail) {
        return mail.isAttachmentsClaimed() || mail.getAttachments().isEmpty();
    }

    private static void sendAttachmentFeedback(net.minecraft.server.level.ServerPlayer player, top.atdove.dovemail.mail.Mail mail) {
        if (mail.isAttachmentsClaimed()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.dovemail.attachments.already_claimed"));
        } else if (mail.getAttachments().isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.dovemail.attachments.none"));
        }
    }

    private static void giveAllAttachmentsToPlayer(net.minecraft.server.level.ServerPlayer player, java.util.List<net.minecraft.world.item.ItemStack> attachments) {
        for (var stack : new java.util.ArrayList<>(attachments)) {
            if (stack == null || stack.isEmpty()) continue;
            var toGive = stack.copy();
            if (!player.getInventory().add(toGive)) {
                player.drop(toGive, false);
            }
        }
    }

    private static void finalizeClaim(top.atdove.dovemail.saveddata.MailStorage storage, top.atdove.dovemail.mail.Mail mail) {
        mail.getAttachments().clear();
        mail.markAttachmentsClaimed().markRead();
        storage.setDirty();
    }

    private static void sendMailDetail(net.minecraft.server.level.ServerPlayer player, java.util.UUID mailId, java.util.List<net.minecraft.world.item.ItemStack> attachments) {
        var pkt = new top.atdove.dovemail.network.payload.client.ClientboundMailDetailPayload(mailId, attachments);
        top.atdove.dovemail.network.DovemailNetwork.sendDetailTo(player, pkt);
    }

    private static void sendMailSummary(net.minecraft.server.level.ServerPlayer player, top.atdove.dovemail.mail.Mail mail) {
        var sumPkt = new top.atdove.dovemail.network.payload.client.ClientboundMailSummaryPayload(mail.toSummary());
        top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(player, sumPkt);
    }

    private static void onServerComposeMail(top.atdove.dovemail.network.payload.server.ServerboundComposeMailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer sender)) return;
            String target = payload.recipientName();
            boolean asSystem = payload.asSystem();
            boolean asAnnouncement = payload.asAnnouncement();

            if (isBroadcastTarget(target)) {
                handleBroadcastCompose(sender, payload.subject(), payload.body(), isIncludeKnownOffline(target), asSystem, asAnnouncement);
                return;
            }

            var storage = top.atdove.dovemail.saveddata.MailStorage.get(sender.serverLevel());
            var recipient = resolveRecipient(sender.server, storage, target);
            if (recipient == null) {
                sendUiAlert(sender, "message.dovemail.compose.recipient_not_found", target);
                return;
            }

            var mail = createMailForCompose(sender, payload.subject(), payload.body(), asSystem, asAnnouncement);
            attachAndConsumeIfAny(sender, mail);
            storage.addOrUpdate(recipient.uuid(), mail);
            notifyComposeResult(sender, target, recipient, mail);
        });
    }

    private static boolean isBroadcastTarget(String target) {
        return "@a".equals(target) || "*".equals(target);
    }

    private static boolean isIncludeKnownOffline(String target) {
        return "*".equals(target);
    }

    private static top.atdove.dovemail.mail.Mail createMailForCompose(net.minecraft.server.level.ServerPlayer sender,
                                                                      String subject,
                                                                      String body,
                                                                      boolean asSystem,
                                                                      boolean asAnnouncement) {
        var mail = new top.atdove.dovemail.mail.Mail();
        mail.setSubject(subject)
            .setBodyPlain(body)
            .setSenderName(sender.getGameProfile().getName())
            .setTimestamp(System.currentTimeMillis())
            .setRead(false)
            .setAttachmentsClaimed(false);

        if (asSystem) {
            if (sender.hasPermissions(3)) {
                mail.setSenderName("System");
                if (asAnnouncement) mail.setAnnouncement(true);
            } else {
                sendUiAlert(sender, "message.dovemail.compose.no_permission");
            }
        }
        return mail;
    }

    private static void attachAndConsumeIfAny(net.minecraft.server.level.ServerPlayer sender, top.atdove.dovemail.mail.Mail mail) {
        var attachments = top.atdove.dovemail.menu.AttachmentManager.consume(sender);
        if (!attachments.isEmpty()) mail.setAttachments(attachments);
    }

    private static void notifyComposeResult(net.minecraft.server.level.ServerPlayer sender,
                                            String target,
                                            ResolvedRecipient recipient,
                                            top.atdove.dovemail.mail.Mail mail) {
        if (recipient.online() != null) {
            var summaryPkt = new top.atdove.dovemail.network.payload.client.ClientboundMailSummaryPayload(mail.toSummary());
            top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(recipient.online(), summaryPkt);
            // 使用 UI 提示，便于在写信界面内联显示
            sendUiAlert(sender, "message.dovemail.compose.sent", target);
        } else {
            // 使用 UI 提示，便于在写信界面内联显示
            sendUiAlert(sender, "message.dovemail.compose.sent_offline", target);
        }
    }

    private static void handleBroadcastCompose(net.minecraft.server.level.ServerPlayer sender, String subject, String body, boolean includeKnownOffline, boolean asSystem, boolean asAnnouncement) {
        if (!sender.hasPermissions(3)) {
            sendUiAlert(sender, "message.dovemail.compose.no_permission");
            return;
        }
        var storage = top.atdove.dovemail.saveddata.MailStorage.get(sender.serverLevel());
        var makeMail = createMailSupplierForBroadcast(sender, subject, body, asSystem, asAnnouncement);

        var onlinePlayers = sender.server.getPlayerList().getPlayers();
        var onlineSet = new java.util.HashSet<java.util.UUID>();
        int onlineCount = distributeToOnline(storage, makeMail, onlinePlayers, onlineSet);
        int offlineCount = includeKnownOffline ? distributeToOffline(storage, makeMail, onlineSet) : 0;

        sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
            "message.dovemail.compose.broadcast_result", onlineCount + offlineCount, onlineCount, offlineCount
        ));
    }

    private static java.util.function.Supplier<top.atdove.dovemail.mail.Mail> createMailSupplierForBroadcast(
            net.minecraft.server.level.ServerPlayer sender,
            String subject,
            String body,
            boolean asSystem,
            boolean asAnnouncement) {
        var baseAttachments = top.atdove.dovemail.menu.AttachmentManager.consume(sender);
        return () -> buildMailForBroadcast(sender, subject, body, asSystem, asAnnouncement, baseAttachments);
    }

    private static top.atdove.dovemail.mail.Mail buildMailForBroadcast(
            net.minecraft.server.level.ServerPlayer sender,
            String subject,
            String body,
            boolean asSystem,
            boolean asAnnouncement,
            java.util.List<net.minecraft.world.item.ItemStack> baseAttachments) {
        var m = new top.atdove.dovemail.mail.Mail();
        m.setSubject(subject)
         .setBodyPlain(body)
         .setSenderName(sender.getGameProfile().getName())
         .setTimestamp(System.currentTimeMillis())
         .setRead(false)
         .setAttachmentsClaimed(false);
        if (asSystem) {
            m.setSenderName("System");
            if (asAnnouncement) m.setAnnouncement(true);
        }
        if (!baseAttachments.isEmpty()) {
            var copy = new java.util.ArrayList<net.minecraft.world.item.ItemStack>(baseAttachments.size());
            for (var st : baseAttachments) copy.add(st.copy());
            m.setAttachments(copy);
        }
        return m;
    }

    private static int distributeToOnline(
            top.atdove.dovemail.saveddata.MailStorage storage,
            java.util.function.Supplier<top.atdove.dovemail.mail.Mail> makeMail,
            java.util.List<net.minecraft.server.level.ServerPlayer> onlinePlayers,
            java.util.Set<java.util.UUID> onlineSet) {
        int count = 0;
        for (var op : onlinePlayers) {
            onlineSet.add(op.getUUID());
            var mail = makeMail.get();
            storage.addOrUpdate(op.getUUID(), mail);
            var summaryPkt = new top.atdove.dovemail.network.payload.client.ClientboundMailSummaryPayload(mail.toSummary());
            top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(op, summaryPkt);
            count++;
        }
        return count;
    }

    private static int distributeToOffline(
            top.atdove.dovemail.saveddata.MailStorage storage,
            java.util.function.Supplier<top.atdove.dovemail.mail.Mail> makeMail,
            java.util.Set<java.util.UUID> onlineSet) {
        int count = 0;
        for (var uuid : storage.getKnownPlayers()) {
            if (onlineSet.contains(uuid)) continue;
            storage.addOrUpdate(uuid, makeMail.get());
            count++;
        }
        return count;
    }

    private static void sendUiAlert(net.minecraft.server.level.ServerPlayer player, String key, String... args) {
        java.util.List<String> list = java.util.Arrays.asList(args);
        var pkt = new top.atdove.dovemail.network.payload.client.ClientboundUiAlertPayload(key, list);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
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
    private static void onServerOpenMailbox(top.atdove.dovemail.network.payload.server.ServerboundOpenMailboxPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(player.serverLevel());
            var summaries = storage.getSummaries(player.getUUID());
            var pkt = new top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload(summaries);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
        });
    }

    private static void onServerDeleteReadMails(top.atdove.dovemail.network.payload.server.ServerboundDeleteReadMailsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            var result = deleteReadForPlayer(player);
            // 刷新邮箱摘要
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(player.serverLevel());
            var summaries = storage.getSummaries(player.getUUID());
            var pkt = new top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload(summaries);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
            // 反馈结果（改为 UI 提示，使其在邮箱界面底部显示）
            sendUiAlert(player, "message.dovemail.delete_read.result",
                Integer.toString(result.deleted()), Integer.toString(result.skipped())
            );
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

    private static void onServerOpenAttachments(top.atdove.dovemail.network.payload.server.ServerboundOpenAttachmentsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            player.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, @javax.annotation.Nonnull net.minecraft.world.entity.player.Inventory inv, @javax.annotation.Nonnull net.minecraft.world.entity.player.Player playerEntity) {
                    return new top.atdove.dovemail.menu.AttachmentMenu(id, inv, top.atdove.dovemail.menu.AttachmentManager.get(player));
                }

                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return net.minecraft.network.chat.Component.translatable("screen.dovemail.attachments");
                }
            });
        });
    }
}
