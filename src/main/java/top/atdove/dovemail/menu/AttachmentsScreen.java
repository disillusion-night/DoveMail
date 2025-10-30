package top.atdove.dovemail.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import javax.annotation.Nonnull;

public class AttachmentsScreen extends AbstractContainerScreen<AttachmentMenu> {
    private static final ResourceLocation DISPENSER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    public AttachmentsScreen(AttachmentMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    // no top-right close button; ESC will close and return to compose via onClose()

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        guiGraphics.blit(DISPENSER_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        super.onClose();
        // Return to compose screen if we have a snapshot
        var mc = this.minecraft;
        if (mc != null && top.atdove.dovemail.client.ComposeState.hasSnapshot()) {
            var screen = top.atdove.dovemail.client.ComposeState.restoreAndClear();
            mc.setScreen(screen);
        }
    }
}
