//written by etianll :D
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

public class Teleport extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Integer> reach = sgGeneral.add(new IntSetting.Builder()
            .name("Reach")
            .description("Reach")
            .defaultValue(64)
            .sliderRange(8, 64)
            .build());
    public final Setting<Double> tpTimer = sgGeneral.add(new DoubleSetting.Builder()
            .name("Timer")
            .description("The multiplier value for speed of movement.")
            .defaultValue(2)
            .min(1)
            .sliderMax(10)
            .build()
    );
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where you will be teleported.")
            .defaultValue(true)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> render.get())
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 15))
            .visible(() -> render.get())
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .visible(() -> render.get())
            .build()
    );

    public Teleport() {
        super(Trouser.Main, "Teleport", "Teleports you to where you are aiming.");
    }
    private BlockPos location;
    private Vec3d startpos;
    int ticks=1;
    private boolean notponactivateplz;

    @Override
    public void onActivate() {
        error("Press UseKey (RightClick) to Teleport to the target!");
        notponactivateplz=true;
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        if (mc.options.useKey.isPressed()){
            notponactivateplz=false;
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.useKey.setPressed(true);
            Modules.get().get(Timer.class).setOverride(tpTimer.get());
            ticks++;
            if (ticks==2 && notponactivateplz==false){
                location=target();
                startpos = mc.player.getPos();}
            else if (ticks==3 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.125), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.125)+1.0125), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.125));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks==4 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.25), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.25)+1.025), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.25));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks==5 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.375), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.375)+1.0375), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.375));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks==6 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.50), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.50)+1.05), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.50));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks==7 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.625), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.625)+1.0625), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.625));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks==8 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.75), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.75)+1.075), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.75));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks==9 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.875), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.875)+1.0875), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.875));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks==10 && notponactivateplz==false){
                mc.player.setPos(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                mc.player.setVelocity(0,0.2,0);
                mc.options.useKey.setPressed(false);
            }
        }else if (!mc.options.useKey.isPressed() && ticks<10 && notponactivateplz==false){
            mc.options.useKey.setPressed(true);
        }else if (!mc.options.useKey.isPressed() && ticks>10 && notponactivateplz==false){
            mc.options.useKey.setPressed(false);
            ticks=1;
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!mc.options.useKey.isPressed()){
        location=target();}
        if (location == null) return;
        double x1 = location.getX();
        double y1 = location.getY()+1;
        double z1 = location.getZ();
        double x2 = x1+1;
        double y2 = y1+1;
        double z2 = z1+1;


        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {toggle();}
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {toggle();}
    private BlockPos target() {
        HitResult blockHit = mc.cameraEntity.raycast(reach.get(), 0, false);
        return ((BlockHitResult) blockHit).getBlockPos();
    }

}