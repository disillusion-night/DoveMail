package top.atdove.dovemail.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import top.atdove.dovemail.network.DovemailNetwork;
import top.atdove.dovemail.client.gui.MailboxScreen;
import top.atdove.dovemail.network.DovemailClientHooks;

public final class DovemailClientInit {
    private static KeyMapping openMailboxKey;

    private DovemailClientInit() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
    openMailboxKey = new KeyMapping(
        "key.dovemail.open_mailbox",
        InputConstants.Type.KEYSYM,
        -1,
        "key.categories.dovemail"
    );
        event.register(openMailboxKey);
    }

    @SuppressWarnings("unused")
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openMailboxKey != null && openMailboxKey.consumeClick()) {
            DovemailNetwork.openMailbox();
        }
        // Trigger delayed mailbox refresh if scheduled and currently in mailbox
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (DovemailClientHooks.tryConsumeDueMailboxRefresh() && mc.screen instanceof MailboxScreen) {
            DovemailNetwork.openMailbox();
        }
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(top.atdove.dovemail.init.ModMenus.ATTACHMENTS.get(), top.atdove.dovemail.menu.AttachmentsScreen::new);
    }
}
