package br.club.nuven.skywarslab;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class MirageService implements Listener {
    private final SkyWarsLabPlugin plugin;
    private final LabSessionManager sessions;
    private final LabItems items;
    private final NamespacedKey cloneOwnerKey;
    private final Map<UUID, Integer> charges = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cloneCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> invisibilityUsed = ConcurrentHashMap.newKeySet();
    private final Map<UUID, EffectScope> ownerScopes = new ConcurrentHashMap<>();
    private final Map<UUID, EffectScope> cloneScopes = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> casts = new ConcurrentHashMap<>();
    private final Set<UUID> invisible = ConcurrentHashMap.newKeySet();

    MirageService(SkyWarsLabPlugin plugin, LabSessionManager sessions, LabItems items) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.items = items;
        this.cloneOwnerKey = new NamespacedKey(plugin, "mirage_owner");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!sessions.isEnrolled(player) || sessions.kit(player) != KitType.MIRAGE) return;
        if (!items.is(event.getItem(), "mirage_prism")) return;
        event.setCancelled(true);
        if (player.isSneaking()) startInvisibilityCast(player);
        else spawnClone(player);
    }

    private void spawnClone(Player owner) {
        int maximum = plugin.getConfig().getInt("mirage.clone-charges", 3);
        int available = charges.computeIfAbsent(owner.getUniqueId(), ignored -> maximum);
        if (available <= 0) {
            owner.sendActionBar(Component.text("Você usou todos os clones.", NamedTextColor.RED));
            return;
        }
        long remaining = cloneCooldowns.getOrDefault(owner.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remaining > 0) {
            owner.sendActionBar(Component.text("Clone recarrega em " + ((remaining + 999) / 1000) + "s", NamedTextColor.RED));
            return;
        }

        charges.put(owner.getUniqueId(), available - 1);
        int cooldown = plugin.getConfig().getInt("mirage.clone-cooldown-seconds", 5);
        cloneCooldowns.put(owner.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);
        EffectScope scope = ownerScopes.computeIfAbsent(owner.getUniqueId(), ignored -> new EffectScope());
        Location start = owner.getLocation().clone();
        Mannequin clone = scope.track(owner.getWorld().spawn(start, Mannequin.class, mannequin -> {
            mannequin.setProfile(ResolvableProfile.resolvableProfile(owner.getPlayerProfile()));
            mannequin.setDescription(Component.empty());
            mannequin.setCustomNameVisible(false);
            mannequin.setPersistent(false);
            mannequin.setInvulnerable(false);
            mannequin.setCollidable(false);
            mannequin.setImmovable(true);
            mannequin.setAI(false);
            mannequin.getPersistentDataContainer().set(cloneOwnerKey, PersistentDataType.STRING,
                    owner.getUniqueId().toString());
            copyEquipment(owner.getEquipment(), mannequin.getEquipment());
        }));
        cloneScopes.put(clone.getUniqueId(), scope);
        owner.getWorld().playSound(start, "nuven:mirage.clone", SoundCategory.PLAYERS, 0.9f, 1.4f);
        owner.getWorld().spawnParticle(Particle.WITCH, start.clone().add(0, 1, 0),
                sessions.reducedEffects(owner) ? 12 : 35, 0.45, 0.9, 0.45, 0.02);
        owner.sendActionBar(Component.text("Clone criado • " + (available - 1) + " cargas", NamedTextColor.LIGHT_PURPLE));

        Vector direction = start.getDirection().setY(0).normalize().multiply(0.22);
        int lifetime = plugin.getConfig().getInt("mirage.clone-lifetime-seconds", 5) * 20;
        BukkitTask task = new BukkitRunnable() {
            int age;
            double fallingVelocity;

            @Override
            public void run() {
                if (!clone.isValid() || !owner.isOnline() || !sessions.isEnrolled(owner) || age++ >= lifetime) {
                    collapseClone(clone, false);
                    cancel();
                    return;
                }
                Location current = clone.getLocation();
                Location next = current.clone().add(direction);
                if (next.getBlock().isPassable() && next.clone().add(0, 1, 0).getBlock().isPassable()) {
                    if (current.clone().subtract(0, 0.1, 0).getBlock().isPassable()) {
                        fallingVelocity = Math.max(-0.5, fallingVelocity - 0.08);
                    } else {
                        fallingVelocity = 0;
                    }
                    next.add(0, fallingVelocity, 0);
                    next.setYaw(start.getYaw());
                    next.setPitch(start.getPitch());
                    clone.teleport(next);
                } else {
                    Location step = next.clone().add(0, 1, 0);
                    if (step.getBlock().isPassable() && step.clone().add(0, 1, 0).getBlock().isPassable()) {
                        clone.teleport(step);
                    }
                }
                if (age % 28 == 0) clone.swingMainHand();
                if (!sessions.reducedEffects(owner) && age % 8 == 0) {
                    owner.getWorld().spawnParticle(Particle.DUST,
                            clone.getLocation().add(0, 1, 0), 2, 0.2, 0.45, 0.2, 0,
                            new Particle.DustOptions(Color.fromRGB(95, 225, 255), 0.7f));
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        scope.track(task);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCloneHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Mannequin clone)) return;
        String ownerId = clone.getPersistentDataContainer().get(cloneOwnerKey, PersistentDataType.STRING);
        if (ownerId == null) return;
        event.setCancelled(true);
        collapseClone(clone, true);
    }

    private void collapseClone(Mannequin clone, boolean hit) {
        if (!clone.isValid()) return;
        Location location = clone.getLocation().add(0, 1, 0);
        clone.getWorld().playSound(location, "nuven:mirage.clone", SoundCategory.PLAYERS, 0.8f, hit ? 0.75f : 1.65f);
        clone.getWorld().spawnParticle(Particle.ITEM_COBWEB, location, 20, 0.4, 0.8, 0.4, 0.08);
        cloneScopes.remove(clone.getUniqueId());
        clone.remove();
    }

    private void startInvisibilityCast(Player player) {
        UUID id = player.getUniqueId();
        if (invisibilityUsed.contains(id)) {
            player.sendActionBar(Component.text("A invisibilidade já foi usada nesta partida.", NamedTextColor.RED));
            return;
        }
        if (casts.containsKey(id) || invisible.contains(id)) return;
        int castTicks = plugin.getConfig().getInt("mirage.invisibility-cast-ticks", 25);
        Location origin = player.getLocation().clone();
        player.sendActionBar(Component.text("Canalizando invisibilidade • fique parado", NamedTextColor.AQUA));
        BukkitTask task = new BukkitRunnable() {
            int age;
            @Override
            public void run() {
                if (!player.isOnline() || !sessions.isEnrolled(player)) {
                    casts.remove(id); cancel(); return;
                }
                if (player.getLocation().distanceSquared(origin) > 0.025) {
                    casts.remove(id);
                    player.sendActionBar(Component.text("Canalização cancelada por movimento.", NamedTextColor.RED));
                    cancel();
                    return;
                }
                if (age % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0),
                            sessions.reducedEffects(player) ? 4 : 12, 0.5, 0.9, 0.5, 0.02);
                }
                if (++age >= castTicks) {
                    casts.remove(id);
                    enableInvisibility(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        casts.put(id, task);
    }

    private void enableInvisibility(Player player) {
        UUID id = player.getUniqueId();
        invisibilityUsed.add(id);
        invisible.add(id);
        player.setInvisible(true);
        player.getWorld().playSound(player.getLocation(), "nuven:mirage.fade", SoundCategory.PLAYERS, 0.9f, 1.2f);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0),
                sessions.reducedEffects(player) ? 15 : 45, 0.5, 0.9, 0.5, 0.06);
        int duration = plugin.getConfig().getInt("mirage.invisibility-seconds", 15);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> reveal(player, false), duration * 20L);
        ownerScopes.computeIfAbsent(id, ignored -> new EffectScope()).track(task);
        player.sendActionBar(Component.text("Invisível por até " + duration + "s", NamedTextColor.AQUA));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) reveal(player, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) reveal(player, true);
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) reveal(player, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) reveal(player, true);
    }

    private void reveal(Player player, boolean forced) {
        if (!invisible.remove(player.getUniqueId())) return;
        player.setInvisible(false);
        player.getWorld().playSound(player.getLocation(), "nuven:mirage.fade", SoundCategory.PLAYERS, 0.8f, 0.8f);
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0),
                sessions.reducedEffects(player) ? 10 : 30, 0.4, 0.8, 0.4, 0.03);
        player.sendActionBar(Component.text(forced ? "Você foi revelado." : "A invisibilidade terminou.",
                forced ? NamedTextColor.RED : NamedTextColor.GRAY));
    }

    void cleanupPlayer(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask cast = casts.remove(id);
        if (cast != null) cast.cancel();
        reveal(player, false);
        EffectScope scope = ownerScopes.remove(id);
        if (scope != null) scope.close();
        charges.remove(id);
        cloneCooldowns.remove(id);
        invisibilityUsed.remove(id);
    }

    void cleanupAll() {
        Bukkit.getOnlinePlayers().forEach(this::cleanupPlayer);
        cloneScopes.clear();
    }

    private static void copyEquipment(EntityEquipment source, EntityEquipment target) {
        target.setHelmet(cloneOrNull(source.getHelmet()));
        target.setChestplate(cloneOrNull(source.getChestplate()));
        target.setLeggings(cloneOrNull(source.getLeggings()));
        target.setBoots(cloneOrNull(source.getBoots()));
        target.setItemInMainHand(source.getItemInMainHand().clone());
        target.setItemInOffHand(source.getItemInOffHand().clone());
    }

    private static org.bukkit.inventory.ItemStack cloneOrNull(org.bukkit.inventory.ItemStack item) {
        return item == null ? null : item.clone();
    }
}
