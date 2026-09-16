package br.club.nuven.skywarslab;

import java.util.Locale;
import java.util.Optional;

public enum ProjectileCosmetic {
    FLAMES("Chamas"), NINJA("Ninja"), MYSTIC("Místico"), CLOUD("Nuven");

    private final String displayName;

    ProjectileCosmetic(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<ProjectileCosmetic> parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "flames", "chamas" -> Optional.of(FLAMES);
            case "ninja" -> Optional.of(NINJA);
            case "mystic", "mistico", "místico" -> Optional.of(MYSTIC);
            case "cloud", "nuven", "nuvem" -> Optional.of(CLOUD);
            default -> Optional.empty();
        };
    }
}
