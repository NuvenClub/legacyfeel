package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Inject(method = "submitFire", at = @At("HEAD"))
    private static void legacyfeel$lowerFire(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        TextureAtlasSprite sprite,
        CallbackInfo callback
    ) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        if (config.legacyPreset && config.lowFire) poseStack.translate(0.0F, config.fireOverlayOffset, 0.0F);
    }
}
