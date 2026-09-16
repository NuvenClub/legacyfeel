package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import br.club.nuven.legacyfeel.render.ClassicSwordBlockingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemInHandLayer.class)
public abstract class PlayerItemInHandLayerMixin<S extends AvatarRenderState,
    M extends EntityModel<S> & ArmedModel<S> & HeadedModel> extends ItemInHandLayer<S, M> {

    protected PlayerItemInHandLayerMixin(RenderLayerParent<S, M> parent) {
        super(parent);
    }

    @Inject(
        method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacyfeel$renderSwordAcrossChest(S state, ItemStackRenderState item, ItemStack itemStack,
                                                    HumanoidArm arm, PoseStack poseStack,
                                                    SubmitNodeCollector submitNodeCollector, int lightCoords,
                                                    CallbackInfo ci) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        if (!config.legacyPreset || !config.pvpAnimations || !config.classicThirdPersonBlock
            || !HandshakeClient.allows("legacyCombat") || item.isEmpty() || !itemStack.is(ItemTags.SWORDS)
            || !state.isUsingItem) return;

        InteractionHand hand = arm == state.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (state.useItemHand != hand) return;

        ClassicSwordBlockingRenderer.submit(state, getParentModel(), item, arm, poseStack,
            submitNodeCollector, lightCoords, state.outlineColor);
        ci.cancel();
    }
}
