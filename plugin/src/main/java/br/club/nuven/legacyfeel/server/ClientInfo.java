package br.club.nuven.legacyfeel.server;

import java.util.Set;

public record ClientInfo(String modVersion, String minecraftVersion, Set<String> features) {}
