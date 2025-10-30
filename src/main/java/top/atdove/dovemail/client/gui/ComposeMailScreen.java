package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import top.atdove.dovemail.network.DovemailNetwork;

public class ComposeMailScreen extends Screen {
    private final Screen parent;
    private EditBox toBox;
    private EditBox subjectBox;
    private EditBox bodyBox;

    public ComposeMailScreen(Screen parent) {
        super(Component.translatable("screen.dovemail.compose"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 50;

        toBox = new EditBox(this.font, centerX - 120, y, 240, 20, Component.translatable("screen.dovemail.compose.to"));
        toBox.setMaxLength(64);
        addRenderableWidget(toBox);
        y += 26;

        subjectBox = new EditBox(this.font, centerX - 120, y, 240, 20, Component.translatable("screen.dovemail.compose.subject"));
        subjectBox.setMaxLength(120);
        addRenderableWidget(subjectBox);
        y += 26;

        bodyBox = new EditBox(this.font, centerX - 120, y, 240, 20, Component.translatable("screen.dovemail.compose.body"));
        bodyBox.setMaxLength(512);
        addRenderableWidget(bodyBox);

        y += 36;
        Button send = Button.builder(Component.translatable("screen.dovemail.compose.send"), btn -> doSend())
                .pos(centerX - 120, y)
                .size(100, 20)
                .build();
        Button cancel = Button.builder(Component.translatable("gui.cancel"), btn -> onClose())
                .pos(centerX + 20, y)
                .size(100, 20)
                .build();
        addRenderableWidget(send);
        addRenderableWidget(cancel);

        setInitialFocus(toBox);
    }

    private void doSend() {
        String to = toBox.getValue().trim();
        String subject = subjectBox.getValue().trim();
        String body = bodyBox.getValue().trim();
        if (!to.isEmpty() && !subject.isEmpty()) {
            DovemailNetwork.composeMail(to, subject, body);
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, this.title, this.width / 2, this.height / 2 - 80, 0xFFFFFF);
        int left = this.width / 2 - 120;
        int y = this.height / 2 - 62;
        g.drawString(font, Component.translatable("screen.dovemail.compose.to"), left, y, 0xA0A0A0, false);
        y += 26;
        g.drawString(font, Component.translatable("screen.dovemail.compose.subject"), left, y, 0xA0A0A0, false);
        y += 26;
        g.drawString(font, Component.translatable("screen.dovemail.compose.body"), left, y, 0xA0A0A0, false);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
