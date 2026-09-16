package br.club.nuven.legacyfeel.network;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.HashSet;
import java.util.Set;

public final class HandshakeClient {
    private static volatile String serverMode = "SEM_SUPORTE";
    private static volatile boolean shieldOnSneak;
    private static volatile Set<String> forceOff = Set.of();
    private static int helloTicksRemaining;
    private static int retryDelay;

    private HandshakeClient() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE, (payload, context) -> accept(payload.json()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            serverMode = "AGUARDANDO";
            shieldOnSneak = false;
            forceOff = Set.of();
            helloTicksRemaining = 100;
            retryDelay = 0;
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
            shieldOnSneak = false;
            forceOff = Set.of();
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
            hello.addProperty("v", 1);
            hello.addProperty("mod", "0.1.0");
            hello.addProperty("mc", "26.2");
            hello.addProperty("loader", "fabric");
            hello.add("features", features);
            ClientPlayNetworking.send(new HandshakePayload(hello.toString()));
            helloTicksRemaining = 0;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // O registro do canal ainda não chegou; o retry é limitado a cinco segundos.
        }
    }

    private static void accept(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String type = root.has("t") ? root.get("t").getAsString() : "";
            if ("WELCOME".equals(type) || "POLICY".equals(type)) {
                serverMode = "MOD";
                if (root.has("rules") && root.get("rules").isJsonObject()) {
                    JsonObject rules = root.getAsJsonObject("rules");
                    if (rules.has("shieldOnSneak")) shieldOnSneak = rules.get("shieldOnSneak").getAsBoolean();
                }
                if (root.has("forceOff") && root.get("forceOff").isJsonArray()) {
                    Set<String> next = new HashSet<>();
                    root.getAsJsonArray("forceOff").forEach(element -> {
                        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                            next.add(element.getAsString());
                        }
                    });
                    forceOff = Set.copyOf(next);
                }
            }
        } catch (RuntimeException ignored) {
            // Payload inválido é ignorado; o cliente permanece utilizável sem suporte.
        }
    }

    public static String serverMode() { return serverMode; }
    public static boolean shieldOnSneak() { return shieldOnSneak; }
    public static boolean allows(String featureId) { return !forceOff.contains(featureId); }
}
