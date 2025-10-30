package top.atdove.dovemail.network.payload.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;

/**
 * 客户端请求：删除已读邮件（服务器将跳过仍有未领取附件的邮件）。
 */
public record ServerboundDeleteReadMailsPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundDeleteReadMailsPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "delete_read_mails"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDeleteReadMailsPayload> STREAM_CODEC =
            StreamCodec.unit(new ServerboundDeleteReadMailsPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return PACKET_TYPE; }
}
