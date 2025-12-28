package pwn.noobs.trouserstreak.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.BoatNoclip;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin({AbstractBoatEntity.class})
public class AbstractBoatEntityMixin {
    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true)
    private void collidesWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        AbstractBoatEntity boat = (AbstractBoatEntity)(Object)this;

        if (Modules.get().isActive(BoatNoclip.class)
                && boat.getControllingPassenger() == mc.player) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;pressingLeft:Z", opcode = Opcodes.GETFIELD))
    private boolean pressingLeft(boolean original) {
        if (Modules.get().isActive(BoatNoclip.class)) return false;
        return original;
    }

    @ModifyExpressionValue(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;pressingRight:Z", opcode = Opcodes.GETFIELD))
    private boolean pressingRight(boolean original) {
        if (Modules.get().isActive(BoatNoclip.class)) return false;
        return original;
    }
}