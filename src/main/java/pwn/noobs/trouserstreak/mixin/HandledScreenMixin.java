package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.modules.RemoteEnderChest;

@Mixin(GenericContainerScreen.class)
public class HandledScreenMixin {
    @Inject(method = "drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V",
            at = @At("HEAD"), cancellable = true)
    private void onDrawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (Modules.get().get(RemoteEnderChest.class) != null && Modules.get().get(RemoteEnderChest.class).isActive()) {
            ci.cancel();  // Skip chest texture
        }
    }
}