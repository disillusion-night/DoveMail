package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
import java.util.Comparator;
import java.util.List;

/**
 * 简易邮件收件箱界面。
 */
public class MailboxScreen extends Screen {
    private static final int LIST_TOP_PADDING = 50;
    private static final int CARD_HEIGHT = 42;
    private static final int CARD_WIDTH = 260;
    private static final int ICON_SIZE = 20;
    private static final int PAGE_SIZE = 20;

    private final List<MailSummary> mailSummaries;
    private int currentPage;
    private Button prevButton;
    private Button nextButton;
    private MailCardRenderer cardRenderer;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public MailboxScreen(List<MailSummary> summaries) {
        super(Component.translatable("screen.dovemail.mailbox"));
        this.mailSummaries = new ArrayList<>(summaries != null ? summaries : Collections.emptyList());
        this.mailSummaries.sort(Comparator.comparingLong(MailSummary::getTimestamp).reversed());
    }

    @Override
    protected void init() {
        super.init();
    int centerX = this.width / 2;
    int bottom = this.height - 28;

    prevButton = Button.builder(Component.translatable("button.dovemail.prevPage"), btn -> flipPage(-1))
        .pos(centerX - 110, bottom)
        .size(80, 20)
        .build();
    nextButton = Button.builder(Component.translatable("button.dovemail.nextPage"), btn -> flipPage(1))
        .pos(centerX + 30, bottom)
        .size(80, 20)
        .build();
        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);
    // Compose button at top-right of the list
    Button compose = Button.builder(Component.translatable("button.dovemail.compose"), btn ->
        this.minecraft.setScreen(new ComposeMailScreen(this))
    ).pos(centerX + CARD_WIDTH / 2 - 80, 14).size(80, 20).build();
    addRenderableWidget(compose);
    // 初始化卡片渲染器（依赖 font 和时间格式化器）
    this.cardRenderer = new MailCardRenderer(CARD_WIDTH, CARD_HEIGHT, ICON_SIZE, this.font, timeFormatter);
        updateButtonStates();
    }

    private void updateButtonStates() {
        int totalPages = Math.max((mailSummaries.size() + PAGE_SIZE - 1) / PAGE_SIZE, 1);
        prevButton.active = currentPage > 0;
        nextButton.active = currentPage + 1 < totalPages;
    }

    private void flipPage(int direction) {
        int totalPages = Math.max((mailSummaries.size() + PAGE_SIZE - 1) / PAGE_SIZE, 1);
        currentPage = Mth.clamp(currentPage + direction, 0, Math.max(totalPages - 1, 0));
        updateButtonStates();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 顶部标题与背景面板
        int centerX = this.width / 2;
        int panelLeft = centerX - CARD_WIDTH / 2 - 10;
        int panelRight = centerX + CARD_WIDTH / 2 + 10;
        int panelTop = 34;
        int panelBottom = this.height - 54;
        // 背景面板（半透明）
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0x66000000);
        // 边框
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x33FFFFFF);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x33000000);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0x33FFFFFF);
        guiGraphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0x33000000);

        guiGraphics.drawCenteredString(font, this.title, this.width / 2, 10, 0xFFFFFF);

        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, mailSummaries.size());
        int y = LIST_TOP_PADDING;
        for (int i = startIndex; i < endIndex; i++) {
            MailSummary summary = mailSummaries.get(i);
            cardRenderer.renderEntry(guiGraphics, this.width / 2, y, summary, i, mouseX, mouseY);
            y += CARD_HEIGHT + 4;
        }
        if (mailSummaries.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("screen.dovemail.mailbox.empty"), this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        }
        renderPageIndicator(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 计算当前页卡片矩形，命中后打开详情界面
        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, mailSummaries.size());
        int y = LIST_TOP_PADDING;
        int centerX = this.width / 2;
        int left = centerX - CARD_WIDTH / 2;
        int right = centerX + CARD_WIDTH / 2;
        for (int i = startIndex; i < endIndex; i++) {
            int top = y;
            int bottom = top + CARD_HEIGHT;
            if (mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
                MailSummary summary = mailSummaries.get(i);
                this.minecraft.setScreen(new MailDetailScreen(summary, java.util.Collections.emptyList(), s -> DovemailNetwork.claimAttachments(s.getId())));
                return true;
            }
            y += CARD_HEIGHT + 4;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderPageIndicator(GuiGraphics guiGraphics) {
        int totalPages = Math.max((mailSummaries.size() + PAGE_SIZE - 1) / PAGE_SIZE, 1);
        Component indicator = Component.literal((currentPage + 1) + "/" + totalPages);
        guiGraphics.drawCenteredString(font, indicator, this.width / 2, this.height - 44, 0xFFFFFF);
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
}
