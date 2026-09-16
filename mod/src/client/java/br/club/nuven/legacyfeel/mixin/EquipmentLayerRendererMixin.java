package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {
    @Unique private int legacyfeel$damageOverlay = OverlayTexture.NO_OVERLAY;

    @Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V", at = @At("HEAD"))
    private <S> void legacyfeel$captureDamageOverlay(EquipmentClientInfo.LayerType layerType,
                                                      ResourceKey<EquipmentAsset> equipmentAssetId, Model<? super S> model,
                                                      S state, ItemStack itemStack, PoseStack poseStack,
                                                      SubmitNodeCollector submitNodeCollector, int lightCoords,
                                                      Identifier playerTextureOverride, int outlineColor, int order,
                                                      CallbackInfo ci) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        boolean oldTint = config.legacyPreset && config.pvpAnimations && config.classicDamageTint
            && HandshakeClient.allows("legacyCombat")
            && state instanceof LivingEntityRenderState living && living.hasRedOverlay;
        legacyfeel$damageOverlay = oldTint
            ? OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true))
            : OverlayTexture.NO_OVERLAY;
    }

    @ModifyArg(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", ordinal = 0),
        index = 5
    )
    private int legacyfeel$applyDamageOverlayToArmor(int vanillaOverlay) {
        return legacyfeel$damageOverlay;
    }
}
