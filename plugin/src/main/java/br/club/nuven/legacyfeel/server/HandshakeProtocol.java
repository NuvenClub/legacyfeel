package br.club.nuven.legacyfeel.server;

final class HandshakeProtocol {
    static final int CURRENT = 2;
    static final int MINIMUM = 1;

    private HandshakeProtocol() {}

    static int negotiate(int requested) {
        if (requested < MINIMUM || requested > CURRENT) return 0;
        return Math.min(CURRENT, requested);
    }
}
