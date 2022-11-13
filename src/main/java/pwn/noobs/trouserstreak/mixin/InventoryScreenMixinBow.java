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
        ButtonWidget toggle = new ButtonWidget(1, 1, 100, 20, Text.of("InstantKill: " + (InstantKill.shouldAddVelocity ? "On" : "Off")), b -> {
            InstantKill.shouldAddVelocity = !InstantKill.shouldAddVelocity;
            InstantKill.mc.setScreen(new InventoryScreen(InstantKill.mc.player));
        });
        this.addDrawableChild(toggle);
    }}

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(MatrixStack matrices, int a, int b, float d, CallbackInfo ci){
        if(Modules.get().isActive(InstantKill.class)) {
            int pp = this.textRenderer.getWidth("Made by Saturn5Vfive <3") / 2;
            drawCenteredText(matrices, textRenderer, Text.of("Made by Saturn5Vfive <3"), pp + 5, InstantKill.mc.getWindow().getScaledHeight() - 10, new Color(255, 255, 255, 255).getRGB());
        }}
}