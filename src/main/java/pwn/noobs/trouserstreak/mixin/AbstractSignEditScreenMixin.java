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

import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @Redirect(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"
            )
    )
    private Stream<String> trouserstreak$filterSignText(
            Stream<Component> stream,
            Function<? super Component, ? extends String> originalMapper
    ) {
        NoModDetection module = Modules.get().get(NoModDetection.class);

        if (module == null || !module.isActive()) {
            return stream.map(originalMapper);
        }

        return stream.map(component -> {
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
        });
    }
}