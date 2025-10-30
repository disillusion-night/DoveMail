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
                (payload, ctx) -> onClientMailSummary(payload, ctx)
        );
        registrar.playToClient(
                top.atdove.dovemail.network.payload.ClientboundMailDetailPayload.PACKET_TYPE,
                top.atdove.dovemail.network.payload.ClientboundMailDetailPayload.STREAM_CODEC,
                (payload, ctx) -> onClientMailDetail(payload, ctx)
        );

        // Serverbound
        registrar.playToServer(
                top.atdove.dovemail.network.payload.ServerboundRequestMailDetailPayload.PACKET_TYPE,
                top.atdove.dovemail.network.payload.ServerboundRequestMailDetailPayload.STREAM_CODEC,
                (payload, ctx) -> onServerRequestMailDetail(payload, ctx)
        );
        registrar.playToServer(
                top.atdove.dovemail.network.payload.ServerboundClaimAttachmentsPayload.PACKET_TYPE,
                top.atdove.dovemail.network.payload.ServerboundClaimAttachmentsPayload.STREAM_CODEC,
                (payload, ctx) -> onServerClaimAttachments(payload, ctx)
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

    private static void onServerRequestMailDetail(top.atdove.dovemail.network.payload.ServerboundRequestMailDetailPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer player)) return;
            // 服务端：读取存储，收集附件并下发详情
            var level = player.serverLevel();
            var storage = top.atdove.dovemail.saveddata.MailStorage.get(level);
            storage.get(player.getUUID(), payload.mailId()).ifPresent(mail -> {
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
            });
        });
    }
    // endregion
}
