//Written By etianll with a little bit of skidded codes and some new ideas. Credits to Meteor Rejects for  a bit of skids
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BaseCoralFanBlock;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.ConduitBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class AutoLavaCaster extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBuild = settings.createGroup("Build Options");
    private final SettingGroup sgTimer = settings.createGroup("Timer Settings");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> estlavatime = sgTimer.add(new BoolSetting.Builder()
            .name("EstimateLavaTimer")
            .description("ChooseBottomY mode estimates time based stairs going down to the Y level you set in the timer options from your position.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Modes> mode = sgTimer.add(new EnumSetting.Builder<Modes>()
            .name("Timer Estimation Mode")
            .description("FortyFiveDegreeStairs estimates based on 45degree stairs down to sealevel(Y63), or down to Y-60 if you are below Y64. Other two options are self-explanatory.")
            .defaultValue(Modes.UseLastMountain)
            .visible(estlavatime::get)
            .build());
    private final Setting<Boolean> aposition = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoPosition")
            .description("Positions you automatically for casting when you are not standing on a block already. May not work correctly in caves.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> estbotY = sgTimer.add(new IntSetting.Builder()
            .name("BottomYofStairs")
            .description("The lowest Y level of your staircase.")
            .defaultValue(128)
            .sliderRange(-64, 320)
            .visible(() -> estlavatime.get() && mode.get() == Modes.ChooseBottomY)
            .build()
    );
    private final Setting<Integer> lavatime = sgTimer.add(new IntSetting.Builder()
            .name("LavaTimerInSeconds")
            .description("The amount of time to let lava flow, in seconds. Use the .lavacalc Command to get a suggested time. Based on 20ticks/second.")
            .defaultValue(120)
            .sliderRange(15, 900)
            .min(10)
            .visible(() -> !estlavatime.get())
            .build()
    );
    private final Setting<Integer> watertime1 = sgTimer.add(new IntSetting.Builder()
            .name("WaterPlaceDelayInTicks")
            .description("The amount of time to delay water placement, in ticks.")
            .defaultValue(65)
            .sliderRange(60, 200)
            .min(1)
            .build()
    );
    private final Setting<Integer> waterdelay = sgTimer.add(new IntSetting.Builder()
            .name("PickupWaterDelayInTicks")
            .description("The amount of time to delay picking up water, in ticks.")
            .defaultValue(30)
            .sliderRange(20, 100)
            .min(1)
            .build()
    );
    private final Setting<Boolean> incY = sgBuild.add(new BoolSetting.Builder()
            .name("IncreaseYlevelPerLayer")
            .description("Increase Y+1 per flow cycle. Keep Blocks in your hotbar for it to work.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<Block>> skippableBlox = sgBuild.add(new BlockListSetting.Builder()
            .name("Blocks to not use")
            .description("Do not use these blocks for mountains.")
            .visible(incY::get)
            .build()
    );
    private final Setting<Boolean> bstyle = sgBuild.add(new BoolSetting.Builder()
            .name("SwitchBuildStyletoPlusSign")
            .description("Switches build style to increase flow")
            .defaultValue(true)
            .visible(incY::get)
            .build()
    );
    private final Setting<Integer> watertime2 = sgTimer.add(new IntSetting.Builder()
            .name("WaterPlaceDelayAFTERStyleChange")
            .description("The amount of time to delay water placement, in ticks.")
            .defaultValue(115)
            .sliderRange(60, 200)
            .min(1)
            .visible(() -> (incY.get() && bstyle.get()))
            .build()
    );
    private final Setting<Integer> lay = sgBuild.add(new IntSetting.Builder()
            .name("LayersUntilSwitchBuildStyle")
            .description("How many layers until a plus sign is placed to help flow.")
            .defaultValue(2)
            .sliderRange(0,4)
            .min(0)
            .visible(() -> (incY.get() && bstyle.get()))
            .build()
    );
    private final Setting<Integer> layerstop = sgBuild.add(new IntSetting.Builder()
            .name("LayersUntilStop")
            .description("How many layers until the bot stops.")
            .defaultValue(4)
            .sliderRange(1,100)
            .min(1)
            .visible(incY::get)
            .build()
    );
    private final Setting<Integer> buildlimit = sgBuild.add(new IntSetting.Builder()
            .name("Stop at Y level")
            .description("Halts bot when you reach this Y level")
            .defaultValue(319)
            .sliderRange(-63,319)
            .visible(incY::get)
            .build()
    );
    private final Setting<Boolean> MountainsOfLavaInc = sgBuild.add(new BoolSetting.Builder()
            .name("MountainsOfLava")
            .description("Leaves Lava as the last layer on your Mountain, for added danger when the noobs log back in.")
            .defaultValue(false)
            .visible(incY::get)
            .build()
    );
    private final Setting<Boolean> sneaky = sgGeneral.add(new BoolSetting.Builder()
            .name("SneakWhenActive")
            .description("Holds sneak key to help you reach the top block of your Staircase.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
            .name("YourReach")
            .description("Your Reach, in blocks. Maybe turn it down if not using the Reach module.")
            .defaultValue(4.6)
            .min (2)
            .max (4.6)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the next stair will be placed.")
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
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 15))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .visible(render::get)
            .build()
    );
    public final Setting<Boolean> lowYrst = sgGeneral.add(new BoolSetting.Builder()
            .name("ResetLowestBlockOnDEACTIVATE")
            .description("CHECK for proper timings for UseLastMountain timing mode when NOT clicking to pause with AutoMountain.")
            .defaultValue(false)
            .build()
    );

    public AutoLavaCaster() {
        super(Trouser.Main, "AutoLavaCaster", "Make Layers of Cobble on your Stairs! Aim at the top of the block you want to lava on.");
    }
    private BlockPos lava;
    private boolean tryanotherpos=false;
    private int estimatedlavatime=0;
    public boolean firstplace=true;
    public static int lavamountainticks;
    AutoMountain aMountain=new AutoMountain();
    int layers;
    @Override
    public void onActivate() {
        mc.player.setNoGravity(false);
        if (Modules.get().get(Flight.class).isActive()) {
            Modules.get().get(Flight.class).toggle();
        }
        if (Modules.get().get(FlightAntikick.class).isActive()) {
            Modules.get().get(FlightAntikick.class).toggle();
        }
        if (Modules.get().get(TPFly.class).isActive()) {
            Modules.get().get(TPFly.class).toggle();
        }
        BlockPos hover = new BlockPos(mc.player.getBlockX(),mc.player.getBlockY()-1,mc.player.getBlockZ());
        if (mc.level.getBlockState(hover).canBeReplaced() && !aposition.get() && !aMountain.autocasttimenow==true){
            if (mc.level.getBlockState(hover).canBeReplaced()){
                error("Not on a block, try again.");
            }
            if (mc.options.keyShift.isDown()){
                mc.options.keyShift.setDown(false);
            }
            lavamountainticks = 0;
            mc.player.setNoGravity(false);
            aMountain.autocasttimenow=false;
            toggle();
            return;

        }
        if (aMountain.autocasttimenow==true) {
            if (aMountain.lowestblock.getY()==666){
                toggle();
                error("Use AutoMountain first to get the timings for the last Mountain.");
                return;
            }
            if (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)+(((new AutoMountain().lowestblock.getY()-new AutoMountain().groundY)*30)/20)) <= (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)/2)+(((new AutoMountain().highestblock.getY()-new AutoMountain().groundY2)*30)/20))){
                estimatedlavatime= (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)/2)+(((new AutoMountain().highestblock.getY()-new AutoMountain().groundY2)*30)/20));
            }
            else if (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)+(((new AutoMountain().lowestblock.getY()-new AutoMountain().groundY)*30)/20)) > (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)/2)+(((new AutoMountain().highestblock.getY()-new AutoMountain().groundY2)*30)/20))){
                estimatedlavatime= ((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)+(((new AutoMountain().lowestblock.getY()-new AutoMountain().groundY)*30)/20));
            }
        }else if (estlavatime.get() && !aMountain.autocasttimenow==true){
            switch (mode.get()) {
                case UseLastMountain -> {
                    if (aMountain.lowestblock.getY()==666){
                        toggle();
                        error("Use AutoMountain first to get the timings for the last Mountain.");
                        return;
                    }
                    if (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)+(((new AutoMountain().lowestblock.getY()-new AutoMountain().groundY)*30)/20)) <= (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)/2)+(((new AutoMountain().highestblock.getY()-new AutoMountain().groundY2)*30)/20))){
                        estimatedlavatime= (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)/2)+(((new AutoMountain().highestblock.getY()-new AutoMountain().groundY2)*30)/20));
                    }
                    else if (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)+(((new AutoMountain().lowestblock.getY()-new AutoMountain().groundY)*30)/20)) > (((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)/2)+(((new AutoMountain().highestblock.getY()-new AutoMountain().groundY2)*30)/20))){
                        estimatedlavatime= ((((2+new AutoMountain().highestblock.getY()-new AutoMountain().lowestblock.getY())*60)/20)+(((new AutoMountain().lowestblock.getY()-new AutoMountain().groundY)*30)/20));
                    }
                }
                case FortyFiveDegreeStairs -> {
                    if (mc.player.getBlockY()>64)
                        estimatedlavatime= (((mc.player.getBlockY()-64)*60)/20);
                    else if (mc.player.getBlockY()<=64)
                        estimatedlavatime= (((mc.player.getBlockY()-(-60))*60)/20);
                }
                case ChooseBottomY -> {
                    estimatedlavatime= (((mc.player.getBlockY()-estbotY.get())*60)/20);
                }
            }
        }
        if (Modules.get().get(Timer.class).isActive()) {
            error("Timer off.");
            Modules.get().get(Timer.class).toggle();
        }
        if (mc.player.getXRot()<=5){
            if (mc.player.getXRot()<=5){
                error("Aim Down at the Top of a Block");
            }
            if (mc.options.keyShift.isDown()){
                mc.options.keyShift.setDown(false);
            }
            lavamountainticks = 0;
            toggle();
            return;
        }
        lavamountainticks = 0;
        layers = 1;
        lava = cast();
        if (mc.player.getBlockX()==lava.getX() && mc.player.getBlockY()-1==lava.getY() && mc.player.getBlockZ()==lava.getZ()){
            if (mc.player.getBlockX()==lava.getX() && mc.player.getBlockY()-1==lava.getY() && mc.player.getBlockZ()==lava.getZ()){
                error("Don't lava yourself, that's silly.");
            }
            if (mc.options.keyShift.isDown()){
                mc.options.keyShift.setDown(false);
            }
            lavamountainticks = 0;
            mc.player.setNoGravity(false);
            aMountain.autocasttimenow=false;
            toggle();
            return;
        }
        if (sneaky.get()){
            mc.options.keyShift.setDown(true);
        }
        if (mc.level.getBlockState(lava).getBlock() == Blocks.AIR){
            lavamountainticks = 0;
            mc.player.setNoGravity(false);
            aMountain.autocasttimenow=false;
            toggle();
            return;
        }
        if (!(mc.level.getBlockState(lava).getBlock() == Blocks.AIR) && !(mc.level.getBlockState(hover).getBlock() == Blocks.AIR) && !aposition.get() && !aMountain.autocasttimenow==true){
            placeLava();
        }
        firstplace=true;
    }
    @Override
    public void onDeactivate() {
        if (lowYrst.get()) aMountain.lowestblock=new BlockPos(666,666,666);
        aMountain.autocasttimenow=false;
        lavamountainticks = 0;
        if (mc.options.keyShift.isDown()){
            mc.options.keyShift.setDown(false);
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (lava == null) return;
        double x1 = lava.getX();
        double y1 = lava.getY()+1;
        double z1 = lava.getZ();
        double x2 = x1+1;
        double y2 = y1+1;
        double z2 = z1+1;
        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (Modules.get().get(AutoMountain.class).isActive()) {
            Modules.get().get(AutoMountain.class).toggle();
            error ("Wait until casting is done to AutoMountain again.");
        }
        BlockPos ceiling = new BlockPos(mc.player.getBlockX(),mc.player.getBlockY()+2,mc.player.getBlockZ());
        BlockPos hover = new BlockPos(mc.player.getBlockX(),mc.player.getBlockY()-1,mc.player.getBlockZ());
        lavamountainticks++;
        if (incY.get() && isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) {
            cascadingpileof();
        }
        PlayerUtils.centerPlayer();
        mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(lava.getX()+0.5,lava.getY()+1.05,lava.getZ()+0.5));
        if (sneaky.get()){
            mc.options.keyShift.setDown(true);
        }

        if (layers<lay.get()|!bstyle.get()){
            if (mc.player.getBlockX()==lava.getX() && mc.player.getBlockY()-1==lava.getY() && mc.player.getBlockZ()==lava.getZ()){
                if (mc.player.getBlockX()==lava.getX() && mc.player.getBlockY()-1==lava.getY() && mc.player.getBlockZ()==lava.getZ()){
                    error("Don't lava yourself, that's silly.");
                }
                if (mc.options.keyShift.isDown()){
                    mc.options.keyShift.setDown(false);
                }
                lavamountainticks = 0;
                mc.player.setNoGravity(false);
                aMountain.autocasttimenow=false;
                toggle();
                return;
            }
            if (lavamountainticks<=5 && firstplace == true){
                if (aposition.get() || aMountain.autocasttimenow==true){
                    autoposition();
                }
                if (lavamountainticks == 2){
                    if (estlavatime.get() || aMountain.autocasttimenow==true){
                        ChatUtils.sendMsg(Component.nullToEmpty("Starting layer 1. Lava will take "+estimatedlavatime+" more seconds to flow."));
                    }else
                        ChatUtils.sendMsg(Component.nullToEmpty("Starting layer 1. Lava will take "+lavatime.get()+" more seconds to flow."));
                }
                if (lavamountainticks >= 2){
                    BlockPos pos = mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                    if (mc.level.getBlockState(pos).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }else if (lavamountainticks==7 && firstplace == true && (aposition.get() || aMountain.autocasttimenow==true)){
                if (!(mc.level.getBlockState(lava).getBlock() == Blocks.AIR) && !(mc.level.getBlockState(hover).getBlock() == Blocks.AIR)) placeLava();
            }
            else if (firstplace==false && lavamountainticks==55){
                if (aMountain.autocasttimenow==true) {
                    if (aMountain.lowestblock.getY()==666){
                        toggle();
                        error("Use AutoMountain first to get the timings for the last Mountain.");
                        return;
                    }
                    estimatedlavatime = estimatedlavatime+((layers*45)/20);
                }else if (estlavatime.get() && !aMountain.autocasttimenow==true){
                    switch (mode.get()) {
                        case UseLastMountain -> {
                            if (aMountain.lowestblock.getY()==666){
                                toggle();
                                error("Use AutoMountain first to get the timings for the last Mountain.");
                                return;
                            }
                            estimatedlavatime = estimatedlavatime+((layers*45)/20);
                        }
                        case FortyFiveDegreeStairs -> {
                            if (mc.player.getBlockY()>64)
                                estimatedlavatime= (((mc.player.getBlockY()-64)*60)/20);
                            else if (mc.player.getBlockY()<=64)
                                estimatedlavatime= (((mc.player.getBlockY()-(-60))*60)/20);
                        }
                        case ChooseBottomY -> {
                            estimatedlavatime= (((mc.player.getBlockY()-estbotY.get())*60)/20);
                        }
                    }
                }
                placeLava();
                if (estlavatime.get() || aMountain.autocasttimenow==true){
                    ChatUtils.sendMsg(Component.nullToEmpty("Starting layer "+layers+". Lava will take "+estimatedlavatime+" more seconds to flow."));
                }else
                    ChatUtils.sendMsg(Component.nullToEmpty("Starting layer "+layers+". Lava will take "+lavatime.get()+" more seconds to flow."));
            }
            else if (MountainsOfLavaInc.get() && lavamountainticks==60 && layers>=layerstop.get()){
                if (mc.options.keyShift.isDown()){
                    mc.options.keyShift.setDown(false);
                }
                ChatUtils.sendMsg(Component.nullToEmpty("Done Building!"));
                lavamountainticks = 0;
                mc.player.setNoGravity(false);
                aMountain.autocasttimenow=false;
                toggle();
                return;
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20) || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)){
                firstplace=false;
                pickupLiquid();
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get() || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()){
                placeWater();
                ChatUtils.sendMsg(Component.nullToEmpty("Finishing layer "+layers));
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get() || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()){
                pickupLiquid();
                if (!incY.get()){
                    lavamountainticks=0;
                    layers++;
                }
                if (!MountainsOfLavaInc.get() && layers>=layerstop.get()){
                    if (mc.options.keyShift.isDown()){
                        mc.options.keyShift.setDown(false);
                    }
                    ChatUtils.sendMsg(Component.nullToEmpty("Done Building!"));
                    lavamountainticks = 0;
                    mc.player.setNoGravity(false);
                    aMountain.autocasttimenow=false;
                    toggle();
                    return;
                }
                if (incY.get()){
                    if (!mc.level.getBlockState(ceiling).canBeReplaced()){
                        if (!mc.level.getBlockState(ceiling).canBeReplaced()){
                            error("Hit the ceiling");
                        }
                        if (mc.options.keyShift.isDown()){
                            mc.options.keyShift.setDown(false);
                        }
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                    if (mc.player.getY()>=buildlimit.get()){
                        if (mc.player.getY()>=buildlimit.get()){
                            error("Hit your Y Stop Value");
                        }
                        if (mc.options.keyShift.isDown()){
                            mc.options.keyShift.setDown(false);
                        }
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                    cascadingpileof();
                    if (incY.get() && isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) {
                        error("Not Enough Suitable Blocks in Hand.");
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                }
            }
            if (incY.get()){
                if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+5 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+5){
                    BlockPos pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ());
                    if (mc.level.getBlockState(pos).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                    lava = new BlockPos(lava.getX(), lava.getY()+1, lava.getZ());
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+10 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+10){
                    mc.player.jumpFromGround();
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && (lavamountainticks>=(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+10 && lavamountainticks<=(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+15) || !estlavatime.get() && !aMountain.autocasttimenow==true && (lavamountainticks>=(lavatime.get()*20)+watertime1.get()+waterdelay.get()+10 && lavamountainticks<=(lavatime.get()*20)+watertime1.get()+waterdelay.get()+15)) {
                    BlockPos pos = mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                    if (mc.level.getBlockState(pos).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+16 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+16){
                    lavamountainticks=0;
                    layers++;
                }
            }}
        else if (layers==lay.get() && bstyle.get()){
            if (lavamountainticks<=5 && firstplace == true){
                if (aposition.get() || aMountain.autocasttimenow==true){
                    autoposition();
                }
                if (lavamountainticks == 2){
                    if (estlavatime.get() || aMountain.autocasttimenow==true){
                        ChatUtils.sendMsg(Component.nullToEmpty("Starting layer 1. Lava will take "+estimatedlavatime+" more seconds to flow."));
                    }else
                        ChatUtils.sendMsg(Component.nullToEmpty("Starting layer 1. Lava will take "+lavatime.get()+" more seconds to flow."));
                }
                if (lavamountainticks >= 2){
                    BlockPos pos = mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                    if (mc.level.getBlockState(pos).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }else if (lavamountainticks==7 && firstplace == true && (aposition.get() || aMountain.autocasttimenow==true)){
                if (!(mc.level.getBlockState(lava).getBlock() == Blocks.AIR) && !(mc.level.getBlockState(hover).getBlock() == Blocks.AIR)) placeLava();
            }
            else if (firstplace==false && lavamountainticks==55){
                if (aMountain.autocasttimenow==true) {
                    if (aMountain.lowestblock.getY()==666){
                        toggle();
                        error("Use AutoMountain first to get the timings for the last Mountain.");
                        return;
                    }
                    estimatedlavatime = estimatedlavatime+((layers*45)/20);
                }else if (estlavatime.get() && !aMountain.autocasttimenow==true){
                    switch (mode.get()) {
                        case UseLastMountain -> {
                            if (aMountain.lowestblock.getY()==666){
                                toggle();
                                error("Use AutoMountain first to get the timings for the last Mountain.");
                                return;
                            }
                            estimatedlavatime = estimatedlavatime+((layers*45)/20);
                        }
                        case FortyFiveDegreeStairs -> {
                            if (mc.player.getBlockY()>64)
                                estimatedlavatime= (((mc.player.getBlockY()-64)*60)/20);
                            else if (mc.player.getBlockY()<=64)
                                estimatedlavatime= (((mc.player.getBlockY()-(-60))*60)/20);
                        }
                        case ChooseBottomY -> {
                            estimatedlavatime= (((mc.player.getBlockY()-estbotY.get())*60)/20);
                        }
                    }
                }
                placeLava();
                if (estlavatime.get() || aMountain.autocasttimenow==true){
                    ChatUtils.sendMsg(Component.nullToEmpty("Starting layer "+layers+". Lava will take "+estimatedlavatime+" more seconds to flow."));
                }else
                    ChatUtils.sendMsg(Component.nullToEmpty("Starting layer "+layers+". Lava will take "+lavatime.get()+" more seconds to flow."));
            }
            else if (MountainsOfLavaInc.get() && lavamountainticks==60 && layers>=layerstop.get()+1){
                if (mc.options.keyShift.isDown()){
                    mc.options.keyShift.setDown(false);
                }
                ChatUtils.sendMsg(Component.nullToEmpty("Done Building!"));
                lavamountainticks = 0;
                mc.player.setNoGravity(false);
                aMountain.autocasttimenow=false;
                toggle();
                return;
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20) || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)){
                firstplace=false;
                pickupLiquid();
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get() || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()){
                placeWater();
                ChatUtils.sendMsg(Component.nullToEmpty("Finishing layer "+layers));
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get() || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()){
                pickupLiquid();
                if (!incY.get()){
                    lavamountainticks=0;
                    layers++;
                }
                if (!MountainsOfLavaInc.get() && layers>=layerstop.get()){
                    if (mc.options.keyShift.isDown()){
                        mc.options.keyShift.setDown(false);
                    }
                    ChatUtils.sendMsg(Component.nullToEmpty("Done Building!"));
                    lavamountainticks = 0;
                    mc.player.setNoGravity(false);
                    aMountain.autocasttimenow=false;
                    toggle();
                    return;
                }
                if (incY.get()){
                    if (!mc.level.getBlockState(ceiling).canBeReplaced()){
                        if (!mc.level.getBlockState(ceiling).canBeReplaced()){
                            error("Hit the ceiling");
                        }
                        if (mc.options.keyShift.isDown()){
                            mc.options.keyShift.setDown(false);
                        }
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                    if (mc.player.getY()>=buildlimit.get()){
                        if (mc.player.getY()>=buildlimit.get()){
                            error("Hit your Y Stop Value");
                        }
                        if (mc.options.keyShift.isDown()){
                            mc.options.keyShift.setDown(false);
                        }
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                    cascadingpileof();
                    if (bstyle.get() && incY.get() && isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) {
                        error("Not Enough Suitable Blocks in Hand.");
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                }
            }if (incY.get()){
                if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+4 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+4){
                    BlockPos pos2 = new BlockPos(lava.getX()+1,lava.getY()+1,lava.getZ());
                    if (mc.level.getBlockState(pos2).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos2), Direction.DOWN, pos2, false));
                    }
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+8 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+8) {
                    BlockPos pos3 = new BlockPos(lava.getX()-1,lava.getY()+1,lava.getZ());
                    if (mc.level.getBlockState(pos3).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos3), Direction.DOWN, pos3, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+12 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+12) {
                    BlockPos pos4 = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()+1);
                    if (mc.level.getBlockState(pos4).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos4), Direction.DOWN, pos4, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+16 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+16) {
                    BlockPos pos5 = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()-1);
                    if (mc.level.getBlockState(pos5).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos5), Direction.DOWN, pos5, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+20 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+20){
                    BlockPos pos1 = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ());
                    if (mc.level.getBlockState(pos1).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos1), Direction.DOWN, pos1, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+21 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+21) {
                    lava = new BlockPos(lava.getX(), lava.getY()+1, lava.getZ());
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+25 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+25){
                    mc.player.jumpFromGround();
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && (lavamountainticks>=(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+25 && lavamountainticks<=(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+30) || !estlavatime.get() && !aMountain.autocasttimenow==true && (lavamountainticks>=(lavatime.get()*20)+watertime1.get()+waterdelay.get()+25 && lavamountainticks<=(lavatime.get()*20)+watertime1.get()+waterdelay.get()+30)) {
                    BlockPos pos = mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                    if (mc.level.getBlockState(pos).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime1.get()+waterdelay.get()+31 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime1.get()+waterdelay.get()+31){
                    lavamountainticks=0;
                    layers++;
                }
            }
        }
        else if (layers>lay.get() && bstyle.get()){
            if (lavamountainticks<=5 && firstplace == true){
                if (aposition.get() || aMountain.autocasttimenow==true){
                    autoposition();
                }
                if (lavamountainticks == 2){
                    if (estlavatime.get() || aMountain.autocasttimenow==true){
                        ChatUtils.sendMsg(Component.nullToEmpty("Starting layer 1. Lava will take "+estimatedlavatime+" more seconds to flow."));
                    }else
                        ChatUtils.sendMsg(Component.nullToEmpty("Starting layer 1. Lava will take "+lavatime.get()+" more seconds to flow."));
                }
                if (lavamountainticks >= 2){
                    BlockPos pos = mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                    if (mc.level.getBlockState(pos).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }else if (lavamountainticks==7 && firstplace == true && (aposition.get() || aMountain.autocasttimenow==true)){
                if (!(mc.level.getBlockState(lava).getBlock() == Blocks.AIR) && !(mc.level.getBlockState(hover).getBlock() == Blocks.AIR)) placeLava();
            }
            else if (firstplace==false && lavamountainticks==55){
                if (aMountain.autocasttimenow==true) {
                    if (aMountain.lowestblock.getY()==666){
                        toggle();
                        error("Use AutoMountain first to get the timings for the last Mountain.");
                        return;
                    }
                    estimatedlavatime = estimatedlavatime+((layers*45)/20);
                }else if (estlavatime.get() && !aMountain.autocasttimenow==true){
                    switch (mode.get()) {
                        case UseLastMountain -> {
                            if (aMountain.lowestblock.getY()==666){
                                toggle();
                                error("Use AutoMountain first to get the timings for the last Mountain.");
                                return;
                            }
                            estimatedlavatime = estimatedlavatime+((layers*45)/20);
                        }
                        case FortyFiveDegreeStairs -> {
                            if (mc.player.getBlockY()>64)
                                estimatedlavatime= (((mc.player.getBlockY()-64)*60)/20);
                            else if (mc.player.getBlockY()<=64)
                                estimatedlavatime= (((mc.player.getBlockY()-(-60))*60)/20);
                        }
                        case ChooseBottomY -> {
                            estimatedlavatime= (((mc.player.getBlockY()-estbotY.get())*60)/20);
                        }
                    }
                }
                placeLava();
                if (estlavatime.get() || aMountain.autocasttimenow==true){
                    ChatUtils.sendMsg(Component.nullToEmpty("Starting layer "+layers+". Lava will take "+estimatedlavatime+" more seconds to flow."));
                }else
                    ChatUtils.sendMsg(Component.nullToEmpty("Starting layer "+layers+". Lava will take "+lavatime.get()+" more seconds to flow."));
            }
            else if (MountainsOfLavaInc.get() && lavamountainticks==60 && layers>=layerstop.get()+1){
                if (mc.options.keyShift.isDown()){
                    mc.options.keyShift.setDown(false);
                }
                ChatUtils.sendMsg(Component.nullToEmpty("Done Building!"));
                lavamountainticks = 0;
                mc.player.setNoGravity(false);
                aMountain.autocasttimenow=false;
                toggle();
                return;
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20) || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)){
                firstplace=false;
                pickupLiquid();
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get() || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()){
                placeWater();
                ChatUtils.sendMsg(Component.nullToEmpty("Finishing layer "+layers));
            }
            else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get() || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()){
                pickupLiquid();
                if (!incY.get()){
                    lavamountainticks=0;
                    layers++;
                }
                if (!MountainsOfLavaInc.get() && layers>=layerstop.get()){
                    if (mc.options.keyShift.isDown()){
                        mc.options.keyShift.setDown(false);
                    }
                    ChatUtils.sendMsg(Component.nullToEmpty("Done Building!"));
                    lavamountainticks = 0;
                    mc.player.setNoGravity(false);
                    aMountain.autocasttimenow=false;
                    toggle();
                    return;
                }
                if (incY.get()){
                    if (!mc.level.getBlockState(ceiling).canBeReplaced()){
                        if (!mc.level.getBlockState(ceiling).canBeReplaced()){
                            error("Hit the ceiling");
                        }
                        if (mc.options.keyShift.isDown()){
                            mc.options.keyShift.setDown(false);
                        }
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                    if (mc.player.getY()>=buildlimit.get()){
                        if (mc.player.getY()>=buildlimit.get()){
                            error("Hit your Y Stop Value");
                        }
                        if (mc.options.keyShift.isDown()){
                            mc.options.keyShift.setDown(false);
                        }
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                    cascadingpileof();
                    if (bstyle.get() && incY.get() && isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) {
                        error("Not Enough Suitable Blocks in Hand.");
                        lavamountainticks = 0;
                        mc.player.setNoGravity(false);
                        aMountain.autocasttimenow=false;
                        toggle();
                        return;
                    }
                }
            }if (incY.get()){
                if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+4 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()+4){
                    BlockPos pos2 = new BlockPos(lava.getX()+1,lava.getY()+1,lava.getZ());
                    if (mc.level.getBlockState(pos2).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos2), Direction.DOWN, pos2, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+8 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()+8){
                    BlockPos pos3 = new BlockPos(lava.getX()-1,lava.getY()+1,lava.getZ());
                    if (mc.level.getBlockState(pos3).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos3), Direction.DOWN, pos3, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+12 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()+12){
                    BlockPos pos4 = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()+1);
                    if (mc.level.getBlockState(pos4).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos4), Direction.DOWN, pos4, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+16 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()+16){
                    BlockPos pos5 = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()-1);
                    if (mc.level.getBlockState(pos5).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos5), Direction.DOWN, pos5, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+20 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()+20){
                    BlockPos pos1 = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ());
                    if (mc.level.getBlockState(pos1).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos1), Direction.DOWN, pos1, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+21 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()+21) {
                    lava = new BlockPos(lava.getX(), lava.getY()+1, lava.getZ());
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+25 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()+25){
                    mc.player.jumpFromGround();
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && (lavamountainticks>=(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+25 && lavamountainticks<=(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+30) || !estlavatime.get() && !aMountain.autocasttimenow==true && (lavamountainticks>=(lavatime.get()*20)+watertime2.get()+waterdelay.get()+25 && lavamountainticks<=(lavatime.get()*20)+watertime2.get()+waterdelay.get()+30)) {
                    BlockPos pos = mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                    if (mc.level.getBlockState(pos).canBeReplaced()) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                else if ((estlavatime.get() || aMountain.autocasttimenow==true) && lavamountainticks==(estimatedlavatime*20)+watertime2.get()+waterdelay.get()+31 || !estlavatime.get() && !aMountain.autocasttimenow==true && lavamountainticks==(lavatime.get()*20)+watertime2.get()+waterdelay.get()+31){
                    lavamountainticks=0;
                    layers++;
                }
            }
        }
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            lavamountainticks = 0;
            mc.player.setNoGravity(false);
            aMountain.autocasttimenow=false;
            toggle();
        }
        if (event.screen instanceof DeathScreen) {
            lavamountainticks = 0;
            mc.player.setNoGravity(false);
            aMountain.autocasttimenow=false;
            toggle();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        lavamountainticks = 0;
        mc.player.setNoGravity(false);
        aMountain.autocasttimenow=false;
        toggle();
    }
    private void placeLava() {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.LAVA_BUCKET);
        if (!findItemResult.found()) {
            error("No lava bucket found.");
            if (mc.options.keyShift.isDown()){
                mc.options.keyShift.setDown(false);
            }
            lavamountainticks = 0;
            mc.player.setNoGravity(false);
            aMountain.autocasttimenow=false;
            toggle();
            return;
        }
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(findItemResult.slot());
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prevSlot);
    }

    private void placeWater() {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.WATER_BUCKET);
        if (!findItemResult.found()) {
            error("No water bucket found.");
            if (mc.options.keyShift.isDown()){
                mc.options.keyShift.setDown(false);
            }
            lavamountainticks = 0;
            mc.player.setNoGravity(false);
            aMountain.autocasttimenow=false;
            toggle();
            return;
        }
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(findItemResult.slot());
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prevSlot);
    }

    private void pickupLiquid() {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.BUCKET);
        if (!findItemResult.found()) {
            error("No bucket found.");
            if (mc.options.keyShift.isDown()){
                mc.options.keyShift.setDown(false);
            }
            lavamountainticks = 0;
            mc.player.setNoGravity(false);
            aMountain.autocasttimenow=false;
            toggle();
            return;
        }
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(findItemResult.slot());
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prevSlot);
    }

    private BlockPos cast() {
        HitResult blockHit = mc.getCameraEntity().pick(reach.get(), 0, false);
        if (((BlockHitResult) blockHit).getDirection() == Direction.UP){
            return ((BlockHitResult) blockHit).getBlockPos();}
        else{
            error("Target the Top of a block");
            return ((BlockHitResult) blockHit).getBlockPos().offset(6,6,6);}
    }
    private void autoposition() {
        BlockPos pos = mc.player.blockPosition().offset(new Vec3i(0,-1,0));
        if (mc.level.getBlockState(pos).canBeReplaced()) {
            if (aMountain.autocasttimenow==true && aMountain.wasfacingBOT==Direction.EAST|| aMountain.autocasttimenow==false && mc.player.getYRot()>=90 && mc.player.getYRot()<=180 || tryanotherpos==true){ //NORTHWEST
                BlockPos isair = BlockPos.containing(lava.getX()+2.5,lava.getY()+3,lava.getZ()+2.5);
                BlockPos isair2 = BlockPos.containing(lava.getX()+2.5,lava.getY()+4,lava.getZ()+2.5);
                if (mc.level.getBlockState(isair).canBeReplaced() && mc.level.getFluidState(isair).isEmpty() && !mc.level.getBlockState(isair).is(Blocks.POWDER_SNOW) && mc.level.getBlockState(isair2).canBeReplaced() && mc.level.getFluidState(isair2).isEmpty() && !mc.level.getBlockState(isair2).is(Blocks.POWDER_SNOW)) {
                    mc.player.setPosRaw(lava.getX()+2.5,lava.getY()+3,lava.getZ()+2.5);
                    tryanotherpos=false;
                } else {
                    error("Position is occupied, trying another.");
                    tryanotherpos=true;}
            } else if (aMountain.autocasttimenow==true && aMountain.wasfacingBOT==Direction.SOUTH|| aMountain.autocasttimenow==false && mc.player.getYRot()>=-180 && mc.player.getYRot()<-90 || tryanotherpos==true){ //NORTHEAST
                BlockPos isair = BlockPos.containing(lava.getX()-1.5,lava.getY()+3,lava.getZ()+2.5);
                BlockPos isair2 = BlockPos.containing(lava.getX()-1.5,lava.getY()+4,lava.getZ()+2.5);
                if (mc.level.getBlockState(isair).canBeReplaced() && mc.level.getFluidState(isair).isEmpty() && !mc.level.getBlockState(isair).is(Blocks.POWDER_SNOW) && mc.level.getBlockState(isair2).canBeReplaced() && mc.level.getFluidState(isair2).isEmpty() && !mc.level.getBlockState(isair2).is(Blocks.POWDER_SNOW)) {
                    mc.player.setPosRaw(lava.getX()-1.5,lava.getY()+3,lava.getZ()+2.5);
                    tryanotherpos=false;
                } else {
                    error("Position is occupied, trying another.");
                    tryanotherpos=true;}
            } else if (aMountain.autocasttimenow==true && aMountain.wasfacingBOT==Direction.WEST|| aMountain.autocasttimenow==false && mc.player.getYRot()>=-90 && mc.player.getYRot()<0 || tryanotherpos==true){ //SOUTHEAST
                BlockPos isair = BlockPos.containing(lava.getX()-1.5,lava.getY()+3,lava.getZ()-1.5);
                BlockPos isair2 = BlockPos.containing(lava.getX()-1.5,lava.getY()+4,lava.getZ()-1.5);
                if (mc.level.getBlockState(isair).canBeReplaced() && mc.level.getFluidState(isair).isEmpty() && !mc.level.getBlockState(isair).is(Blocks.POWDER_SNOW) && mc.level.getBlockState(isair2).canBeReplaced() && mc.level.getFluidState(isair2).isEmpty() && !mc.level.getBlockState(isair2).is(Blocks.POWDER_SNOW)) {
                    mc.player.setPosRaw(lava.getX()-1.5,lava.getY()+3,lava.getZ()-1.5);
                    tryanotherpos=false;
                } else {
                    error("Position is occupied, trying another.");
                    tryanotherpos=true;}
            } else if (aMountain.autocasttimenow==true && aMountain.wasfacingBOT==Direction.NORTH|| aMountain.autocasttimenow==false && mc.player.getYRot()>=0 && mc.player.getYRot()<90 || tryanotherpos==true){ //SOUTHWEST
                BlockPos isair = BlockPos.containing(lava.getX()+2.5,lava.getY()+3,lava.getZ()-1.5);
                BlockPos isair2 = BlockPos.containing(lava.getX()+2.5,lava.getY()+4,lava.getZ()-1.5);
                if (mc.level.getBlockState(isair).canBeReplaced() && mc.level.getFluidState(isair).isEmpty() && !mc.level.getBlockState(isair).is(Blocks.POWDER_SNOW) && mc.level.getBlockState(isair2).canBeReplaced() && mc.level.getFluidState(isair2).isEmpty() && !mc.level.getBlockState(isair2).is(Blocks.POWDER_SNOW)) {
                    mc.player.setPosRaw(lava.getX()+2.5,lava.getY()+3,lava.getZ()-1.5);
                    tryanotherpos=false;
                } else {
                    error("Position is occupied, trying another.");
                    tryanotherpos=true;}
            }
        }
    }
    private void cascadingpileof() {
        FindItemResult findResult = InvUtils.findInHotbar(block -> !isInvalidBlock(block));
        if (!findResult.found()) {
            return;
        }
        mc.player.getInventory().setSelectedSlot(findResult.slot());
    }

    private boolean isInvalidBlock(ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem)
                || stack.getItem() instanceof BedItem
                || stack.getItem() instanceof SolidBucketItem
                || stack.getItem() instanceof ScaffoldingBlockItem
                || stack.getItem() instanceof DoubleHighBlockItem
                || stack.getItem() instanceof StandingAndWallBlockItem
                || stack.getItem() instanceof PlaceOnWaterBlockItem
                || ((BlockItem) stack.getItem()).getBlock() instanceof VegetationBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TorchBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof DiodeBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof RedStoneWireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WallBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceGateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FallingBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseRailBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SignBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BellBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CarpetBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ConduitBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseCoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseCoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripWireHookBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PointedDripstoneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripWireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SnowLayerBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PressurePlateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FaceAttachedHorizontalDirectionalBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AmethystClusterBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BuddingAmethystBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusFlowerBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusPlantBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LanternBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CandleBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TntBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CakeBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WebBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SugarCaneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SporeBlossomBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof KelpBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof GlowLichenBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CactusBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BambooStalkBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FlowerPotBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LadderBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SimpleWaterloggedBlock
                || skippableBlox.get().contains(((BlockItem) stack.getItem()).getBlock());
    }
    public enum Modes {
        FortyFiveDegreeStairs, ChooseBottomY, UseLastMountain
    }
}