package top.atdove.dovemail.client;

import net.minecraft.client.gui.screens.Screen;
import top.atdove.dovemail.client.gui.ComposeMailScreen;

/**
 * 临时保存“写信界面”的输入状态，供附件界面关闭后恢复。
 */
public final class ComposeState {
    private static Screen parent;
    private static String to;
    private static String subject;
    private static String body;
    private static boolean sendAsSystem;
    private static boolean sendAsAnnouncement;
    private static boolean has;

    private ComposeState() {}

    public static void save(Screen parentScreen, String toValue, String subjectValue, String bodyValue,
                             boolean asSystem, boolean asAnnouncement) {
        parent = parentScreen;
        to = toValue != null ? toValue : "";
        subject = subjectValue != null ? subjectValue : "";
        body = bodyValue != null ? bodyValue : "";
        sendAsSystem = asSystem;
        sendAsAnnouncement = asAnnouncement;
        has = true;
    }

    public static boolean hasSnapshot() {
        return has;
    }

    public static ComposeMailScreen restoreAndClear() {
        if (!has) return null;
        ComposeMailScreen screen = new ComposeMailScreen(parent);
        screen.applyInitial(to, subject, body, sendAsSystem, sendAsAnnouncement);
        clear();
        return screen;
    }

    public static void clear() {
        parent = null;
        to = subject = body = null;
        sendAsSystem = false;
        sendAsAnnouncement = false;
        has = false;
    }
}
