package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pwn.noobs.trouserstreak.modules.NoModDetection;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @Redirect(
            method = "onSlotUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/text/Text;getString()Ljava/lang/String;"
            )
    )
    private String trouserstreak$filterAnvilSlotName(Text text) {
        return trouserstreak$sanitizeAnvilText(text);
    }

    @Redirect(
            method = "onRenamed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/text/Text;getString()Ljava/lang/String;"
            )
    )
    private String trouserstreak$filterAnvilRename(Text text) {
        return trouserstreak$sanitizeAnvilText(text);
    }

    @Unique
    private static String trouserstreak$sanitizeAnvilText(Text text) {
        NoModDetection module = Modules.get().get(NoModDetection.class);

        if (module == null || !module.isActive() || !module.filterAnvil.get()) {
            return text.getString();
        }

        if (text.getContent() instanceof TranslatableTextContent translatable) {
            String fallback = translatable.getFallback();

            return fallback != null
                    ? fallback
                    : translatable.getKey();
        }

        if (text.getContent() instanceof KeybindTextContent) {
            return "";
        }

        return text.getString();
    }
}