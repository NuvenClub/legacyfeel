package br.club.nuven.legacyfeel;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/** Aplica os modelos 26.x aos itens marcados pelo backend SkyWars 1.8. */
final class LabItemVisuals {
    private LabItemVisuals() {
    }

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++) {
                apply(client.player.getInventory().getItem(slot));
            }
        });
    }

    private static void apply(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String model = null;

        if (stack.is(Items.BEACON) && (name.contains("dominio") || name.contains("domínio"))) {
            model = "kit/dominio/seal";
        } else if ((stack.is(Items.SNOWBALL) || stack.is(Items.ENDER_EYE))
                && (name.contains("marcador") || name.contains("invers") || name.contains("revers"))) {
            model = "kit/reverse/marker";
        } else if (stack.is(Items.CLOCK) && (name.contains("espionagem") || name.contains("mirage"))) {
            model = "kit/mirage/prism";
        }

        if (model != null) {
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("nuven", model));
        }
    }
}
