package br.club.nuven.legacyfeel.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LegacyFeelPlugin extends JavaPlugin implements Listener {
    public static final String CHANNEL = "legacyfeel:handshake";
    private static final Set<String> SERVER_PROFILES = Set.of(
        "CLASSIC_PARITY", "MODERN_LEGACY_COMBAT", "SKYWARS_LAB", "VANILLA_SAFE");
    private final Map<UUID, ClientMode> modes = new HashMap<>();
    private final Map<UUID, ClientInfo> clients = new HashMap<>();
    private final Map<UUID, Long> lastHello = new HashMap<>();
    private CombatTelemetry telemetry;
    private String profileOverride;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this::receive);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);
        telemetry = new CombatTelemetry(this);
        getServer().getPluginManager().registerEvents(telemetry, this);
        telemetry.enable();
        getLogger().info("LegacyFeel quick lab ativo; combate e shield-on-sneak habilitados.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        if (telemetry != null) telemetry.close();
        telemetry = null;
        modes.clear();
        clients.clear();
        lastHello.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        modes.put(player.getUniqueId(), ClientMode.UNKNOWN);
        applyAttackSpeed(player);
        applyNoDamageTicks(player);
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
        if (!getConfig().getBoolean("combat.enabled")) return;

        if (event.getDamager() instanceof Player attacker && isAxe(attacker.getInventory().getItemInMainHand().getType())) {
            event.setDamage(6.0D);
        }

        if (!(event.getEntity() instanceof Player victim)) return;
        if (getConfig().getBoolean("qol.shield-on-sneak") && victim.isSneaking() && hasShield(victim)) {
            event.setCancelled(true);
            return;
        }

        // OCM is the authoritative damage pipeline in the quick lab. Keep this
        // implementation as a standalone fallback so the reduction is never applied twice.
        if (hasOldCombatMechanics() || !getConfig().getBoolean("combat.legacy-blocking") || !victim.isBlocking()) return;
        ItemStack active = victim.getActiveItem();
        if (!isSword(active.getType())) return;
        double original = event.getDamage();
        event.setDamage((original + 1.0D) / 2.0D);
        if (getConfig().getBoolean("debug")) {
            getLogger().info("block victim=" + victim.getName() + " before=" + original + " after=" + event.getDamage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShieldRightClick(PlayerInteractEvent event) {
        if (!getConfig().getBoolean("qol.shield-on-sneak")) return;
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.SHIELD && event.getAction().isRightClick()) {
            event.setCancelled(true);
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
            if (!"HELLO".equals(root.get("t").getAsString())) return;
            int requestedVersion = root.has("v") ? root.get("v").getAsInt() : 1;
            int negotiatedVersion = HandshakeProtocol.negotiate(requestedVersion);
            if (negotiatedVersion == 0) return;
            Set<String> features = new HashSet<>();
            if (root.has("features") && root.get("features").isJsonObject()) {
                root.getAsJsonObject("features").entrySet().stream()
                    .filter(entry -> entry.getValue().isJsonPrimitive() && entry.getValue().getAsBoolean())
                    .forEach(entry -> features.add(entry.getKey()));
            }
            clients.put(player.getUniqueId(), new ClientInfo(negotiatedVersion,
                root.has("mod") ? root.get("mod").getAsString() : "unknown",
                root.has("mc") ? root.get("mc").getAsString() : "unknown",
                root.has("loader") ? root.get("loader").getAsString() : "unknown",
                Set.copyOf(features)));
            modes.put(player.getUniqueId(), ClientMode.MOD);
            if (!player.getListeningPluginChannels().contains(CHANNEL)) return;
            player.sendPluginMessage(this, CHANNEL, PayloadCodec.encode(policyPayload("WELCOME", negotiatedVersion).toString()));
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

    private void applyNoDamageTicks(Player player) {
        if (!getConfig().getBoolean("combat.enabled")) return;
        int ticks = Math.max(1, getConfig().getInt("combat.hurt-resistance-ticks", 20));
        player.setMaximumNoDamageTicks(ticks);
        player.setNoDamageTicks(Math.min(player.getNoDamageTicks(), ticks));
    }

    private boolean hasOldCombatMechanics() {
        return getServer().getPluginManager().isPluginEnabled("OldCombatMechanics");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lfkit")) {
            if (!(sender instanceof Player player)) return true;
            ItemStack sword = legacySword();
            player.getInventory().addItem(sword, legacyAxe(), new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 64),
                new ItemStack(Material.FISHING_ROD), new ItemStack(Material.SHIELD));
            sender.sendMessage("§aKit LegacyFeel entregue.");
            return true;
        }
        if (telemetry != null && telemetry.handleCommand(sender, args)) return true;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            Bukkit.getOnlinePlayers().forEach(player -> {
                applyAttackSpeed(player);
                applyNoDamageTicks(player);
            });
            sendPolicyToModClients();
            sender.sendMessage("§aLegacyFeel recarregado.");
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("policy")) {
            String requested = args[1].toUpperCase(java.util.Locale.ROOT);
            if ("CLASSIC".equals(requested)) requested = "CLASSIC_PARITY";
            if ("MODERN".equals(requested)) requested = "MODERN_LEGACY_COMBAT";
            if ("LAB".equals(requested)) requested = "SKYWARS_LAB";
            if ("CONFIG".equals(requested)) {
                profileOverride = null;
            } else if (SERVER_PROFILES.contains(requested)) {
                profileOverride = requested;
            } else {
                sender.sendMessage("§cUse /legacyfeel policy <classic|modern|lab|vanilla_safe|config>.");
                return true;
            }
            sendPolicyToModClients();
            sender.sendMessage("§aPerfil simulado: §f" + serverProfile() + " §7| ruleset=" + rulesetId());
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

    private JsonObject policyPayload(String type, int version) {
        JsonObject rules = new JsonObject();
        rules.addProperty("shieldOnSneak", getConfig().getBoolean("qol.shield-on-sneak"));
        rules.addProperty("armorSwap", getConfig().getBoolean("qol.armor-swap"));

        JsonArray advertisedCapabilities = new JsonArray();
        capabilities().forEach(advertisedCapabilities::add);

        JsonObject payload = new JsonObject();
        payload.addProperty("t", type);
        payload.addProperty("v", version);
        payload.addProperty("server", "NuvenClub");
        payload.addProperty("plugin", getPluginMeta().getVersion());
        payload.addProperty("serverProfile", serverProfile());
        payload.addProperty("rulesetId", rulesetId());
        payload.add("capabilities", advertisedCapabilities);
        payload.add("rules", rules);
        return payload;
    }

    private void sendPolicyToModClients() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ClientInfo client = clients.get(player.getUniqueId());
            if (client == null || !player.getListeningPluginChannels().contains(CHANNEL)) return;
            player.sendPluginMessage(this, CHANNEL,
                PayloadCodec.encode(policyPayload("POLICY", client.handshakeVersion()).toString()));
        });
    }

    String serverProfile() {
        if (profileOverride != null) return profileOverride;
        String configured = getConfig().getString("integration.server-profile", "MODERN_LEGACY_COMBAT")
            .toUpperCase(java.util.Locale.ROOT);
        return SERVER_PROFILES.contains(configured) ? configured : "MODERN_LEGACY_COMBAT";
    }

    String rulesetId() {
        return getConfig().getString("integration.ruleset-id", "legacyfeel-1.7-v1");
    }

    Set<String> capabilities() {
        return Set.copyOf(getConfig().getStringList("integration.capabilities"));
    }

    ClientInfo clientInfo(UUID playerId) {
        return clients.get(playerId);
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

    private ItemStack legacyAxe() {
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Machado LegacyFeel"));
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
            new NamespacedKey(this, "legacy_axe_damage"), 5.0D,
            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        axe.setItemMeta(meta);
        return axe;
    }

    private static boolean hasShield(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.SHIELD
            || player.getInventory().getItemInOffHand().getType() == Material.SHIELD;
    }

    private static boolean isSword(Material material) {
        return material.name().endsWith("_SWORD");
    }

    private static boolean isAxe(Material material) {
        return material.name().endsWith("_AXE");
    }
}
