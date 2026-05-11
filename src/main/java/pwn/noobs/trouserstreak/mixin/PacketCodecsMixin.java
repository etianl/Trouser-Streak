package pwn.noobs.trouserstreak.mixin;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ByteBufCodecs.class)
public interface PacketCodecsMixin {
    @Inject(method = "writeCount(Lio/netty/buffer/ByteBuf;II)V", at = @At("HEAD"), cancellable = true)
    private static void writeCollectionsSize(ByteBuf buf, int size, int maxSize, CallbackInfo ci){
        VarInt.write(buf, size);
        ci.cancel();
    }
}