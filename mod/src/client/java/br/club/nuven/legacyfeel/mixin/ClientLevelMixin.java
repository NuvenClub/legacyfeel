package br.club.nuven.legacyfeel.mixin;

import br.club.nuven.legacyfeel.LegacyFeelSounds;
import br.club.nuven.legacyfeel.config.LegacyFeelConfig;
import br.club.nuven.legacyfeel.network.HandshakeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(
        method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacyfeel$restoreClassicNetworkRodSounds(Entity except, double x, double y, double z,
                                                            Holder<SoundEvent> sound, SoundSource source,
                                                            float volume, float pitch, long seed, CallbackInfo ci) {
        if (!legacyfeel$classicRodSoundsEnabled()) return;
        if (sound.value() == SoundEvents.FISHING_BOBBER_RETRIEVE) {
            ci.cancel();
            return;
        }
        if (sound.value() != SoundEvents.FISHING_BOBBER_THROW) return;

        ci.cancel();
        if (except == minecraft.player) {
            ((ClientLevel)(Object)this).playLocalSound(
                x, y, z, LegacyFeelSounds.CLASSIC_ROD_THROW, source, 0.5F, pitch, false);
        }
    }

    @Inject(
        method = "playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacyfeel$restoreClassicRodSounds(double x, double y, double z, SoundEvent sound,
                                                     SoundSource source, float volume, float pitch,
                                                     boolean distanceDelay, CallbackInfo ci) {
        if (!legacyfeel$classicRodSoundsEnabled()) return;

        if (sound == SoundEvents.FISHING_BOBBER_RETRIEVE) {
            ci.cancel();
            return;
        }
        if (sound != SoundEvents.FISHING_BOBBER_THROW) return;

        ci.cancel();
        ((ClientLevel)(Object)this).playLocalSound(
            x, y, z, LegacyFeelSounds.CLASSIC_ROD_THROW, source, 0.5F, pitch, distanceDelay);
    }

    private static boolean legacyfeel$classicRodSoundsEnabled() {
        LegacyFeelConfig config = LegacyFeelConfig.get();
        return config.legacyPreset && config.pvpAnimations && config.classicRodSounds
            && HandshakeClient.allows("legacyCombat");
    }
}
