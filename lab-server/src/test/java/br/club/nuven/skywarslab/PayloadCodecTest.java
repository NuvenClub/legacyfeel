package br.club.nuven.skywarslab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadCodecTest {
    @Test
    void roundTripsUtf8Handshake() {
        String json = "{\"t\":\"HELLO\",\"v\":2,\"kit\":\"Domínio\"}";
        assertEquals(json, PayloadCodec.decode(PayloadCodec.encode(json)));
    }

    @Test
    void rejectsPayloadWithMismatchedLength() {
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.decode(new byte[] { 5, '{', '}' }));
    }

    @Test
    void rejectsOversizedPayload() {
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.encode("x".repeat(8192)));
    }
}
