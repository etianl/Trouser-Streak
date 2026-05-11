package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(SplashManager.class)
public class TrouserSplashTextMixin {
    @Unique
    private boolean override = true;
    @Unique
    private int currentIndex = 0;
    @Unique
    private final List<String> TrouserSplashes = getTrouserSplashes();

    @Inject(method = "getSplash", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfoReturnable<SplashRenderer> cir) {
        if (Config.get() == null || !Config.get().titleScreenSplashes.get()) return;

        if (override) {
            currentIndex = new Random().nextInt(TrouserSplashes.size());
            cir.setReturnValue(new SplashRenderer(Component.nullToEmpty(TrouserSplashes.get(currentIndex))));
        }
        override = !override;
    }

    @Unique
    private static List<String> getTrouserSplashes() {
        return List.of(
                "Sorry about the turts.",
                "Sponsored by Mountains of Lava Inc!™",
                "Mods tweaked, Trousers streaked.",
                "Grief Bitches, Get Money",
                "Currently raiding your apartment for Illegal Blocks",
                "in ur servers, breaking ur blocks",
                "Steve did nothing wrong.",
                "Make Minecraft Great Again!",
                "Make Mountains, not war.",
                "Old versions of Meteor are not supported! -Meteor Client Discord",
                "Griefing is just love with a little bit of TNT.",
                "The floor is lava, the roof is lava, Everything is lava.",
                "If at first you don't succeed, just use more TNT.",
                "If at first you don't succeed, grief, grief again",
                "Stop and take time to smell the explosions.",
                "fdrgeafqrRESGJTURYKJRTDSRGHR!!!!!!",
                "ehuewifgewiudhyiewqiofyhfliuweqgfouiwq3tgodiwayghfeoiewhjikhlewhfwkuagfdlalihbflo;sdl;pihfehw083yr0328whefoiwuy3prhfjewoiyufhildwshiofc"
        );
    }
}