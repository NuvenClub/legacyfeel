package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Unique private AbstractClientPlayer legacyfeel$player;
    @Unique private ItemStack legacyfeel$stack = ItemStack.EMPTY;
    @Unique private InteractionHand legacyfeel$hand;
    @Unique private float legacyfeel$attack;

    @Shadow
    private void swingArm(float attack, PoseStack poseStack, int invert, HumanoidArm arm) {
        throw new AssertionError();
    }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.4F))
    private float legacyfeel$equipAnimationStep(float vanillaStep) {
        if (!HandshakeClient.allows("fastEquip")) return vanillaStep;
        return LegacyFeelConfig.get().equipAnimationStep();
    }

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F")
    )
    private float legacyfeel$ignoreModernAttackDip(LocalPlayer player, float partialTick) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        if (config.legacyPreset && config.stableSwordBlock && HandshakeClient.allows("legacyCombat")
            && player.isUsingItem() && player.getUseItem().is(ItemTags.SWORDS)) {
            return 1.0F;
        }
        return player.getItemSwapScale(partialTick);
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void legacyfeel$keepSwordRaisedWhenBlocking(InteractionHand hand, CallbackInfo ci) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        LocalPlayer player = minecraft.player;
        if (!config.legacyPreset || !config.stableSwordBlock || !HandshakeClient.allows("legacyCombat")
            || player == null || !player.isUsingItem() || player.getUsedItemHand() != hand
            || !player.getUseItem().is(ItemTags.SWORDS)) return;
        ci.cancel();
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void legacyfeel$captureUseAnimation(AbstractClientPlayer player, float frameInterp, float xRot,
                                                 InteractionHand hand, float attack, ItemStack itemStack,
                                                 float inverseArmHeight, PoseStack poseStack,
                                                 SubmitNodeCollector submitNodeCollector, int lightCoords,
                                                 CallbackInfo ci) {
        legacyfeel$player = player;
        legacyfeel$stack = itemStack;
        legacyfeel$hand = hand;
        legacyfeel$attack = attack;
    }

    @Inject(method = "submitArmWithItem", at = @At("TAIL"))
    private void legacyfeel$clearUseAnimation(AbstractClientPlayer player, float frameInterp, float xRot,
                                               InteractionHand hand, float attack, ItemStack itemStack,
                                               float inverseArmHeight, PoseStack poseStack,
                                               SubmitNodeCollector submitNodeCollector, int lightCoords,
                                               CallbackInfo ci) {
        legacyfeel$player = null;
        legacyfeel$stack = ItemStack.EMPTY;
        legacyfeel$hand = null;
        legacyfeel$attack = 0.0F;
    }

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void legacyfeel$composeClassicUseAndSwing(LivingEntity entity, ItemStack itemStack,
                                                       ItemDisplayContext displayContext, PoseStack poseStack,
                                                       SubmitNodeCollector submitNodeCollector, int lightCoords,
                                                       CallbackInfo ci) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        if (!config.legacyPreset || !config.pvpAnimations || !HandshakeClient.allows("legacyCombat")) return;
        if (entity != legacyfeel$player || itemStack != legacyfeel$stack || legacyfeel$hand == null) return;

        HumanoidArm arm = legacyfeel$hand == InteractionHand.MAIN_HAND
            ? legacyfeel$player.getMainArm()
            : legacyfeel$player.getMainArm().getOpposite();
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        boolean activeUse = legacyfeel$player.isUsingItem() && legacyfeel$player.getUsedItemHand() == legacyfeel$hand;

        if (config.oldRod && (itemStack.getItem() instanceof FishingRodItem || itemStack.is(Items.CARROT_ON_A_STICK))) {
            poseStack.translate(invert * 0.08F, -0.027F, -0.33F);
            poseStack.scale(0.93F, 1.0F, 1.0F);
        }
        if (config.oldBow && activeUse && itemStack.is(Items.BOW)) {
            poseStack.translate(invert * -0.01F, 0.05F, -0.06F);
        }
        if (config.oldSwing && !activeUse && legacyfeel$attack > 0.0F) {
            poseStack.scale(0.85F, 0.85F, 0.85F);
            poseStack.translate(invert * -0.06F, 0.003F, 0.05F);
        }
        if (!activeUse) return;

        ItemUseAnimation useAnimation = itemStack.getUseAnimation();

        if (config.swingWhileUsing && legacyfeel$attack > 0.0F
            && (useAnimation == ItemUseAnimation.BLOCK
                || useAnimation == ItemUseAnimation.EAT
                || useAnimation == ItemUseAnimation.DRINK)) {
            swingArm(legacyfeel$attack, poseStack, invert, arm);
        }

        if (config.oldBlockHit && useAnimation == ItemUseAnimation.BLOCK) {
            poseStack.scale(0.83F, 0.88F, 0.85F);
            poseStack.translate(invert * -0.3F, 0.1F, 0.0F);
        } else if (config.oldEat && (useAnimation == ItemUseAnimation.EAT || useAnimation == ItemUseAnimation.DRINK)) {
            poseStack.scale(0.8F, 1.0F, 1.0F);
            poseStack.translate(invert * -0.2F, -0.1F, 0.0F);
        }
    }
}
