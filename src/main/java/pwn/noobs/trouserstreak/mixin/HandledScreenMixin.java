package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.ScreenHandlerType;
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
        GenericContainerScreen screen = (GenericContainerScreen) (Object) this;
        if (Modules.get().get(RemoteEnderChest.class).isActive() &&
                screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3 &&
                screen.getTitle().getString().toLowerCase().contains("ender")) {
            ci.cancel();
        }
    }
}
