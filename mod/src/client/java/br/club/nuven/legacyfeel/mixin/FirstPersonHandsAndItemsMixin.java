package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.client.player.FirstPersonHandsAndItems")
public abstract class FirstPersonHandsAndItemsMixin {
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.4F), require = 0)
    private float legacyfeel$equipAnimationStep(float vanillaStep) {
        if (!HandshakeClient.allows("fastEquip")) return vanillaStep;
        return LegacyFeelConfig.get().equipAnimationStep();
    }

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"),
        require = 0
    )
    private boolean legacyfeel$keepSwordBlockHeightStable(LocalPlayer player) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        if (config.legacyPreset && config.stableSwordBlock && HandshakeClient.allows("legacyCombat")
            && player.isUsingItem() && player.getUseItem().is(ItemTags.SWORDS)) {
            return false;
        }
        return player.isHandsBusy();
    }

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"),
        require = 0
    )
    private float legacyfeel$ignoreModernAttackDip(LocalPlayer player, float partialTick) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        if (config.legacyPreset && config.stableSwordBlock && HandshakeClient.allows("legacyCombat")
            && player.isUsingItem() && player.getUseItem().is(ItemTags.SWORDS)) {
            return 1.0F;
        }
        return player.getItemSwapScale(partialTick);
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyfeel$keepSwordRaisedWhenBlocking(InteractionHand hand, CallbackInfo ci) {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        LocalPlayer player = Minecraft.getInstance().player;
        if (!config.legacyPreset || !config.stableSwordBlock || !HandshakeClient.allows("legacyCombat")
            || player == null || !player.getItemInHand(hand).is(ItemTags.SWORDS)) return;
        ci.cancel();
    }
}
