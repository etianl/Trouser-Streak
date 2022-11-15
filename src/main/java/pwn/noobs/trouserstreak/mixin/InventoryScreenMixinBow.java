package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import pwn.noobs.trouserstreak.modules.DupeModule;
import pwn.noobs.trouserstreak.modules.InstantKill;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import java.awt.Color;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixinBow extends Screen{

    protected InventoryScreenMixinBow(Text title){
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void onInit(CallbackInfo ci){
        if(Modules.get().isActive(InstantKill.class)) {
            ButtonWidget toggle = new ButtonWidget(1, 1, 90, 12, Text.of("MovePackets: " + (InstantKill.shouldAddVelocity0 ? "50" : "OFF")), b -> {
                InstantKill.shouldAddVelocity0 = !InstantKill.shouldAddVelocity0;
                InstantKill.mc.setScreen(new InventoryScreen(InstantKill.mc.player));
            });
            this.addDrawableChild(toggle);
        }
        if(Modules.get().isActive(InstantKill.class)) {
        ButtonWidget toggle = new ButtonWidget(1, 13, 90, 12, Text.of("MovePackets: " + (InstantKill.shouldAddVelocity ? "100" : "OFF")), b -> {
            InstantKill.shouldAddVelocity = !InstantKill.shouldAddVelocity;
            InstantKill.mc.setScreen(new InventoryScreen(InstantKill.mc.player));
        });
        this.addDrawableChild(toggle);
        }
        if(Modules.get().isActive(InstantKill.class)) {
            ButtonWidget toggle = new ButtonWidget(1, 25, 90, 12, Text.of("MovePackets: " + (InstantKill.shouldAddVelocity1 ? "150" : "OFF")), b -> {
                InstantKill.shouldAddVelocity1 = !InstantKill.shouldAddVelocity1;
                InstantKill.mc.setScreen(new InventoryScreen(InstantKill.mc.player));
            });
            this.addDrawableChild(toggle);
        }
        if(Modules.get().isActive(InstantKill.class)) {
            ButtonWidget toggle = new ButtonWidget(1, 37, 90, 12, Text.of("MovePackets: " + (InstantKill.shouldAddVelocity2 ? "200" : "OFF")), b -> {
                InstantKill.shouldAddVelocity2 = !InstantKill.shouldAddVelocity2;
                InstantKill.mc.setScreen(new InventoryScreen(InstantKill.mc.player));
            });
            this.addDrawableChild(toggle);
        }
        if(Modules.get().isActive(InstantKill.class)) {
            ButtonWidget toggle = new ButtonWidget(1, 49, 90, 12, Text.of("MovePackets: " + (InstantKill.shouldAddVelocity3 ? "300" : "OFF")), b -> {
                InstantKill.shouldAddVelocity3 = !InstantKill.shouldAddVelocity3;
                InstantKill.mc.setScreen(new InventoryScreen(InstantKill.mc.player));
            });
            this.addDrawableChild(toggle);
        }
    }
}