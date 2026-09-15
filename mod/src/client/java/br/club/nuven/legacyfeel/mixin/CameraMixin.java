package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Entity entity;
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;

    @Inject(method = "tick", at = @At("TAIL"))
    private void legacyfeel$useImmediateVisualEyeHeight(CallbackInfo ci) {
        if (entity != null && LegacyFeelConfig.get().instantSneakCamera) {
            // Estes campos pertencem exclusivamente à câmera renderizada. A altura,
            // a pose e o raycast da entidade continuam sob controle vanilla.
            float target = entity.getEyeHeight();
            eyeHeight = target;
            eyeHeightOld = target;
        }
    }
}
