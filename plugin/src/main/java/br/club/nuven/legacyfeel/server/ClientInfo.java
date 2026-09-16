package br.club.nuven.legacyfeel.server;

import java.util.Set;

public record ClientInfo(int handshakeVersion, String modVersion, String minecraftVersion,
                         String loader, Set<String> features) {}
