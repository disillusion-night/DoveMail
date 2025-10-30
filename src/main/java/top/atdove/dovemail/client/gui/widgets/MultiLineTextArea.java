package top.atdove.dovemail.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import javax.annotation.Nonnull;

public class MultiLineTextArea extends AbstractWidget {
    private final net.minecraft.client.gui.Font font;
    private static final int LINE_SPACING = 2;
    private final StringBuilder value = new StringBuilder();
    private int cursor = 0; // cursor index in value
    private int desiredColumn = -1; // track target column for up/down
    private int firstVisibleLine = 0; // visual line index for soft wrap
    private int selectionStart = -1; // inclusive, -1 means no selection
    private int selectionEnd = -1;   // exclusive
    // Soft-wrap layout cache
    private java.util.List<VisualLine> layout = java.util.Collections.emptyList();
    private int layoutForWidth = -1; // cache key
    private int layoutForVersion = -1; // cache key for content version
    private int contentVersion = 0; // increments whenever text content changes

    private static final int H_PADDING = 4; // left/right padding for text

    private static class VisualLine {
        final int start; // start index in value (inclusive)
        final int end;   // end index in value (exclusive)
        final String text; // value.substring(start, end)
        VisualLine(int start, int end, String text) { this.start = start; this.end = end; this.text = text; }
    }

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
        contentVersion++;
    }

    public void append(String text) {
        if (text == null || text.isEmpty()) return;
        clampCursor();
        int pos = Mth.clamp(cursor, 0, value.length());
        value.insert(pos, text);
        cursor = pos + text.length();
        ensureCursorVisible();
        contentVersion++;
    }

    private int lineCount() { ensureLayout(); return layout.size(); }

    private int getLineOfIndex(int idx) { ensureLayout(); return getVisualLineOfIndex(idx); }

    private int getColumnOfIndex(int idx) {
        ensureLayout();
        int vline = getVisualLineOfIndex(idx);
        VisualLine vl = layout.get(Mth.clamp(vline, 0, Math.max(0, layout.size() - 1)));
        int norm = normalizeIndexForLayout(idx);
        return norm - vl.start;
    }

    private int getIndexOfLineColumn(int line, int column) {
        ensureLayout();
        VisualLine vl = layout.get(Mth.clamp(line, 0, Math.max(0, layout.size() - 1)));
        int col = Mth.clamp(column, 0, vl.end - vl.start);
        return vl.start + col;
    }

    private void ensureCursorVisible() {
        int visibleLines = Math.max(1, (this.height - 4) / (font.lineHeight + LINE_SPACING));
        int line = getLineOfIndex(cursor);
        if (line < firstVisibleLine) firstVisibleLine = line;
        if (line >= firstVisibleLine + visibleLines) firstVisibleLine = line - visibleLines + 1;
        if (firstVisibleLine < 0) firstVisibleLine = 0;
    }

    @Override
    protected void renderWidget(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // background
        int bg = this.isFocused() ? 0xAA000000 : 0x88000000;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        // border
        g.fill(getX(), getY(), getX() + width, getY() + 1, 0x33FFFFFF);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0x33000000);
        g.fill(getX(), getY(), getX() + 1, getY() + height, 0x33FFFFFF);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0x33000000);

        // draw text lines & selection background (soft wrap)
        ensureLayout();
        int visibleLines = Math.max(1, (this.height - 4) / (font.lineHeight + LINE_SPACING));
        int y = getY() + 2;
        for (int i = 0; i < visibleLines; i++) {
            int lineIdx = firstVisibleLine + i;
            if (lineIdx >= layout.size()) break;
            VisualLine vl = layout.get(lineIdx);
            highlightSelectionOnVisualLine(g, y, vl);
            g.drawString(font, vl.text, getX() + H_PADDING, y, 0xEEEEEE, false);
            y += font.lineHeight + LINE_SPACING;
        }

        // caret
        if (this.isFocused() && Minecraft.getInstance().gui.getGuiTicks() / 6 % 2 == 0) {
            int curLine = getLineOfIndex(cursor);
            int curCol = getColumnOfIndex(cursor);
            if (curLine >= firstVisibleLine && curLine < firstVisibleLine + visibleLines) {
                VisualLine vl = layout.get(curLine);
                int cx = getX() + H_PADDING + font.width(safeSubstring(vl.text, 0, curCol));
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
            ensureLayout();
            int line = (int) Math.floor((mouseY - getY() - 2) / (font.lineHeight + LINE_SPACING)) + firstVisibleLine;
            line = Mth.clamp(line, 0, Math.max(0, layout.size() - 1));
            int col = xToColumnOnVisualLine(line, (int) (mouseX - getX() - H_PADDING));
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
        ensureLayout();
        int line = (int) Math.floor((mouseY - getY() - 2) / (font.lineHeight + LINE_SPACING)) + firstVisibleLine;
        line = Mth.clamp(line, 0, Math.max(0, layout.size() - 1));
        int col = xToColumnOnVisualLine(line, (int) (mouseX - getX() - H_PADDING));
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
        ensureLayout();
        int len = 0;
        if (!layout.isEmpty()) {
            VisualLine vl = layout.get(Mth.clamp(line, 0, layout.size()-1));
            len = vl.end - vl.start;
        }
        if (home) {
            cursor = getIndexOfLineColumn(line, 0);
            desiredColumn = 0;
        } else {
            cursor = getIndexOfLineColumn(line, len);
            desiredColumn = len;
        }
        ensureCursorVisible();
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    // Selection & clipboard helpers
    private boolean hasSelection() { return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd; }
    private void clearSelection() { selectionStart = -1; selectionEnd = -1; }
    private void startOrExtendSelection() { if (!hasSelection()) { selectionStart = cursor; selectionEnd = cursor; } }
    private void selectAll() { selectionStart = 0; selectionEnd = value.length(); cursor = selectionEnd; ensureCursorVisible(); }
    private void deleteSelection() {
        clampSelectionToBounds();
        int a = Math.min(selectionStart, selectionEnd);
        int b = Math.max(selectionStart, selectionEnd);
        value.delete(a, b);
        cursor = a;
        clearSelection();
        ensureCursorVisible();
        contentVersion++;
    }
    private void replaceSelectionIfAny() { if (hasSelection()) deleteSelection(); }
    private void copySelection() {
        if (!hasSelection()) return;
        clampSelectionToBounds();
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
    private void highlightSelectionOnVisualLine(GuiGraphics g, int y, VisualLine vl) {
        if (!hasSelection()) return;
        int selA = Math.min(selectionStart, selectionEnd);
        int selB = Math.max(selectionStart, selectionEnd);
        int a = Math.max(selA, vl.start);
        int b = Math.min(selB, vl.end);
        if (a >= b) return;
        int aCol = a - vl.start;
        int bCol = b - vl.start;
        int ax = getX() + H_PADDING + font.width(safeSubstring(vl.text, 0, aCol));
        int bx = getX() + H_PADDING + font.width(safeSubstring(vl.text, 0, bCol));
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
        clampCursor();
        if (cursor > 0 && cursor <= value.length()) {
            value.deleteCharAt(cursor - 1);
            cursor--;
            ensureCursorVisible();
            contentVersion++;
        }
    }

    private void handleDelete() {
        if (hasSelection()) { deleteSelection(); return; }
        clampCursor();
        if (cursor < value.length() && cursor >= 0) {
            value.deleteCharAt(cursor);
            ensureCursorVisible();
            contentVersion++;
        }
    }

    private void moveLeft(boolean shift) {
        if (shift) startOrExtendSelection();
        clampCursor();
        if (cursor > 0) cursor--;
        if (!shift) clearSelection();
        desiredColumn = -1;
        ensureCursorVisible();
    }

    private void moveRight(boolean shift) {
        if (shift) startOrExtendSelection();
        clampCursor();
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

    // Soft-wrap layout helpers

    private void clampCursor() {
        cursor = Mth.clamp(cursor, 0, value.length());
    }

    private void clampSelectionToBounds() {
        int len = value.length();
        if (selectionStart >= 0) selectionStart = Mth.clamp(selectionStart, 0, len);
        if (selectionEnd >= 0) selectionEnd = Mth.clamp(selectionEnd, 0, len);
    }

    private void ensureLayout() {
        int available = Math.max(1, this.width - H_PADDING * 2);
        if (available == layoutForWidth && contentVersion == layoutForVersion && !layout.isEmpty()) return;
        layoutForWidth = available;
        layoutForVersion = contentVersion;
        layout = new java.util.ArrayList<>();
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '\n') {
                layout.add(new VisualLine(i, i, ""));
                i++;
            } else {
                i = appendWrappedSegment(i, available);
            }
        }
        // trailing empty line if text ends with newline
        int len = value.length();
        char last = len == 0 ? '\0' : value.charAt(len - 1);
        if (last == '\n') {
            layout.add(new VisualLine(len, len, ""));
        }
        if (layout.isEmpty()) {
            layout.add(new VisualLine(0, 0, ""));
        }
        // clamp firstVisibleLine if needed
        int visibleLines = Math.max(1, (this.height - 4) / (font.lineHeight + LINE_SPACING));
        firstVisibleLine = Mth.clamp(firstVisibleLine, 0, Math.max(0, layout.size() - visibleLines));
    }

    private int appendWrappedSegment(int start, int available) {
        int end = start;
        while (end < value.length()) {
            char ch = value.charAt(end);
            boolean stop = (ch == '\n') || (font.width(value.substring(start, end + 1)) > available);
            if (stop) break;
            end++;
        }
        if (end == start) {
            end = Math.min(value.length(), start + 1);
        }
        layout.add(new VisualLine(start, end, value.substring(start, end)));
        return end;
    }

    private int normalizeIndexForLayout(int idx) {
        // 不向前折返到换行符前一列：当光标位于换行后（idx 位于 '\n' 之后），
        // 应该定位到下一可见行的列 0，而不是上一行的末尾，避免“换行后光标跳到上一行末尾”的错觉。
        return Mth.clamp(idx, 0, value.length());
    }

    private int getVisualLineOfIndex(int idx) {
        ensureLayout();
        int norm = normalizeIndexForLayout(idx);
        for (int i = 0; i < layout.size(); i++) {
            VisualLine vl = layout.get(i);
            // idx in [start, end], prefer this line
            if (norm >= vl.start && norm <= vl.end) return i;
        }
        return Math.max(0, layout.size() - 1);
    }

    private int xToColumnOnVisualLine(int vline, int relX) {
        VisualLine vl = layout.get(Mth.clamp(vline, 0, Math.max(0, layout.size() - 1)));
        if (relX <= 0) return 0;
        int best = 0;
        for (int i = 0; i <= vl.text.length(); i++) {
            int w = font.width(safeSubstring(vl.text, 0, i));
            if (w >= relX) { best = i; break; }
            best = i;
        }
        return best;
    }
}
