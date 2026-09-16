package br.club.nuven.legacyfeel;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;

import java.util.List;

public final class LegacyFeelClient implements ClientModInitializer {
    private static boolean forcedShieldUse;

    @Override
    public void onInitializeClient() {
        LegacyFeelConfig.load();
        HandshakeClient.register();
        ItemTooltipCallback.EVENT.register(LegacyFeelClient::replaceAxeDamageTooltip);
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.legacyfeel.toggle_preset", InputConstants.KEY_F8, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.consumeClick()) {
                LegacyFeelConfig.togglePreset();
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.translatable(
                        LegacyFeelConfig.get().legacyPreset ? "legacyfeel.preset.legacy" : "legacyfeel.preset.vanilla"));
                }
            }
            updateSneakShield(client);
        });
    }

    private static void replaceAxeDamageTooltip(net.minecraft.world.item.ItemStack stack,
                                                net.minecraft.world.item.Item.TooltipContext context,
                                                net.minecraft.world.item.TooltipFlag flag,
                                                List<Component> lines) {
        if (!LegacyFeelConfig.get().legacyPreset || !(stack.getItem() instanceof AxeItem)) return;
        Component legacyDamage = Component.literal(" ").append(Component.translatable(
            "attribute.modifier.equals.0", "6", Component.translatable("attribute.name.attack_damage")))
            .withStyle(ChatFormatting.DARK_GREEN);
        for (int i = 0; i < lines.size(); i++) {
            if (containsTranslation(lines.get(i), "attribute.name.attack_damage")) {
                lines.set(i, legacyDamage);
                return;
            }
        }
        lines.add(legacyDamage);
    }

    private static boolean containsTranslation(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if (key.equals(translated.getKey())) return true;
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Component nested && containsTranslation(nested, key)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (containsTranslation(sibling, key)) return true;
        }
        return false;
    }

    private static void updateSneakShield(net.minecraft.client.Minecraft client) {
        if (client.player == null) {
            forcedShieldUse = false;
            return;
        }

        InteractionHand shieldHand = client.player.getMainHandItem().is(Items.SHIELD)
            ? InteractionHand.MAIN_HAND
            : client.player.getOffhandItem().is(Items.SHIELD) ? InteractionHand.OFF_HAND : null;
        boolean shouldBlock = LegacyFeelConfig.get().legacyPreset
            && HandshakeClient.shieldOnSneak()
            && client.player.isCrouching()
            && shieldHand != null;

        if (forcedShieldUse && (!shouldBlock || !client.player.getUseItem().is(Items.SHIELD))) {
            client.player.stopUsingItem();
            forcedShieldUse = false;
        }
        if (shouldBlock && !client.player.isUsingItem()) {
            client.player.startUsingItem(shieldHand);
            forcedShieldUse = true;
        }
    }
}
