package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import top.atdove.dovemail.mail.MailSummary;
import top.atdove.dovemail.network.DovemailNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 邮件详情界面：显示主题（大号字体）、正文（JSON 文本渲染）、附件清单、领取按钮与发件人。
 */
public class MailDetailScreen extends Screen {
    private static final int PADDING = 16;
    private static final int BODY_LINE_SPACING = 2;
    private static final int ATTACHMENT_SLOT_SIZE = 18;
    private static final int ATTACHMENT_GAP = 6;

    private final MailSummary summary;
    private final List<ItemStack> attachments;
    private final Consumer<MailSummary> claimAction; // 回调由外部注入：负责发包并在服务端标记与发放

    private Button claimButton;
    private int scrollY = 0;
    private int maxScrollY = 0;

    public MailDetailScreen(MailSummary summary, List<ItemStack> attachments, Consumer<MailSummary> claimAction) {
        super(Component.translatable("screen.dovemail.mail.detail"));
        this.summary = summary;
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        this.claimAction = claimAction;
    }

    @Override
    protected void init() {
        super.init();

        boolean canClaim = summary.hasAttachments() && !summary.isAttachmentsClaimed();
        int btnWidth = 100;
        int btnHeight = 20;
        int btnY = this.height - PADDING - btnHeight - 14; // 留出发件人文字
        claimButton = Button.builder(Component.translatable("button.dovemail.claim"), b -> onClaim())
                .pos(this.width / 2 - btnWidth / 2, btnY)
                .size(btnWidth, btnHeight)
                .build();
        claimButton.active = canClaim;
        if (summary.hasAttachments()) {
            addRenderableWidget(claimButton);
        }

        // 打开时请求服务端下发附件详情
        DovemailNetwork.requestMailDetail(summary.getId());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@javax.annotation.Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);
    }

    private void onClaim() {
        if (claimAction != null) {
            claimAction.accept(summary);
        }
        DovemailNetwork.claimAttachments(summary.getId());
        // 本地立即禁用按钮，避免重复点击；真正状态由服务端回写同步
        if (claimButton != null) {
            claimButton.active = false;
        }
    }

    @Override
    public void render(@javax.annotation.Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        int left = PADDING;
        int right = this.width - PADDING;
        int top = PADDING;
        int bottom = this.height - PADDING - 24;
        renderPanel(g, left, right, top, bottom);

        String subject = summary.getSubject() != null ? summary.getSubject() : "";
        int subjectY = top - 2;
        g.drawCenteredString(this.font, subject, this.width / 2, subjectY, 0xFFFFFF);

        int bodyTop = subjectY + 18;
        int bodyLeft = left;
        int bodyRight = right;
        int bodyWidth = Math.max(bodyRight - bodyLeft, 10);
        int viewportBottom = (claimButton != null ? claimButton.getY() : (this.height - PADDING - 36)) - 12;
        int viewportTop = bodyTop;
        int viewportHeight = Math.max(0, viewportBottom - viewportTop);

        List<FormattedCharSequence> lines = this.font.split(summary.bodyComponent(), bodyWidth);
        int contentY = renderBody(g, lines, bodyLeft, viewportTop, viewportBottom);
        contentY = renderAttachments(g, bodyLeft, bodyRight, viewportTop, viewportBottom, contentY);

        int contentHeight = contentY - (viewportTop - scrollY);
        maxScrollY = Math.max(0, contentHeight - viewportHeight);
        if (scrollY > maxScrollY) scrollY = maxScrollY;

        renderScrollbar(g, right, viewportTop, viewportBottom, viewportHeight, contentHeight);

        // 发件人（底部居中）
        String rawSender = summary.getSenderName() != null && !summary.getSenderName().isEmpty() ? summary.getSenderName() : null;
        String sender;
        if ("System".equals(rawSender)) {
            sender = Component.translatable("sender.dovemail.system").getString();
        } else if (rawSender == null) {
            sender = Component.translatable("screen.dovemail.mailbox.sender.unknown").getString();
        } else {
            sender = rawSender;
        }
        Component senderText = Component.translatable("screen.dovemail.mail.sender_prefix", sender);
        g.drawCenteredString(this.font, senderText, this.width / 2, this.height - PADDING - 12, 0xAAAAAA);
    }

    private void renderPanel(GuiGraphics g, int left, int right, int top, int bottom) {
        g.fill(left - 6, top - 6, right + 6, bottom + 6, 0x66000000);
        g.fill(left - 6, top - 6, right + 6, top - 5, 0x33FFFFFF);
        g.fill(left - 6, bottom + 5, right + 6, bottom + 6, 0x33000000);
        g.fill(left - 6, top - 6, left - 5, bottom + 6, 0x33FFFFFF);
        g.fill(right + 5, top - 6, right + 6, bottom + 6, 0x33000000);
    }

    private int renderBody(GuiGraphics g, List<FormattedCharSequence> lines, int bodyLeft, int viewportTop, int viewportBottom) {
        int contentY = viewportTop - scrollY;
        int lineHeight = this.font.lineHeight + BODY_LINE_SPACING;
        for (FormattedCharSequence line : lines) {
            if (contentY + this.font.lineHeight >= viewportTop && contentY <= viewportBottom) {
                g.drawString(this.font, line, bodyLeft, contentY, 0xDDDDDD, false);
            }
            contentY += lineHeight;
        }
        return contentY;
    }

    private int renderAttachments(GuiGraphics g, int bodyLeft, int bodyRight, int viewportTop, int viewportBottom, int contentYStart) {
        int contentY = contentYStart;
        int attachmentsTop = contentY + 12;
        if (!attachments.isEmpty() || summary.hasAttachments()) {
            if (attachmentsTop - 12 + this.font.lineHeight >= viewportTop && attachmentsTop - 12 <= viewportBottom) {
                g.drawString(this.font, Component.translatable("screen.dovemail.mail.attachments"), bodyLeft, attachmentsTop - 12, 0xFFFFFF, false);
            }
            int x = bodyLeft;
            int ySlots = attachmentsTop;
            int slotSize = ATTACHMENT_SLOT_SIZE + ATTACHMENT_GAP;
            for (ItemStack stack : attachments) {
                if (ySlots + ATTACHMENT_SLOT_SIZE >= viewportTop && ySlots <= viewportBottom) {
                    g.fill(x - 1, ySlots - 1, x + ATTACHMENT_SLOT_SIZE + 1, ySlots + ATTACHMENT_SLOT_SIZE + 1, 0x55333333);
                    g.renderItem(stack, x, ySlots);
                    g.renderItemDecorations(this.font, stack, x, ySlots);
                }
                x += slotSize;
                if (x + ATTACHMENT_SLOT_SIZE > bodyRight) {
                    x = bodyLeft;
                    ySlots += slotSize;
                }
            }
            contentY = Math.max(contentY, ySlots + ATTACHMENT_SLOT_SIZE);
        }
        return contentY;
    }

    private void renderScrollbar(GuiGraphics g, int right, int viewportTop, int viewportBottom, int viewportHeight, int contentHeight) {
        if (maxScrollY <= 0) return;
        int trackLeft = right + 2;
        int trackRight = trackLeft + 4;
        int trackTop = viewportTop;
        int trackBottom = viewportBottom;
        g.fill(trackLeft, trackTop, trackRight, trackBottom, 0x33000000);
        int thumbHeight = Math.max(8, (int) ((viewportHeight / (float) (contentHeight)) * (trackBottom - trackTop)));
        int thumbTop = trackTop + (int) ((scrollY / (float) maxScrollY) * (trackBottom - trackTop - thumbHeight));
        g.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, 0x88FFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int step = deltaY > 0 ? -14 : 14;
        int old = scrollY;
        scrollY = Mth.clamp(scrollY + step, 0, Math.max(0, maxScrollY));
        return scrollY != old || super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    public java.util.UUID getMailId() {
        return summary.getId();
    }

    public void setAttachments(List<ItemStack> stacks) {
        this.attachments.clear();
        if (stacks != null) this.attachments.addAll(stacks);
    }
}
