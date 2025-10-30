package top.atdove.dovemail.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.atdove.dovemail.Dovemail;

import java.util.ArrayList;
import java.util.List;

public record ClientboundUiAlertPayload(String key, List<String> args) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundUiAlertPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dovemail.MODID, "ui_alert"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUiAlertPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                        buf.writeUtf(payload.key(), 32767);
                        List<String> as = payload.args();
                        buf.writeVarInt(as.size());
                        for (String s : as) buf.writeUtf(s, 32767);
                    },
                    buf -> {
                        String k = buf.readUtf(32767);
                        int n = buf.readVarInt();
                        List<String> as = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) as.add(buf.readUtf(32767));
                        return new ClientboundUiAlertPayload(k, as);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
