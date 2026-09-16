package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(
        method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;setupAttackAnimation(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V"
        )
    )
    private void legacyfeel$restoreClassicSwordBlockPose(HumanoidRenderState state, CallbackInfo ci) {
        if (!state.isUsingItem || !LegacyFeelConfig.get().legacyPreset || !HandshakeClient.allows("legacyCombat")) return;
        boolean rightHanded = state.mainArm == HumanoidArm.RIGHT;
        boolean mainHandUsed = state.useItemHand == InteractionHand.MAIN_HAND;
        HumanoidArm arm = mainHandUsed == rightHanded ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        if (!state.getUseItemStackForArm(arm).is(ItemTags.SWORDS)) return;

        HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;
        var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot -= Mth.clamp(model.head.xRot, (float)(-Math.PI * 4.0D / 9.0D), 0.43633232F);
        part.yRot = 0.0F;
    }

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
