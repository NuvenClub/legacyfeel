package br.club.nuven.skywarslab;

import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class CosmeticsService implements Listener {
    private final SkyWarsLabPlugin plugin;
    private final LabSessionManager sessions;
    private final NamespacedKey projectileCosmeticKey;
    private final Map<UUID, Set<BukkitTask>> playerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, EffectScope> cagePreviews = new ConcurrentHashMap<>();

    CosmeticsService(SkyWarsLabPlugin plugin, LabSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.projectileCosmeticKey = new NamespacedKey(plugin, "projectile_cosmetic");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player) || !sessions.isEnrolled(player)) return;
        ProjectileCosmetic cosmetic = sessions.state(player).projectile;
        Projectile projectile = event.getEntity();
        projectile.getPersistentDataContainer().set(projectileCosmeticKey, PersistentDataType.STRING, cosmetic.name());

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }
                spawnTrail(projectile.getLocation(), cosmetic, sessions.reducedEffects(player));
            }
        }.runTaskTimer(plugin, 0L, sessions.reducedEffects(player) ? 3L : 1L);
        playerTasks.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(task);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onImpact(ProjectileHitEvent event) {
        String raw = event.getEntity().getPersistentDataContainer().get(projectileCosmeticKey, PersistentDataType.STRING);
        if (raw == null) return;
        try {
            ProjectileCosmetic cosmetic = ProjectileCosmetic.valueOf(raw);
            Location location = event.getEntity().getLocation();
            switch (cosmetic) {
                case FLAMES -> location.getWorld().spawnParticle(Particle.LAVA, location, 8, 0.25, 0.25, 0.25, 0.02);
                case NINJA -> location.getWorld().spawnParticle(Particle.SQUID_INK, location, 8, 0.25, 0.25, 0.25, 0.02);
                case MYSTIC -> location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, location, 14, 0.3, 0.3, 0.3, 0.04);
                case CLOUD -> location.getWorld().spawnParticle(Particle.CLOUD, location, 10, 0.3, 0.2, 0.3, 0.02);
            }
        } catch (IllegalArgumentException ignored) {
            // Projétil de uma versão de catálogo desconhecida: não renderiza efeito.
        }
    }

    void previewCage(Player player, CageCosmetic cage) {
        EffectScope previous = cagePreviews.remove(player.getUniqueId());
        if (previous != null) previous.close();
        EffectScope scope = new EffectScope();
        cagePreviews.put(player.getUniqueId(), scope);
        scope.onClose(() -> cagePreviews.remove(player.getUniqueId(), scope));

        Material material = switch (cage) {
            case PRISM -> Material.TINTED_GLASS;
            case CRYO -> Material.BLUE_ICE;
            case CONFETTI -> Material.WHITE_CONCRETE;
            case SANCTUARY -> Material.CRYING_OBSIDIAN;
        };
        Location center = player.getLocation().clone().add(0, 1.1, 0);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8.0;
            Location location = center.clone().add(Math.cos(angle) * 1.35, (i % 2) * 1.25 - 0.6, Math.sin(angle) * 1.35);
            ItemDisplay display = scope.track(player.getWorld().spawn(location, ItemDisplay.class, entity -> {
                entity.setItemStack(new ItemStack(material));
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                entity.setPersistent(false);
                entity.setViewRange(2.0f);
                entity.setInterpolationDuration(10);
                entity.setTeleportDuration(10);
                entity.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(0.38f, 0.38f, 0.38f), new AxisAngle4f()));
            }));
            player.showEntity(plugin, display);
        }
        player.getWorld().playSound(center, cage == CageCosmetic.CONFETTI ? Sound.ENTITY_FIREWORK_ROCKET_LAUNCH
                : Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.8f, 1.2f);
        scope.track(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getWorld().spawnParticle(cage == CageCosmetic.CRYO ? Particle.SNOWFLAKE : Particle.FIREWORK,
                    center, sessions.reducedEffects(player) ? 15 : 45, 1.2, 1.2, 1.2, 0.04);
            scope.close();
        }, 60L));
    }

    private static void spawnTrail(Location location, ProjectileCosmetic cosmetic, boolean reduced) {
        int count = reduced ? 1 : 3;
        switch (cosmetic) {
            case FLAMES -> location.getWorld().spawnParticle(Particle.SMALL_FLAME, location, count, 0.04, 0.04, 0.04, 0.005);
            case NINJA -> location.getWorld().spawnParticle(Particle.SMOKE, location, count, 0.05, 0.05, 0.05, 0.005);
            case MYSTIC -> location.getWorld().spawnParticle(Particle.ENCHANT, location, count, 0.08, 0.08, 0.08, 0.01);
            case CLOUD -> location.getWorld().spawnParticle(Particle.CLOUD, location, count, 0.06, 0.06, 0.06, 0.005);
        }
    }

    void cleanupPlayer(Player player) {
        Set<BukkitTask> tasks = playerTasks.remove(player.getUniqueId());
        if (tasks != null) tasks.forEach(BukkitTask::cancel);
        EffectScope cage = cagePreviews.remove(player.getUniqueId());
        if (cage != null) cage.close();
    }

    void cleanupAll() {
        playerTasks.values().forEach(tasks -> tasks.forEach(BukkitTask::cancel));
        playerTasks.clear();
        cagePreviews.values().forEach(EffectScope::close);
        cagePreviews.clear();
    }
}
