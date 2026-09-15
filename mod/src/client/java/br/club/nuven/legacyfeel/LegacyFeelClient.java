package br.club.nuven.legacyfeel;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public final class LegacyFeelClient implements ClientModInitializer {
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
        });
    }
}
