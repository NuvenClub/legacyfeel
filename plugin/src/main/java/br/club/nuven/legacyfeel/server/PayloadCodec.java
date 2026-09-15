package br.club.nuven.legacyfeel.server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class PayloadCodec {
    private PayloadCodec() {}

    static String decode(byte[] payload) {
        int value = 0;
        int position = 0;
        int index = 0;
        byte current;
        do {
            if (index >= payload.length || position >= 35) throw new IllegalArgumentException("VarInt inválido");
            current = payload[index++];
            value |= (current & 0x7F) << position;
            position += 7;
        } while ((current & 0x80) != 0);
        if (value < 0 || value > 8191 || value != payload.length - index) throw new IllegalArgumentException("Tamanho inválido");
        return new String(payload, index, value, StandardCharsets.UTF_8);
    }

    static byte[] encode(String json) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        if (body.length > 8191) throw new IllegalArgumentException("Payload grande demais");
        ByteArrayOutputStream out = new ByteArrayOutputStream(body.length + 3);
        int value = body.length;
        do {
            int part = value & 0x7F;
            value >>>= 7;
            if (value != 0) part |= 0x80;
            out.write(part);
        } while (value != 0);
        out.writeBytes(body);
        return out.toByteArray();
    }
}
