//written by etianll :D
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Material;
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
            .name("reach")
            .description("Reach")
            .defaultValue(64)
            .sliderRange(8, 96)
            .build());
    public final Setting<Double> tpTimer = sgGeneral.add(new DoubleSetting.Builder()
            .name("timer")
            .description("The multiplier value for speed of movement.")
            .defaultValue(3)
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
            .visible(() -> render.get())
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color-solid-block")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 15))
            .visible(() -> render.get())
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color-solid-bloc")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .visible(() -> render.get())
            .build()
    );
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder()
            .name("side-color-non-solid")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(0, 255, 255, 15))
            .visible(() -> render.get())
            .build()
    );

    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder()
            .name("line-color-non-solid")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(0, 255, 255, 255))
            .visible(() -> render.get())
            .build()
    );

    public Teleport() {
        super(Trouser.Main, "teleport", "Teleports you to where you are aiming.");
    }
    private BlockPos location;
    private Vec3d startpos;
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
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.attackKey.isPressed()){
            notponactivateplz=false;
            ticks=0;}
        if (mc.options.attackKey.isPressed()){
            notponactivateplz=false;
            TPnow=true;}
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        if (TPnow = true && notponactivateplz == false){
            ticks++;
            if (ticks == 1 && notponactivateplz == false){
                location=target();
                startpos = mc.player.getPos();}
            if (location.getX()+0.5-startpos.getX() <= 8 && location.getX()+0.5-startpos.getX()>=-8 && location.getY()+0.5-startpos.getY()<=8 && location.getY()+0.5-startpos.getY()>=-8 && location.getZ()+0.5-startpos.getZ()<=8 && location.getZ()+0.5-startpos.getZ()>=-8 ){
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (mc.world.getBlockState(location).getMaterial().isSolid() && mc.world.getBlockState(tptarget).getMaterial().isSolid() && ticks==2 && notponactivateplz==false){
                    error("Blocks in the target zone.");
                } else if (liquids.get() && !mc.world.getBlockState(location).getMaterial().equals(Material.AIR) && !mc.world.getBlockState(tptarget).getMaterial().equals(Material.AIR) && ticks==2 && notponactivateplz==false){
                    error("Blocks in the target zone.");
                }
                else if (ticks==2 && notponactivateplz==false){
                    mc.player.setPos(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setVelocity(0,0.2,0);
                }
                else if (ticks <=2 && notponactivateplz==false){
                    mc.options.jumpKey.setPressed(false);
                    mc.options.sneakKey.setPressed(false);
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.player.setVelocity(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 2){
                    ticks = 666;
                    TPnow = false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >=1 && ticks <=2 && notponactivateplz==false){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            } else if (location.getX()+0.5-startpos.getX()<=16 && location.getX()+0.5-startpos.getX()>=-16 && location.getY()+0.5-startpos.getY()<=16 && location.getY()+0.5-startpos.getY()>=-16 && location.getZ()+0.5-startpos.getZ()<=16 && location.getZ()+0.5-startpos.getZ()>=-16 ){
                if (ticks==2 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.5), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.5)+1.05), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.5));
                    mc.player.setVelocity(0,0.05,0);}
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (mc.world.getBlockState(location).getMaterial().isSolid() && mc.world.getBlockState(tptarget).getMaterial().isSolid() && ticks==3 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.5), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.5)+1.05), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.5));
                    mc.player.setVelocity(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                } else if (liquids.get() && !mc.world.getBlockState(location).getMaterial().equals(Material.AIR) && !mc.world.getBlockState(tptarget).getMaterial().equals(Material.AIR) && ticks==3 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.5), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.5)+1.05), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.5));
                    mc.player.setVelocity(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                }
                else if (ticks == 3 && notponactivateplz==false){
                    mc.player.setPos(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setVelocity(0,0.2,0);
                }
                else if (ticks <=3 && notponactivateplz==false){
                    mc.options.jumpKey.setPressed(false);
                    mc.options.sneakKey.setPressed(false);
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.player.setVelocity(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 3){
                    ticks = 666;
                    TPnow = false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >= 1 && ticks <=3 && notponactivateplz==false){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            } else if (location.getX()+0.5-startpos.getX() <= 32 && location.getX()+0.5-startpos.getX() >=-32 && location.getY()+0.5-startpos.getY()<=32 && location.getY()+0.5-startpos.getY()>=-32 && location.getZ()+0.5-startpos.getZ()<=32 && location.getZ()+0.5-startpos.getZ()>=-32 ){
                if (ticks ==2 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.25), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.25)+1.025), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.25));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks ==3 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.5), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.5)+1.05), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.5));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks ==4 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.75), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.75)+1.075), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.75));
                    mc.player.setVelocity(0,0.05,0);}
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (mc.world.getBlockState(location).getMaterial().isSolid() && mc.world.getBlockState(tptarget).getMaterial().isSolid() && ticks==5 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.75), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.75)+1.075), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.75));
                    mc.player.setVelocity(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                } else if (liquids.get() && !mc.world.getBlockState(location).getMaterial().equals(Material.AIR) && !mc.world.getBlockState(tptarget).getMaterial().equals(Material.AIR) && ticks==5 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.75), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.75)+1.075), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.75));
                    mc.player.setVelocity(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                }
                else if (ticks ==5 && notponactivateplz==false){
                    mc.player.setPos(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setVelocity(0,0.2,0);
                }
                else if (ticks <=5 && notponactivateplz == false){
                    mc.options.jumpKey.setPressed(false);
                    mc.options.sneakKey.setPressed(false);
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.player.setVelocity(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 5){
                    ticks = 666;
                    TPnow=false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >=1 && ticks <=5 && notponactivateplz == false){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            } else if (location.getX()+0.5-startpos.getX()<=64 && location.getX()+0.5-startpos.getX()>=-64 && location.getY()+0.5-startpos.getY()<=64 && location.getY()+0.5-startpos.getY()>=-64 && location.getZ()+0.5-startpos.getZ()<=64 && location.getZ()+0.5-startpos.getZ()>=-64 ){
            if (ticks ==2 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.125), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.125)+1.0125), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.125));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks ==3 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.25), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.25)+1.025), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.25));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks == 4 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.375), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.375)+1.0375), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.375));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks == 5 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.50), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.50)+1.05), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.50));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks == 6 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.625), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.625)+1.0625), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.625));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks == 7 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.75), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.75)+1.075), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.75));
                mc.player.setVelocity(0,0.05,0);}
            else if (ticks == 8 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.875), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.875)+1.0875), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.875));
                mc.player.setVelocity(0,0.05,0);}
            BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
            if (mc.world.getBlockState(location).getMaterial().isSolid() && mc.world.getBlockState(tptarget).getMaterial().isSolid() && ticks==9 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.875), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.875)+1.0875), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.875));
                mc.player.setVelocity(0,0.2,0);
                error("Blocks in the target zone. Teleporting you near the target.");
            } else if (liquids.get() && !mc.world.getBlockState(location).getMaterial().equals(Material.AIR) && !mc.world.getBlockState(tptarget).getMaterial().equals(Material.AIR) && ticks==9 && notponactivateplz==false){
                mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.875), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.875)+1.0875), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.875));
                mc.player.setVelocity(0,0.2,0);
                error("Blocks in the target zone. Teleporting you near the target.");
            }
            else if (ticks ==9 && notponactivateplz==false){
                mc.player.setPos(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                mc.player.setVelocity(0,0.2,0);
            }
            else if (ticks <=9 && notponactivateplz==false){
                mc.options.jumpKey.setPressed(false);
                mc.options.sneakKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.player.setVelocity(0,0,0);
                TPnow=true;
            }
                if (ticks >= 9){
                    ticks = 666;
                    TPnow = false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >=1 && ticks <=9 && notponactivateplz == false){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
        } else if (location.getX()+0.5-startpos.getX() > 64 || location.getX()+0.5-startpos.getX() <-64 || location.getY()+0.5-startpos.getY()>64 || location.getY()+0.5-startpos.getY()<-64 || location.getZ()+0.5-startpos.getZ()>64 || location.getZ()+0.5-startpos.getZ()<-64 ) {
                if (ticks==2 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.0833333333333333), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.0833333333333333)+1.00833333333333333), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.0833333333333333));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 3 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.1666666666666667), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.1666666666666667)+1.01666666666666667), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.1666666666666667));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 4 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.25), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.25)+1.025), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.25));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 5 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.3333333333333333), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.3333333333333333)+1.03333333333333333), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.3333333333333333));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 6 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.4166666666666667), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.4166666666666667)+1.04166666666666667), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.4166666666666667));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 7 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.5), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.5)+1.05), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.5));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 8 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.5833333333333333), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.5833333333333333)+1.05833333333333333), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.5833333333333333));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 9 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.6666666666666667), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.6666666666666667)+1.06666666666666667), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.6666666666666667));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 10 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.75), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.75)+1.075), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.75));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 11 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.8333333333333333), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.8333333333333333)+1.08333333333333333), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.8333333333333333));
                    mc.player.setVelocity(0,0.05,0);}
                else if (ticks == 12 && notponactivateplz == false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.9166666666666667), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.9166666666666667)+1.09166666666666667), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.9166666666666667));
                    mc.player.setVelocity(0,0.05,0);}
                BlockPos tptarget= new BlockPos(location.getX(), location.getY()+1, location.getZ());
                if (mc.world.getBlockState(location).getMaterial().isSolid() && mc.world.getBlockState(tptarget).getMaterial().isSolid() && ticks==13 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.9166666666666667), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.9166666666666667)+1.09166666666666667), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.9166666666666667));
                    mc.player.setVelocity(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                } else if (liquids.get() && !mc.world.getBlockState(location).getMaterial().equals(Material.AIR) && !mc.world.getBlockState(tptarget).getMaterial().equals(Material.AIR) && ticks==13 && notponactivateplz==false){
                    mc.player.setPos(startpos.getX()+((location.getX()+0.5-startpos.getX())*0.9166666666666667), startpos.getY()+(((location.getY()+0.5-startpos.getY()+0.5)*0.9166666666666667)+1.09166666666666667), startpos.getZ()+((location.getZ()+0.5-startpos.getZ())*0.9166666666666667));
                    mc.player.setVelocity(0,0.2,0);
                    error("Blocks in the target zone. Teleporting you near the target.");
                }
                else if (ticks == 13 && notponactivateplz == false){
                    mc.player.setPos(location.getX()+0.5, location.getY()+1.1, location.getZ()+0.5);
                    mc.player.setVelocity(0,0.2,0);
                }
                else if (ticks <=13 && notponactivateplz==false){
                    mc.options.jumpKey.setPressed(false);
                    mc.options.sneakKey.setPressed(false);
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.player.setVelocity(0,0,0);
                    TPnow=true;
                }
                if (ticks >= 13){
                    TPnow=false;
                    Modules.get().get(Timer.class).setOverride(Timer.OFF);
                }
                if (ticks >= 1 && ticks <=13 && notponactivateplz == false){
                    Modules.get().get(Timer.class).setOverride(tpTimer.get());
                }
            }
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (TPnow==false){
        location=target();}
        if (location == null) return;
        double x1 = location.getX();
        double y1 = location.getY()+1;
        double z1 = location.getZ();
        double x2 = x1+1;
        double y2 = y1+1;
        double z2 = z1+1;

        if (render.get()){
            if (!liquids.get()){
            if (!mc.world.getBlockState(location).getMaterial().equals(Material.AIR) && !mc.world.getBlockState(location).getMaterial().isLiquid()){
        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }else if (mc.world.getBlockState(location).getMaterial().equals(Material.AIR) || mc.world.getBlockState(location).getMaterial().isLiquid()){
                event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor2.get(), lineColor2.get(), shapeMode.get(), 0);
            }
        }else if (liquids.get()) {
                if (!mc.world.getBlockState(location).getMaterial().equals(Material.AIR)){
                    event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }else if (mc.world.getBlockState(location).getMaterial().equals(Material.AIR)){
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
        HitResult blockHit = mc.cameraEntity.raycast(reach.get(), 0, liquids.get());
        return ((BlockHitResult) blockHit).getBlockPos();
    }

}
