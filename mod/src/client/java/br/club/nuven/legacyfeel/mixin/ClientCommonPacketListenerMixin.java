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
            access.legacyfeel$markUseItemOnSequence(legacyfeel$sequence(useItemOn));
        }
    }

    private static int legacyfeel$sequence(ServerboundUseItemOnPacket packet) {
        try {
            try {
                return (int) packet.getClass().getMethod("getSequence").invoke(packet);
            } catch (NoSuchMethodException ignored) {
                return (int) packet.getClass().getMethod("sequence").invoke(packet);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Não foi possível ler a sequência de uso do bloco", exception);
        }
    }
}
