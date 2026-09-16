package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow public LocalPlayer player;
    @Shadow @Final public Options options;
    @Shadow private boolean startAttack() { throw new AssertionError(); }

    private static boolean legacyCombat() {
        return LegacyFeelConfig.get().legacyPreset && HandshakeClient.allows("legacyCombat");
    }

    @ModifyConstant(method = "startAttack", constant = @Constant(intValue = 10))
    private int legacyfeel$removeMissPenalty(int vanillaTicks) {
        return legacyCombat() ? 0 : vanillaTicks;
    }

    @Redirect(
        method = "startAttack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z")
    )
    private boolean legacyfeel$allowSwordBlockHit(LocalPlayer player) {
        boolean hasSwordAttack = player.getMainHandItem().is(ItemTags.SWORDS);
        return player.isHandsBusy() && !(legacyCombat() && (hasSwordAttack || player.getUseItem().is(ItemTags.SWORDS)));
    }

    @Inject(
        method = "handleKeybinds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
            ordinal = 0
        )
    )
    private void legacyfeel$attackWhileSwordBlocking(CallbackInfo callback) {
        if (!legacyCombat() || player == null || !player.isUsingItem()) return;
        boolean swordBlock = player.getMainHandItem().is(ItemTags.SWORDS) || player.getUseItem().is(ItemTags.SWORDS);
        while (options.keyAttack.consumeClick()) {
            if (swordBlock) {
                startAttack();
            } else if (LegacyFeelConfig.get().pvpAnimations && LegacyFeelConfig.get().swingWhileUsing) {
                legacyfeel$startVisualSwing(player);
            }
        }
    }

    private static void legacyfeel$startVisualSwing(LocalPlayer player) {
        int duration = player.getMainHandItem().getSwingAnimation().duration();
        if (!player.swinging || player.swingTime >= duration / 2 || player.swingTime < 0) {
            player.swingTime = -1;
            player.swinging = true;
            player.swingingArm = InteractionHand.MAIN_HAND;
        }
    }

    @Redirect(
        method = "startUseItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;")
    )
    private InteractionResult legacyfeel$disableShieldRightClick(MultiPlayerGameMode gameMode, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (legacyCombat() && stack.is(Items.SHIELD)) return InteractionResult.PASS;
        return gameMode.useItem(player, hand);
    }
}
