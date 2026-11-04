package pwn.noobs.trouserstreak.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.utils.MovementPatch;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonPacketListenerImplMixin {

    @Shadow
    public abstract void sendPacket(Packet<?> packet);

    @Unique
    private static final ThreadLocal<Boolean> trouser$sendingTickEnd =
            ThreadLocal.withInitial(() -> false);

    @Unique
    private boolean trouser$lastPacketWasTickEnd;

    @Inject(
            method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD")
    )
    private void trouser$sendTickEndBeforePositionMove(Packet<?> packet, CallbackInfo ci) {
        trouser$lastPacketWasTickEnd = packet instanceof ClientTickEndC2SPacket;

        if (!MovementPatch.isEnabled()) {
            return;
        }

        if (!(packet instanceof PlayerMoveC2SPacket movePacket) || !movePacket.changesPosition()) {
            return;
        }

        if (trouser$sendingTickEnd.get()) {
            return;
        }

        if (trouser$lastPacketWasTickEnd) {
            trouser$lastPacketWasTickEnd = false;
            return;
        }

        trouser$sendingTickEnd.set(true);

        try {
            sendPacket(ClientTickEndC2SPacket.INSTANCE);
        } finally {
            trouser$sendingTickEnd.set(false);
            trouser$lastPacketWasTickEnd = false;
        }
    }
}