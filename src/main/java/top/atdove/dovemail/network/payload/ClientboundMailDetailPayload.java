package top.atdove.dovemail.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.atdove.dovemail.Dovemail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ClientboundMailDetailPayload(UUID mailId, List<ItemStack> attachments) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundMailDetailPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "mail_detail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMailDetailPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                        buf.writeUtf(payload.mailId.toString());
                        List<ItemStack> list = payload.attachments;
                        buf.writeVarInt(list.size());
                        for (ItemStack stack : list) {
                            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                        }
                    },
                    buf -> {
                        UUID id = UUID.fromString(buf.readUtf(32767));
                        int size = buf.readVarInt();
                        List<ItemStack> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            list.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                        }
                        return new ClientboundMailDetailPayload(id, list);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
