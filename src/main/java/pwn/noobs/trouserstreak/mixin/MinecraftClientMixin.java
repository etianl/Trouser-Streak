package pwn.noobs.trouserstreak.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.InfiniteReach;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(
        method = "doAttack",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (InfiniteReach.INSTANCE != null && InfiniteReach.INSTANCE.isActive() && InfiniteReach.INSTANCE.hoveredTarget != null) {
            InfiniteReach.INSTANCE.hitEntity(InfiniteReach.INSTANCE.hoveredTarget);
            cir.setReturnValue(true);
        }
    }
}