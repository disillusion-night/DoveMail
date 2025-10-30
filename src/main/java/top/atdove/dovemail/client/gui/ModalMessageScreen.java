package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import top.atdove.dovemail.client.gui.widgets.SimpleTextButton;

import javax.annotation.Nonnull;

public class ModalMessageScreen extends Screen {
    private final Screen parent;
    private final Component message;

    public ModalMessageScreen(Screen parent, Component message) {
        super(Component.empty());
        this.parent = parent;
        this.message = message != null ? message : Component.empty();
    }

    @Override
    protected void init() {
        int w = 200;
        int h = 80;
        int left = (this.width - w) / 2;
        int top = (this.height - h) / 2;
        var ok = new SimpleTextButton(left + w/2 - 30, top + h - 26, 60, 20, Component.translatable("gui.ok"), b -> this.onClose());
        addRenderableWidget(ok);
    }

    @Override
    public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        int w = 220;
        int h = 90;
        int left = (this.width - w) / 2;
        int top = (this.height - h) / 2;
        // panel
        g.fill(left, top, left + w, top + h, 0xCC000000);
        g.fill(left, top, left + w, top + 1, 0x22FFFFFF);
        g.fill(left, top + h - 1, left + w, top + h, 0x22000000);
        // text wrap
        var lines = this.font.split(this.message, w - 20);
        int y = top + 14;
        for (var line : lines) {
            g.drawString(this.font, line, left + 10, y, 0xFFFFFF, false);
            y += this.font.lineHeight + 2;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
}
