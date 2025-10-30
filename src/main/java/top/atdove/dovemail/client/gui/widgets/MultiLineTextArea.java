package top.atdove.dovemail.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class MultiLineTextArea extends AbstractWidget {
    private final net.minecraft.client.gui.Font font;
    private static final int LINE_SPACING = 2;
    private final StringBuilder value = new StringBuilder();
    private int cursor = 0; // cursor index in value
    private int desiredColumn = -1; // track target column for up/down
    private int firstVisibleLine = 0;

    public MultiLineTextArea(int x, int y, int width, int height, net.minecraft.client.gui.Font font, Component title) {
        super(x, y, width, height, title);
        this.font = font;
    }

    public String getValue() {
        return value.toString();
    }

    public void setValue(String text) {
        value.setLength(0);
        if (text != null) value.append(text);
        cursor = Math.min(cursor, value.length());
        ensureCursorVisible();
    }

    public void append(String text) {
        if (text == null || text.isEmpty()) return;
        value.insert(cursor, text);
        cursor += text.length();
        ensureCursorVisible();
    }

    private int lineCount() {
        int cnt = 1;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == '\n') cnt++;
        return cnt;
    }

    private int getLineOfIndex(int idx) {
        int line = 0;
        for (int i = 0; i < Math.min(idx, value.length()); i++) if (value.charAt(i) == '\n') line++;
        return line;
    }

    private int getColumnOfIndex(int idx) {
        int lastNl = value.lastIndexOf("\n", Math.max(0, Math.min(idx, value.length()) - 1));
        return idx - (lastNl + 1);
    }

    private int getIndexOfLineColumn(int line, int column) {
        int curLine = 0;
        int i = 0;
        while (i < value.length() && curLine < line) {
            if (value.charAt(i) == '\n') curLine++;
            i++;
        }
        // now at start of desired line
        int start = i;
        int col = 0;
        while (i < value.length() && value.charAt(i) != '\n' && col < column) {
            i++; col++;
        }
        return start + col;
    }

    private void ensureCursorVisible() {
        int visibleLines = Math.max(1, (this.height - 4) / (font.lineHeight + LINE_SPACING));
        int line = getLineOfIndex(cursor);
        if (line < firstVisibleLine) firstVisibleLine = line;
        if (line >= firstVisibleLine + visibleLines) firstVisibleLine = line - visibleLines + 1;
        if (firstVisibleLine < 0) firstVisibleLine = 0;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // background
        int bg = this.isFocused() ? 0xAA000000 : 0x88000000;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        // border
        g.fill(getX(), getY(), getX() + width, getY() + 1, 0x33FFFFFF);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0x33000000);
        g.fill(getX(), getY(), getX() + 1, getY() + height, 0x33FFFFFF);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0x33000000);

        // draw text lines
        String[] lines = getValue().split("\n", -1);
        int visibleLines = Math.max(1, (this.height - 4) / (font.lineHeight + LINE_SPACING));
        int y = getY() + 2;
        for (int i = 0; i < visibleLines; i++) {
            int lineIdx = firstVisibleLine + i;
            if (lineIdx >= lines.length) break;
            String line = lines[lineIdx];
            g.drawString(font, line, getX() + 4, y, 0xEEEEEE, false);
            y += font.lineHeight + LINE_SPACING;
        }

        // caret
        if (this.isFocused() && Minecraft.getInstance().gui.getGuiTicks() / 6 % 2 == 0) {
            int curLine = getLineOfIndex(cursor);
            int curCol = getColumnOfIndex(cursor);
            if (curLine >= firstVisibleLine && curLine < firstVisibleLine + visibleLines) {
                int cx = getX() + 4 + font.width(lines.length > 0 ? safeSubstring(lines[curLine], 0, curCol) : "");
                int cy = getY() + 2 + (curLine - firstVisibleLine) * (font.lineHeight + LINE_SPACING);
                g.fill(cx, cy, cx + 1, cy + font.lineHeight, 0xFFFFFFFF);
            }
        }
    }

    private static String safeSubstring(String s, int start, int end) {
        if (s == null) return "";
        start = Mth.clamp(start, 0, s.length());
        end = Mth.clamp(end, start, s.length());
        return s.substring(start, end);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        boolean inside = mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
        if (inside && button == 0) {
            this.setFocused(true);
            // place cursor by line/column where clicked (approximate)
            int line = (int) Math.floor((mouseY - getY() - 2) / (font.lineHeight + LINE_SPACING)) + firstVisibleLine;
            String[] lines = getValue().split("\n", -1);
            line = Mth.clamp(line, 0, lines.length - 1);
            int col = 0;
            int relX = (int) (mouseX - getX() - 4);
            if (relX > 0) {
                String l = lines[line];
                for (int i = 0; i <= l.length(); i++) {
                    int w = font.width(safeSubstring(l, 0, i));
                    if (w >= relX) { col = i; break; }
                    col = i;
                }
            }
            cursor = getIndexOfLineColumn(line, col);
            desiredColumn = col;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.isFocused() || !this.active) return false;
        if (codePoint == '\r') codePoint = '\n';
        if (codePoint == '\n') {
            append("\n");
            desiredColumn = -1;
            return true;
        }
        if (codePoint >= 32 && codePoint != 127) { // printable
            append(String.valueOf(codePoint));
            desiredColumn = -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused() || !this.active) return false;
        switch (keyCode) {
            case 257, 335: // Enter
                append("\n");
                return true;
            case 259: // Backspace
                if (cursor > 0) {
                    value.deleteCharAt(cursor - 1);
                    cursor--;
                    ensureCursorVisible();
                }
                return true;
            case 261: // Delete
                if (cursor < value.length()) {
                    value.deleteCharAt(cursor);
                    ensureCursorVisible();
                }
                return true;
            case 263: // Left
                if (cursor > 0) cursor--;
                desiredColumn = -1;
                ensureCursorVisible();
                return true;
            case 262: // Right
                if (cursor < value.length()) cursor++;
                desiredColumn = -1;
                ensureCursorVisible();
                return true;
            case 265: // Up
                handleArrowUpDown(true);
                return true;
            case 264: // Down
                handleArrowUpDown(false);
                return true;
            case 268: // Home
                handleHomeEnd(true);
                return true;
            case 269: // End
                handleHomeEnd(false);
                return true;
            default:
                return false;
        }
    }

    private void handleArrowUpDown(boolean up) {
        int line = getLineOfIndex(cursor);
        int col = getColumnOfIndex(cursor);
        if (desiredColumn >= 0) col = desiredColumn; else desiredColumn = col;
        int total = lineCount();
        if (up) {
            if (line > 0) {
                line -= 1;
                cursor = getIndexOfLineColumn(line, col);
                ensureCursorVisible();
            }
        } else {
            if (line < total - 1) {
                line += 1;
                cursor = getIndexOfLineColumn(line, col);
                ensureCursorVisible();
            }
        }
    }

    private void handleHomeEnd(boolean home) {
        int line = getLineOfIndex(cursor);
        if (home) {
            int col = 0;
            cursor = getIndexOfLineColumn(line, col);
            desiredColumn = col;
        } else {
            String[] lines = getValue().split("\n", -1);
            int len = lines.length > 0 ? lines[Math.min(line, lines.length - 1)].length() : 0;
            cursor = getIndexOfLineColumn(line, len);
            desiredColumn = len;
        }
        ensureCursorVisible();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }
}
