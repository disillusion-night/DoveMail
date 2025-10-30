package top.atdove.dovemail.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import top.atdove.dovemail.client.gui.MailDetailScreen;

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
}
