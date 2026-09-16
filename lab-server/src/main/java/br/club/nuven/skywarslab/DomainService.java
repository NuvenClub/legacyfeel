package br.club.nuven.skywarslab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class DomainService implements Listener {
    private final SkyWarsLabPlugin plugin;
    private final LabSessionManager sessions;
    private final LabItems items;
    private final NamespacedKey domainIdKey;
    private final Map<UUID, DomainInstance> domains = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    DomainService(SkyWarsLabPlugin plugin, LabSessionManager sessions, LabItems items) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.items = items;
        this.domainIdKey = new NamespacedKey(plugin, "domain_id");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (!sessions.isEnrolled(player) || sessions.kit(player) != KitType.DOMAIN) return;
        if (!items.is(event.getItem(), "domain_seal")) return;
        event.setCancelled(true);

        long remaining = cooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remaining > 0) {
            player.sendActionBar(Component.text("Domínio recarrega em " + ((remaining + 999) / 1000) + "s", NamedTextColor.RED));
            return;
        }

        Location center = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0.05, 0.5);
        if (!center.getBlock().isPassable()) {
            player.sendMessage(Component.text("O Núcleo precisa de espaço livre.", NamedTextColor.RED));
            return;
        }

        cleanupOwner(player.getUniqueId());
        DomainInstance instance = new DomainInstance(player, center);
        domains.put(instance.id, instance);
        instance.start();
        int cooldown = plugin.getConfig().getInt("domain.cooldown-seconds", 45);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCoreHit(EntityDamageByEntityEvent event) {
        String rawId = event.getEntity().getPersistentDataContainer().get(domainIdKey, PersistentDataType.STRING);
        if (rawId == null) return;
        event.setCancelled(true);
        if (!(event.getDamager() instanceof Player attacker)) return;
        try {
            DomainInstance instance = domains.get(UUID.fromString(rawId));
            if (instance != null) instance.hit(attacker);
        } catch (IllegalArgumentException ignored) {
            event.getEntity().remove();
        }
    }

    void cleanupPlayer(Player player) {
        cleanupOwner(player.getUniqueId());
        cooldowns.remove(player.getUniqueId());
        domains.values().forEach(instance -> instance.removeAffected(player));
    }

    void cleanupAll() {
        new ArrayList<>(domains.values()).forEach(instance -> instance.close("limpeza"));
        domains.clear();
        cooldowns.clear();
    }

    private void cleanupOwner(UUID ownerId) {
        new ArrayList<>(domains.values()).stream()
                .filter(instance -> instance.owner.getUniqueId().equals(ownerId))
                .forEach(instance -> instance.close("substituído"));
    }

    private final class DomainInstance {
        private final UUID id = UUID.randomUUID();
        private final Player owner;
        private final Location center;
        private final EffectScope scope = new EffectScope();
        private final Map<UUID, AttributeModifier> healthModifiers = new HashMap<>();
        private final Map<UUID, Long> hitTimes = new HashMap<>();
        private final double radius = plugin.getConfig().getDouble("domain.radius", 8.0);
        private final int durationTicks = plugin.getConfig().getInt("domain.duration-seconds", 25) * 20;
        private final double maxHealth = plugin.getConfig().getDouble("domain.core-health", 40.0);
        private double health = maxHealth;
        private ItemDisplay model;
        private TextDisplay label;
        private boolean closed;

        private DomainInstance(Player owner, Location center) {
            this.owner = owner;
            this.center = center.clone();
        }

        private void start() {
            World world = center.getWorld();
            model = scope.track(world.spawn(center.clone().add(0, 0.2, 0), ItemDisplay.class, display -> {
                display.setItemStack(items.domainCore("intact"));
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                display.setPersistent(false);
                display.setInvulnerable(true);
                display.setViewRange(3.0f);
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(196, 65, 255));
            }));
            Interaction hitbox = scope.track(world.spawn(center.clone(), Interaction.class, interaction -> {
                interaction.setInteractionWidth(1.35f);
                interaction.setInteractionHeight(2.4f);
                interaction.setResponsive(true);
                interaction.setPersistent(false);
                interaction.getPersistentDataContainer().set(domainIdKey, PersistentDataType.STRING, id.toString());
            }));
            hitbox.setInvulnerable(false);
            label = scope.track(world.spawn(center.clone().add(0, 2.7, 0), TextDisplay.class, text -> {
                text.text(Component.text("Núcleo 40❤", NamedTextColor.LIGHT_PURPLE));
                text.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                text.setPersistent(false);
                text.setShadowed(true);
                text.setViewRange(2.5f);
            }));

            int absorptionSeconds = plugin.getConfig().getInt("domain.owner-absorption-seconds", 10);
            owner.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, absorptionSeconds * 20, 0, false, false, true));
            applyEnemies();
            world.playSound(center, "nuven:domain.activate", SoundCategory.PLAYERS, 1.2f, 0.9f);
            world.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 1, 0), 90, radius / 3, 1.0, radius / 3, 0.08);

            BukkitTask task = new BukkitRunnable() {
                int age;
                @Override
                public void run() {
                    if (closed) { cancel(); return; }
                    age += 2;
                    if (!owner.isOnline() || !sessions.isEnrolled(owner)) {
                        close("dono ausente");
                        cancel();
                        return;
                    }
                    if (age >= durationTicks) {
                        close("tempo");
                        cancel();
                        return;
                    }
                    if (age % 10 == 0) drawBoundary();
                    if (age % 20 == 0) applyEnemies();
                }
            }.runTaskTimer(plugin, 2L, 2L);
            scope.track(task);
        }

        private void applyEnemies() {
            double reduction = plugin.getConfig().getDouble("domain.enemy-max-health-reduction", 2.0);
            for (Player player : center.getWorld().getPlayers()) {
                if (player.equals(owner) || !sessions.isEnrolled(player)) continue;
                boolean inside = player.getLocation().distanceSquared(center) <= radius * radius;
                if (inside && !healthModifiers.containsKey(player.getUniqueId())) {
                    AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
                    if (attribute == null) continue;
                    NamespacedKey key = new NamespacedKey(plugin, "domain_" + id.toString().replace("-", "") + "_" +
                            player.getUniqueId().toString().replace("-", ""));
                    AttributeModifier modifier = new AttributeModifier(key, -Math.abs(reduction), AttributeModifier.Operation.ADD_NUMBER);
                    attribute.addTransientModifier(modifier);
                    healthModifiers.put(player.getUniqueId(), modifier);
                    if (player.getHealth() > attribute.getValue()) player.setHealth(Math.max(1.0, attribute.getValue()));
                } else if (!inside) {
                    removeAffected(player);
                }
            }
        }

        private void removeAffected(Player player) {
            AttributeModifier modifier = healthModifiers.remove(player.getUniqueId());
            AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (modifier != null && attribute != null) attribute.removeModifier(modifier);
        }

        private void hit(Player attacker) {
            if (closed || attacker.equals(owner)) {
                if (attacker.equals(owner)) attacker.sendActionBar(Component.text("Você não pode atacar seu Núcleo.", NamedTextColor.RED));
                return;
            }
            if (attacker.getWorld() != center.getWorld() || attacker.getLocation().distanceSquared(center) > radius * radius) {
                attacker.sendActionBar(Component.text("Entre no Domínio para atingir o Núcleo.", NamedTextColor.RED));
                return;
            }
            long now = System.currentTimeMillis();
            long delay = plugin.getConfig().getLong("domain.hit-cooldown-ms", 500L);
            if (now - hitTimes.getOrDefault(attacker.getUniqueId(), 0L) < delay) return;
            hitTimes.put(attacker.getUniqueId(), now);
            health -= plugin.getConfig().getDouble("domain.hit-damage", 2.0);
            center.getWorld().playSound(center, "nuven:domain.hit", SoundCategory.PLAYERS, 0.8f, 1.1f);
            center.getWorld().spawnParticle(Particle.ENCHANTED_HIT, center.clone().add(0, 1.1, 0), 20, 0.5, 0.8, 0.5, 0.1);
            updateModel();
            if (health <= 0) close("destruído");
        }

        private void updateModel() {
            double ratio = Math.max(0, health) / maxHealth;
            String state = ratio <= 0.33 ? "critical" : ratio <= 0.66 ? "damaged" : "intact";
            model.setItemStack(items.domainCore(state));
            label.text(Component.text("Núcleo " + Math.max(0, Math.ceil(health)) + "❤",
                    ratio <= 0.33 ? NamedTextColor.RED : NamedTextColor.LIGHT_PURPLE));
        }

        private void drawBoundary() {
            int points = sessions.reducedEffects(owner)
                    ? plugin.getConfig().getInt("visuals.reduced-domain-points", 16)
                    : plugin.getConfig().getInt("visuals.full-domain-points", 48);
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(183, 57, 255), 1.15f);
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2 * i / points;
                Location point = center.clone().add(Math.cos(angle) * radius, 0.12, Math.sin(angle) * radius);
                center.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
            }
        }

        private void close(String reason) {
            if (closed) return;
            closed = true;
            domains.remove(id);
            for (UUID playerId : new ArrayList<>(healthModifiers.keySet())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) removeAffected(player);
            }
            center.getWorld().playSound(center, "nuven:domain.break", SoundCategory.PLAYERS, 1.1f,
                    "destruído".equals(reason) ? 0.75f : 1.25f);
            center.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, center.clone().add(0, 1, 0),
                    45, 0.9, 1.0, 0.9, 0.04);
            scope.close();
            if (owner.isOnline()) owner.sendActionBar(Component.text("Domínio encerrado: " + reason, NamedTextColor.GRAY));
        }
    }
}
