package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.4F))
    private float legacyfeel$equipAnimationStep(float vanillaStep) {
        if (!HandshakeClient.allows("fastEquip")) return vanillaStep;
        return LegacyFeelConfig.get().equipAnimationStep();
    }
}
