package br.club.nuven.legacyfeel.network;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class HandshakeClient {
    private static volatile String serverMode = "SEM_SUPORTE";
    private static volatile boolean shieldOnSneak;

    private HandshakeClient() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE, (payload, context) -> accept(payload.json()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            serverMode = "AGUARDANDO";
            shieldOnSneak = false;
            JsonObject features = new JsonObject();
            features.addProperty("instantSneakCamera", LegacyFeelConfig.get().instantSneakCamera);
            JsonObject hello = new JsonObject();
            hello.addProperty("t", "HELLO");
            hello.addProperty("v", 1);
            hello.addProperty("mod", "0.1.0");
            hello.addProperty("mc", "26.2");
            hello.addProperty("loader", "fabric");
            hello.add("features", features);
            ClientPlayNetworking.send(new HandshakePayload(hello.toString()));
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverMode = "SEM_SUPORTE";
            shieldOnSneak = false;
        });
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
            }
        } catch (RuntimeException ignored) {
            // Payload inválido é ignorado; o cliente permanece utilizável sem suporte.
        }
    }

    public static String serverMode() { return serverMode; }
    public static boolean shieldOnSneak() { return shieldOnSneak; }
}
