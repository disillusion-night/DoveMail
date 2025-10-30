package top.atdove.dovemail.mail;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.atdove.dovemail.saveddata.MailStorage;

/**
 * 玩家相关的邮件事件监听。
 */
public final class MailEvents {

    private MailEvents() {
        // 禁止实例化
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        MailStorage storage = MailStorage.get(level);
        storage.ensureInbox(serverPlayer.getUUID());
    }
}
