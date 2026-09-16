package br.club.nuven.skywarslab;

import java.util.Locale;
import java.util.Optional;

public enum KitType {
    DOMAIN("Domínio"),
    REVERSE("Reverso"),
    MIRAGE("Mirage");

    private final String displayName;

    KitType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<KitType> parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "domain", "dominio", "domínio" -> Optional.of(DOMAIN);
            case "reverse", "reverso", "inversao", "inversão" -> Optional.of(REVERSE);
            case "mirage" -> Optional.of(MIRAGE);
            default -> Optional.empty();
        };
    }
}
