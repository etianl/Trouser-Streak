package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import pwn.noobs.trouserstreak.modules.LoadingTerrainDisconnect;

@Mixin(LevelLoadingScreen.class)
public class LoadingTerrainScreenMixin extends Screen {
    public LoadingTerrainScreenMixin(Component title) {
        super(title);
    }

    @Override
    protected void init()
    {
        super.init();
        LoadingTerrainDisconnect module = Modules.get().get(LoadingTerrainDisconnect.class);
        if (module != null && module.isActive()) {
            addRenderableWidget(new Button.Builder(Component.literal("Disconnect"), button -> disconnect())
                    .bounds(this.width / 2 - module.x.get(), this.height / 4 + module.y.get() + 12, 200, 20).build()
            );
        }
    }

    private void disconnect() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() != null) {
            client.disconnectFromWorld(Component.nullToEmpty("Disconnected."));
        }
    }
}