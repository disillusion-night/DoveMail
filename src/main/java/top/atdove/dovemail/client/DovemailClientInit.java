package top.atdove.dovemail.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import top.atdove.dovemail.network.DovemailNetwork;

public final class DovemailClientInit {
    private static KeyMapping openMailboxKey;

    private DovemailClientInit() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        openMailboxKey = new KeyMapping(
                "key.dovemail.open_mailbox",
                InputConstants.KEY_M,
                "key.categories.dovemail"
        );
        event.register(openMailboxKey);
    }

    @SuppressWarnings("unused")
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openMailboxKey != null && openMailboxKey.consumeClick()) {
            DovemailNetwork.openMailbox();
        }
    }
}
