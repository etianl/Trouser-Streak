package pwn.noobs.trouserstreak.mixin;

import pwn.noobs.trouserstreak.modules.BoatNoclip;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class AbstractBlockMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!(context instanceof EntityCollisionContext esc)) return;
        if (esc.getEntity() == null) return;

        Entity entity = esc.getEntity();
        if (Modules.get() == null) return;

        if (Modules.get().isActive(BoatNoclip.class)
            && entity instanceof Boat boat
            && boat.isClientAuthoritative()) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}