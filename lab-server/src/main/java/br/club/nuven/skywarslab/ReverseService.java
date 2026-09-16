package br.club.nuven.skywarslab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ReverseService implements Listener {
    private record Mark(UUID targetId, long expiresAt) {}

    private final SkyWarsLabPlugin plugin;
    private final LabSessionManager sessions;
    private final LabItems items;
    private final NamespacedKey projectileOwnerKey;
    private final Map<UUID, Mark> marks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> pending = new ConcurrentHashMap<>();

    ReverseService(SkyWarsLabPlugin plugin, LabSessionManager sessions, LabItems items) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.items = items;
        this.projectileOwnerKey = new NamespacedKey(plugin, "reverse_owner");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!sessions.isEnrolled(player) || sessions.kit(player) != KitType.REVERSE) return;
        if (!items.is(event.getItem(), "reverse_marker")) return;
        event.setCancelled(true);

        long remaining = cooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remaining > 0) {
            player.sendActionBar(Component.text("Reverso recarrega em " + ((remaining + 999) / 1000) + "s", NamedTextColor.RED));
            return;
        }

        Mark mark = marks.get(player.getUniqueId());
        if (mark != null && mark.expiresAt > System.currentTimeMillis()) {
            beginSwap(player, mark);
        } else {
            marks.remove(player.getUniqueId());
            Snowball projectile = player.launchProjectile(Snowball.class);
            projectile.getPersistentDataContainer().set(projectileOwnerKey, PersistentDataType.STRING,
                    player.getUniqueId().toString());
            projectile.setItem(items.reverseMarker());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.4f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Snowball snowball
                && snowball.getPersistentDataContainer().has(projectileOwnerKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        String ownerId = snowball.getPersistentDataContainer().get(projectileOwnerKey, PersistentDataType.STRING);
        if (ownerId == null || !(event.getHitEntity() instanceof Player target)) return;
        Player owner;
        try {
            owner = Bukkit.getPlayer(UUID.fromString(ownerId));
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (owner == null || owner.equals(target) || !sessions.isEnrolled(owner) || !sessions.isEnrolled(target)) return;
        double maxDistance = plugin.getConfig().getDouble("reverse.max-mark-distance", 40.0);
        if (owner.getWorld() != target.getWorld() || owner.getLocation().distanceSquared(target.getLocation()) > maxDistance * maxDistance) return;

        int seconds = plugin.getConfig().getInt("reverse.mark-seconds", 10);
        Mark expected = new Mark(target.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        marks.put(owner.getUniqueId(), expected);
        owner.sendActionBar(Component.text(target.getName() + " marcado • use novamente para trocar", NamedTextColor.AQUA));
        target.sendActionBar(Component.text("Você foi marcado por Reverso", NamedTextColor.LIGHT_PURPLE));
        target.getWorld().playSound(target.getLocation(), "nuven:reverse.mark", SoundCategory.PLAYERS, 1.0f, 1.1f);
        ring(owner, Color.fromRGB(60, 190, 255));
        ring(target, Color.fromRGB(255, 70, 190));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Mark current = marks.get(owner.getUniqueId());
            if (expected.equals(current) && current.expiresAt <= System.currentTimeMillis()) {
                marks.remove(owner.getUniqueId());
                if (owner.isOnline()) owner.sendActionBar(Component.text("A marca de Reverso expirou.", NamedTextColor.GRAY));
            }
        }, seconds * 20L + 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (marks.remove(player.getUniqueId()) != null) {
            cancelPending(player.getUniqueId());
            player.sendActionBar(Component.text("Marca perdida ao receber dano.", NamedTextColor.RED));
        }
    }

    private void beginSwap(Player owner, Mark mark) {
        if (pending.containsKey(owner.getUniqueId())) return;
        Player target = Bukkit.getPlayer(mark.targetId);
        if (target == null || !target.isOnline() || !sessions.isEnrolled(target)) {
            marks.remove(owner.getUniqueId());
            owner.sendActionBar(Component.text("O alvo não está mais disponível.", NamedTextColor.RED));
            return;
        }

        int delay = plugin.getConfig().getInt("reverse.activation-delay-ticks", 7);
        ring(owner, Color.fromRGB(60, 190, 255));
        ring(target, Color.fromRGB(255, 70, 190));
        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.8f, 1.6f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.8f, 1.3f);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(owner.getUniqueId());
            executeSwap(owner, target, mark);
        }, delay);
        pending.put(owner.getUniqueId(), task);
    }

    private void executeSwap(Player owner, Player target, Mark expected) {
        if (!owner.isOnline() || !target.isOnline() || marks.get(owner.getUniqueId()) != expected) return;
        Location ownerLocation = owner.getLocation().clone();
        Location targetLocation = target.getLocation().clone();
        SwapSafety.Result ownerDestination = SwapSafety.validate(ownerLocation, targetLocation);
        SwapSafety.Result targetDestination = SwapSafety.validate(targetLocation, ownerLocation);
        if (ownerDestination != SwapSafety.Result.SAFE || targetDestination != SwapSafety.Result.SAFE) {
            owner.sendActionBar(Component.text("Troca cancelada: posição " +
                    (ownerDestination != SwapSafety.Result.SAFE ? ownerDestination : targetDestination), NamedTextColor.RED));
            return;
        }

        Vector ownerVelocity = owner.getVelocity().clone();
        Vector targetVelocity = target.getVelocity().clone();
        float ownerFall = owner.getFallDistance();
        float targetFall = target.getFallDistance();
        boolean ownerMoved = owner.teleport(targetLocation);
        boolean targetMoved = target.teleport(ownerLocation);
        if (!ownerMoved || !targetMoved) {
            if (ownerMoved) owner.teleport(ownerLocation);
            if (targetMoved) target.teleport(targetLocation);
            owner.sendActionBar(Component.text("Troca cancelada pelo servidor.", NamedTextColor.RED));
            return;
        }
        owner.setVelocity(ownerVelocity);
        target.setVelocity(targetVelocity);
        owner.setFallDistance(ownerFall);
        target.setFallDistance(targetFall);
        marks.remove(owner.getUniqueId());
        int cooldown = plugin.getConfig().getInt("reverse.cooldown-seconds", 20);
        cooldowns.put(owner.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);
        owner.getWorld().playSound(owner.getLocation(), "nuven:reverse.swap", SoundCategory.PLAYERS, 1.0f, 1.2f);
        target.getWorld().playSound(target.getLocation(), "nuven:reverse.swap", SoundCategory.PLAYERS, 1.0f, 0.85f);
        ring(owner, Color.fromRGB(255, 70, 190));
        ring(target, Color.fromRGB(60, 190, 255));
    }

    private void ring(Player player, Color color) {
        int points = sessions.reducedEffects(player) ? 8 : 20;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            Location point = player.getLocation().add(Math.cos(angle) * 0.8, 0.15, Math.sin(angle) * 0.8);
            player.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
        }
    }

    void cleanupPlayer(Player player) {
        UUID id = player.getUniqueId();
        marks.remove(id);
        cooldowns.remove(id);
        cancelPending(id);
        marks.entrySet().removeIf(entry -> entry.getValue().targetId.equals(id));
    }

    void cleanupAll() {
        pending.values().forEach(BukkitTask::cancel);
        pending.clear();
        marks.clear();
        cooldowns.clear();
    }

    private void cancelPending(UUID id) {
        BukkitTask task = pending.remove(id);
        if (task != null) task.cancel();
    }
}
