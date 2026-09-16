package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void legacyfeel$restoreLegacyCrouchPose(HumanoidRenderState state, CallbackInfo ci) {
        if (!state.isCrouching || !LegacyFeelConfig.get().legacyPreset || !HandshakeClient.allows("legacyCrouch")) return;
        HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;
        model.head.y = 1.0F;
        model.body.y = 0.0F;
        model.leftArm.y = 0.0F;
        model.rightArm.y = 0.0F;
        model.leftLeg.y = 9.0F;
        model.rightLeg.y = 9.0F;
    }
}
