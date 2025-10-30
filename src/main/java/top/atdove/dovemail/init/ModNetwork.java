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
    registrar.playToClient(
        top.atdove.dovemail.network.payload.ClientboundUiAlertPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ClientboundUiAlertPayload.STREAM_CODEC,
        ModNetwork::onClientUiAlert
    );
    registrar.playToClient(
        top.atdove.dovemail.network.payload.ClientboundOpenMailDetailPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ClientboundOpenMailDetailPayload.STREAM_CODEC,
        ModNetwork::onClientOpenMailDetail
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
    registrar.playToServer(
        top.atdove.dovemail.network.payload.ServerboundOpenAttachmentsPayload.PACKET_TYPE,
        top.atdove.dovemail.network.payload.ServerboundOpenAttachmentsPayload.STREAM_CODEC,
        ModNetwork::onServerOpenAttachments
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

    private static void onClientOpenMailDetail(top.atdove.dovemail.network.payload.ClientboundOpenMailDetailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailClientHooks.onOpenMailDetail(payload.summary(), payload.attachments()));
    }

    private static void onClientUiAlert(top.atdove.dovemail.network.payload.ClientboundUiAlertPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> top.atdove.dovemail.network.DovemailClientHooks.onUiAlert(payload.key(), payload.args()));
    }

    private static void onServerRequestMailDetail(top.atdove.dovemail.network.payload.ServerboundRequestMailDetailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            var level = player.serverLevel();
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(level);
            storage.get(player.getUUID(), payload.mailId()).ifPresent(mail -> {
                if (!mail.isRead()) {
                    mail.markRead();
                    storage.setDirty();
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
                // Idempotent guard: if already claimed or no attachments, just refresh detail and exit
                if (mail.isAttachmentsClaimed() || mail.getAttachments().isEmpty()) {
                    // Feedback message
                    if (mail.isAttachmentsClaimed()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.dovemail.attachments.already_claimed"
                        ));
                    } else if (mail.getAttachments().isEmpty()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.dovemail.attachments.none"
                        ));
                    }
                    var detail0 = new top.atdove.dovemail.network.payload.ClientboundMailDetailPayload(mail.getId(), java.util.List.of());
                    top.atdove.dovemail.network.DovemailNetwork.sendDetailTo(player, detail0);
                    return;
                }

                var list = new java.util.ArrayList<>(mail.getAttachments());
                for (var stack : list) {
                    if (stack == null || stack.isEmpty()) continue;
                    var toGive = stack.copy();
                    if (!player.getInventory().add(toGive)) {
                        player.drop(toGive, false);
                    }
                }
                // Clear attachments to avoid double-claiming on repeated requests
                mail.getAttachments().clear();
                mail.markAttachmentsClaimed().markRead();
                storage.setDirty();

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
            String target = payload.recipientName();
            boolean asSystem = payload.asSystem();
            boolean asAnnouncement = payload.asAnnouncement();

            // 管理员群发：@a 在线所有玩家；* 所有已知玩家（含不在线）
            if ("@a".equals(target)) {
                handleBroadcastCompose(sender, payload.subject(), payload.body(), false, asSystem, asAnnouncement);
                return;
            }
            if ("*".equals(target)) {
                handleBroadcastCompose(sender, payload.subject(), payload.body(), true, asSystem, asAnnouncement);
                return;
            }

            var level = sender.serverLevel();
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(level);
            var server = sender.server;

            // 普通单人发送逻辑（支持离线但已知的玩家）
            var maybeRecipient = resolveRecipient(server, storage, target);
            if (maybeRecipient == null) {
                sendUiAlert(sender, "message.dovemail.compose.recipient_not_found", target);
                return;
            }

            var mail = new top.atdove.dovemail.mail.Mail();
            mail.setSubject(payload.subject())
                .setBodyPlain(payload.body())
                .setSenderName(sender.getGameProfile().getName())
                .setTimestamp(System.currentTimeMillis())
                .setRead(false)
                .setAttachmentsClaimed(false);

            // 权限与 System/公告设置：仅权限级别>=3 才能以 System 身份发送；公告仅在以 System 身份时生效
            if (asSystem) {
                if (sender.hasPermissions(3)) {
                    mail.setSenderName("System");
                    if (asAnnouncement) {
                        mail.setAnnouncement(true);
                    }
                } else {
                    sendUiAlert(sender, "message.dovemail.compose.no_permission");
                }
            }

            // 将玩家附件容器中的物品作为邮件附件并清空容器
            var attachments = top.atdove.dovemail.menu.AttachmentManager.consume(sender);
            if (!attachments.isEmpty()) {
                mail.setAttachments(attachments);
            }

            storage.addOrUpdate(maybeRecipient.uuid(), mail);

            if (maybeRecipient.online() != null) {
                var summary = mail.toSummary();
                var summaryPkt = new top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload(summary);
                top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(maybeRecipient.online(), summaryPkt);
                sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.dovemail.compose.sent", target));
            } else {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.dovemail.compose.sent_offline", target));
            }
        });
    }

    private static void handleBroadcastCompose(net.minecraft.server.level.ServerPlayer sender, String subject, String body, boolean includeKnownOffline, boolean asSystem, boolean asAnnouncement) {
        if (!sender.hasPermissions(3)) {
            sendUiAlert(sender, "message.dovemail.compose.no_permission");
            return;
        }
        var level = sender.serverLevel();
        var storage = top.atdove.dovemail.saveddata.MailStorage.get(level);
        var server = sender.server;

        // 一次取出附件（从发送者的附件容器）；对每个收件人复制副本
        var baseAttachments = top.atdove.dovemail.menu.AttachmentManager.consume(sender);
        java.util.function.Supplier<top.atdove.dovemail.mail.Mail> makeMail = () -> {
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
        };

        int onlineCount = 0;
        int offlineCount = 0;

        var onlinePlayers = server.getPlayerList().getPlayers();
        var onlineSet = new java.util.HashSet<java.util.UUID>();
        for (var op : onlinePlayers) {
            onlineSet.add(op.getUUID());
            var mail = makeMail.get();
            storage.addOrUpdate(op.getUUID(), mail);
            var summaryPkt = new top.atdove.dovemail.network.payload.ClientboundMailSummaryPayload(mail.toSummary());
            top.atdove.dovemail.network.DovemailNetwork.sendSummaryTo(op, summaryPkt);
            onlineCount++;
        }

        if (includeKnownOffline) {
            for (var uuid : storage.getKnownPlayers()) {
                if (onlineSet.contains(uuid)) continue;
                storage.addOrUpdate(uuid, makeMail.get());
                offlineCount++;
            }
        }

        sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.dovemail.compose.broadcast_result", onlineCount + offlineCount, onlineCount, offlineCount
        ));
    }

    private static void sendUiAlert(net.minecraft.server.level.ServerPlayer player, String key, String... args) {
        java.util.List<String> list = java.util.Arrays.asList(args);
        var pkt = new top.atdove.dovemail.network.payload.ClientboundUiAlertPayload(key, list);
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

    private static void onServerOpenAttachments(top.atdove.dovemail.network.payload.ServerboundOpenAttachmentsPayload payload, IPayloadContext ctx) {
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
