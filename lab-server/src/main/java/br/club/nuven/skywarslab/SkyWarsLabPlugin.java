package br.club.nuven.skywarslab;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SkyWarsLabPlugin extends JavaPlugin implements Listener {
    private ResourcePackGate packGate;
    private HandshakeBridge handshake;
    private LabSessionManager sessions;
    private DomainService domain;
    private ReverseService reverse;
    private MirageService mirage;
    private CosmeticsService cosmetics;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        LabItems items = new LabItems(this);
        packGate = new ResourcePackGate(this);
        if (!getServer().getPluginManager().isPluginEnabled("LegacyFeel-Server")) {
            handshake = new HandshakeBridge(this);
            handshake.enable();
        } else {
            getLogger().info("Handshake e PvP delegados ao LegacyFeel-Server.");
        }
        sessions = new LabSessionManager(packGate, items);
        domain = new DomainService(this, sessions, items);
        reverse = new ReverseService(this, sessions, items);
        mirage = new MirageService(this, sessions, items);
        cosmetics = new CosmeticsService(this, sessions);

        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(this, this);
        manager.registerEvents(packGate, this);
        manager.registerEvents(domain, this);
        manager.registerEvents(reverse, this);
        manager.registerEvents(mirage, this);
        manager.registerEvents(cosmetics, this);

        LabCommand command = new LabCommand(this, packGate, sessions, domain, reverse, mirage, cosmetics);
        Objects.requireNonNull(getCommand("swlab")).setExecutor(command);
        Objects.requireNonNull(getCommand("swlab")).setTabCompleter(command);
        getLogger().info("SkyWars Laboratório ativo • " + getConfig().getString("ruleset-id"));

        for (Player player : Bukkit.getOnlinePlayers()) packGate.send(player);
    }

    @Override
    public void onDisable() {
        cleanupAll();
        if (handshake != null) handshake.disable();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer());
        if (handshake != null) handshake.remove(event.getPlayer());
        sessions.remove(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cleanupPlayer(event.getEntity());
        sessions.leave(event.getEntity());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (sessions.isEnrolled(event.getPlayer())) {
            cleanupPlayer(event.getPlayer());
            sessions.leave(event.getPlayer());
        }
    }

    void cleanupPlayer(Player player) {
        domain.cleanupPlayer(player);
        reverse.cleanupPlayer(player);
        mirage.cleanupPlayer(player);
        cosmetics.cleanupPlayer(player);
    }

    void cleanupAll() {
        domain.cleanupAll();
        reverse.cleanupAll();
        mirage.cleanupAll();
        cosmetics.cleanupAll();
    }
}
