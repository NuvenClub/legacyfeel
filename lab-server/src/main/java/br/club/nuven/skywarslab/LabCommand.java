package br.club.nuven.skywarslab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class LabCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT = List.of("status", "pack", "enter", "leave", "kit", "cage",
            "projectile", "effects", "cleanup");

    private final SkyWarsLabPlugin plugin;
    private final ResourcePackGate packGate;
    private final LabSessionManager sessions;
    private final DomainService domain;
    private final ReverseService reverse;
    private final MirageService mirage;
    private final CosmeticsService cosmetics;

    LabCommand(SkyWarsLabPlugin plugin, ResourcePackGate packGate, LabSessionManager sessions,
               DomainService domain, ReverseService reverse, MirageService mirage, CosmeticsService cosmetics) {
        this.plugin = plugin;
        this.packGate = packGate;
        this.sessions = sessions;
        this.domain = domain;
        this.reverse = reverse;
        this.mirage = mirage;
        this.cosmetics = cosmetics;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("cleanup")) {
            if (!sender.hasPermission("skywarslab.admin")) return noPermission(sender);
            plugin.cleanupAll();
            sender.sendMessage(Component.text("Recursos do Laboratório removidos.", NamedTextColor.GREEN));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando requer um jogador.");
            return true;
        }
        if (!player.hasPermission("skywarslab.play")) return noPermission(sender);

        switch (sub) {
            case "status" -> showStatus(player);
            case "pack" -> packGate.send(player);
            case "enter" -> sessions.enter(player);
            case "leave" -> {
                plugin.cleanupPlayer(player);
                sessions.leave(player);
                player.sendMessage(Component.text("Você saiu da experiência do Laboratório.", NamedTextColor.GRAY));
            }
            case "kit" -> selectKit(player, args);
            case "cage" -> selectCage(player, args);
            case "projectile" -> selectProjectile(player, args);
            case "effects" -> selectEffects(player, args);
            default -> player.sendMessage(Component.text("Use /swlab " + String.join("|", ROOT), NamedTextColor.YELLOW));
        }
        return true;
    }

    private void showStatus(Player player) {
        LabPlayerState state = sessions.state(player);
        player.sendMessage(Component.text("SkyWars Laboratório", NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(Component.text("Ruleset: " + plugin.getConfig().getString("ruleset-id"), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Pacote: " + packGate.state(player), packGate.isReady(player) ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Entrada: " + (state.enrolled ? "ativa" : "inativa"), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Kit: " + state.kit.displayName(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Jaula: " + state.cage.displayName(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Projétil: " + state.projectile.displayName(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Efeitos: " + (state.reducedEffects ? "reduzidos" : "completos"), NamedTextColor.GRAY));
    }

    private void selectKit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Kits: dominio, reverso, mirage", NamedTextColor.YELLOW));
            return;
        }
        KitType.parse(args[1]).ifPresentOrElse(kit -> {
            plugin.cleanupPlayer(player);
            sessions.selectKit(player, kit);
            player.sendMessage(Component.text("Kit selecionado: " + kit.displayName(), NamedTextColor.GREEN));
        }, () -> player.sendMessage(Component.text("Kit desconhecido.", NamedTextColor.RED)));
    }

    private void selectCage(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Jaulas: prisma, cryo, confetti, sanctuary", NamedTextColor.YELLOW));
            return;
        }
        CageCosmetic.parse(args[1]).ifPresentOrElse(cage -> {
            sessions.state(player).cage = cage;
            cosmetics.previewCage(player, cage);
            player.sendMessage(Component.text("Jaula selecionada: " + cage.displayName(), NamedTextColor.GREEN));
        }, () -> player.sendMessage(Component.text("Jaula desconhecida.", NamedTextColor.RED)));
    }

    private void selectProjectile(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Projéteis: flames, ninja, mystic, cloud", NamedTextColor.YELLOW));
            return;
        }
        ProjectileCosmetic.parse(args[1]).ifPresentOrElse(cosmetic -> {
            sessions.state(player).projectile = cosmetic;
            player.sendMessage(Component.text("Projétil selecionado: " + cosmetic.displayName(), NamedTextColor.GREEN));
        }, () -> player.sendMessage(Component.text("Cosmético desconhecido.", NamedTextColor.RED)));
    }

    private void selectEffects(Player player, String[] args) {
        if (args.length < 2 || (!args[1].equalsIgnoreCase("full") && !args[1].equalsIgnoreCase("reduced"))) {
            player.sendMessage(Component.text("Use /swlab effects <full|reduced>", NamedTextColor.YELLOW));
            return;
        }
        sessions.state(player).reducedEffects = args[1].equalsIgnoreCase("reduced");
        player.sendMessage(Component.text("Efeitos " + (sessions.reducedEffects(player) ? "reduzidos" : "completos") + ".",
                NamedTextColor.GREEN));
    }

    private static boolean noPermission(CommandSender sender) {
        sender.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> values;
        if (args.length == 1) values = ROOT;
        else if (args.length == 2) values = switch (args[0].toLowerCase(Locale.ROOT)) {
            case "kit" -> List.of("dominio", "reverso", "mirage");
            case "cage" -> List.of("prisma", "cryo", "confetti", "sanctuary");
            case "projectile" -> List.of("flames", "ninja", "mystic", "cloud");
            case "effects" -> List.of("full", "reduced");
            default -> List.of();
        };
        else return List.of();
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.startsWith(prefix)) result.add(value);
        return result;
    }
}
