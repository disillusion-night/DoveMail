package top.atdove.dovemail.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class ComposeSettingsScreen extends Screen {
    private final Screen parent;
    private final Consumer<Boolean> onSystemChanged;
    private final Consumer<Boolean> onAnnouncementChanged;
    private boolean sendAsSystem;
    private boolean sendAsAnnouncement;

    private Checkbox systemCheckbox;
    private Checkbox announcementCheckbox;

    public ComposeSettingsScreen(Screen parent,
                                 Consumer<Boolean> onSystemChanged,
                                 Consumer<Boolean> onAnnouncementChanged,
                                 boolean sendAsSystem,
                                 boolean sendAsAnnouncement) {
        super(Component.translatable("screen.dovemail.compose.settings"));
        this.parent = parent;
        this.onSystemChanged = onSystemChanged;
        this.onAnnouncementChanged = onAnnouncementChanged;
        this.sendAsSystem = sendAsSystem;
        this.sendAsAnnouncement = sendAsAnnouncement;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 30;

        systemCheckbox = Checkbox.builder(Component.translatable("option.dovemail.send_as_system"), this.font)
                .pos(centerX - 120, y)
                .selected(sendAsSystem)
                .onValueChange((cb, value) -> {
                    sendAsSystem = value;
                    if (!sendAsSystem) {
                        sendAsAnnouncement = false;
                        if (announcementCheckbox != null) announcementCheckbox.setSelected(false);
                    }
                    updateAnnouncementEnabled();
                })
                .build();
        addRenderableWidget(systemCheckbox);
        y += 24;

        announcementCheckbox = Checkbox.builder(Component.translatable("option.dovemail.send_as_announcement"), this.font)
                .pos(centerX - 120, y)
                .selected(sendAsAnnouncement)
                .build();
        addRenderableWidget(announcementCheckbox);
        updateAnnouncementEnabled();
        y += 30;

        Button done = Button.builder(Component.translatable("gui.done"), btn -> applyAndClose())
                .pos(centerX - 100, y)
                .size(80, 20)
                .build();
        Button cancel = Button.builder(Component.translatable("gui.cancel"), btn -> onClose())
                .pos(centerX + 20, y)
                .size(80, 20)
                .build();
        addRenderableWidget(done);
        addRenderableWidget(cancel);
    }

    private void updateAnnouncementEnabled() {
        if (announcementCheckbox != null) {
            announcementCheckbox.active = sendAsSystem;
        }
    }

    private void applyAndClose() {
        if (onSystemChanged != null) onSystemChanged.accept(systemCheckbox.selected());
        if (onAnnouncementChanged != null) onAnnouncementChanged.accept(announcementCheckbox.selected());
        onClose();
    }

    @Override
    public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
}
