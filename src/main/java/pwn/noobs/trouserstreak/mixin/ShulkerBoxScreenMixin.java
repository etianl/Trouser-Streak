package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import pwn.noobs.trouserstreak.modules.ShulkerDupe;

@Mixin(ShulkerBoxScreen.class)
public class ShulkerBoxScreenMixin extends Screen {
    public ShulkerBoxScreenMixin(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        if (Modules.get().isActive(ShulkerDupe.class)) {
            addDrawableChild(new ButtonWidget.Builder(Text.literal("Dupe"), button -> dupe())
                    .position(240, height / 2 + 35 - 140)
                    .size(50, 15)
                    .build()
            );
            addDrawableChild(new ButtonWidget.Builder(Text.literal("Dupe All"), button -> dupeAll())
                    .position(295, height / 2 + 35 - 140)
                    .size(50, 15)
                    .build()
            );

        }
    }

    private void dupe() {
        ShulkerDupe.shouldDupe = true;
    }

    private void dupeAll() {
        ShulkerDupe.shouldDupeAll = true;
    }
}
