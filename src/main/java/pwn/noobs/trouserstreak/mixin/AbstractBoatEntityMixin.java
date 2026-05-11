package pwn.noobs.trouserstreak.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.BoatNoclip;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin({AbstractBoat.class})
public class AbstractBoatEntityMixin {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void collidesWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        AbstractBoat boat = (AbstractBoat)(Object)this;

        if (Modules.get().isActive(BoatNoclip.class)
                && boat.getControllingPassenger() == mc.player) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "controlBoat", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;inputLeft:Z", opcode = Opcodes.GETFIELD))
    private boolean pressingLeft(boolean original) {
        if (Modules.get().isActive(BoatNoclip.class)) return false;
        return original;
    }

    @ModifyExpressionValue(method = "controlBoat", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;inputRight:Z", opcode = Opcodes.GETFIELD))
    private boolean pressingRight(boolean original) {
        if (Modules.get().isActive(BoatNoclip.class)) return false;
        return original;
    }
}