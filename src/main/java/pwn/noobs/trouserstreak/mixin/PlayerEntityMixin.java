package pwn.noobs.trouserstreak.mixin;

import pwn.noobs.trouserstreak.events.OffGroundSpeedEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerEntityMixin {
    @Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
    private void onGetOffGroundSpeed(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(MeteorClient.EVENT_BUS.post(OffGroundSpeedEvent.get(cir.getReturnValueF())).speed);
    }
}