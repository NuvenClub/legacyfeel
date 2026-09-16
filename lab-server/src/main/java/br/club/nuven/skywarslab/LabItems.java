package br.club.nuven.skywarslab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

final class LabItems {
    private final NamespacedKey itemTypeKey;

    LabItems(Plugin plugin) {
        this.itemTypeKey = new NamespacedKey(plugin, "item_type");
    }

    ItemStack domainSeal() {
        return create(Material.ECHO_SHARD, "domain_seal", "Selo do Domínio", NamedTextColor.LIGHT_PURPLE,
                "kit/dominio/seal");
    }

    ItemStack reverseMarker() {
        return create(Material.ENDER_EYE, "reverse_marker", "Marcador de Paradoxo", NamedTextColor.AQUA,
                "kit/reverse/marker");
    }

    ItemStack miragePrism() {
        return create(Material.AMETHYST_SHARD, "mirage_prism", "Prisma de Mirage", NamedTextColor.LIGHT_PURPLE,
                "kit/mirage/prism");
    }

    ItemStack domainCore(String state) {
        return create(Material.NETHER_STAR, "domain_core", "Núcleo do Domínio", NamedTextColor.DARK_PURPLE,
                "kit/dominio/core_" + state);
    }

    boolean is(ItemStack item, String type) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        String value = item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
        return type.equals(value);
    }

    private ItemStack create(Material material, String type, String name, NamedTextColor color, String model) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.itemName(Component.text(name, color));
        meta.setItemModel(new NamespacedKey("nuven", model));
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, type);
        stack.setItemMeta(meta);
        return stack;
    }
}
