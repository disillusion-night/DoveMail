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
        final int x1 = this.getX();
        final int y1 = this.getY();
        final int x2 = x1 + this.getWidth();
        final int y2 = y1 + this.getHeight();

        boolean hoveredOrFocused = this.isHoveredOrFocused();

        // background (slightly stronger on hover/focus)
        int bgColor;
        if (!this.active) {
            bgColor = 0x33000000; // disabled
        } else if (hoveredOrFocused) {
            bgColor = 0x88000000; // hovered stronger
        } else {
            bgColor = 0x66000000; // normal
        }
        g.fill(x1, y1, x2, y2, bgColor);

        // subtle top/bottom separators
        g.fill(x1, y1, x2, y1 + 1, 0x22FFFFFF);
        g.fill(x1, y2 - 1, x2, y2, 0x22000000);

        // hover highlight border: bright outline when hovered/focused, faint when idle
        int borderColor;
        if (!this.active) {
            borderColor = 0x22000000; // almost none when disabled
        } else if (hoveredOrFocused) {
            borderColor = 0xCCFFFFFF; // strong white highlight
        } else {
            borderColor = 0x33FFFFFF; // subtle idle border
        }
        // draw 1px outline
        g.fill(x1, y1, x2, y1 + 1, borderColor); // top
        g.fill(x1, y2 - 1, x2, y2, borderColor); // bottom
        g.fill(x1, y1, x1 + 1, y2, borderColor); // left
        g.fill(x2 - 1, y1, x2, y2, borderColor); // right

        var font = Minecraft.getInstance().font;
        // text color: slightly warmer on hover/focus
        int textColor;
        if (!this.active) {
            textColor = 0xA0A0A0;
        } else if (hoveredOrFocused) {
            textColor = 0xFFFFA0; // minecraft-style hover yellow
        } else {
            textColor = 0xFFFFFF;
        }
        int tx = x1 + (this.getWidth() - font.width(this.getMessage())) / 2;
        int ty = y1 + (this.getHeight() - font.lineHeight) / 2;
        g.drawString(font, this.getMessage(), tx, ty, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
