package top.atdove.dovemail.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * 半透明背景 + 文本样式的简洁按钮。
 */
public class SimpleTextButton extends AbstractButton {
    private final Consumer<SimpleTextButton> onPress;

    public SimpleTextButton(int x, int y, int width, int height, Component message, Consumer<SimpleTextButton> onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        if (onPress != null) onPress.accept(this);
    }

    @Override
    protected void renderWidget(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int bgColor;
        if (!this.active) {
            bgColor = 0x33000000; // disabled
        } else if (this.isHovered) {
            bgColor = 0x88000000; // hovered stronger
        } else {
            bgColor = 0x66000000; // normal
        }
        // background
        g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);
        // subtle top/bottom lines
        g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, 0x22FFFFFF);
        g.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x22000000);

        var font = Minecraft.getInstance().font;
        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        int tx = this.getX() + (this.getWidth() - font.width(this.getMessage())) / 2;
        int ty = this.getY() + (this.getHeight() - font.lineHeight) / 2;
        g.drawString(font, this.getMessage(), tx, ty, color, false);
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
