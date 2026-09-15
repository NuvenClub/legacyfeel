package br.club.nuven.legacyfeel.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadCodecTest {
    @Test void roundTripUtf8() {
        String json = "{\"t\":\"OLÁ\",\"v\":1}";
        assertEquals(json, PayloadCodec.decode(PayloadCodec.encode(json)));
    }

    @Test void rejectsLengthMismatch() {
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.decode(new byte[] {2, '{'}));
    }
}
