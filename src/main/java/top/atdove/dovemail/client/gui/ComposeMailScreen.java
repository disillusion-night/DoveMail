package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import javax.annotation.Nonnull;
import top.atdove.dovemail.network.DovemailNetwork;
import top.atdove.dovemail.client.gui.widgets.MultiLineTextArea;

public class ComposeMailScreen extends Screen {
    private final Screen parent;
    private EditBox toBox;
    private EditBox subjectBox;
    private MultiLineTextArea bodyArea;
    // Inline info message area
    private Component infoMessage;
    private boolean sendAsSystem = false;
    private boolean sendAsAnnouncement = false;
    // initial values for restoring from attachments
    private String initialTo;
    private String initialSubject;
    private String initialBody;
    private Boolean initialSystem;
    private Boolean initialAnnouncement;

    public ComposeMailScreen(Screen parent) {
        super(Component.translatable("screen.dovemail.compose"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        final int centerX = this.width / 2;
        final int panelWidth = 300;
        final int panelLeft = centerX - panelWidth / 2;
        final int vShiftUp = 12; // 上移以保证视觉平衡

        // 根据配置行数计算正文区域高度（行高 + 4px 内边距以匹配文本区域可见行数计算）
        int lines = Math.max(1, top.atdove.dovemail.Config.getComposeBodyLines());
        int lineAdvance = this.font.lineHeight; // bodyArea 将使用 lineSpacing(0)
        int areaHeight = lines * lineAdvance + 4;

        // 计算面板内布局：顶部留白（含标题）、两行输入区域高度、提示栏高度
        int panelInnerTopMargin = 36; // 标题与输入之间的视觉缓冲
        int twoRowsHeight = 26 + 26;  // 两行 EditBox（各 20 高 + 6 间距）
        int infoBarHeight = this.font.lineHeight + 6; // 预留一行提示
        int bottomPadding = 12;
        int panelInnerHeight = panelInnerTopMargin + twoRowsHeight + areaHeight + infoBarHeight + bottomPadding;

        int centerY = this.height / 2;
        int panelTop = Math.max(12, centerY - panelInnerHeight / 2 - vShiftUp);
        int panelBottom = panelTop + panelInnerHeight;

        int y = panelTop + panelInnerTopMargin;

        toBox = new EditBox(this.font, panelLeft + 70, y, 210, 20,
                Component.translatable("screen.dovemail.compose.to"));
        toBox.setMaxLength(64);
        addRenderableWidget(toBox);
        y += 26;

        subjectBox = new EditBox(this.font, panelLeft + 70, y, 210, 20,
                Component.translatable("screen.dovemail.compose.subject"));
        subjectBox.setMaxLength(120);
        addRenderableWidget(subjectBox);
        y += 26;

    bodyArea = new MultiLineTextArea(panelLeft + 12, y, 268, areaHeight, this.font,
                Component.translatable("screen.dovemail.compose.body"));
        // 使用紧凑行距，并通过软换行指示符提示折行位置
        bodyArea.setLineSpacing(0);
        addRenderableWidget(bodyArea);

    // 功能按钮放在面板下方，避免与面板重叠
    int underY = panelBottom + 8;
    var attach = new top.atdove.dovemail.client.gui.widgets.SimpleTextButton(panelLeft + 30, underY, 100, 20,
                Component.translatable("button.dovemail.add_attachments"), b -> {
                    // save current compose state then open attachments
                    top.atdove.dovemail.client.ComposeState.save(this.parent, toBox.getValue(), subjectBox.getValue(),
                            bodyArea.getValue(), sendAsSystem, sendAsAnnouncement);
                    top.atdove.dovemail.network.DovemailNetwork.openAttachments();
                });
    var send = new top.atdove.dovemail.client.gui.widgets.SimpleTextButton(panelLeft + 140, underY, 100, 20,
                Component.translatable("screen.dovemail.compose.send"), b -> doSend());
    var cancel = new top.atdove.dovemail.client.gui.widgets.SimpleTextButton(panelLeft + 250, underY, 100, 20,
                Component.translatable("gui.cancel"), b -> onClose());
    var settings = new top.atdove.dovemail.client.gui.widgets.SimpleTextButton(panelLeft - 80, underY, 70, 20,
                Component.translatable("button.dovemail.settings"), b -> openSettings());
        addRenderableWidget(attach);
        addRenderableWidget(send);
        addRenderableWidget(cancel);
        addRenderableWidget(settings);

        setInitialFocus(toBox);

        // apply initial values if present
        if (initialTo != null)
            toBox.setValue(initialTo);
        if (initialSubject != null)
            subjectBox.setValue(initialSubject);
        if (initialBody != null)
            bodyArea.setValue(initialBody);
        if (initialSystem != null)
            this.sendAsSystem = initialSystem;
        if (initialAnnouncement != null)
            this.sendAsAnnouncement = initialAnnouncement;
    }

    private void doSend() {
        String to = toBox.getValue().trim();
        String subject = subjectBox.getValue().trim();
        String body = bodyArea.getValue().trim();
        if (to.isEmpty()) {
            // 未填写收件人时给出提醒
            showInfoMessage(Component.translatable("message.dovemail.compose.recipient_required"));
            return;
        }
        if (subject.isEmpty()) {
            // 未填写主题时给出提醒
            showInfoMessage(Component.translatable("message.dovemail.compose.subject_required"));
            return;
        }
        if (!to.isEmpty()) {
            DovemailNetwork.composeMail(to, subject, body, sendAsSystem, sendAsAnnouncement && sendAsSystem);
            // 不要立即关闭：等待服务器结果。若有错误，DovemailClientHooks 会将提示路由到本窗口底部信息栏。
        }
    }

    private void openSettings() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ComposeSettingsScreen(this, value -> this.sendAsSystem = value,
                    value -> this.sendAsAnnouncement = value, sendAsSystem, sendAsAnnouncement));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);
    }

    // called by ComposeState to pre-seed the UI
    public void applyInitial(String to, String subject, String body, boolean asSystem, boolean asAnnouncement) {
        this.initialTo = to;
        this.initialSubject = subject;
        this.initialBody = body;
        this.initialSystem = asSystem;
        this.initialAnnouncement = asAnnouncement;
    }

    @Override
    public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        final int centerX = this.width / 2;
        final int panelWidth = 300;
        final int panelLeft = centerX - panelWidth / 2;
        final int vShiftUp = 12;

        int lines = Math.max(1, top.atdove.dovemail.Config.getComposeBodyLines());
        int lineAdvance = this.font.lineHeight;
        int areaHeight = lines * lineAdvance + 4;
        int panelInnerTopMargin = 36;
        int twoRowsHeight = 26 + 26;
        int infoBarHeight = this.font.lineHeight + 6;
        int bottomPadding = 12;
        int panelInnerHeight = panelInnerTopMargin + twoRowsHeight + areaHeight + infoBarHeight + bottomPadding;
        int centerY = this.height / 2;
        int panelTop = Math.max(12, centerY - panelInnerHeight / 2 - vShiftUp);
        int panelBottom = panelTop + panelInnerHeight;
        // 面板背景与边框
        g.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xAA000000);
        g.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, 0x33FFFFFF);
        g.fill(panelLeft, panelBottom - 1, panelLeft + panelWidth, panelBottom, 0x33000000);
        g.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0x33FFFFFF);
        g.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelBottom, 0x33000000);

        g.drawCenteredString(font, this.title, centerX, panelTop + 8, 0xFFFFFF);
        int labelLeft = panelLeft + 12;
        // Align labels vertically with actual widgets
        int toLabelY = toBox.getY() + (toBox.getHeight() - font.lineHeight) / 2;
        g.drawString(font, Component.translatable("screen.dovemail.compose.to"), labelLeft, toLabelY, 0xA0A0A0, false);
        int subjectLabelY = subjectBox.getY() + (subjectBox.getHeight() - font.lineHeight) / 2;
        g.drawString(font, Component.translatable("screen.dovemail.compose.subject"), labelLeft, subjectLabelY,
                0xA0A0A0,
                false);

        // 占位提示（当输入框为空时）
        if (toBox.getValue().isEmpty()) {
            g.drawString(font, Component.literal("e.g. PlayerName"), toBox.getX() + 4, toBox.getY() + 6, 0x555555,
                    false);
        }
        if (subjectBox.getValue().isEmpty()) {
            g.drawString(font, Component.literal("e.g. 交易邀请"), subjectBox.getX() + 4, subjectBox.getY() + 6, 0x555555,
                    false);
        }
        if (bodyArea.getValue().isEmpty()) {
            g.drawString(font, Component.literal("e.g. 今晚 8 点村口见"), bodyArea.getX() + 6, bodyArea.getY() + 6, 0x555555,
                    false);
        }

        // 面板内的状态提示栏（占用预留高度区域）
        if (infoMessage != null) {
            int innerPad = 4;
            int barTop = panelBottom - infoBarHeight + innerPad;
            g.fill(panelLeft + 1, barTop, panelLeft + panelWidth - 1, barTop + infoBarHeight - innerPad * 2, 0x44000000);
            int textY = barTop + (infoBarHeight - innerPad * 2 - this.font.lineHeight) / 2;
            g.drawCenteredString(font, infoMessage, centerX, textY, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    // Called by client hook when server sends an inline alert
    public void showInfoMessage(Component message) {
        this.infoMessage = message;
    }
}
