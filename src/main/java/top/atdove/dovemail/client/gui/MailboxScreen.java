package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import top.atdove.dovemail.mail.MailSummary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 简易邮件收件箱界面。
 */
public class MailboxScreen extends Screen {
    private static final int LIST_TOP_PADDING = 32;
    private static final int ENTRY_HEIGHT = 24;

    private final List<MailSummary> mailSummaries;
    private int scrollOffset;
    private Button prevButton;
    private Button nextButton;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public MailboxScreen(List<MailSummary> summaries) {
        super(Component.translatable("screen.dovemail.mailbox"));
        this.mailSummaries = new ArrayList<>(summaries != null ? summaries : Collections.emptyList());
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int bottom = this.height - 28;

        prevButton = Button.builder(Component.literal("<"), btn -> scroll(-1))
                .pos(centerX - 60, bottom)
                .size(40, 20)
                .build();
        nextButton = Button.builder(Component.literal(">"), btn -> scroll(1))
                .pos(centerX + 20, bottom)
                .size(40, 20)
                .build();
        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);
        updateButtonStates();
    }

    private void updateButtonStates() {
        int visible = visibleCount();
        prevButton.active = scrollOffset > 0;
        nextButton.active = mailSummaries.size() > scrollOffset + visible;
    }

    private int visibleCount() {
        return Math.max((this.height - LIST_TOP_PADDING - 40) / ENTRY_HEIGHT, 1);
    }

    private void scroll(int direction) {
        int visible = visibleCount();
    int maxOffset = Math.max(mailSummaries.size() - visible, 0);
    long newOffset = Math.clamp((long) scrollOffset + (long) direction * visible, 0L, (long) maxOffset);
    scrollOffset = (int) newOffset;
        updateButtonStates();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, this.title, this.width / 2, 12, 0xFFFFFF);

        int visible = visibleCount();
        int start = scrollOffset;
        int end = Math.min(start + visible, mailSummaries.size());
        int y = LIST_TOP_PADDING;
        for (int i = start; i < end; i++) {
            MailSummary summary = mailSummaries.get(i);
            renderEntry(guiGraphics, summary, y);
            y += ENTRY_HEIGHT;
        }
        if (mailSummaries.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("screen.dovemail.mailbox.empty"), this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        }
    }

    private void renderEntry(GuiGraphics guiGraphics, MailSummary summary, int y) {
        int left = this.width / 2 - 110;
        int right = this.width / 2 + 110;
        int top = y;
        int bottom = y + ENTRY_HEIGHT - 4;

        int backgroundColor = summary.isRead() ? 0x44000000 : 0x88000000;
        guiGraphics.fill(left, top, right, bottom, backgroundColor);

        Component subject = Component.literal(summary.getSubject());
        guiGraphics.drawString(font, subject, left + 6, top + 6, 0xFFFFFF, false);

        String timeText = timeFormatter.format(Instant.ofEpochMilli(summary.getTimestamp()));
        guiGraphics.drawString(font, timeText, right - font.width(timeText) - 6, top + 6, 0xCCCCCC, false);

        if (!summary.isAttachmentsClaimed()) {
            Component attachments = Component.translatable("screen.dovemail.mailbox.attachments");
            guiGraphics.drawString(font, attachments, left + 6, top + 14, 0xFFD700, false);
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

        public Builder addMail(UUID id, String subject, String bodyJson, long timestamp, boolean read, boolean attachmentsClaimed) {
            mails.add(new MailSummary(id, subject, bodyJson, timestamp, read, attachmentsClaimed));
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
