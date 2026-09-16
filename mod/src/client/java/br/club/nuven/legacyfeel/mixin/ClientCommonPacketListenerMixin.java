package br.club.nuven.legacyfeel.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerMixin {
    @Inject(method = "send", at = @At("HEAD"))
    private void legacyfeel$trackUseItemOnSequence(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ServerboundUseItemOnPacket useItemOn)) return;
        if (Minecraft.getInstance().level instanceof ClassicBlockPredictionAccess access) {
            access.legacyfeel$markUseItemOnSequence(useItemOn.getSequence());
        }
    }
}
