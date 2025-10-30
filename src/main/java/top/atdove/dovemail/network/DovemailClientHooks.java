package top.atdove.dovemail.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import top.atdove.dovemail.client.gui.MailDetailScreen;
import top.atdove.dovemail.client.gui.MailboxScreen;
import top.atdove.dovemail.mail.MailSummary;

import java.util.List;
import java.util.UUID;

public final class DovemailClientHooks {
    private DovemailClientHooks() {}

    public static void onMailDetailReceived(UUID mailId, List<ItemStack> attachments) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof MailDetailScreen screen && screen.getMailId().equals(mailId)) {
            screen.setAttachments(attachments);
        }
    }

    public static void onOpenMailbox(List<MailSummary> summaries) {
        var mc = Minecraft.getInstance();
        mc.setScreen(new MailboxScreen.Builder().fromMails(summaries).build());
    }

    public static void onMailSummaryReceived(MailSummary summary) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof MailboxScreen screen) {
            screen.updateOrAppendSummary(summary);
        }
    }

    public static void onOpenMailDetail(MailSummary summary, java.util.List<ItemStack> attachments) {
        var mc = Minecraft.getInstance();
        mc.setScreen(new MailDetailScreen(summary, attachments, s -> DovemailNetwork.claimAttachments(s.getId())));
    }

    public static void onUnreadHint(int count) {
        if (!top.atdove.dovemail.Config.isShowUnreadToastOnLogin()) {
            return;
        }
        var mc = Minecraft.getInstance();
        Component msg = Component.translatable("message.dovemail.inbox.unread_on_login", count);
        if (mc.gui != null) {
            mc.gui.setOverlayMessage(msg, false);
        }
    }
}
