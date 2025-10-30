package top.atdove.dovemail.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;

public record ClientboundUnreadHintPayload(int count) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundUnreadHintPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "unread_hint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUnreadHintPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> buf.writeVarInt(payload.count()),
                    buf -> new ClientboundUnreadHintPayload(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
