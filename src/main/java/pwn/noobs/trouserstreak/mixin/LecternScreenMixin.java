package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.modules.LecternCrash;

@Mixin(LecternScreen.class)
public class LecternScreenMixin extends Screen {
    protected LecternScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if(Modules.get().isActive(LecternCrash.class)){
        this.addRenderableWidget(new Button.Builder(Component.nullToEmpty("CrashServer"), (button) -> {
            // Send the quick-move click through the game's own click handling. Since 1.21.5 the click
            // packet only carries hashed item data, which ViaFabricPlus cannot remap to older protocols
            // when a mod writes the packet to the channel directly. handleContainerInput lets the
            // client (and ViaFabricPlus) translate the click into whatever format the server expects.
            minecraft.gameMode.handleContainerInput(minecraft.player.containerMenu.containerId, 0, 0, ContainerInput.QUICK_MOVE, minecraft.player);
            minecraft.player.sendSystemMessage(Component.nullToEmpty("Crashing Server..."));
            button.active = false;
        })
                        .pos(5, 25)
                        .size(100, 20)
                        .build()
        );
        }
    }
}
