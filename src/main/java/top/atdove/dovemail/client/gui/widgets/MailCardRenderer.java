package top.atdove.dovemail.client.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import top.atdove.dovemail.mail.MailSummary;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * 负责绘制单个邮件条目的卡片渲染器。
 */
public class MailCardRenderer {
    private final int cardWidth;
    private final int cardHeight;
    private final int iconSize;
    private final Font font;
    private final DateTimeFormatter timeFormatter;

    public MailCardRenderer(int cardWidth, int cardHeight, int iconSize, Font font, DateTimeFormatter timeFormatter) {
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;
        this.iconSize = iconSize;
        this.font = font;
        this.timeFormatter = timeFormatter;
    }

    /**
     * 在给定中心 X 坐标处渲染一个邮件卡片。
     */
    public void renderEntry(GuiGraphics g, int centerX, int y, MailSummary summary, int index,
                            int mouseX, int mouseY) {
        int left = centerX - cardWidth / 2;
        int top = y;
        int right = centerX + cardWidth / 2;
        int bottom = top + cardHeight;

        int backgroundColor = summary.isRead() ? 0x33000000 : 0x66000000;
        g.fill(left, top, right, bottom, backgroundColor);
        g.fill(left, bottom - 1, right, bottom, 0x22000000);

        boolean hovered = mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
        if (hovered) {
            int borderColor = 0xCCF9D77E;
            g.fill(left, top, right, top + 1, borderColor);
            g.fill(left, bottom - 1, right, bottom, borderColor);
            g.fill(left, top, left + 1, bottom, borderColor);
            g.fill(right - 1, top, right, bottom, borderColor);
        }

        int iconX = left + 8;
        int iconY = top + (cardHeight - iconSize) / 2;
        renderMailIcon(g, iconX, iconY, iconSize,
                summary.hasAttachments(), !summary.isRead(), summary.hasAttachments() && !summary.isAttachmentsClaimed());

        int textLeft = iconX + iconSize + 6;
        int textRight = right - 10;
        int textWidth = Math.max(textRight - textLeft, 40);

        Component subject = Component.literal(summary.getSubject() != null ? summary.getSubject() : "");
        String subjectLine = font.plainSubstrByWidth(subject.getString(), textWidth);
        g.drawString(font, subjectLine, textLeft, top + 4, 0xFFFFFF, false);

        String senderName;
        if (summary.getSenderName() == null || summary.getSenderName().isEmpty()) {
            senderName = Component.translatable("screen.dovemail.mailbox.sender.unknown").getString();
        } else if ("System".equals(summary.getSenderName())) {
            senderName = Component.translatable("sender.dovemail.system").getString();
        } else {
            senderName = summary.getSenderName();
        }
        String senderLine = font.plainSubstrByWidth(senderName, textWidth);
        g.drawString(font, Component.literal(senderLine), textLeft, top + 16, 0xB0E0FF, false);

        Component bodyComponent = summary.bodyComponent();
        String bodyLine = font.plainSubstrByWidth(bodyComponent.getString(), textWidth);
        g.drawString(font, Component.literal(bodyLine), textLeft, top + 26, 0xDDDDDD, false);

        if (summary.hasAttachments() && !summary.isAttachmentsClaimed()) {
            Component attachments = Component.translatable("screen.dovemail.mailbox.attachments");
            g.drawString(font, attachments, textLeft, top + 34, 0xFFF4B45C, false);
        }

        String timeText = timeFormatter.format(Instant.ofEpochMilli(summary.getTimestamp()));
        g.drawString(font, timeText, right - font.width(timeText) - 6, top + 4, 0xCCCCCC, false);

        Component indexLabel = Component.literal(String.valueOf(index + 1));
        g.drawString(font, indexLabel, left + 4, bottom - 10, 0x888888, false);
    }

    private void renderMailIcon(GuiGraphics g, int x, int y, int size,
                                boolean hasAttachments, boolean highlightUnread, boolean highlightAttachment) {
        int envelopeColor = hasAttachments ? 0xFFB0D9FF : 0xFFE7E7E7;
        int borderColor = hasAttachments ? 0xFF6AA9FF : 0xFFB5B5B5;
        int flapColor = hasAttachments ? 0xFF8FC4FF : 0xFFD8D8D8;

        if (highlightUnread) {
            g.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0x22FFD966);
        }

        g.fill(x, y + 4, x + size, y + size, envelopeColor);
        g.fill(x, y + 4, x + size, y + 5, flapColor);
        g.hLine(x, x + size, y + size, borderColor);
        g.hLine(x, x + size, y + 4, borderColor);
        g.vLine(x, y + 4, y + size, borderColor);
        g.vLine(x + size, y + 4, y + size, borderColor);

        g.hLine(x, x + size, y + 10, flapColor);
        g.hLine(x + 1, x + size - 1, y + 9, flapColor);

        g.hLine(x + 1, x + size - 1, y + 11, borderColor);
        g.hLine(x + 2, x + size - 2, y + 12, borderColor);

        g.hLine(x + 2, x + size - 2, y + 14, borderColor);
        g.hLine(x + 3, x + size - 3, y + 15, borderColor);

        if (highlightAttachment) {
            int clipLeft = x + size - 8;
            int clipTop = y + 2;
            g.fill(clipLeft, clipTop, clipLeft + 6, clipTop + 12, 0x44FFFFFF);
            g.hLine(clipLeft, clipLeft + 6, clipTop, 0xFFB5E0FF);
            g.vLine(clipLeft, clipTop, clipTop + 12, 0xFFB5E0FF);
            g.vLine(clipLeft + 6, clipTop, clipTop + 12, 0xFFB5E0FF);
            g.hLine(clipLeft, clipLeft + 6, clipTop + 12, 0xFFB5E0FF);
        }
    }
}
