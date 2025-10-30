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
    private int selectionStart = -1; // inclusive, -1 means no selection
    private int selectionEnd = -1;   // exclusive

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

        // draw text lines & selection background
        String[] lines = getValue().split("\n", -1);
        int[] lineStartIdx = computeLineStartIdx(lines);
        int visibleLines = Math.max(1, (this.height - 4) / (font.lineHeight + LINE_SPACING));
        int y = getY() + 2;
        for (int i = 0; i < visibleLines; i++) {
            int lineIdx = firstVisibleLine + i;
            if (lineIdx >= lines.length) break;
            String line = lines[lineIdx];
            highlightSelectionIfAny(g, y, line, lineStartIdx[lineIdx]);
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
            // reset selection to a caret anchor
            selectionStart = cursor;
            selectionEnd = cursor;
            desiredColumn = col;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.active || !this.visible || !this.isFocused()) return false;
        if (button != 0) return false;
        int line = (int) Math.floor((mouseY - getY() - 2) / (font.lineHeight + LINE_SPACING)) + firstVisibleLine;
        String[] lines = getValue().split("\n", -1);
        line = Mth.clamp(line, 0, Math.max(0, lines.length - 1));
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
        int idx = getIndexOfLineColumn(line, col);
        selectionEnd = idx;
        cursor = idx;
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int total = lineCount();
        int visible = Math.max(1, (this.height - 4) / (font.lineHeight + LINE_SPACING));
        if (total > visible) {
            int step = deltaY > 0 ? -1 : 1;
            firstVisibleLine = Mth.clamp(firstVisibleLine + step, 0, Math.max(0, total - visible));
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.isFocused() || !this.active) return false;
        if (codePoint == '\r') codePoint = '\n';
        if (codePoint == '\n') {
            replaceSelectionIfAny();
            append("\n");
            desiredColumn = -1;
            return true;
        }
        if (codePoint >= 32 && codePoint != 127) { // printable
            replaceSelectionIfAny();
            append(String.valueOf(codePoint));
            desiredColumn = -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused() || !this.active) return false;
        boolean shift = (modifiers & 1) != 0; // GLFW_MOD_SHIFT
        boolean ctrlOrMeta = (modifiers & 2) != 0 || (modifiers & 8) != 0; // CTRL or SUPER
        if (handleShortcut(keyCode, ctrlOrMeta)) return true;
        switch (keyCode) {
            case 257, 335: // Enter
                replaceSelectionIfAny();
                append("\n");
                return true;
            case 259: // Backspace
                handleBackspace();
                return true;
            case 261: // Delete
                handleDelete();
                return true;
            case 263: // Left
                moveLeft(shift);
                return true;
            case 262: // Right
                moveRight(shift);
                return true;
            case 265: // Up
                moveUp(shift);
                return true;
            case 264: // Down
                moveDown(shift);
                return true;
            case 268: // Home
                handleHomeEnd(true);
                if (!shift) clearSelection();
                return true;
            case 269: // End
                handleHomeEnd(false);
                if (!shift) clearSelection();
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

    // Selection & clipboard helpers
    private boolean hasSelection() { return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd; }
    private void clearSelection() { selectionStart = -1; selectionEnd = -1; }
    private void startOrExtendSelection() { if (!hasSelection()) { selectionStart = cursor; selectionEnd = cursor; } }
    private void selectAll() { selectionStart = 0; selectionEnd = value.length(); cursor = selectionEnd; ensureCursorVisible(); }
    private void deleteSelection() {
        int a = Math.min(selectionStart, selectionEnd);
        int b = Math.max(selectionStart, selectionEnd);
        value.delete(a, b);
        cursor = a;
        clearSelection();
        ensureCursorVisible();
    }
    private void replaceSelectionIfAny() { if (hasSelection()) deleteSelection(); }
    private void copySelection() {
        if (!hasSelection()) return;
        int a = Math.min(selectionStart, selectionEnd);
        int b = Math.max(selectionStart, selectionEnd);
        String s = value.substring(a, b);
        Minecraft.getInstance().keyboardHandler.setClipboard(s);
    }
    private void cutSelection() { if (hasSelection()) { copySelection(); deleteSelection(); } }
    private void pasteClipboard() {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        replaceSelectionIfAny();
        append(clip);
    }

    // Helpers extracted to reduce complexity
    private int[] computeLineStartIdx(String[] lines) {
        int[] arr = new int[lines.length];
        int acc = 0;
        for (int i = 0; i < lines.length; i++) {
            arr[i] = acc;
            acc += lines[i].length() + (i < lines.length - 1 ? 1 : 0);
        }
        return arr;
    }

    private void highlightSelectionIfAny(GuiGraphics g, int y, String line, int lineStart) {
        if (!hasSelection()) return;
        int selA = Math.min(selectionStart, selectionEnd);
        int selB = Math.max(selectionStart, selectionEnd);
        int lineEnd = lineStart + line.length();
        int a = Math.max(selA, lineStart);
        int b = Math.min(selB, lineEnd);
        if (a >= b) return;
        int aCol = a - lineStart;
        int bCol = b - lineStart;
        int ax = getX() + 4 + font.width(safeSubstring(line, 0, aCol));
        int bx = getX() + 4 + font.width(safeSubstring(line, 0, bCol));
        g.fill(ax, y - 1, bx, y + font.lineHeight + 1, 0x554A90E2);
    }

    private boolean handleShortcut(int keyCode, boolean ctrlOrMeta) {
        if (!ctrlOrMeta) return false;
        // A C V X
        if (keyCode == 65) { selectAll(); return true; }
        if (keyCode == 67) { copySelection(); return true; }
        if (keyCode == 86) { pasteClipboard(); return true; }
        if (keyCode == 88) { cutSelection(); return true; }
        return false;
    }

    private void handleBackspace() {
        if (hasSelection()) { deleteSelection(); return; }
        if (cursor > 0) {
            value.deleteCharAt(cursor - 1);
            cursor--;
            ensureCursorVisible();
        }
    }

    private void handleDelete() {
        if (hasSelection()) { deleteSelection(); return; }
        if (cursor < value.length()) {
            value.deleteCharAt(cursor);
            ensureCursorVisible();
        }
    }

    private void moveLeft(boolean shift) {
        if (shift) startOrExtendSelection();
        if (cursor > 0) cursor--;
        if (!shift) clearSelection();
        desiredColumn = -1;
        ensureCursorVisible();
    }

    private void moveRight(boolean shift) {
        if (shift) startOrExtendSelection();
        if (cursor < value.length()) cursor++;
        if (!shift) clearSelection();
        desiredColumn = -1;
        ensureCursorVisible();
    }

    private void moveUp(boolean shift) {
        if (shift) startOrExtendSelection();
        handleArrowUpDown(true);
        if (!shift) clearSelection();
    }

    private void moveDown(boolean shift) {
        if (shift) startOrExtendSelection();
        handleArrowUpDown(false);
        if (!shift) clearSelection();
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }
}
