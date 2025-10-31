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
    private static final int CARD_WIDTH = 252; // 原 260：两侧各缩小 4px，总计缩小 8px
    private static final int ICON_SIZE = 20;
    private static final int CARD_SPACING = 4;
    // 整体上移偏移量（像素），用于改善整体视觉均衡
    private static final int VERTICAL_SHIFT_UP = 12;

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

    @Override
    protected void init() {
        super.init();
    int centerX = this.width / 2;
    // 面板区域用于定位箭头与刷新按钮（高度随每页展示数量动态变化）
    int panelLeft = centerX - CARD_WIDTH / 2 - 10;
    int panelRight = centerX + CARD_WIDTH / 2 + 10;
    int basePanelTop = 34;
    int panelTop = Math.max(12, basePanelTop - VERTICAL_SHIFT_UP);
    int listTopInit = Math.max(panelTop + 10, LIST_TOP_PADDING - VERTICAL_SHIFT_UP);
    int pageSlotsInit = getPageSize();
    int marginInit = listTopInit - panelTop;
    int listHeightInit = pageSlotsInit * CARD_HEIGHT + Math.max(0, pageSlotsInit - 1) * CARD_SPACING;
    int infoBarHeightInit = this.font.lineHeight + 6; // reserve one line height for info bar
    int panelBottom = listTopInit + listHeightInit + infoBarHeightInit + marginInit;

    // 左右翻页箭头：位于收件箱面板左右两侧内部，垂直居中
    int arrowSize = 16;
    int arrowY = panelTop + (panelBottom - panelTop - arrowSize) / 2;
    prevArrow = new AbstractTriangleButton(panelLeft + 4, arrowY, arrowSize, arrowSize, Direction.LEFT, () -> flipPage(-1));
    nextArrow = new AbstractTriangleButton(panelRight - 4 - arrowSize, arrowY, arrowSize, arrowSize, Direction.RIGHT, () -> flipPage(1));
    addRenderableWidget(prevArrow);
    addRenderableWidget(nextArrow);

    // 顶部左上角刷新图标按钮
    addRenderableWidget(new RefreshIconButton(panelLeft + 4, panelTop + 4, 14, 14, DovemailNetwork::openMailbox));

    // 功能按钮移动到面板下方：删除已读、写信
    int underY = panelBottom + 8;
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

    // 顶部标题与背景面板（高度根据每页展示数量动态渲染，预留一行用于提示），整体上移
    int centerX = this.width / 2;
    int panelLeft = centerX - CARD_WIDTH / 2 - 10;
    int panelRight = centerX + CARD_WIDTH / 2 + 10;
    int basePanelTopR = 34;
    int panelTop = Math.max(12, basePanelTopR - VERTICAL_SHIFT_UP);
    int listTopRender = Math.max(panelTop + 10, LIST_TOP_PADDING - VERTICAL_SHIFT_UP);
    int pageSlotsRender = getPageSize();
    int marginRender = listTopRender - panelTop;
    int listHeightRender = pageSlotsRender * CARD_HEIGHT + Math.max(0, pageSlotsRender - 1) * CARD_SPACING;
    int infoBarHeight = this.font.lineHeight + 6;
    int panelBottom = listTopRender + listHeightRender + infoBarHeight + marginRender;
        // 背景面板（半透明）
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0x66000000);
        // 边框
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x33FFFFFF);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x33000000);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0x33FFFFFF);
        guiGraphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0x33000000);

        guiGraphics.drawCenteredString(font, this.title, this.width / 2, 10, 0xFFFFFF);

    int pageSize = getPageSize();
    int startIndex = currentPage * pageSize;
    int endIndex = Math.min(startIndex + pageSize, mailSummaries.size());
    int y = listTopRender;
        for (int i = startIndex; i < endIndex; i++) {
            MailSummary summary = mailSummaries.get(i);
            cardRenderer.renderEntry(guiGraphics, this.width / 2, y, summary, i, mouseX, mouseY);
            y += CARD_HEIGHT + CARD_SPACING;
        }
        if (mailSummaries.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("screen.dovemail.mailbox.empty"),
                    this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        }
        renderPageIndicator(guiGraphics);

        // 提示区域纳入面板内：在面板底部内部绘制
        if (infoMessage != null) {
            int innerPad = 4;
            int barTop = panelBottom - infoBarHeight + innerPad; // inside panel bottom with small padding
            guiGraphics.fill(panelLeft + 1, barTop, panelRight - 1, barTop + infoBarHeight - innerPad * 2, 0x44000000);
            int textY = barTop + (infoBarHeight - innerPad * 2 - this.font.lineHeight) / 2;
            guiGraphics.drawCenteredString(font, infoMessage, this.width / 2, textY, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 计算当前页卡片矩形，命中后打开详情界面
    int pageSize2 = getPageSize();
    int startIndex = currentPage * pageSize2;
    int endIndex = Math.min(startIndex + pageSize2, mailSummaries.size());
    int basePanelTopClick = 34;
    int panelTopClick = Math.max(12, basePanelTopClick - VERTICAL_SHIFT_UP);
    int y = Math.max(panelTopClick + 10, LIST_TOP_PADDING - VERTICAL_SHIFT_UP);
        int centerX = this.width / 2;
        int left = centerX - CARD_WIDTH / 2;
        int right = centerX + CARD_WIDTH / 2;
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
            y += CARD_HEIGHT + 4;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderPageIndicator(GuiGraphics guiGraphics) {
    int ps = getPageSize();
    int totalPages = Math.max((mailSummaries.size() + ps - 1) / ps, 1);
        Component indicator = Component.literal((currentPage + 1) + "/" + totalPages);
        guiGraphics.drawCenteredString(font, indicator, this.width / 2, this.height - 44, 0xFFFFFF);
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
