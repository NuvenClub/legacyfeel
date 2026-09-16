package br.club.nuven.legacyfeel.server;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class CombatTelemetry implements Listener, AutoCloseable {
    private static final long MATCH_WINDOW_NANOS = 1_000_000_000L;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final LegacyFeelPlugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private PacketListenerCommon packetListener;

    CombatTelemetry(LegacyFeelPlugin plugin) {
        this.plugin = plugin;
    }

    void enable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
            plugin.getLogger().warning("Telemetria de tentativas indisponível: PacketEvents não está ativo.");
            return;
        }
        packetListener = new PacketListenerAbstract(PacketListenerPriority.MONITOR) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                int targetId;
                if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
                    targetId = new WrapperPlayClientAttack(event).getEntityId();
                } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                    WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                    if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
                    targetId = wrapper.getEntityId();
                } else {
                    return;
                }

                UUID playerId = event.getUser().getUUID();
                Session session = sessions.get(playerId);
                if (session == null) return;
                session.add(new Attempt(
                    sequence.incrementAndGet(), System.currentTimeMillis(), System.nanoTime(),
                    targetId, event.getPacketName(), event.getClientVersion().toString(), event.isCancelled()));
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("record")) return false;
        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : sender instanceof Player player ? player : null;

        if (action.equals("status")) {
            if (target != null) {
                Session session = sessions.get(target.getUniqueId());
                sender.sendMessage(session == null
                    ? "§7Nenhuma gravação ativa para " + target.getName() + "."
                    : "§eGravação ativa: §f" + target.getName() + " §7| tentativas=" + session.size());
            } else {
                sender.sendMessage("§eGravações ativas: §f" + sessions.size());
            }
            return true;
        }

        if (target == null) {
            sender.sendMessage("§cInforme um jogador: /legacyfeel record " + action + " <jogador>.");
            return true;
        }

        if (action.equals("start")) {
            Session previous = sessions.put(target.getUniqueId(), new Session(target.getName()));
            sender.sendMessage(previous == null
                ? "§aTelemetria iniciada para " + target.getName() + "."
                : "§eA gravação anterior foi descartada e reiniciada para " + target.getName() + ".");
            return true;
        }

        if (action.equals("stop")) {
            Session session = sessions.remove(target.getUniqueId());
            if (session == null) {
                sender.sendMessage("§7Nenhuma gravação ativa para " + target.getName() + ".");
                return true;
            }
            try {
                Report report = write(session);
                sender.sendMessage("§aTelemetria salva: §f" + report.path().getFileName());
                sender.sendMessage(String.format(Locale.ROOT,
                    "§7tentativas=%d hits=%d cancelados=%d sem-evento=%d CPS=%.2f distância média=%.3f",
                    report.attempts(), report.accepted(), report.cancelled(), report.noEvent(), report.cps(), report.averageDistance()));
            } catch (IOException exception) {
                plugin.getLogger().severe("Falha ao salvar telemetria: " + exception.getMessage());
                sender.sendMessage("§cNão foi possível salvar a telemetria; consulte o log.");
            }
            return true;
        }

        sender.sendMessage("§cUse /legacyfeel record <start|stop|status> [jogador].");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        Session session = sessions.get(attacker.getUniqueId());
        if (session == null) return;

        Attempt attempt = session.match(event.getEntity().getEntityId(), System.nanoTime());
        if (attempt == null) return;
        Entity victim = event.getEntity();
        Vector before = victim.getVelocity().clone();
        attempt.damageEventNanos = System.nanoTime();
        attempt.serverTick = Bukkit.getCurrentTick();
        attempt.cancelled = event.isCancelled();
        attempt.damage = event.getDamage();
        attempt.finalDamage = event.getFinalDamage();
        attempt.distance = distanceToBox(attacker.getEyeLocation(), victim.getBoundingBox());
        attempt.ping = attacker.getPing();
        attempt.sprinting = attacker.isSprinting();
        attempt.attackerOnGround = attacker.isOnGround();
        attempt.victimNoDamageTicksBefore = victim instanceof org.bukkit.entity.LivingEntity living ? living.getNoDamageTicks() : -1;
        attempt.velocityBeforeX = before.getX();
        attempt.velocityBeforeY = before.getY();
        attempt.velocityBeforeZ = before.getZ();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Vector after = victim.getVelocity();
            attempt.velocityAfterX = after.getX();
            attempt.velocityAfterY = after.getY();
            attempt.velocityAfterZ = after.getZ();
            attempt.victimNoDamageTicksAfter = victim instanceof org.bukkit.entity.LivingEntity living ? living.getNoDamageTicks() : -1;
        });
    }

    private Report write(Session session) throws IOException {
        List<Attempt> attempts = session.snapshot();
        Path directory = plugin.getDataFolder().toPath().resolve("telemetry");
        Files.createDirectories(directory);
        Path file = directory.resolve(FILE_TIME.format(Instant.now()) + "-" + safeName(session.playerName) + ".csv");
        int accepted = 0;
        int cancelled = 0;
        double distanceTotal = 0.0D;
        int distanceCount = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("sequence,utc_ms,packet,protocol,target_id,packet_cancelled,outcome,packet_to_damage_ms,server_tick,ping_ms,distance,sprinting,attacker_on_ground,damage,final_damage,no_damage_before,no_damage_after,velocity_before_x,velocity_before_y,velocity_before_z,velocity_after_x,velocity_after_y,velocity_after_z");
            writer.newLine();
            for (Attempt attempt : attempts) {
                String outcome;
                if (attempt.damageEventNanos == null) {
                    outcome = "no_damage_event";
                } else if (attempt.cancelled) {
                    outcome = "cancelled";
                    cancelled++;
                } else {
                    outcome = "accepted";
                    accepted++;
                }
                if (attempt.distance != null) {
                    distanceTotal += attempt.distance;
                    distanceCount++;
                }
                writer.write(attempt.csv(outcome));
                writer.newLine();
            }
        }

        double durationSeconds = Math.max(0.001D, (System.nanoTime() - session.startedNanos) / 1_000_000_000.0D);
        return new Report(file, attempts.size(), accepted, cancelled, attempts.size() - accepted - cancelled,
            attempts.size() / durationSeconds, distanceCount == 0 ? 0.0D : distanceTotal / distanceCount);
    }

    private static double distanceToBox(Location eye, BoundingBox box) {
        double x = Math.max(box.getMinX(), Math.min(eye.getX(), box.getMaxX()));
        double y = Math.max(box.getMinY(), Math.min(eye.getY(), box.getMaxY()));
        double z = Math.max(box.getMinZ(), Math.min(eye.getZ(), box.getMaxZ()));
        return Math.sqrt(Math.pow(eye.getX() - x, 2) + Math.pow(eye.getY() - y, 2) + Math.pow(eye.getZ() - z, 2));
    }

    private static String safeName(String input) {
        return input.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    @Override
    public void close() {
        if (packetListener != null) PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        packetListener = null;
        sessions.clear();
    }

    private final class Session {
        private final String playerName;
        private final long startedNanos = System.nanoTime();
        private final Deque<Attempt> attempts = new ArrayDeque<>();

        private Session(String playerName) {
            this.playerName = playerName;
        }

        private synchronized void add(Attempt attempt) {
            int max = Math.max(100, plugin.getConfig().getInt("telemetry.max-attempts", 20_000));
            if (attempts.size() >= max) attempts.removeFirst();
            attempts.addLast(attempt);
        }

        private synchronized Attempt match(int targetId, long now) {
            Attempt best = null;
            for (Attempt attempt : attempts) {
                if (attempt.damageEventNanos != null || attempt.targetId != targetId) continue;
                long age = now - attempt.packetNanos;
                if (age < 0 || age > MATCH_WINDOW_NANOS) continue;
                if (best == null || attempt.packetNanos > best.packetNanos) best = attempt;
            }
            return best;
        }

        private synchronized int size() {
            return attempts.size();
        }

        private synchronized List<Attempt> snapshot() {
            return new ArrayList<>(attempts);
        }
    }

    private static final class Attempt {
        private final long sequence;
        private final long epochMillis;
        private final long packetNanos;
        private final int targetId;
        private final String packet;
        private final String protocol;
        private final boolean packetCancelled;
        private volatile Long damageEventNanos;
        private volatile Integer serverTick;
        private volatile boolean cancelled;
        private volatile Double damage;
        private volatile Double finalDamage;
        private volatile Double distance;
        private volatile Integer ping;
        private volatile Boolean sprinting;
        private volatile Boolean attackerOnGround;
        private volatile Integer victimNoDamageTicksBefore;
        private volatile Integer victimNoDamageTicksAfter;
        private volatile Double velocityBeforeX;
        private volatile Double velocityBeforeY;
        private volatile Double velocityBeforeZ;
        private volatile Double velocityAfterX;
        private volatile Double velocityAfterY;
        private volatile Double velocityAfterZ;

        private Attempt(long sequence, long epochMillis, long packetNanos, int targetId, String packet, String protocol, boolean packetCancelled) {
            this.sequence = sequence;
            this.epochMillis = epochMillis;
            this.packetNanos = packetNanos;
            this.targetId = targetId;
            this.packet = packet;
            this.protocol = protocol;
            this.packetCancelled = packetCancelled;
        }

        private String csv(String outcome) {
            Double latency = damageEventNanos == null ? null : (damageEventNanos - packetNanos) / 1_000_000.0D;
            return String.join(",",
                Long.toString(sequence), Long.toString(epochMillis), quote(packet), quote(protocol), Integer.toString(targetId),
                Boolean.toString(packetCancelled), outcome, value(latency), value(serverTick), value(ping), value(distance),
                value(sprinting), value(attackerOnGround), value(damage), value(finalDamage),
                value(victimNoDamageTicksBefore), value(victimNoDamageTicksAfter),
                value(velocityBeforeX), value(velocityBeforeY), value(velocityBeforeZ),
                value(velocityAfterX), value(velocityAfterY), value(velocityAfterZ));
        }

        private static String value(Object value) {
            return value == null ? "" : String.valueOf(value);
        }

        private static String quote(String value) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
    }

    private record Report(Path path, int attempts, int accepted, int cancelled, int noEvent, double cps, double averageDistance) {}
}
