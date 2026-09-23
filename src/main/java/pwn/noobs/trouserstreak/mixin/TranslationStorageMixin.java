package pwn.noobs.trouserstreak.mixin;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.NoModDetection;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClientLanguage.class)
public abstract class TranslationStorageMixin {
    @Unique
    private static final Logger TROUSER_LOGGER = LogUtils.getLogger();
    @Unique
    private static final Map<String, String> KEY_TO_PACK_MAP = new HashMap<>();

    @Inject(
            method = "appendFrom(Ljava/lang/String;Ljava/util/List;Ljava/util/Map;)V",
            at = @At("HEAD")
    )
    private static void inspectResourceLoading(String languageCode, List<Resource> resources, Map<String, String> translations, CallbackInfo ci) {
        Map<String, Integer> packKeyCounts = new HashMap<>();

        for (Resource resource : resources) {
            String packId = resource.sourcePackId();
            TROUSER_LOGGER.info("Processing translation resource from Pack ID: {}", packId);
            try (InputStream stream = resource.open()) {
                Language.loadFromJson(stream, (key, value) -> {
                    KEY_TO_PACK_MAP.putIfAbsent(key, packId);
                    packKeyCounts.put(packId, packKeyCounts.getOrDefault(packId, 0) + 1);
                });
            } catch (Exception e) {
                TROUSER_LOGGER.warn("Failed to load resource stream from pack {}: {}", packId, e.getMessage());
            }
        }
    }

    @Inject(
            method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGet(String key, String fallback, CallbackInfoReturnable<String> cir) {
        // Translations can be requested before Meteor's module system is initialized.
        Modules modules = Modules.get();
        if (modules == null) {
            return;
        }

        NoModDetection module = modules.get(NoModDetection.class);
        if (module == null || !module.isActive()) {
            return;
        }

        if (key == null) {
            return;
        }

        boolean matches = false;
        NoModDetection.Modes mode = module.filterMode.get();

        if (mode == NoModDetection.Modes.AllKeys) {
            matches = true;
        } else if (mode == NoModDetection.Modes.PrefixList) {
            for (String prefix : module.keys.get()) {
                if (key.startsWith(prefix)) {
                    matches = true;
                    break;
                }
            }
        } else if (mode == NoModDetection.Modes.InstalledMods) {
            String packId = KEY_TO_PACK_MAP.get(key);
            if (packId == null || !"vanilla".equals(packId)) {
                matches = true;
            }
        }

        if (matches) {
            cir.setReturnValue(fallback != null ? fallback : key);
        }
    }
}