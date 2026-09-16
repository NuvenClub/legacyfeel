package br.club.nuven.skywarslab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

final class ResourcePackGate implements Listener {
    enum GateState { NOT_SENT, LOADING, READY, FAILED }

    private static final Pattern SHA1 = Pattern.compile("[0-9a-fA-F]{40}");

    private final SkyWarsLabPlugin plugin;
    private final Map<UUID, GateState> states = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final boolean required;
    private final UUID packId;
    private final String url;
    private final String sha1;
    private final Component prompt;
    private final boolean validConfiguration;

    ResourcePackGate(SkyWarsLabPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("resource-pack.enabled", true);
        this.required = config.getBoolean("resource-pack.required", true);
        this.url = config.getString("resource-pack.url", "").trim();
        this.sha1 = config.getString("resource-pack.sha1", "").trim();
        this.prompt = Component.text(config.getString("resource-pack.prompt",
                "O SkyWars Laboratório precisa de seu pacote visual."), NamedTextColor.LIGHT_PURPLE);

        UUID parsedId = null;
        try {
            parsedId = UUID.fromString(config.getString("resource-pack.id", ""));
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().severe("resource-pack.id não é um UUID válido.");
        }
        this.packId = parsedId;
        this.validConfiguration = enabled && packId != null && !url.isBlank() && SHA1.matcher(sha1).matches();
        if (!validConfiguration) {
            plugin.getLogger().warning("Pacote obrigatório ainda não está configurado. Execute pack/build-pack.ps1 e atualize resource-pack.sha1.");
        }
    }

    boolean isReady(Player player) {
        return states.getOrDefault(player.getUniqueId(), GateState.NOT_SENT) == GateState.READY;
    }

    GateState state(Player player) {
        return states.getOrDefault(player.getUniqueId(), GateState.NOT_SENT);
    }

    boolean canSend() {
        return validConfiguration;
    }

    void send(Player player) {
        if (!validConfiguration) {
            states.put(player.getUniqueId(), GateState.FAILED);
            player.sendMessage(Component.text("O pacote do Laboratório ainda não foi configurado neste servidor.", NamedTextColor.RED));
            return;
        }
        states.put(player.getUniqueId(), GateState.LOADING);
        player.setResourcePack(packId, url, hexToBytes(sha1), prompt, required);
        player.sendMessage(Component.text("Preparando os modelos e sons do SkyWars Laboratório…", NamedTextColor.GRAY));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        states.put(event.getPlayer().getUniqueId(), GateState.NOT_SENT);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) send(event.getPlayer());
        }, 10L);
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        if (packId == null || !packId.equals(event.getID())) return;
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> {
                states.put(player.getUniqueId(), GateState.READY);
                player.sendMessage(Component.text("Pacote carregado. O Laboratório está disponível.", NamedTextColor.GREEN));
            }
            case ACCEPTED, DOWNLOADED -> states.put(player.getUniqueId(), GateState.LOADING);
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> {
                states.put(player.getUniqueId(), GateState.FAILED);
                if (!required) {
                    player.sendMessage(Component.text("Não foi possível carregar o pacote do Laboratório.", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    private static byte[] hexToBytes(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return bytes;
    }
}
