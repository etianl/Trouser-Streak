package pwn.noobs.trouserstreak.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.encoding.StringEncoding;
import net.minecraft.network.encoding.VarInts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StringEncoding.class)
public class StringEncodingMixin {
    @Inject(method ="Lnet/minecraft/network/encoding/StringEncoding;encode(Lio/netty/buffer/ByteBuf;Ljava/lang/CharSequence;I)V" , at = @At("HEAD"), cancellable = true)
    private static void writeCollectionsSize(ByteBuf buf, CharSequence string, int maxLength, CallbackInfo ci){
        int i = ByteBufUtil.utf8MaxBytes(string);
        ByteBuf byteBuf = buf.alloc().buffer(i);

        try {
            int j = ByteBufUtil.writeUtf8(byteBuf, string);
            int k = ByteBufUtil.utf8MaxBytes(maxLength);
            VarInts.write(buf, j);
            buf.writeBytes(byteBuf);
        } finally {
            byteBuf.release();
        }
        ci.cancel();
    }
}