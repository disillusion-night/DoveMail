package top.atdove.dovemail;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = Dovemail.MODID)
public class Config {
    private Config() {}
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Removed example settings; keep only DoveMail settings below.

    // Example list config removed to avoid deprecated APIs; keep file minimal.

    // DoveMail settings (COMMON)
    private static final ModConfigSpec.BooleanValue AUTO_OPEN_MAILBOX_ON_LOGIN_WHEN_UNREAD =
        BUILDER.comment("Automatically open mailbox UI when player logs in and has unread mails.")
            .define("autoOpenMailboxOnLoginWhenUnread", false);

    private static final ModConfigSpec.BooleanValue SHOW_UNREAD_TOAST_ON_LOGIN =
        BUILDER.comment("Show a client toast/banner when player logs in and has unread mails.")
            .define("showUnreadToastOnLogin", true);

    private static final ModConfigSpec.BooleanValue ENABLE_TEST_COMMANDS =
        BUILDER.comment("Enable DoveMail test commands (server). Default: false")
            .define("enableTestCommands", false);

    static final ModConfigSpec SPEC = BUILDER.build();
    // example items set removed
    private static boolean autoOpenMailboxOnLoginWhenUnread;
    private static boolean showUnreadToastOnLogin;
    private static boolean enableTestCommands;

    // no-op: validation helpers removed

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Dovemail settings
        autoOpenMailboxOnLoginWhenUnread = AUTO_OPEN_MAILBOX_ON_LOGIN_WHEN_UNREAD.get();
        showUnreadToastOnLogin = SHOW_UNREAD_TOAST_ON_LOGIN.get();
        enableTestCommands = ENABLE_TEST_COMMANDS.get();
    }

    // Accessors
    // No example accessors; only Dovemail toggles remain.
    // removed getItems()
    public static boolean isAutoOpenMailboxOnLoginWhenUnread() { return autoOpenMailboxOnLoginWhenUnread; }
    public static boolean isShowUnreadToastOnLogin() { return showUnreadToastOnLogin; }
    public static boolean isEnableTestCommands() { return enableTestCommands; }
}
