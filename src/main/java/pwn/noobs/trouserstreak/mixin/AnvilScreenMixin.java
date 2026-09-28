package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pwn.noobs.trouserstreak.modules.NoModDetection;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @Redirect(
            method = "slotChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"
            )
    )
    private String trouserstreak$filterAnvilSlotName(Component component) {
        return trouserstreak$sanitizeAnvilText(component);
    }

    @Redirect(
            method = "onNameChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"
            )
    )
    private String trouserstreak$filterAnvilRename(Component component) {
        return trouserstreak$sanitizeAnvilText(component);
    }

    @Unique
    private static String trouserstreak$sanitizeAnvilText(Component component) {
        NoModDetection module = Modules.get().get(NoModDetection.class);

        if (module == null || !module.isActive() || !module.filterAnvil.get()) {
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