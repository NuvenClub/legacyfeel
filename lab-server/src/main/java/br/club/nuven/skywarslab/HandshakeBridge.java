package br.club.nuven.skywarslab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class HandshakeBridge {
    static final String CHANNEL = "legacyfeel:handshake";
    private final SkyWarsLabPlugin plugin;
    private final Map<UUID, Integer> negotiatedVersions = new ConcurrentHashMap<>();

    HandshakeBridge(SkyWarsLabPlugin plugin) {
        this.plugin = plugin;
    }

    void enable() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this::receive);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    void disable() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
        negotiatedVersions.clear();
    }

    void remove(Player player) {
        negotiatedVersions.remove(player.getUniqueId());
    }

    boolean isLegacyFeelClient(Player player) {
        return negotiatedVersions.containsKey(player.getUniqueId());
    }

    private void receive(String channel, Player player, byte[] bytes) {
        if (!CHANNEL.equals(channel) || bytes.length > 8194) return;
        try {
            JsonObject hello = JsonParser.parseString(PayloadCodec.decode(bytes)).getAsJsonObject();
            if (!"HELLO".equals(hello.has("t") ? hello.get("t").getAsString() : "")) return;
            int requested = hello.has("v") ? hello.get("v").getAsInt() : 1;
            if (requested < 1 || requested > 2) return;
            negotiatedVersions.put(player.getUniqueId(), requested);
            if (!player.getListeningPluginChannels().contains(CHANNEL)) return;
            player.sendPluginMessage(plugin, CHANNEL, PayloadCodec.encode(welcome(requested).toString()));
        } catch (RuntimeException ignored) {
            // Cliente sem protocolo válido continua podendo usar o pacote vanilla.
        }
    }

    private JsonObject welcome(int version) {
        JsonArray capabilities = new JsonArray();
        capabilities.add("labKitsV1");
        capabilities.add("labDisplaysV1");
        capabilities.add("labAudioV1");
        capabilities.add("labCosmeticsV1");
        capabilities.add("labPackV1");

        JsonObject rules = new JsonObject();
        rules.addProperty("shieldOnSneak", false);
        rules.addProperty("armorSwap", true);
        rules.addProperty("resourcePackRequired", plugin.getConfig().getBoolean("resource-pack.required", true));

        JsonObject payload = new JsonObject();
        payload.addProperty("t", "WELCOME");
        payload.addProperty("v", version);
        payload.addProperty("server", "NuvenClub");
        payload.addProperty("plugin", plugin.getPluginMeta().getVersion());
        payload.addProperty("serverProfile", "SKYWARS_LAB");
        payload.addProperty("rulesetId", plugin.getConfig().getString("ruleset-id", "skywars-lab-pilot-v1"));
        payload.addProperty("resourcePackId", plugin.getConfig().getString("resource-pack.id", ""));
        payload.addProperty("visualCatalog", "skywars-lab-pilot-v1");
        payload.add("capabilities", capabilities);
        payload.add("rules", rules);
        return payload;
    }
}
