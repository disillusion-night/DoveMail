package top.atdove.dovemail.network;

import net.minecraft.client.Minecraft;
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
        if (mc.screen instanceof MailDetailScreen screen) {
            if (screen.getMailId().equals(mailId)) {
                screen.setAttachments(attachments);
            }
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
}
