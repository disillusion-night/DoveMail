package top.atdove.dovemail.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.atdove.dovemail.Dovemail;
import top.atdove.dovemail.mail.MailSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ClientboundOpenMailDetailPayload(MailSummary summary, List<ItemStack> attachments) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundOpenMailDetailPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "open_mail_detail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenMailDetailPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                        MailSummary s = payload.summary();
                        buf.writeUtf(s.getId().toString());
                        buf.writeUtf(s.getSubject());
                        buf.writeUtf(s.getBodyJson());
                        buf.writeUtf(s.getSenderName() == null ? "" : s.getSenderName());
                        buf.writeLong(s.getTimestamp());
                        buf.writeBoolean(s.read());
                        buf.writeBoolean(s.isAttachmentsClaimed());
                        buf.writeBoolean(s.hasAttachments());
                        List<ItemStack> list = payload.attachments();
                        buf.writeVarInt(list.size());
                        for (ItemStack stack : list) {
                            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                        }
                    },
                    buf -> {
                        UUID id = UUID.fromString(buf.readUtf(32767));
                        String subject = buf.readUtf(32767);
                        String bodyJson = buf.readUtf(32767);
                        String sender = buf.readUtf(32767);
                        long ts = buf.readLong();
                        boolean read = buf.readBoolean();
                        boolean claimed = buf.readBoolean();
                        boolean hasAtt = buf.readBoolean();
                        int size = buf.readVarInt();
                        List<ItemStack> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            list.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                        }
                        return new ClientboundOpenMailDetailPayload(
                                new top.atdove.dovemail.mail.MailSummary(id, subject, bodyJson, sender, ts, read, claimed, hasAtt),
                                list
                        );
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
