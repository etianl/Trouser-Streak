package pwn.noobs.trouserstreak.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.utils.MovementPatch;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {

    @Shadow
    public abstract void send(Packet<?> packet);

    @Unique
    private static final ThreadLocal<Boolean> trouser$sendingTickEnd =
            ThreadLocal.withInitial(() -> false);

    @Unique
    private boolean trouser$lastPacketWasTickEnd;

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD")
    )
    private void trouser$sendTickEndBeforePositionMove(Packet<?> packet, CallbackInfo ci) {
        trouser$lastPacketWasTickEnd = packet instanceof ServerboundClientTickEndPacket;

        if (!MovementPatch.isEnabled()) {
            return;
        }

        if (!(packet instanceof ServerboundMovePlayerPacket movePacket) || !movePacket.hasPosition()) {
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
            send(ServerboundClientTickEndPacket.INSTANCE);
        } finally {
            trouser$sendingTickEnd.set(false);
            trouser$lastPacketWasTickEnd = false;
        }
    }
}