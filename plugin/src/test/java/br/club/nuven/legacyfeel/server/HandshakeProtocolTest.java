package br.club.nuven.legacyfeel.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandshakeProtocolTest {
    @Test void keepsV1Compatibility() {
        assertEquals(1, HandshakeProtocol.negotiate(1));
    }

    @Test void negotiatesV2() {
        assertEquals(2, HandshakeProtocol.negotiate(2));
    }

    @Test void rejectsUnsupportedVersions() {
        assertEquals(0, HandshakeProtocol.negotiate(0));
        assertEquals(0, HandshakeProtocol.negotiate(3));
    }
}
