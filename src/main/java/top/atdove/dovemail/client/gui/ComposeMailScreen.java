package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import top.atdove.dovemail.network.DovemailNetwork;
import top.atdove.dovemail.client.gui.widgets.MultiLineTextArea;

public class ComposeMailScreen extends Screen {
    private final Screen parent;
    private EditBox toBox;
    private EditBox subjectBox;
    private MultiLineTextArea bodyArea;

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
    Button send = Button.builder(Component.translatable("screen.dovemail.compose.send"), btn -> doSend())
        .pos(panelLeft + 30, y)
                .size(100, 20)
                .build();
    Button cancel = Button.builder(Component.translatable("gui.cancel"), btn -> onClose())
        .pos(panelLeft + 170, y)
                .size(100, 20)
                .build();
        addRenderableWidget(send);
        addRenderableWidget(cancel);

        setInitialFocus(toBox);
    }

    private void doSend() {
        String to = toBox.getValue().trim();
        String subject = subjectBox.getValue().trim();
    String body = bodyArea.getValue().trim();
        if (!to.isEmpty() && !subject.isEmpty()) {
            DovemailNetwork.composeMail(to, subject, body);
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
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
        int y = panelTop + 34;
        g.drawString(font, Component.translatable("screen.dovemail.compose.to"), labelLeft, y, 0xA0A0A0, false);
        y += 26;
        g.drawString(font, Component.translatable("screen.dovemail.compose.subject"), labelLeft, y, 0xA0A0A0, false);
    y += 26;
    g.drawString(font, Component.translatable("screen.dovemail.compose.body"), labelLeft, y, 0xA0A0A0, false);

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
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
