package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AntiHunger;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.modules.InstantKill;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(at = @At("HEAD"), method = "stopUsingItem")
    public void onStopUsingItem(PlayerEntity player, CallbackInfo ci) {
        if ((Modules.get().get(InstantKill.class).isActive())) {
        }
        if (Modules.get().isActive(InstantKill.class)) {
            if (player.getInventory().getMainHandStack().getItem().equals(Items.BOW)) {
                if (Modules.get().get(AntiHunger.class).isActive()) {
                    Modules.get().get(AntiHunger.class).toggle();
                }
                InstantKill.addVelocityToPlayer();
            }
            if (player.getInventory().getMainHandStack().getItem().equals(Items.TRIDENT)) {
                if (Modules.get().get(AntiHunger.class).isActive()) {
                    Modules.get().get(AntiHunger.class).toggle();
                }
                InstantKill.addVelocityToPlayer();
            }
        }
    }
}

