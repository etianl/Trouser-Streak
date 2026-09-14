package pwn.noobs.trouserstreak.mixin;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ClientLanguage.class)
public interface ClientLanguageAccessor {
    @Accessor("translations") // adjust name if your mappings differ
    Map<String, String> getTranslationsMap();
}