package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import top.atdove.dovemail.client.gui.widgets.SimpleTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import top.atdove.dovemail.mail.MailSummary;
import top.atdove.dovemail.client.gui.widgets.MailCardRenderer;
import top.atdove.dovemail.network.DovemailNetwork;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 邮件收件箱界面。
 */
public class MailboxScreen extends Screen {
    private static final String SYSTEM_SENDER = "System";
    private static final int LIST_TOP_PADDING = 50;
    private static final int CARD_HEIGHT = 42;
    private static final int CARD_WIDTH = 246; // 原 260：两侧各缩小 4px，总计缩小 8px
    private static final int ICON_SIZE = 20;
    private static final int CARD_SPACING = 4;
    // 整体上移偏移量（像素），用于改善整体视觉均衡
    private static final int VERTICAL_SHIFT_UP = 12;
    private static final int PANEL_SIDE_PADDING = 16; // 面板内边距（相对卡片宽度）
    // 侧边功能区（刷新/翻页）与底部功能区/页码栏的保留尺寸
    private static final int ARROW_SIZE = 16;
    private static final int REFRESH_SIZE = 14;
    private static final int SIDE_COL_PADDING = 6; // 侧边栏内边距
    private static final int FUNCTION_BAR_HEIGHT = 22; // 底部功能按钮区域高度（按钮20 + 2余量）
    private static final int PAGE_BAR_EXTRA = 4; // 页码栏比字体额外增加的高度

    private final List<MailSummary> mailSummaries;
    private int currentPage;
    private AbstractTriangleButton prevArrow;
    private AbstractTriangleButton nextArrow;
    private MailCardRenderer cardRenderer;
    // Inline info message area
    private net.minecraft.network.chat.Component infoMessage;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public MailboxScreen(List<MailSummary> summaries) {
        super(Component.translatable("screen.dovemail.mailbox"));
        this.mailSummaries = new ArrayList<>(summaries != null ? summaries : Collections.emptyList());
        this.mailSummaries.sort((a, b) -> {
            boolean aSysUnread = !a.read() && SYSTEM_SENDER.equals(a.getSenderName());
            boolean bSysUnread = !b.read() && SYSTEM_SENDER.equals(b.getSenderName());
            if (aSysUnread != bSysUnread)
                return aSysUnread ? -1 : 1;
            return Long.compare(b.getTimestamp(), a.getTimestamp());
        });
    }

    // 封装本界面布局计算，确保 init/render/mouseClicked 使用一致的几何
    private static class Layout {
        // 面板外框
        int panelLeft;
        int panelRight;
        int panelTop;
        int panelBottom;
        // 面板中心X（卡片居中点）
        int panelCenterX;
        // 侧边栏（刷新/翻页）左右界
        int leftColLeft;
        int leftColRight;
        int rightColLeft;
        int rightColRight;
        // 卡片列表顶部Y（面板内）
        int listTop;
        // 信息栏高度（面板内）
        int infoBarHeight;
        // 面板下方功能按钮区顶Y
        int functionBarTop;
        // 页码栏顶Y（功能按钮区下方）
        int pageBarTop;
    }

    private Layout computeLayout() {
        Layout layout = new Layout();
        // 计算左右侧栏预留宽度（取刷新与箭头尺寸的较大值，再加两侧内边距）
        int leftReserved = Math.max(ARROW_SIZE, REFRESH_SIZE) + SIDE_COL_PADDING * 2;
        int rightReserved = ARROW_SIZE + SIDE_COL_PADDING * 2;

        // 面板宽度基于卡片宽度与内边距
        int desiredPanelWidth = CARD_WIDTH + PANEL_SIDE_PADDING * 2;
        int availableMiddle = Math.max(0, this.width - leftReserved - rightReserved);
        int panelWidth = Math.min(desiredPanelWidth, availableMiddle);

        // 将面板居中放在中间区域（左右侧栏之外）
    int middleLeft = leftReserved;
    int middleRight = this.width - rightReserved;
    int middleWidth = middleRight - middleLeft;
    int middleOffset = (middleWidth - panelWidth) / 2;
    layout.panelLeft = middleLeft + middleOffset;
    layout.panelRight = layout.panelLeft + panelWidth;
    layout.panelCenterX = (layout.panelLeft + layout.panelRight) / 2;

        // 侧栏区域坐标
    layout.leftColLeft = Math.max(0, layout.panelLeft - leftReserved);
    layout.leftColRight = layout.panelLeft;
    layout.rightColLeft = layout.panelRight;
    layout.rightColRight = Math.min(this.width, layout.panelRight + rightReserved);

        // 垂直方向：面板顶部（整体上移），列表顶部、信息栏、底部功能/页码栏
    int basePanelTop = 34;
    layout.panelTop = Math.max(12, basePanelTop - VERTICAL_SHIFT_UP);
    layout.listTop = Math.max(layout.panelTop + 10, LIST_TOP_PADDING - VERTICAL_SHIFT_UP);

    int pageSlots = getPageSize();
    int listHeight = pageSlots * CARD_HEIGHT + Math.max(0, pageSlots - 1) * CARD_SPACING;
    int margin = layout.listTop - layout.panelTop; // 顶部留白
    layout.infoBarHeight = this.font.lineHeight + 6; // 面板内信息栏高度
    layout.panelBottom = layout.listTop + listHeight + layout.infoBarHeight + margin;

        // 面板下方：功能按钮区与页码栏
        layout.functionBarTop = layout.panelBottom + 8; // 与面板间距 8px
        layout.pageBarTop = layout.functionBarTop + FUNCTION_BAR_HEIGHT + 6; // 与功能区间距 6px

        return layout;
    }

