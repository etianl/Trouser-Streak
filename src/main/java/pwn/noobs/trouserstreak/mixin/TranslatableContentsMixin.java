package pwn.noobs.trouserstreak.mixin;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.BoomGuard;

import java.util.function.Consumer;

/**
 * The first line of defense during the rendering phase: perform an expansion budget check at the entry point, {@code TranslatableContents#decomposeTemplate}.
 * Upon a match, replace the entire segment with a placeholder and remove the original method.
 */
@Mixin(TranslatableContents.class)
public class TranslatableContentsMixin {

    @Inject(method = "decomposeTemplate", at = @At("HEAD"), cancellable = true)
    private void whistle$boomGuard(String template, Consumer<FormattedText> out, CallbackInfo ci) {
        if (!BoomGuard.isEnabled()) {
            return;
        }
        if (BoomGuard.allow(BoomGuard.countSpecifiers(template))) {
            return;
        }
        int n = BoomGuard.noteBlock();
        if (n <= 3) {
            Trouser.LOG.warn(
                    "[BoomGuard] Intercepted an expansion (occurrence #{}), template length={}, conversion specifier={}",
                    n, template.length(), BoomGuard.countSpecifiers(template));
        }
        out.accept(FormattedText.of("[BoomGuard]"));
        ci.cancel();
    }
}