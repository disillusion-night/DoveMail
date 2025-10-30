package top.atdove.dovemail.network.payload.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;

/**
 * 提示客户端在邮箱界面时刷新邮箱（客户端自行决定是否在当前界面触发刷新）。
 */
public record ClientboundRefreshMailboxPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundRefreshMailboxPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "refresh_mailbox"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRefreshMailboxPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {}, buf -> new ClientboundRefreshMailboxPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
