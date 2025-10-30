package top.atdove.dovemail.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 邮件详情（仅附件清单，下发到客户端）。
 */
public record ClientboundMailDetailPacket(UUID mailId, List<ItemStack> attachments) {
    public static ClientboundMailDetailPacket decode(FriendlyByteBuf buf, HolderLookup.Provider provider) {
        UUID id = buf.readUUID();
        int size = buf.readVarInt();
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag == null) tag = new CompoundTag();
            ItemStack stack = ItemStack.parseOptional(provider, tag);
            list.add(stack);
        }
        return new ClientboundMailDetailPacket(id, list);
    }

    public void encode(FriendlyByteBuf buf, HolderLookup.Provider provider) {
        buf.writeUUID(mailId);
        buf.writeVarInt(attachments.size());
        for (ItemStack stack : attachments) {
            CompoundTag tag = (CompoundTag) stack.save(provider);
            buf.writeNbt(tag != null ? tag : new CompoundTag());
        }
    }
}
