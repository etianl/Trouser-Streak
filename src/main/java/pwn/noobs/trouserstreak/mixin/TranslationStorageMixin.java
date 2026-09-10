package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.NoModDetection;

@Mixin(TranslationStorage.class)
public abstract class TranslationStorageMixin {

    @Inject(
            method = "get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGet(String key, String fallback, CallbackInfoReturnable<String> cir) {
        NoModDetection module = Modules.get().get(NoModDetection.class);
        if (module == null || !module.isActive()) {
            return;
        }

        if (key == null) {
            return;
        }
        // Most of these keys were gathered from https://github.com/branduzzo/CheckHacks/blob/main/src/main/resources/checkhacks.yml
        // Thank you to the CheckHacks plugin for providing the things needed to prevent their own plugin from working
        if (key.startsWith("key.meteor-client.")
                || key.startsWith("module.")
                || key.startsWith("addon.")
                || key.startsWith("liquidbounce.module")
                || key.startsWith("key.freecam")
                || key.startsWith("key.wurst")
                || key.startsWith("xray.config")
                || key.startsWith("key.chestesp")
                || key.startsWith("key.killaura")
                || key.startsWith("key.autofish")
                || key.startsWith("key.lumina")
                || key.startsWith("bleachhack.module")
                || key.startsWith("emc.module")
                || key.startsWith("coffee.module")
                || key.startsWith("key.wdl")
                || key.startsWith("autoclicker-fabric.")
                || key.startsWith("key.antiafk")
                || key.startsWith("key.auto-clicker_")
                || key.startsWith("key.ui-utils")
                || key.startsWith("cracker.")
                || key.startsWith("swd.")
                || key.startsWith("litematica.")
                || key.startsWith("gui.xaero")
                || key.startsWith("baritone.")
                || key.startsWith("voicechat.")
        ) {
            cir.setReturnValue(fallback != null ? fallback : key);
        }
    }
}