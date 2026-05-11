//written by etianll :D
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

public class Teleport extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Integer> reach = sgGeneral.add(new IntSetting.Builder()
            .name("reach")
            .description("Reach")
            .defaultValue(48)
            .sliderRange(8,96)
            .min (8)
            .max (96)
            .build());
    public final Setting<Double> tpTimer = sgGeneral.add(new DoubleSetting.Builder()
            .name("timer")
            .description("The multiplier value for speed of movement.")
            .defaultValue(2)
            .sliderRange(1,10)
            .build()
    );
    private final Setting<Boolean> liquids = sgGeneral.add(new BoolSetting.Builder()
            .name("tp-ontop-of-liquid")
            .description("TP you ontop of, or through the liquids.")
            .defaultValue(true)
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
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color-solid-block")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 15))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color-solid-block")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .visible(render::get)
            .build()
    );
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder()
            .name("side-color-non-solid")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(0, 255, 255, 15))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder()
            .name("line-color-non-solid")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(0, 255, 255, 255))
            .visible(render::get)
            .build()
    );

    public Teleport() {
        super(Trouser.Main, "teleport", "Teleports you to where you are aiming.");
    }
    private BlockPos location;
    private Vec3 startpos;
    int ticks = 0;
    private boolean TPnow;
    private boolean notponactivateplz;

    @Override
    public void onActivate() {
        notponactivateplz=true;
        error("press attack (left click) to teleport ontop of the target!");
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }
    @EventHandler
    private void onMouseButton(MouseClickEvent event) {
        if (mc.options.keyAttack.isDown()){
            notponactivateplz=false;
            TPnow=true;
            ticks=0;
        }
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        if (TPnow && !notponactivateplz && mc.player != null && mc.level != null){
            ticks++;
            if (ticks == 1 && !notponactivateplz){
                location=target();
                startpos = mc.player.position();}
            if (location.getX()+0.5-startpos.x() <= 8 && location.getX()+0.5-startpos.x()>=-8 && location.getY()+0.5-startpos.y()<=8 && location.getY()+0.5-startpos.y()>=-8 && location.getZ()+0.5-startpos.z()<=8 && location.getZ()+0.5-startpos.z()>=-8 ){
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (!mc.level.getBlockState(location).canBeReplaced() && !mc.level.getBlockState(tptarget).canBeReplaced() && ticks==2 && !notponactivateplz){
                    error("Blocks in the target zone.");
                } else if (liquids.get() && !mc.level.getBlockState(location).is(Blocks.AIR) && !mc.level.getBlockState(tptarget).is(Blocks.AIR) && ticks==2 && !notponactivateplz){
                    error("Blocks in the target zone.");
                }
                else if (ticks==2 && !notponactivateplz){
                    mc.player.setPosRaw(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setDeltaMovement(0,0.2,0);
                }
                else if (ticks <=2 && !notponactivateplz){
                    mc.options.keyJump.setDown(false);
                    mc.options.keyShift.setDown(false);
                    mc.options.keyUp.setDown(false);
                    mc.options.keyDown.setDown(false);
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(false);
                    mc.player.setDeltaMovement(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 2){
                    ticks = 666;
                    TPnow = false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >=1 && ticks <=2 && !notponactivateplz){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            } else if (location.getX()+0.5-startpos.x()<=16 && location.getX()+0.5-startpos.x()>=-16 && location.getY()+0.5-startpos.y()<=16 && location.getY()+0.5-startpos.y()>=-16 && location.getZ()+0.5-startpos.z()<=16 && location.getZ()+0.5-startpos.z()>=-16 ){
                if (ticks==2 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.5), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.5)+1.05), startpos.z()+((location.getZ()+0.5-startpos.z())*0.5));
                    mc.player.setDeltaMovement(0,0.05,0);}
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (!mc.level.getBlockState(location).canBeReplaced() && !mc.level.getBlockState(tptarget).canBeReplaced() && ticks==3 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.5), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.5)+1.05), startpos.z()+((location.getZ()+0.5-startpos.z())*0.5));
                    mc.player.setDeltaMovement(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                } else if (liquids.get() && !mc.level.getBlockState(location).is(Blocks.AIR) && !mc.level.getBlockState(tptarget).is(Blocks.AIR) && ticks==3 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.5), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.5)+1.05), startpos.z()+((location.getZ()+0.5-startpos.z())*0.5));
                    mc.player.setDeltaMovement(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                }
                else if (ticks == 3 && !notponactivateplz){
                    mc.player.setPosRaw(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setDeltaMovement(0,0.2,0);
                }
                else if (ticks <=3 && !notponactivateplz){
                    mc.options.keyJump.setDown(false);
                    mc.options.keyShift.setDown(false);
                    mc.options.keyUp.setDown(false);
                    mc.options.keyDown.setDown(false);
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(false);
                    mc.player.setDeltaMovement(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 3){
                    ticks = 666;
                    TPnow = false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >= 1 && ticks <=3 && !notponactivateplz){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            } else if (location.getX()+0.5-startpos.x() <= 32 && location.getX()+0.5-startpos.x() >=-32 && location.getY()+0.5-startpos.y()<=32 && location.getY()+0.5-startpos.y()>=-32 && location.getZ()+0.5-startpos.z()<=32 && location.getZ()+0.5-startpos.z()>=-32 ){
                if (ticks ==2 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.25), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.25)+1.025), startpos.z()+((location.getZ()+0.5-startpos.z())*0.25));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks ==3 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.5), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.5)+1.05), startpos.z()+((location.getZ()+0.5-startpos.z())*0.5));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks ==4 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.75), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.75)+1.075), startpos.z()+((location.getZ()+0.5-startpos.z())*0.75));
                    mc.player.setDeltaMovement(0,0.05,0);}
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (!mc.level.getBlockState(location).canBeReplaced() && !mc.level.getBlockState(tptarget).canBeReplaced() && ticks==5 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.75), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.75)+1.075), startpos.z()+((location.getZ()+0.5-startpos.z())*0.75));
                    mc.player.setDeltaMovement(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                } else if (liquids.get() && !mc.level.getBlockState(location).is(Blocks.AIR) && !mc.level.getBlockState(tptarget).is(Blocks.AIR) && ticks==5 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.75), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.75)+1.075), startpos.z()+((location.getZ()+0.5-startpos.z())*0.75));
                    mc.player.setDeltaMovement(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                }
                else if (ticks ==5 && !notponactivateplz){
                    mc.player.setPosRaw(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setDeltaMovement(0,0.2,0);
                }
                else if (ticks <=5 && !notponactivateplz){
                    mc.options.keyJump.setDown(false);
                    mc.options.keyShift.setDown(false);
                    mc.options.keyUp.setDown(false);
                    mc.options.keyDown.setDown(false);
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(false);
                    mc.player.setDeltaMovement(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 5){
                    ticks = 666;
                    TPnow=false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >=1 && ticks <=5 && !notponactivateplz){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            } else if (location.getX()+0.5-startpos.x()<=64 && location.getX()+0.5-startpos.x()>=-64 && location.getY()+0.5-startpos.y()<=64 && location.getY()+0.5-startpos.y()>=-64 && location.getZ()+0.5-startpos.z()<=64 && location.getZ()+0.5-startpos.z()>=-64 ){
                if (ticks ==2 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.125), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.125)+1.0125), startpos.z()+((location.getZ()+0.5-startpos.z())*0.125));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks ==3 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.25), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.25)+1.025), startpos.z()+((location.getZ()+0.5-startpos.z())*0.25));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 4 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.375), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.375)+1.0375), startpos.z()+((location.getZ()+0.5-startpos.z())*0.375));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 5 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.50), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.50)+1.05), startpos.z()+((location.getZ()+0.5-startpos.z())*0.50));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 6 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.625), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.625)+1.0625), startpos.z()+((location.getZ()+0.5-startpos.z())*0.625));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 7 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.75), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.75)+1.075), startpos.z()+((location.getZ()+0.5-startpos.z())*0.75));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 8 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.875), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.875)+1.0875), startpos.z()+((location.getZ()+0.5-startpos.z())*0.875));
                    mc.player.setDeltaMovement(0,0.05,0);}
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (!mc.level.getBlockState(location).canBeReplaced() && !mc.level.getBlockState(tptarget).canBeReplaced() && ticks==9 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.875), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.875)+1.0875), startpos.z()+((location.getZ()+0.5-startpos.z())*0.875));
                    mc.player.setDeltaMovement(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                } else if (liquids.get() && !mc.level.getBlockState(location).is(Blocks.AIR) && !mc.level.getBlockState(tptarget).is(Blocks.AIR) && ticks==9 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.875), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.875)+1.0875), startpos.z()+((location.getZ()+0.5-startpos.z())*0.875));
                    mc.player.setDeltaMovement(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                }
                else if (ticks ==9 && !notponactivateplz){
                    mc.player.setPosRaw(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setDeltaMovement(0,0.2,0);
                }
                else if (ticks <=9 && !notponactivateplz){
                    mc.options.keyJump.setDown(false);
                    mc.options.keyShift.setDown(false);
                    mc.options.keyUp.setDown(false);
                    mc.options.keyDown.setDown(false);
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(false);
                    mc.player.setDeltaMovement(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 9){
                    ticks = 666;
                    TPnow = false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >=1 && ticks <=9 && !notponactivateplz){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            } else if (location.getX()+0.5-startpos.x() > 64 || location.getX()+0.5-startpos.x() <-64 || location.getY()+0.5-startpos.y()>64 || location.getY()+0.5-startpos.y()<-64 || location.getZ()+0.5-startpos.z()>64 || location.getZ()+0.5-startpos.z()<-64 ) {
                if (ticks==2 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.0833333333333333), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.0833333333333333)+1.00833333333333333), startpos.z()+((location.getZ()+0.5-startpos.z())*0.0833333333333333));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 3 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.1666666666666667), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.1666666666666667)+1.01666666666666667), startpos.z()+((location.getZ()+0.5-startpos.z())*0.1666666666666667));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 4 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.25), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.25)+1.025), startpos.z()+((location.getZ()+0.5-startpos.z())*0.25));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 5 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.3333333333333333), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.3333333333333333)+1.03333333333333333), startpos.z()+((location.getZ()+0.5-startpos.z())*0.3333333333333333));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 6 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.4166666666666667), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.4166666666666667)+1.04166666666666667), startpos.z()+((location.getZ()+0.5-startpos.z())*0.4166666666666667));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 7 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.5), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.5)+1.05), startpos.z()+((location.getZ()+0.5-startpos.z())*0.5));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 8 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.5833333333333333), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.5833333333333333)+1.05833333333333333), startpos.z()+((location.getZ()+0.5-startpos.z())*0.5833333333333333));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 9 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.6666666666666667), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.6666666666666667)+1.06666666666666667), startpos.z()+((location.getZ()+0.5-startpos.z())*0.6666666666666667));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 10 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.75), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.75)+1.075), startpos.z()+((location.getZ()+0.5-startpos.z())*0.75));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 11 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.8333333333333333), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.8333333333333333)+1.08333333333333333), startpos.z()+((location.getZ()+0.5-startpos.z())*0.8333333333333333));
                    mc.player.setDeltaMovement(0,0.05,0);}
                else if (ticks == 12 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.9166666666666667), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.9166666666666667)+1.09166666666666667), startpos.z()+((location.getZ()+0.5-startpos.z())*0.9166666666666667));
                    mc.player.setDeltaMovement(0,0.05,0);}
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (!mc.level.getBlockState(location).canBeReplaced() && !mc.level.getBlockState(tptarget).canBeReplaced() && ticks==13 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.9166666666666667), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.9166666666666667)+1.09166666666666667), startpos.z()+((location.getZ()+0.5-startpos.z())*0.9166666666666667));
                    mc.player.setDeltaMovement(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                } else if (liquids.get() && !mc.level.getBlockState(location).is(Blocks.AIR) && !mc.level.getBlockState(tptarget).is(Blocks.AIR) && ticks==13 && !notponactivateplz){
                    mc.player.setPosRaw(startpos.x()+((location.getX()+0.5-startpos.x())*0.9166666666666667), startpos.y()+(((location.getY()+0.5-startpos.y()+0.5)*0.9166666666666667)+1.09166666666666667), startpos.z()+((location.getZ()+0.5-startpos.z())*0.9166666666666667));
                    mc.player.setDeltaMovement(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                }
                else if (ticks == 13 && !notponactivateplz){
                    mc.player.setPosRaw(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setDeltaMovement(0,0.2,0);
                }
                else if (ticks <=13 && !notponactivateplz){
                    mc.options.keyJump.setDown(false);
                    mc.options.keyShift.setDown(false);
                    mc.options.keyUp.setDown(false);
                    mc.options.keyDown.setDown(false);
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(false);
                    mc.player.setDeltaMovement(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 13){
                    TPnow=false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >= 1 && ticks <=13 && !notponactivateplz){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            }
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!TPnow){
            location=target();}
        if (location == null || mc.level == null) return;
        double x1 = location.getX();
        double y1 = location.getY()+1;
        double z1 = location.getZ();
        double x2 = x1+1;
        double y2 = y1+1;
        double z2 = z1+1;

        if (render.get()){
            if (!liquids.get()){
                if (!mc.level.getBlockState(location).is(Blocks.AIR) && !mc.level.getFluidState(location).isEmpty()){
                    event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }else if (mc.level.getBlockState(location).is(Blocks.AIR) || mc.level.getFluidState(location).isEmpty()){
                    event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor2.get(), lineColor2.get(), shapeMode.get(), 0);
                }
            }else if (liquids.get()) {
                if (!mc.level.getBlockState(location).is(Blocks.AIR)){
                    event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }else if (mc.level.getBlockState(location).is(Blocks.AIR)){
                    event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor2.get(), lineColor2.get(), shapeMode.get(), 0);
                }
            }
        }
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {toggle();}
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {toggle();}
    private BlockPos target() {
        HitResult blockHit = mc.getCameraEntity().pick(reach.get(), 0, liquids.get());
        return ((BlockHitResult) blockHit).getBlockPos();
    }
}