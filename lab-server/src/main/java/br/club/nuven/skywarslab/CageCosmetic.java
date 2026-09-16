package br.club.nuven.skywarslab;

import java.util.Locale;
import java.util.Optional;

public enum CageCosmetic {
    PRISM("Prisma"), CRYO("Criocápsula"), CONFETTI("Cápsula de Confete"), SANCTUARY("Santuário do Domínio");

    private final String displayName;

    CageCosmetic(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<CageCosmetic> parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "prism", "prisma" -> Optional.of(PRISM);
            case "cryo", "criocapsula", "criocápsula", "gelo" -> Optional.of(CRYO);
            case "confetti", "confete", "foguete" -> Optional.of(CONFETTI);
            case "sanctuary", "santuario", "santuário", "dominio", "domínio" -> Optional.of(SANCTUARY);
            default -> Optional.empty();
        };
    }
}
