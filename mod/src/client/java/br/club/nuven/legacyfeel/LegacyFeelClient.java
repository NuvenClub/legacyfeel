package br.club.nuven.legacyfeel;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class LegacyFeelClient implements ClientModInitializer {
    private static boolean forcedShieldUse;

    @Override
    public void onInitializeClient() {
        LegacyFeelConfig.load();
        HandshakeClient.register();
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
