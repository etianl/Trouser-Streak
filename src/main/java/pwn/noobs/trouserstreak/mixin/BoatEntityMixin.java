package pwn.noobs.trouserstreak.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;  // FIXED: Use BoatEntity directly
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.BoatNoclip;

@Mixin(BoatEntity.class)  // FIXED: Direct BoatEntity mixin
public class BoatEntityMixin {
    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true)
    private void collidesWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        BoatEntity boat = (BoatEntity)(Object)this;  // FIXED: BoatEntity cast

        if (Modules.get().isActive(BoatNoclip.class)
                && boat.getControllingPassenger() == MinecraftClient.getInstance().player) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;pressingLeft:Z", opcode = Opcodes.GETFIELD))  // FIXED: BoatEntity field target
    private boolean pressingLeft(boolean original) {
        if (Modules.get().isActive(BoatNoclip.class)) return false;
        return original;
    }

    @ModifyExpressionValue(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;pressingRight:Z", opcode = Opcodes.GETFIELD))  // FIXED: BoatEntity field target
    private boolean pressingRight(boolean original) {
        if (Modules.get().isActive(BoatNoclip.class)) return false;
        return original;
    }
}