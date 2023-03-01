/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Mixin(SplashTextResourceSupplier.class)
public class TrouserSplashTextMixin {
    private boolean override = true;
    private final Random random = new Random();

    private final List<String> TrouserSplashes = getTrouserSplashes();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfoReturnable<String> cir) {
        if (Config.get() == null || !Config.get().titleScreenSplashes.get()) return;

        if (override) cir.setReturnValue(TrouserSplashes.get(random.nextInt(TrouserSplashes.size())));
        override = !override;
    }

    private static List<String> getTrouserSplashes() {
        return Arrays.asList(
                "Sorry about the turts.",
                "Sponsored by Mountains of Lava Inc!™",
                "Mods tweaked, Trousers streaked.",
                "Grief Bitches, Get Money",
                "Currently raiding your apartment for Illegal Blocks",
                "in ur servers, breaking ur blocks",
                "Steve did nothing wrong.",
                "Make Minecraft Great Again!",
                "Make Mountains, not war."
        );
    }

}
