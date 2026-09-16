package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
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
