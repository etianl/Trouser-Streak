package pwn.noobs.trouserstreak.mixin;

import net.minecraft.text.StringVisitable;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.BoomGuard;

import java.util.function.Consumer;

/**
 * The first line of defense during the rendering phase: perform an expansion budget check at the entry point, {@code TranslatableTextContent#forEachPart}.
 * Upon a match, replace the entire segment with a placeholder and remove the original method.
 */
@Mixin(TranslatableTextContent.class)
public class TranslatableContentsMixin {

    @Inject(method = "forEachPart", at = @At("HEAD"), cancellable = true)
    private void whistle$boomGuard(String template, Consumer<StringVisitable> out, CallbackInfo ci) {
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
        out.accept(StringVisitable.plain("[BoomGuard]"));
        ci.cancel();
    }
}