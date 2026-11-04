package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import pwn.noobs.trouserstreak.modules.NoModDetection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @Redirect(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"
            )
    )
    private String trouserstreak$filterSignText(Component component) {
        NoModDetection module = Modules.get().get(NoModDetection.class);

        if (module == null || !module.isActive() || !module.filterSign.get()) {
            return component.getString();
        }

        if (component.getContents() instanceof TranslatableContents translatable) {
            String fallback = translatable.getFallback();

            return fallback != null
                    ? fallback
                    : translatable.getKey();
        }

        if (component.getContents() instanceof KeybindContents) {
            return "";
        }

        return component.getString();
    }
}