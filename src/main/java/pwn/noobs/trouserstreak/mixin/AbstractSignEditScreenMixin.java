package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pwn.noobs.trouserstreak.modules.NoModDetection;

import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @Redirect(
            method = "<init>(Lnet/minecraft/block/entity/SignBlockEntity;ZZLnet/minecraft/text/Text;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"
            )
    )
    private Stream<String> trouserstreak$filterSignText(
            Stream<Text> stream,
            Function<? super Text, ? extends String> originalMapper
    ) {
        NoModDetection module = Modules.get().get(NoModDetection.class);

        if (module == null || !module.isActive()) {
            return stream.map(originalMapper);
        }

        return stream.map(text -> {
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
        });
    }
}