package br.club.nuven.legacyfeel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LegacyFeelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("legacyfeel.json");
    private static volatile LegacyFeelConfig instance = new LegacyFeelConfig();

    public boolean legacyPreset = true;
    public boolean instantSneakCamera = true;
    public String equipAnimation = "legacy";
    public int equipAnimationTicks = 3;
    public boolean lowFire = true;
    public float fireOverlayOffset = -0.3F;
    public boolean pvpAnimations = true;
    public boolean oldBlockHit = true;
    public boolean swingWhileUsing = true;
    public boolean oldSwing = true;
    public boolean oldEat = true;
    public boolean oldRod = true;
    public boolean oldBow = true;
    public boolean classicRodSounds = true;
    public boolean classicDamageTint = true;

    public static LegacyFeelConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                LegacyFeelConfig loaded = GSON.fromJson(reader, LegacyFeelConfig.class);
                if (loaded != null) instance = loaded;
            } catch (IOException | RuntimeException ignored) {
                instance = new LegacyFeelConfig();
            }
        }
        save();
    }

    public static void togglePreset() {
        instance.legacyPreset = !instance.legacyPreset;
        save();
    }

    public float equipAnimationStep() {
        if (!legacyPreset) return 0.4F;
        if ("instant".equalsIgnoreCase(equipAnimation)) return 1.0F;
        if ("ticks".equalsIgnoreCase(equipAnimation)) {
            return 1.0F / Math.max(1, Math.min(20, equipAnimationTicks));
        }
        return 0.4F;
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException ignored) {
            // A configuração em memória continua válida durante a sessão.
        }
    }
}
