package top.atdove.dovemail.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.atdove.dovemail.saveddata.MailStorage;

public final class DovemailCommands {
    private DovemailCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> mailbox = Commands.literal("mailbox").requires(src -> src.hasPermission(0))
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    if (src.getEntity() instanceof ServerPlayer player) {
                        var storage = MailStorage.get(player.serverLevel());
                        var summaries = storage.getSummaries(player.getUUID());
                        var pkt = new top.atdove.dovemail.network.payload.client.ClientboundOpenMailboxPayload(summaries);
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
                        return 1;
                    }
                    return 0;
                });
        dispatcher.register(mailbox);
    }
}
