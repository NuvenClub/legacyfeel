package br.club.nuven.legacyfeel.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HandshakePayload(String json) implements CustomPacketPayload {
    public static final Type<HandshakePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("legacyfeel", "handshake"));
    public static final StreamCodec<FriendlyByteBuf, HandshakePayload> CODEC =
        ByteBufCodecs.stringUtf8(8191).map(HandshakePayload::new, HandshakePayload::json).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
