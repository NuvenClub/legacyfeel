package br.club.nuven.legacyfeel.network;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.HashSet;
import java.util.Set;

public final class HandshakeClient {
    private static volatile String serverMode = "SEM_SUPORTE";
    private static volatile String serverProfile = "VANILLA_SAFE";
    private static volatile String rulesetId = "none";
    private static volatile int protocolVersion;
    private static volatile boolean shieldOnSneak;
    private static volatile Set<String> forceOff = Set.of();
    private static volatile Set<String> capabilities = Set.of();
    private static int helloTicksRemaining;
    private static int retryDelay;
    private static int nextHelloVersion;

    private HandshakeClient() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE, (payload, context) -> accept(payload.json()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            serverMode = "AGUARDANDO";
            serverProfile = "VANILLA_SAFE";
            rulesetId = "none";
            protocolVersion = 0;
            shieldOnSneak = false;
            forceOff = Set.of();
            capabilities = Set.of();
            // Em redes Bungee/Velocity o backend muda sem uma nova conexão de jogo.
            // Mantemos um HELLO leve periódico para o servidor de destino negociar a política.
            helloTicksRemaining = Integer.MAX_VALUE;
            retryDelay = 0;
            nextHelloVersion = 2;
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (helloTicksRemaining <= 0) return;
            helloTicksRemaining--;
            if (retryDelay-- > 0) return;
            retryDelay = 5;
            trySendHello();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverMode = "SEM_SUPORTE";
            serverProfile = "VANILLA_SAFE";
            rulesetId = "none";
            protocolVersion = 0;
            shieldOnSneak = false;
            forceOff = Set.of();
            capabilities = Set.of();
            helloTicksRemaining = 0;
        });
    }

    private static void trySendHello() {
        try {
            if (!ClientPlayNetworking.canSend(HandshakePayload.TYPE)) return;
            JsonObject features = new JsonObject();
            features.addProperty("instantSneakCamera", LegacyFeelConfig.get().instantSneakCamera);
            features.addProperty("fastEquip", LegacyFeelConfig.get().legacyPreset);
            features.addProperty("legacyCrouch", LegacyFeelConfig.get().legacyPreset);
            features.addProperty("legacyCombat", LegacyFeelConfig.get().legacyPreset);
            JsonObject hello = new JsonObject();
            hello.addProperty("t", "HELLO");
            hello.addProperty("v", nextHelloVersion);
            if (nextHelloVersion >= 2) hello.addProperty("minV", 1);
            hello.addProperty("mod", "0.1.0");
            hello.addProperty("mc", "26.2");
            hello.addProperty("loader", "fabric");
            hello.add("features", features);
            ClientPlayNetworking.send(new HandshakePayload(hello.toString()));
            if (nextHelloVersion >= 2) {
                // Um servidor v1 registra o primeiro HELLO antes de rejeitar a versão.
                // Aguarde além do rate limit de dois segundos antes do fallback.
                nextHelloVersion = 1;
                retryDelay = 50;
            } else {
                retryDelay = 100;
            }
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // O registro do canal ainda não chegou; o retry é limitado a cinco segundos.
        }
    }

    private static void accept(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String type = root.has("t") ? root.get("t").getAsString() : "";
            if ("WELCOME".equals(type) || "POLICY".equals(type)) {
                boolean firstWelcome = "WELCOME".equals(type) && !"MOD".equals(serverMode);
                serverMode = "MOD";
                protocolVersion = root.has("v") ? root.get("v").getAsInt() : 1;
                if (root.has("serverProfile")) serverProfile = root.get("serverProfile").getAsString();
                else if (firstWelcome) serverProfile = "MODERN_LEGACY_COMBAT";
                if (root.has("rulesetId")) rulesetId = root.get("rulesetId").getAsString();
                if (root.has("capabilities") && root.get("capabilities").isJsonArray()) {
                    capabilities = readStringSet(root, "capabilities");
                }
                if (root.has("rules") && root.get("rules").isJsonObject()) {
                    JsonObject rules = root.getAsJsonObject("rules");
                    if (rules.has("shieldOnSneak")) shieldOnSneak = rules.get("shieldOnSneak").getAsBoolean();
                }
                if (root.has("forceOff") && root.get("forceOff").isJsonArray()) {
                    forceOff = readStringSet(root, "forceOff");
                }
                nextHelloVersion = protocolVersion >= 2 ? 2 : 1;
                retryDelay = 100;
                if (firstWelcome) {
                    Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.hud.setOverlayMessage(
                        Component.translatable("legacyfeel.status.active", serverProfile, rulesetId), false));
                }
            }
        } catch (RuntimeException ignored) {
            // Payload inválido é ignorado; o cliente permanece utilizável sem suporte.
        }
    }

    private static Set<String> readStringSet(JsonObject root, String key) {
        Set<String> next = new HashSet<>();
        root.getAsJsonArray(key).forEach(element -> {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) next.add(element.getAsString());
        });
        return Set.copyOf(next);
    }

    public static String serverMode() { return serverMode; }
    public static String serverProfile() { return serverProfile; }
    public static String rulesetId() { return rulesetId; }
    public static int protocolVersion() { return protocolVersion; }
    public static boolean shieldOnSneak() { return shieldOnSneak; }
    public static boolean hasCapability(String capability) { return capabilities.contains(capability); }
    public static boolean allows(String featureId) { return !forceOff.contains(featureId); }
}
