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
    private static final ThreadLocal<Boolean> trouser$sendingTickEnd = ThreadLocal.withInitial(() -> false);

    @Unique
    private boolean trouser$positionSentSinceTickEnd;

    @Inject(
            method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD")
    )

    private void trouser$sendTickEndBeforeDuplicatePosition(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientTickEndC2SPacket) {
            trouser$positionSentSinceTickEnd = false;
            return;
        }

        if (!MovementPatch.isEnabled()) return;

        if (!(packet instanceof PlayerMoveC2SPacket movePacket)
                || !movePacket.changesPosition()) {
            return;
        }

        if (trouser$sendingTickEnd.get()) return;

        if (!trouser$positionSentSinceTickEnd) {
            trouser$positionSentSinceTickEnd = true;
            return;
        }

        trouser$sendingTickEnd.set(true);

        try {
            sendPacket(ClientTickEndC2SPacket.INSTANCE);
        } finally {
            trouser$sendingTickEnd.set(false);
        }

        trouser$positionSentSinceTickEnd = true;
    }
}
