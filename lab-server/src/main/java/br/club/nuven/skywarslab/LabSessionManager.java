package br.club.nuven.skywarslab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class LabSessionManager {
    private final ResourcePackGate packGate;
    private final LabItems items;
    private final Map<UUID, LabPlayerState> players = new ConcurrentHashMap<>();

    LabSessionManager(ResourcePackGate packGate, LabItems items) {
        this.packGate = packGate;
        this.items = items;
    }

    LabPlayerState state(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), ignored -> new LabPlayerState());
    }

    boolean enter(Player player) {
        if (!packGate.isReady(player)) {
            player.sendMessage(Component.text("Aguarde o pacote obrigatório carregar antes de entrar.", NamedTextColor.RED));
            return false;
        }
        LabPlayerState state = state(player);
        if (state.enrolled) {
            player.sendMessage(Component.text("Você já está na experiência do Laboratório.", NamedTextColor.YELLOW));
            return true;
        }
        state.enrolled = true;
        giveKitItem(player, state.kit);
        player.sendMessage(Component.text("SkyWars Laboratório ativo • " + state.kit.displayName(), NamedTextColor.GREEN));
        return true;
    }

    void leave(Player player) {
        LabPlayerState state = players.get(player.getUniqueId());
        if (state != null) state.enrolled = false;
    }

    void remove(Player player) {
        players.remove(player.getUniqueId());
    }

    boolean isEnrolled(Player player) {
        return state(player).enrolled;
    }

    boolean reducedEffects(Player player) {
        return state(player).reducedEffects;
    }

    KitType kit(Player player) {
        return state(player).kit;
    }

    void selectKit(Player player, KitType kit) {
        LabPlayerState state = state(player);
        state.kit = kit;
        if (state.enrolled) giveKitItem(player, kit);
    }

    private void giveKitItem(Player player, KitType kit) {
        switch (kit) {
            case DOMAIN -> player.getInventory().addItem(items.domainSeal());
            case REVERSE -> player.getInventory().addItem(items.reverseMarker());
            case MIRAGE -> player.getInventory().addItem(items.miragePrism());
        }
    }
}
