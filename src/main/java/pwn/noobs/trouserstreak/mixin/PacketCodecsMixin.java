package pwn.noobs.trouserstreak.mixin;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.encoding.VarInts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketCodecs.class)
public interface PacketCodecsMixin {
    @Inject(method = "Lnet/minecraft/network/codec/PacketCodecs;writeCollectionSize(Lio/netty/buffer/ByteBuf;II)V", at = @At("HEAD"), cancellable = true)
    private static void writeCollectionsSize(ByteBuf buf, int size, int maxSize, CallbackInfo ci){
        VarInts.write(buf, size);
        ci.cancel();
    }
}