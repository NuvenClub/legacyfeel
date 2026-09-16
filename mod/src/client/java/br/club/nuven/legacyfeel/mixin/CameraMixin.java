package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
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
        if (entity != null
            && LegacyFeelConfig.get().legacyPreset
            && LegacyFeelConfig.get().instantSneakCamera
            && HandshakeClient.allows("instantSneakCamera")) {
            // Estes campos pertencem exclusivamente à câmera renderizada. A altura,
            // a pose e o raycast da entidade continuam sob controle vanilla.
            // A 1.8.9 usava 1,62 em pé e somente 1,54 agachado. A pose
            // moderna chega a 1,27 e provoca o mergulho forte ao fazer bridge.
            float target = entity.getPose() == Pose.CROUCHING ? 1.54F : entity.getEyeHeight();
            eyeHeight = target;
            eyeHeightOld = target;
        }
    }
}