    @Override
    protected void init() {
        super.init();
    Layout layout = computeLayout();

        // 左右翻页箭头：位于左右侧栏，垂直居中
    int arrowY = layout.panelTop + (layout.panelBottom - layout.panelTop - ARROW_SIZE) / 2;
    int leftColWidth = Math.max(0, layout.leftColRight - layout.leftColLeft);
    int rightColWidth = Math.max(0, layout.rightColRight - layout.rightColLeft);
    int prevX = layout.leftColLeft + (leftColWidth - ARROW_SIZE) / 2;
    int nextX = layout.rightColLeft + (rightColWidth - ARROW_SIZE) / 2;
        prevArrow = new AbstractTriangleButton(prevX, arrowY, ARROW_SIZE, ARROW_SIZE, Direction.LEFT, () -> flipPage(-1));
        nextArrow = new AbstractTriangleButton(nextX, arrowY, ARROW_SIZE, ARROW_SIZE, Direction.RIGHT, () -> flipPage(1));
    addRenderableWidget(prevArrow);
    addRenderableWidget(nextArrow);

        // 刷新按钮位于左侧栏靠近面板顶部
    int refreshX = layout.leftColLeft + (leftColWidth - REFRESH_SIZE) / 2;
    int refreshY = layout.panelTop + 4;
        addRenderableWidget(new RefreshIconButton(refreshX, refreshY, REFRESH_SIZE, REFRESH_SIZE, DovemailNetwork::openMailbox));

        // 功能按钮移动到面板下方：删除已读、写信
    int centerX = layout.panelCenterX;
    int underY = layout.functionBarTop;
        var deleteRead = new SimpleTextButton(centerX - 90, underY, 80, 20,
                Component.translatable("button.dovemail.delete_read"),
                b -> top.atdove.dovemail.network.DovemailNetwork.deleteReadMails());
        var compose = new SimpleTextButton(centerX + 10, underY, 80, 20,
                Component.translatable("button.dovemail.compose"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ComposeMailScreen(this));
                    }
                });
        addRenderableWidget(deleteRead);
        addRenderableWidget(compose);
        // 初始化卡片渲染器（依赖 font 和时间格式化器）
        this.cardRenderer = new MailCardRenderer(CARD_WIDTH, CARD_HEIGHT, ICON_SIZE, this.font, timeFormatter);
        updateButtonStates();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@javax.annotation.Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY,
            float partialTick) {
        this.renderTransparentBackground(guiGraphics);
    }

    private int getPageSize() {
        int sz = top.atdove.dovemail.Config.getMailsPerPage();
        return Math.max(1, sz);
    }

    private void updateButtonStates() {
    int pageSize = getPageSize();
    int totalPages = Math.max((mailSummaries.size() + pageSize - 1) / pageSize, 1);
    if (prevArrow != null) prevArrow.active = currentPage > 0;
    if (nextArrow != null) nextArrow.active = currentPage + 1 < totalPages;
    }

    private void flipPage(int direction) {
        int pageSize = getPageSize();
        int totalPages = Math.max((mailSummaries.size() + pageSize - 1) / pageSize, 1);
        currentPage = Mth.clamp(currentPage + direction, 0, Math.max(totalPages - 1, 0));
        updateButtonStates();
    }

    @Override
    public void render(@javax.annotation.Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 统一按布局渲染
        Layout layout = computeLayout();

        // 背景面板（半透明）
        guiGraphics.fill(layout.panelLeft, layout.panelTop, layout.panelRight, layout.panelBottom, 0x66000000);
        // 边框
        guiGraphics.fill(layout.panelLeft, layout.panelTop, layout.panelRight, layout.panelTop + 1, 0x33FFFFFF);
        guiGraphics.fill(layout.panelLeft, layout.panelBottom - 1, layout.panelRight, layout.panelBottom, 0x33000000);
        guiGraphics.fill(layout.panelLeft, layout.panelTop, layout.panelLeft + 1, layout.panelBottom, 0x33FFFFFF);
        guiGraphics.fill(layout.panelRight - 1, layout.panelTop, layout.panelRight, layout.panelBottom, 0x33000000);

        guiGraphics.drawCenteredString(font, this.title, layout.panelCenterX, 10, 0xFFFFFF);

        int pageSize = getPageSize();
        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, mailSummaries.size());
        int y = layout.listTop;
        for (int i = startIndex; i < endIndex; i++) {
            MailSummary summary = mailSummaries.get(i);
            cardRenderer.renderEntry(guiGraphics, layout.panelCenterX, y, summary, i, mouseX, mouseY);
            y += CARD_HEIGHT + CARD_SPACING;
        }
        if (mailSummaries.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("screen.dovemail.mailbox.empty"),
                    layout.panelCenterX, this.height / 2 - 10, 0xAAAAAA);
        }
        // 页码栏（预留高度后绘制在功能区下方）
        int ps = getPageSize();
        int totalPages = Math.max((mailSummaries.size() + ps - 1) / ps, 1);
        Component indicator = Component.literal((currentPage + 1) + "/" + totalPages);
        int pageBarTextY = layout.pageBarTop + Math.max(0, (this.font.lineHeight + PAGE_BAR_EXTRA - this.font.lineHeight) / 2);
        guiGraphics.drawCenteredString(font, indicator, layout.panelCenterX, pageBarTextY, 0xFFFFFF);

        // 提示区域纳入面板内：在面板底部内部绘制
        if (infoMessage != null) {
            int innerPad = 4;
            int barTop = layout.panelBottom - layout.infoBarHeight + innerPad; // inside panel bottom with small padding
            guiGraphics.fill(layout.panelLeft + 1, barTop, layout.panelRight - 1, barTop + layout.infoBarHeight - innerPad * 2, 0x44000000);
            int textY = barTop + (layout.infoBarHeight - innerPad * 2 - this.font.lineHeight) / 2;
            guiGraphics.drawCenteredString(font, infoMessage, layout.panelCenterX, textY, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 计算当前页卡片矩形，命中后打开详情界面
    Layout layout = computeLayout();
    int pageSize2 = getPageSize();
        int startIndex = currentPage * pageSize2;
        int endIndex = Math.min(startIndex + pageSize2, mailSummaries.size());
    int y = layout.listTop;
    int left = layout.panelCenterX - CARD_WIDTH / 2;
    int right = layout.panelCenterX + CARD_WIDTH / 2;
        for (int i = startIndex; i < endIndex; i++) {
            int top = y;
            int bottom = top + CARD_HEIGHT;
            if (mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
                MailSummary summary = mailSummaries.get(i);
                if (this.minecraft != null)
                    this.minecraft.setScreen(new MailDetailScreen(this, summary, java.util.Collections.emptyList(),
                            s -> DovemailNetwork.claimAttachments(s.getId())));
                return true;
            }
            y += CARD_HEIGHT + CARD_SPACING;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    // --- 小部件：三角形箭头与刷新图标 ---
    private enum Direction { LEFT, RIGHT }

    private static class AbstractTriangleButton extends net.minecraft.client.gui.components.AbstractWidget {
        private final Direction dir;
        private final Runnable onClickAction;
        AbstractTriangleButton(int x, int y, int w, int h, Direction dir, Runnable onClick) {
            super(x, y, w, h, net.minecraft.network.chat.Component.empty());
            this.dir = dir;
            this.onClickAction = onClick;
        }
        @Override
        protected void renderWidget(@javax.annotation.Nonnull GuiGraphics g, int mouseX, int mouseY, float pt) {
            int bg = this.isHoveredOrFocused() ? 0x55FFFFFF : 0x33000000;
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            int color = this.active ? 0xFFFFFFFF : 0x66FFFFFF;
            int cx = getX() + getWidth() / 2;
            int cy = getY() + getHeight() / 2;
            int size = Math.min(getWidth(), getHeight()) - 4;
            drawTriangle(g, cx, cy, size, dir, color);
        }
        private void drawTriangle(GuiGraphics g, int cx, int cy, int size, Direction dir, int color) {
            int half = size / 2;
            if (dir == Direction.LEFT) {
                for (int dy = -half; dy <= half; dy++) {
                    int len = half - Math.abs(dy);
                    int x1 = cx - len;
                    int x2 = cx;
                    g.hLine(x1, x2, cy + dy, color);
                }
            } else {
                for (int dy = -half; dy <= half; dy++) {
                    int len = half - Math.abs(dy);
                    int x1 = cx;
                    int x2 = cx + len;
                    g.hLine(x1, x2, cy + dy, color);
                }
            }
        }
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.active || !this.visible) return false;
            if (this.isMouseOver(mouseX, mouseY) && button == 0) {
                if (onClickAction != null) onClickAction.run();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();
        }
        @Override
        protected void updateWidgetNarration(@javax.annotation.Nonnull net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static class RefreshIconButton extends net.minecraft.client.gui.components.AbstractWidget {
        private final Runnable onClickAction;
        RefreshIconButton(int x, int y, int w, int h, Runnable onClick) {
            super(x, y, w, h, net.minecraft.network.chat.Component.translatable("button.dovemail.refresh"));
            this.onClickAction = onClick;
        }
        @Override
        protected void renderWidget(@javax.annotation.Nonnull GuiGraphics g, int mouseX, int mouseY, float pt) {
            int bg = this.isHoveredOrFocused() ? 0x55FFFFFF : 0x33000000;
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            int color = 0xFFFFFFFF;
            drawRefresh(g, color);
        }
        private void drawRefresh(GuiGraphics g, int color) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int cx = x + w / 2;
            int cy = y + h / 2;
            int r = Math.min(w, h) / 2 - 2;
            // 简化的圆弧（四段短线模拟）
            g.hLine(cx - r, cx + r - 2, cy - 1, color);
            g.hLine(cx - r + 1, cx + r - 3, cy + 1, color);
            g.vLine(cx + r - 2, cy - r / 2, cy + 1, color);
            // 箭头
            g.hLine(cx + r - 2, cx + r, cy - 2, color);
            g.vLine(cx + r, cy - 6, cy - 2, color);
            g.hLine(cx + r - 4, cx + r, cy - 6, color);
        }
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.active || !this.visible) return false;
            if (this.isMouseOver(mouseX, mouseY) && button == 0) {
                if (onClickAction != null) onClickAction.run();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();
        }
        @Override
        protected void updateWidgetNarration(@javax.annotation.Nonnull net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    public static class Builder {
        private final List<MailSummary> mails = new ArrayList<>();

        public Builder addSummary(MailSummary summary) {
            if (summary != null) {
                mails.add(summary);
            }
            return this;
        }

        public Builder fromMails(List<MailSummary> summaries) {
            if (summaries != null) {
                mails.addAll(summaries);
            }
            return this;
        }

        public MailboxScreen build() {
            return new MailboxScreen(mails);
        }
    }

    // 供客户端增量刷新列表时调用
    public void updateOrAppendSummary(MailSummary summary) {
        if (summary == null)
            return;
        boolean replaced = false;
        for (int i = 0; i < mailSummaries.size(); i++) {
            if (mailSummaries.get(i).getId().equals(summary.getId())) {
                mailSummaries.set(i, summary);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            mailSummaries.add(summary);
        }
        // 重新排序、更新分页按钮（未读 System 优先，其次按时间倒序）
        mailSummaries.sort((a, b) -> {
            boolean aSysUnread = !a.read() && SYSTEM_SENDER.equals(a.getSenderName());
            boolean bSysUnread = !b.read() && SYSTEM_SENDER.equals(b.getSenderName());
            if (aSysUnread != bSysUnread)
                return aSysUnread ? -1 : 1;
            return Long.compare(b.getTimestamp(), a.getTimestamp());
        });
        updateButtonStates();
    }

    // Called by client hook when server sends an inline alert
    public void showInfoMessage(net.minecraft.network.chat.Component message) {
        this.infoMessage = message;
    }

    // 替换全部摘要（用于服务器主动下发全量刷新时），不影响底部信息栏
    public void replaceAllSummaries(java.util.List<MailSummary> summaries) {
        this.mailSummaries.clear();
        if (summaries != null)
            this.mailSummaries.addAll(summaries);
        // 排序规则与构造一致：未读 System 优先，其次时间倒序
        this.mailSummaries.sort((a, b) -> {
            boolean aSysUnread = !a.read() && SYSTEM_SENDER.equals(a.getSenderName());
            boolean bSysUnread = !b.read() && SYSTEM_SENDER.equals(b.getSenderName());
            if (aSysUnread != bSysUnread)
                return aSysUnread ? -1 : 1;
            return Long.compare(b.getTimestamp(), a.getTimestamp());
        });
        // 当前页尽量保持，越界则夹取
        int ps3 = getPageSize();
        int totalPages = Math.max((mailSummaries.size() + ps3 - 1) / ps3, 1);
        this.currentPage = net.minecraft.util.Mth.clamp(this.currentPage, 0, Math.max(totalPages - 1, 0));
        updateButtonStates();
    }
}
