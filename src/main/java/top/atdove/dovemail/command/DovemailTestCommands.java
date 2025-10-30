package top.atdove.dovemail.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.atdove.dovemail.mail.Mail;
import top.atdove.dovemail.saveddata.MailStorage;

public final class DovemailTestCommands {
    private DovemailTestCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Respect config toggle: do not register test commands unless enabled
        if (!top.atdove.dovemail.Config.isEnableTestCommands()) {
            return;
        }
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("dovemailtest")
                .requires(src -> src.hasPermission(0));

        // /dovemailtest open
        root.then(Commands.literal("open").executes(ctx -> {
            CommandSourceStack src = ctx.getSource();
            if (src.getEntity() instanceof ServerPlayer player) {
                var storage = MailStorage.get(player.serverLevel());
                var summaries = storage.getSummaries(player.getUUID());
                var pkt = new top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload(summaries);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
                return 1;
            }
            return 0;
        }));

        // /dovemailtest clear
        root.then(Commands.literal("clear").executes(ctx -> {
            CommandSourceStack src = ctx.getSource();
            if (src.getEntity() instanceof ServerPlayer player) {
                var storage = MailStorage.get(player.serverLevel());
                storage.clear(player.getUUID());
                src.sendSuccess(() -> Component.translatable("message.dovemail.test.cleared"), true);
                return 1;
            }
            return 0;
        }));

        // /dovemailtest create <count> [attachments]
        root.then(Commands.literal("create")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> createMails(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), false))
                        .then(Commands.argument("attachments", BoolArgumentType.bool())
                                .executes(ctx -> createMails(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "count"),
                                        BoolArgumentType.getBool(ctx, "attachments")
                                ))
                        )
                )
        );

        dispatcher.register(root);
    }

    private static int createMails(CommandSourceStack src, int count, boolean attachments) {
        if (!(src.getEntity() instanceof ServerPlayer player)) return 0;
        var storage = MailStorage.get(player.serverLevel());
        for (int i = 1; i <= count; i++) {
            Mail mail = new Mail()
                    .setSubject("测试邮件 #" + i)
                    .setBodyPlain("这是第 " + i + " 封测试邮件。")
                    .setSenderName("系统")
                    .setRead(false)
                    .setAttachmentsClaimed(false)
                    .setTimestamp(System.currentTimeMillis());
            if (attachments) {
                mail.addAttachment(new ItemStack(Items.DIAMOND, Math.min(3, i)))
                        .addAttachment(new ItemStack(Items.DIRT, Math.min(32, i * 2)));
            }
            storage.addOrUpdate(player.getUUID(), mail);
        }
        src.sendSuccess(() -> Component.translatable("message.dovemail.test.created", count), true);

        // 打开收件箱
        var summaries = storage.getSummaries(player.getUUID());
    var pkt = new top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload(summaries);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
        return 1;
    }
}
