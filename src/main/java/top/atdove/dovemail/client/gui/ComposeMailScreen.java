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
    int centerX = this.width / 2;
    int panelWidth = 300;
    int panelLeft = centerX - panelWidth / 2;
    int y = this.height / 2 - 70;

    toBox = new EditBox(this.font, panelLeft + 70, y, 210, 20, Component.translatable("screen.dovemail.compose.to"));
        toBox.setMaxLength(64);
        addRenderableWidget(toBox);
        y += 26;

    subjectBox = new EditBox(this.font, panelLeft + 70, y, 210, 20, Component.translatable("screen.dovemail.compose.subject"));
        subjectBox.setMaxLength(120);
        addRenderableWidget(subjectBox);
        y += 26;

    int areaHeight = 80;
    bodyArea = new MultiLineTextArea(panelLeft + 12, y, 268, areaHeight, this.font, Component.translatable("screen.dovemail.compose.body"));
    addRenderableWidget(bodyArea);

    y += areaHeight + 16;
    var attach = new top.atdove.dovemail.client.gui.widgets.SimpleTextButton(panelLeft + 30, y, 100, 20,
        Component.translatable("button.dovemail.add_attachments"), b -> {
    // save current compose state then open attachments
    top.atdove.dovemail.client.ComposeState.save(this.parent, toBox.getValue(), subjectBox.getValue(), bodyArea.getValue(), sendAsSystem, sendAsAnnouncement);
    top.atdove.dovemail.network.DovemailNetwork.openAttachments();
    });
    var send = new top.atdove.dovemail.client.gui.widgets.SimpleTextButton(panelLeft + 140, y, 100, 20,
        Component.translatable("screen.dovemail.compose.send"), b -> doSend());
    var cancel = new top.atdove.dovemail.client.gui.widgets.SimpleTextButton(panelLeft + 250, y, 100, 20,
        Component.translatable("gui.cancel"), b -> onClose());
    var settings = new top.atdove.dovemail.client.gui.widgets.SimpleTextButton(panelLeft - 80, y, 70, 20,
        Component.translatable("button.dovemail.settings"), b -> openSettings());
    addRenderableWidget(attach);
    addRenderableWidget(send);
    addRenderableWidget(cancel);
    addRenderableWidget(settings);

        setInitialFocus(toBox);

        // apply initial values if present
        if (initialTo != null) toBox.setValue(initialTo);
        if (initialSubject != null) subjectBox.setValue(initialSubject);
        if (initialBody != null) bodyArea.setValue(initialBody);
        if (initialSystem != null) this.sendAsSystem = initialSystem;
        if (initialAnnouncement != null) this.sendAsAnnouncement = initialAnnouncement;
    }

    private void doSend() {
        String to = toBox.getValue().trim();
        String subject = subjectBox.getValue().trim();
    String body = bodyArea.getValue().trim();
        if (!to.isEmpty() && !subject.isEmpty()) {
            DovemailNetwork.composeMail(to, subject, body, sendAsSystem, sendAsAnnouncement && sendAsSystem);
            onClose();
        }
    }

    private void openSettings() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ComposeSettingsScreen(this, value -> this.sendAsSystem = value, value -> this.sendAsAnnouncement = value, sendAsSystem, sendAsAnnouncement));
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

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

        int centerX = this.width / 2;
        int panelWidth = 300;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = this.height / 2 - 90;
    int panelBottom = this.height / 2 + 110;
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
        g.drawString(font, Component.translatable("screen.dovemail.compose.subject"), labelLeft, subjectLabelY, 0xA0A0A0, false);
    // 去掉“正文”标签显示

        // 占位提示（当输入框为空时）
        if (toBox.getValue().isEmpty()) {
            g.drawString(font, Component.literal("e.g. PlayerName"), toBox.getX() + 4, toBox.getY() + 6, 0x555555, false);
        }
        if (subjectBox.getValue().isEmpty()) {
            g.drawString(font, Component.literal("e.g. 交易邀请"), subjectBox.getX() + 4, subjectBox.getY() + 6, 0x555555, false);
        }
        if (bodyArea.getValue().isEmpty()) {
            g.drawString(font, Component.literal("e.g. 今晚 8 点村口见"), bodyArea.getX() + 6, bodyArea.getY() + 6, 0x555555, false);
        }

        // bottom info message line inside panel
        if (infoMessage != null) {
            int barHeight = 16;
            int barTop = panelBottom - barHeight - 2;
            g.fill(panelLeft + 1, barTop, panelLeft + panelWidth - 1, barTop + barHeight, 0x44000000);
            g.drawCenteredString(font, infoMessage, centerX, barTop + 4, 0xFFFFFF);
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
