package br.club.nuven.legacyfeel.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LegacyFeelPlugin extends JavaPlugin implements Listener {
    public static final String CHANNEL = "legacyfeel:handshake";
    private final Map<UUID, ClientMode> modes = new HashMap<>();
    private final Map<UUID, ClientInfo> clients = new HashMap<>();
    private final Map<UUID, Long> lastHello = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this::receive);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("LegacyFeel quick lab ativo; shield-on-sneak permanece experimental e desligado.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        modes.clear();
        clients.clear();
        lastHello.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        modes.put(player.getUniqueId(), ClientMode.UNKNOWN);
        applyAttackSpeed(player);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline() || modes.get(player.getUniqueId()) != ClientMode.UNKNOWN) return;
            modes.put(player.getUniqueId(), resolveWithoutMod(player));
        }, 100L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        modes.remove(id);
        clients.remove(id);
        lastHello.remove(id);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSweep(EntityDamageEvent event) {
        if (getConfig().getBoolean("combat.enabled") && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLegacyBlock(EntityDamageByEntityEvent event) {
        if (!getConfig().getBoolean("combat.enabled") || !getConfig().getBoolean("combat.legacy-blocking")) return;
        if (!(event.getEntity() instanceof Player victim) || !victim.isBlocking()) return;
        ItemStack active = victim.getActiveItem();
        if (!isSword(active.getType())) return;
        double original = event.getDamage();
        event.setDamage((original + 1.0D) / 2.0D);
        if (getConfig().getBoolean("debug")) {
            getLogger().info("block victim=" + victim.getName() + " before=" + original + " after=" + event.getDamage());
        }
    }

    private void receive(String channel, Player player, byte[] bytes) {
        if (!CHANNEL.equals(channel) || bytes.length > 8194) return;
        long now = System.currentTimeMillis();
        long previous = lastHello.getOrDefault(player.getUniqueId(), 0L);
        if (now - previous < 2000L) return;
        lastHello.put(player.getUniqueId(), now);
        try {
            JsonObject root = JsonParser.parseString(PayloadCodec.decode(bytes)).getAsJsonObject();
            if (!"HELLO".equals(root.get("t").getAsString()) || root.get("v").getAsInt() != 1) return;
            Set<String> features = new HashSet<>();
            if (root.has("features") && root.get("features").isJsonObject()) {
                root.getAsJsonObject("features").entrySet().stream()
                    .filter(entry -> entry.getValue().isJsonPrimitive() && entry.getValue().getAsBoolean())
                    .forEach(entry -> features.add(entry.getKey()));
            }
            clients.put(player.getUniqueId(), new ClientInfo(
                root.has("mod") ? root.get("mod").getAsString() : "unknown",
                root.has("mc") ? root.get("mc").getAsString() : "unknown",
                Set.copyOf(features)));
            modes.put(player.getUniqueId(), ClientMode.MOD);
            JsonObject rules = new JsonObject();
            rules.addProperty("shieldOnSneak", getConfig().getBoolean("qol.shield-on-sneak"));
            rules.addProperty("armorSwap", getConfig().getBoolean("qol.armor-swap"));
            JsonObject welcome = new JsonObject();
            welcome.addProperty("t", "WELCOME");
            welcome.addProperty("v", 1);
            welcome.addProperty("server", "NuvenClub");
            welcome.addProperty("plugin", getPluginMeta().getVersion());
            welcome.add("rules", rules);
            player.sendPluginMessage(this, CHANNEL, PayloadCodec.encode(welcome.toString()));
        } catch (RuntimeException exception) {
            if (getConfig().getBoolean("debug")) getLogger().warning("HELLO inválido de " + player.getName());
        }
    }

    private ClientMode resolveWithoutMod(Player player) {
        try {
            Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
            Object api = via.getMethod("getAPI").invoke(null);
            int protocol = (int) api.getClass().getMethod("getPlayerVersion", UUID.class).invoke(api, player.getUniqueId());
            if (protocol >= 0 && protocol <= 47) return ClientMode.VIA_LEGACY;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // ViaVersion é opcional; o laboratório moderno continua funcionando sem ele.
        }
        return ClientMode.VANILLA_MODERN;
    }

    private void applyAttackSpeed(Player player) {
        if (!getConfig().getBoolean("combat.enabled")) return;
        var attribute = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attribute != null) attribute.setBaseValue(getConfig().getDouble("combat.attack-speed", 1024.0D));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lfkit")) {
            if (!(sender instanceof Player player)) return true;
            ItemStack sword = legacySword();
            player.getInventory().addItem(sword, new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 64),
                new ItemStack(Material.FISHING_ROD), new ItemStack(Material.SHIELD));
            sender.sendMessage("§aKit LegacyFeel entregue.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            Bukkit.getOnlinePlayers().forEach(this::applyAttackSpeed);
            sender.sendMessage("§aLegacyFeel recarregado.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
            Map<ClientMode, Integer> counts = new EnumMap<>(ClientMode.class);
            Bukkit.getOnlinePlayers().forEach(player -> counts.merge(modes.getOrDefault(player.getUniqueId(), ClientMode.UNKNOWN), 1, Integer::sum));
            sender.sendMessage("§eLegacyFeel: " + counts);
            return true;
        }
        Player target = sender instanceof Player player ? player : null;
        if (args.length > 1 && args[0].equalsIgnoreCase("info")) target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cUse /legacyfeel info <jogador> ou /legacyfeel stats.");
            return true;
        }
        sender.sendMessage("§eLegacyFeel §7| modo=" + modes.getOrDefault(target.getUniqueId(), ClientMode.UNKNOWN)
            + " cliente=" + clients.get(target.getUniqueId()));
        return true;
    }

    private ItemStack legacySword() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        DamageReduction reduction = DamageReduction.damageReduction()
            .horizontalBlockingAngle(180.0F).base(0.0F).factor(0.0F).build();
        BlocksAttacks blocks = BlocksAttacks.blocksAttacks()
            .blockDelaySeconds(0.0F).addDamageReduction(reduction).disableCooldownScale(0.0F).build();
        sword.setData(DataComponentTypes.BLOCKS_ATTACKS, blocks);
        ItemMeta meta = sword.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Espada LegacyFeel"));
        sword.setItemMeta(meta);
        return sword;
    }

    private static boolean isSword(Material material) {
        return material.name().endsWith("_SWORD");
    }
}
