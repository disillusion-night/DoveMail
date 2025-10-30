package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        // 主面板
        int left = PADDING;
        int right = this.width - PADDING;
        int top = PADDING;
        int bottom = this.height - PADDING - 24;
        g.fill(left - 6, top - 6, right + 6, bottom + 6, 0x66000000);
        g.fill(left - 6, top - 6, right + 6, top - 5, 0x33FFFFFF);
        g.fill(left - 6, bottom + 5, right + 6, bottom + 6, 0x33000000);
        g.fill(left - 6, top - 6, left - 5, bottom + 6, 0x33FFFFFF);
        g.fill(right + 5, top - 6, right + 6, bottom + 6, 0x33000000);

        // 主题
        String subject = summary.getSubject() != null ? summary.getSubject() : "";
        int subjectY = top - 2;
        g.drawCenteredString(this.font, subject, this.width / 2, subjectY, 0xFFFFFF);

        // 正文区域
        int bodyTop = subjectY + 18;
        int bodyLeft = left;
        int bodyRight = right;
        int bodyWidth = Math.max(bodyRight - bodyLeft, 10);
        List<FormattedCharSequence> lines = this.font.split(summary.bodyComponent(), bodyWidth);
        int y = bodyTop;
        for (FormattedCharSequence line : lines) {
            g.drawString(this.font, line, bodyLeft, y, 0xDDDDDD, false);
            y += this.font.lineHeight + BODY_LINE_SPACING;
        }

        // 附件标题与网格
        int attachmentsAreaBottom = this.height - PADDING - 42; // 为按钮和发件人留空间
        int attachmentsTop = Math.max(y + 12, attachmentsAreaBottom - ATTACHMENT_SLOT_SIZE - 12);
        if (!attachments.isEmpty() || summary.hasAttachments()) {
            g.drawString(this.font, Component.translatable("screen.dovemail.mail.attachments"), bodyLeft, attachmentsTop - 12, 0xFFFFFF, false);

            int x = bodyLeft;
            int ySlots = attachmentsTop;
            for (ItemStack stack : attachments) {
                // 背板
                g.fill(x - 1, ySlots - 1, x + ATTACHMENT_SLOT_SIZE + 1, ySlots + ATTACHMENT_SLOT_SIZE + 1, 0x55333333);
                g.renderItem(stack, x, ySlots);
                g.renderItemDecorations(this.font, stack, x, ySlots);
                x += ATTACHMENT_SLOT_SIZE + ATTACHMENT_GAP;
                if (x + ATTACHMENT_SLOT_SIZE > bodyRight) {
                    x = bodyLeft;
                    ySlots += ATTACHMENT_SLOT_SIZE + ATTACHMENT_GAP;
                }
            }
        }

        // 发件人（底部居中）
        String sender = summary.getSenderName() != null && !summary.getSenderName().isEmpty() ? summary.getSenderName() : Component.translatable("screen.dovemail.mailbox.sender.unknown").getString();
        Component senderText = Component.translatable("screen.dovemail.mail.sender_prefix", sender);
        g.drawCenteredString(this.font, senderText, this.width / 2, this.height - PADDING - 12, 0xAAAAAA);
    }

    public java.util.UUID getMailId() {
        return summary.getId();
    }

    public void setAttachments(List<ItemStack> stacks) {
        this.attachments.clear();
        if (stacks != null) this.attachments.addAll(stacks);
    }
}
