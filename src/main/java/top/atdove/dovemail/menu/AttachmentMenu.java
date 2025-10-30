package top.atdove.dovemail.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;

public class AttachmentMenu extends AbstractContainerMenu {
    public static final int ROWS = 3;
    public static final int COLUMNS = 3;
    private final Container attachments;

    public AttachmentMenu(int containerId, Inventory playerInv, Container attachments) {
        super(top.atdove.dovemail.init.ModMenus.ATTACHMENTS.get(), containerId);
        this.attachments = attachments != null ? attachments : new SimpleContainer(ROWS * COLUMNS);

        // Attachments grid (3x3)
        int left = 62; // center-ish
        int top = 17;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLUMNS; c++) {
                int index = r * COLUMNS + c;
                this.addSlot(new Slot(this.attachments, index, left + c * 18, top + r * 18));
            }
        }

        // Player inventory
        int invLeft = 8;
        int invTop = 84;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, invLeft + j * 18, invTop + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInv, k, invLeft + k * 18, invTop + 58));
        }
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            int attachmentsSize = ROWS * COLUMNS;
            if (index < attachmentsSize) {
                if (!this.moveItemStackTo(stackInSlot, attachmentsSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stackInSlot, 0, attachmentsSize, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}
