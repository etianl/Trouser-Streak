package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import pwn.noobs.trouserstreak.modules.LoadingTerrainDisconnect;

@Mixin(LevelLoadingScreen.class)
public class LoadingTerrainScreenMixin extends Screen {
    public LoadingTerrainScreenMixin(Text title) {
        super(title);
    }

    @Override
    protected void init()
    {
        super.init();
        LoadingTerrainDisconnect module = Modules.get().get(LoadingTerrainDisconnect.class);
        if (module != null && module.isActive()) {
            addDrawableChild(new ButtonWidget.Builder(Text.literal("Disconnect"), button -> disconnect())
                    .dimensions(this.width / 2 - module.x.get(), this.height / 4 + module.y.get() + 12, 200, 20).build()
            );
        }
    }

    private void disconnect() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            client.disconnect(Text.of("Disconnected."));
        }
    }
}