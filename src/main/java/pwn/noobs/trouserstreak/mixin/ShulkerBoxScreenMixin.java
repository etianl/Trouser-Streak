package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.modules.ShulkerDupe;

@Mixin(ShulkerBoxScreen.class)
public class ShulkerBoxScreenMixin extends Screen {
    public ShulkerBoxScreenMixin(Component title) {
        super(title);
    }

    @Override
    protected void init()
    {
        super.init();
        if(Modules.get().isActive(ShulkerDupe.class)) {
            addRenderableWidget(new Button.Builder(Component.literal("Dupe"), button -> dupe())
                    .pos(240, height / 2 + 35 - 140)
                    .size( 50, 15)
                    .build()
            );
            addRenderableWidget(new Button.Builder(Component.literal("Dupe All"), button -> dupeAll())
                    .pos(295, height / 2 + 35 - 140)
                    .size( 50, 15)
                    .build()
            );

    }
    }

    private void dupe() {
        ShulkerDupe.shouldDupe=true;
    }
    private void dupeAll() {
        ShulkerDupe.shouldDupeAll=true;
    }
}
