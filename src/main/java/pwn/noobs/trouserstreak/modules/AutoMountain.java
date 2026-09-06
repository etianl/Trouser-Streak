package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
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
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
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
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;


/**
 * @Author majorsopa
 * https://github.com/majorsopa
 * @Author evaan
 * https://github.com/evaan
 * @Author etianll
 * https://github.com/etianl
 */
public class AutoMountain extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBuild = settings.createGroup("Build Options");
    private final SettingGroup sgTimings = settings.createGroup("Timings");

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<List<Block>> skippableBlox = sgGeneral.add(new BlockListSetting.Builder()
            .name("Blocks to not use")
            .description("Do not use these blocks for mountains.")
            .build()
    );
    public final Setting<Boolean> autolavamountain = sgGeneral.add(new BoolSetting.Builder()
            .name("MountainMakerBot")
            .description("Starts casting on your stairs with AutoLavaCaster when you reach your build limit. Not intended for use in a closed in space.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> mouseT = sgGeneral.add(new BoolSetting.Builder()
            .name("MouseTurn")
            .description("Changes building direction based on your looking direction")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> startPaused = sgGeneral.add(new BoolSetting.Builder()
            .name("Start Paused")
            .description("AutoMountain is Paused when module activated, for more control.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> spcoffset = sgBuild.add(new IntSetting.Builder()
            .name("OnDemandSpacing")
            .description("Amount of space in blocks between stairs when pressing jumpKey.")
            .defaultValue(1)
            .min(1)
            .max(2)
            .visible(() -> !autolavamountain.get())
            .build());

    public final Setting<Double> StairTimer = sgTimings.add(new DoubleSetting.Builder()
            .name("TimerMultiplier")
            .description("The multiplier value for Timer.")
            .defaultValue(1)
            .sliderRange(0.1, 10)
            .build()
    );
    private final Setting<Integer> spd = sgTimings.add(new IntSetting.Builder()
            .name("PlacementTickDelay")
            .description("Delay block placement to slow down the builder.")
            .min(1)
            .sliderRange(1, 10)
            .defaultValue(1)
            .build());
    private final Setting<Integer> munscher = sgTimings.add(new IntSetting.Builder()
            .name("DiagonalSwitchDelay")
            .description("Delays switching direction by this many ticks when building diagonally.")
            .min (1)
            .sliderRange(1,10)
            .defaultValue(1)
            .visible(() -> !autolavamountain.get())
            .build());
    public final Setting<Boolean> delayakick = sgTimings.add(new BoolSetting.Builder()
            .name("PauseBasedAntiKick")
            .description("Helps if you're flying, or sending too many packets.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> delay = sgTimings.add(new IntSetting.Builder()
            .name("PauseForThisAmountOfTicks")
            .description("The amount of delay in ticks, when pausing. Useful if you're flying, or sending too many packets.")
            .min (1)
            .defaultValue(5)
            .sliderRange(0, 100)
            .visible(delayakick::get)
            .build()
    );
    private final Setting<Integer> offTime = sgTimings.add(new IntSetting.Builder()
            .name("TicksBetweenPause")
            .description("The amount of delay, in ticks, between pauses.")
            .min (1)
            .defaultValue(20)
            .sliderRange(1, 200)
            .visible(delayakick::get)
            .build()
    );
    private final Setting<Integer> botlimit = sgBuild.add(new IntSetting.Builder()
            .name("Mountain Height")
            .description("Builds stairs up to this many blocks from your starting Y level")
            .min (3)
            .sliderRange(3, 380)
            .defaultValue(30)
            .visible(autolavamountain::get)
            .build());
    private final Setting<Integer> limit = sgBuild.add(new IntSetting.Builder()
            .name("UpwardBuildLimit")
            .description("sets the Y level at which the stairs stop going up")
            .sliderRange(-64, 318)
            .defaultValue(318)
            .build());
    private final Setting<Integer> downlimit = sgBuild.add(new IntSetting.Builder()
            .name("DownwardBuildLimit")
            .description("sets the Y level at which the stairs stop going down")
            .sliderRange(-64, 318)
            .defaultValue(-64)
            .visible(() -> !autolavamountain.get())
            .build());
    public final Setting<Boolean> InvertUpDir = sgBuild.add(new BoolSetting.Builder()
            .name("InvertDir@UpwardLimitOrCeiling")
            .description("Inverts Direction from up to down, shortly before you reach your set limit or a ceiling.")
            .defaultValue(false)
            .visible(() -> !autolavamountain.get())
            .build()
    );
    public final Setting<Boolean> InvertDownDir = sgBuild.add(new BoolSetting.Builder()
            .name("InvertDir@DownwardLimitOrFloor")
            .description("Inverts Direction from down to up, shortly before you reach your set limit or a floor.")
            .defaultValue(false)
            .visible(() -> !autolavamountain.get())
            .build()
    );
    public final Setting<Boolean> swap = sgGeneral.add(new BoolSetting.Builder()
            .name("SwapStackonRunOut")
            .description("Swaps to another stack of blocks in your hotbar when you run out")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> swapPause = sgGeneral.add(new IntSetting.Builder()
            .name("Pause On Swap (ticks)")
            .description("Pause for this many ticks when a stack runs out of blocks and you are swapping to the next.")
            .sliderRange(1, 60)
            .min(1)
            .defaultValue(3)
            .visible(() -> swap.get())
            .build());
    public final Setting<Boolean> disabledisconnect = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable On Disconnect")
            .description("Toggles the Module off when you disconnect.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> lagpause = sgTimings.add(new BoolSetting.Builder()
            .name("Pause if Server Lagging")
            .description("Pause Builder if server is lagging")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> lag = sgTimings.add(new DoubleSetting.Builder()
            .name("How many seconds until pause")
            .description("Pause Builder if server is lagging for this many seconds.")
            .min(0)
            .sliderRange(0, 10)
            .defaultValue(1)
            .visible(lagpause::get)
            .build());

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
    private final Setting<Boolean> rendertopbottomblock = sgRender.add(new BoolSetting.Builder()
            .name("render highest/lowest block")
            .description("Renders a block overlay where the highest and lowest blocks are. These positions are used for AutoLavacaster timing calculations.")
            .defaultValue(false)
            .build()
    );
    private final Setting<SettingColor> topbottomsideColor = sgRender.add(new ColorSetting.Builder()
            .name("high/low-block-side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 15, true))
            .visible(() -> render.get() && rendertopbottomblock.get())
            .build()
    );

    private final Setting<SettingColor> topbottomlineColor = sgRender.add(new ColorSetting.Builder()
            .name("high/low-block-line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 255, true))
            .visible(() -> render.get() && rendertopbottomblock.get())
            .build()
    );
    public final Setting<Boolean> lowYrst = sgGeneral.add(new BoolSetting.Builder()
            .name("ResetLowestBlockOnACTIVATE")
            .description("UNCHECK for proper timings for AutoLavaCaster's UseLastLowestBlockfromAutoMountain timing mode if NOT clicking to pause. LOWEST BLOCK ONLY RESET IF AutoLavaCaster is used or button here is pressed.")
            .defaultValue(true)
            .build()
    );
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton rstlowblock = table.add(theme.button("Reset Lowest/Highest Block")).expandX().minWidth(100).widget();
        rstlowblock.action = () -> {
            lowestblock= new BlockPos(666,-666,666);
            highestblock= new BlockPos(666,-666,666);
            isthisfirstblock = true;
        };
        table.row();
        return table;
    }

    public AutoMountain() {
        super(Trouser.Main, "AutoMountain", "Make Mountains!");
    }
    private boolean pause = false;
    public static boolean autocasttimenow = false;
    private boolean resetTimer;
    private float timeSinceLastTick;
    private int delayLeft = delay.get();
    private int offLeft = offTime.get();
    private BlockPos playerPos;
    private BlockPos renderplayerPos;
    private int cookie=0;
    private int speed=0;
    private boolean go=true;
    private float cookieyaw;
    private boolean search=true;
    private boolean search2=true;
    public static BlockPos lowestblock= new BlockPos(666,-666,666);
    public static BlockPos highestblock= new BlockPos(666,-666,666);
    public static int groundY;
    public static int groundY2;
    private int lowblockY=-1;
    private int highblockY=-1;
    public static boolean isthisfirstblock;
    public static Direction wasfacingBOT;
    private Direction wasfacing;
    private int prevPitch;
    private boolean justSwapped = false;
    private int graceTicks = 0;
    private int lastHotbarSlot = -1; // Track slot changes
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen && disabledisconnect.get()) toggle();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disabledisconnect.get()) toggle();
    }

    @Override
    public void onActivate() {
        lastHotbarSlot = mc.player.getInventory().getSelectedSlot();
        if (lowYrst.get() || autolavamountain.get())isthisfirstblock = true;
        groundY=0;
        groundY2=0;
        lowblockY=-1;
        highblockY=-1;
        if (startPaused.get()){
            pause = false;
            if (autolavamountain.get()) ChatUtils.sendMsg(Component.nullToEmpty("Press UseKey (RightClick) to Build a Mountain! Please wait while the bot works."));
            else ChatUtils.sendMsg(Component.nullToEmpty("Press UseKey (RightClick) to Build Stairs!"));
        } else if (!startPaused.get()){
            mc.player.setPosRaw(mc.player.getX(),Math.ceil(mc.player.getY()),mc.player.getZ());
            wasfacing=mc.player.getDirection();
            prevPitch=Math.round(mc.player.getXRot());
            if (swap.get()){
                cascadingpileof();
            }
            if (autolavamountain.get()){
                wasfacingBOT=mc.player.getDirection();
                lavamountainingredients();
            }
            mc.player.setDeltaMovement(0,0,0);
            PlayerUtils.centerPlayer();
            pause = true;
            if (autolavamountain.get()) ChatUtils.sendMsg(Component.nullToEmpty("Building a Mountain! Please wait while the bot works."));
        }
        resetTimer = false;
        playerPos = mc.player.blockPosition();
        renderplayerPos = mc.player.blockPosition();
        if (startPaused.get() || isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) return;
        BlockPos pos = playerPos.offset(new Vec3i(0,-1,0));
        if (mc.level.getBlockState(pos).canBeReplaced()) {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null) return;
        mc.player.setNoGravity(false);
        if (isthisfirstblock){
            highestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
            lowestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
            isthisfirstblock=false;
        }
        if (!startPaused.get() && pause==true){
            if (!isthisfirstblock && mc.player.getY()<lowestblock.getY()) lowestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
            if (!isthisfirstblock && mc.player.getY()>highestblock.getY()+1) highestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
        }
        search=true;
        seekground();
        search2=true;
        seekground2();
        speed=0;
        resetTimer = true;
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        if (isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) return;
        if (!startPaused.get() && !pause){
            BlockPos pos = playerPos.offset(new Vec3i(0,-1,0));
            if (mc.level.getBlockState(pos).canBeReplaced()) {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                mc.player.swing(InteractionHand.MAIN_HAND);}
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (render.get()) {
            if (mc.options.keyJump.isDown() && !autolavamountain.get()){
                if ((mouseT.get() && mc.player.getXRot() <= 40) || (!mouseT.get() && prevPitch <= 40)){            //UP
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.NORTH) || (!mouseT.get() && wasfacing==Direction.NORTH)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(0, +spcoffset.get(), -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.SOUTH) || (!mouseT.get() && wasfacing==Direction.SOUTH)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(0, +spcoffset.get(), 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.EAST) || (!mouseT.get() && wasfacing==Direction.EAST)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(1, +spcoffset.get(), 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.WEST) || (!mouseT.get() && wasfacing==Direction.WEST)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(-1, +spcoffset.get(), 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                }
                else if ((mouseT.get() && mc.player.getXRot() > 40) || (!mouseT.get() && prevPitch > 40)){            //DOWN
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.NORTH) || (!mouseT.get() && wasfacing==Direction.NORTH)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(0, -spcoffset.get()-2, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.SOUTH) || (!mouseT.get() && wasfacing==Direction.SOUTH)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(0, -spcoffset.get()-2, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.EAST) || (!mouseT.get() && wasfacing==Direction.EAST)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(1, -spcoffset.get()-2, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.WEST) || (!mouseT.get() && wasfacing==Direction.WEST)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(-1, -spcoffset.get()-2, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                }
            }
            else if (!mc.options.keyJump.isDown() || autolavamountain.get()) {
                if (((mouseT.get() && mc.player.getXRot() <= 40) || autolavamountain.get()) || (!mouseT.get() && prevPitch <= 40 && !autolavamountain.get())) {            //UP
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.NORTH) || (!mouseT.get() && wasfacing==Direction.NORTH)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(0, 0, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        if (autolavamountain.get() && !pause){
                            BlockPos pos2 = renderplayerPos.offset(new Vec3i(0, botlimit.get()-1, -botlimit.get()));
                            BlockPos pos3 = renderplayerPos.offset(new Vec3i(0, 1, -2));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        } else if (autolavamountain.get() && pause){
                            BlockPos pos2 = lowestblock.offset(new Vec3i(0, botlimit.get(), -botlimit.get()));
                            BlockPos pos3 = renderplayerPos.offset(new Vec3i(0, 1, -2));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.SOUTH) || (!mouseT.get() && wasfacing==Direction.SOUTH)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(0, 0, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        if (autolavamountain.get() && !pause){
                            BlockPos pos2 = renderplayerPos.offset(new Vec3i(0, botlimit.get()-1, botlimit.get()));
                            BlockPos pos3 = renderplayerPos.offset(new Vec3i(0, 1, 2));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        } else if (autolavamountain.get() && pause){
                            BlockPos pos2 = lowestblock.offset(new Vec3i(0, botlimit.get(), botlimit.get()));
                            BlockPos pos3 = renderplayerPos.offset(new Vec3i(0, 1, 2));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.EAST) || (!mouseT.get() && wasfacing==Direction.EAST)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(1, 0, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        if (autolavamountain.get() && !pause){
                            BlockPos pos3 = renderplayerPos.offset(new Vec3i(2, 1, 0));
                            BlockPos pos2 = renderplayerPos.offset(new Vec3i(botlimit.get(), botlimit.get()-1, 0));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        } else if (autolavamountain.get() && pause){
                            BlockPos pos3 = renderplayerPos.offset(new Vec3i(2, 1, 0));
                            BlockPos pos2 = lowestblock.offset(new Vec3i(botlimit.get(), botlimit.get(), 0));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.WEST) || (!mouseT.get() && wasfacing==Direction.WEST)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(-1, 0, -0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        if (autolavamountain.get() && !pause){
                            BlockPos pos3 = renderplayerPos.offset(new Vec3i(-2, 1, 0));
                            BlockPos pos2 = renderplayerPos.offset(new Vec3i(-botlimit.get(), botlimit.get()-1, 0));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        } else if (autolavamountain.get() && pause){
                            BlockPos pos3 = renderplayerPos.offset(new Vec3i(-2, 1, 0));
                            BlockPos pos2 = lowestblock.offset(new Vec3i(-botlimit.get(), botlimit.get(), 0));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                } else if ((mouseT.get() && mc.player.getXRot() > 40) || (!mouseT.get() && prevPitch > 40)) {            //DOWN
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.NORTH) || (!mouseT.get() && wasfacing==Direction.NORTH)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(0, -2, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.SOUTH) || (!mouseT.get() && wasfacing==Direction.SOUTH)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(0, -2, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.EAST) || (!mouseT.get() && wasfacing==Direction.EAST)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(1, -2, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMotionDirection()==Direction.WEST) || (!mouseT.get() && wasfacing==Direction.WEST)) {
                        BlockPos pos1 = renderplayerPos.offset(new Vec3i(-1, -2, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                }
            }
            if (rendertopbottomblock.get()){
                if (!isthisfirstblock) {
                    event.renderer.box(highestblock, topbottomsideColor.get(), topbottomlineColor.get(), shapeMode.get(), 0);
                }
                if (!isthisfirstblock) {
                    event.renderer.box(lowestblock, topbottomsideColor.get(), topbottomlineColor.get(), shapeMode.get(), 0);
                }
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseClickEvent event) {
        if (mc.options.keyUse.isDown()){
            if (pause){
                BlockPos pos = playerPos.offset(new Vec3i(0,-1,0));
                if (mc.level.getBlockState(pos).canBeReplaced()) {
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                    mc.player.swing(InteractionHand.MAIN_HAND);}
            }
            if (!pause)mc.player.setPosRaw(mc.player.getX(),Math.ceil(mc.player.getY()),mc.player.getZ());
            pause = !pause;
            mc.player.setDeltaMovement(0,0,0);
            cookie=0;
            speed=0;
            resetTimer = true;
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
            if (isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) return;
            if (isthisfirstblock){
                highestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                lowestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                isthisfirstblock=false;
            }
            if (!isthisfirstblock &&mc.player.getY()<lowestblock.getY()){
                lowestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                seekground();
            }
            if (!isthisfirstblock && mc.player.getY()>highestblock.getY()+1){
                highestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                seekground2();
            }
        }
    }
    @EventHandler
    private void onKeyEvent(KeyInputEvent event) {
        if (!pause)return;
        if (!autolavamountain.get()){
            if (mc.options.keyUp.isDown()){
                if (mouseT.get())mc.player.setXRot(35);
                if (!mouseT.get())prevPitch=35;
            }
            if (mc.options.keyDown.isDown()){
                if (mouseT.get())mc.player.setXRot(75);
                if (!mouseT.get())prevPitch=75;
            }
            if ((lagpause.get() && timeSinceLastTick >= lag.get()) || isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance()) || !pause) return;
            if (mc.options.keyLeft.isDown() && !mc.options.keyShift.isDown()){
                if (mouseT.get())mc.player.setYRot(mc.player.getYRot()-90);
                if (!mouseT.get()){
                    if (wasfacing==Direction.NORTH){
                        wasfacing=Direction.WEST;
                        return;
                    }
                    if (wasfacing==Direction.SOUTH){
                        wasfacing=Direction.EAST;
                        return;
                    }
                    if (wasfacing==Direction.WEST){
                        wasfacing=Direction.SOUTH;
                        return;
                    }
                    if (wasfacing==Direction.EAST){
                        wasfacing=Direction.NORTH;
                        return;
                    }
                }
            }
            if (mc.options.keyRight.isDown() && !mc.options.keyShift.isDown()){
                if (mouseT.get())mc.player.setYRot(mc.player.getYRot()+90);
                if (!mouseT.get()){
                    if (wasfacing==Direction.NORTH){
                        wasfacing=Direction.EAST;
                        return;
                    }
                    if (wasfacing==Direction.SOUTH){
                        wasfacing=Direction.WEST;
                        return;
                    }
                    if (wasfacing==Direction.WEST){
                        wasfacing=Direction.NORTH;
                        return;
                    }
                    if (wasfacing==Direction.EAST){
                        wasfacing=Direction.SOUTH;
                    }
                }
            }
        }
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundMovePlayerPacket)
            ((ServerboundMovePlayerPacketAccessor) event.packet).meteor$setOnGround(true);
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        playerPos = mc.player.blockPosition();
        if (mc.player.getY() % 1 != 0 && !pause){
            renderplayerPos = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY()+1, mc.player.getBlockZ());
        } else renderplayerPos = mc.player.blockPosition();
        timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();

        if (pause && swap.get()) {
            int currentSlot = mc.player.getInventory().getSelectedSlot();
            cascadingpileof();

            int newSlot = mc.player.getInventory().getSelectedSlot();
            if (newSlot != lastHotbarSlot && newSlot != currentSlot) {
                justSwapped = true;
                graceTicks = swapPause.get();
                lastHotbarSlot = newSlot;
            }
        }

        if (speed < spd.get()) {
            go = false;
            speed++;
        } else {
            speed = 0;
        }

        if (justSwapped) {
            graceTicks--;
            if (graceTicks > 0) {
                go = false;
                speed = 0;
                mc.player.setDeltaMovement(0,0,0);
                PlayerUtils.centerPlayer();
                mc.player.setPosRaw(mc.player.getX(), Math.round(mc.player.getY())+0.25, mc.player.getZ());
                return;
            } else {
                justSwapped = false;
            }
        }

        if (speed >= spd.get()) {
            go = true;
        }

        if (!pause){
            wasfacing=mc.player.getDirection();
            prevPitch=Math.round(mc.player.getXRot());
            if (autolavamountain.get()){
                wasfacingBOT=mc.player.getDirection();
                isthisfirstblock=true;
                lavamountainingredients();
            }
            mc.player.setNoGravity(false);
            search=true;
            search2=true;
        }
        if (!pause) return;
        if (autolavamountain.get()){
            if (wasfacingBOT==Direction.NORTH) mc.player.setYRot(180);
            if (wasfacingBOT==Direction.SOUTH) mc.player.setYRot(0);
            if (wasfacingBOT==Direction.WEST) mc.player.setYRot(90);
            if (wasfacingBOT==Direction.EAST) mc.player.setYRot(-90);
        }
        if (!delayakick.get()){
            offLeft=666666666;
            delayLeft=0;
        }
        else if (delayakick.get() && offLeft>offTime.get()){
            offLeft=offTime.get();
        }
        mc.player.setDeltaMovement(0,0,0);
        PlayerUtils.centerPlayer();
        mc.player.setPosRaw(mc.player.getX(),Math.round(mc.player.getY())+0.25,mc.player.getZ());
        if (Modules.get().get(Flight.class).isActive()) {
            Modules.get().get(Flight.class).toggle();
        }
        if (Modules.get().get(FlightAntikick.class).isActive()) {
            Modules.get().get(FlightAntikick.class).toggle();
        }
        if (Modules.get().get(TPFly.class).isActive()) {
            Modules.get().get(TPFly.class).toggle();
        }
        if (mc.level.getBlockState(mc.player.blockPosition()).getBlock() == Blocks.AIR) {
            resetTimer = false;
            Modules.get().get(Timer.class).setOverride(StairTimer.get());
        } else if (!resetTimer) {
            resetTimer = true;
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
        if ((lagpause.get() && timeSinceLastTick >= lag.get()) || isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance()) || !pause || !go) return;
        if (mc.options.keyShift.isDown() && mc.options.keyRight.isDown() && delayLeft <= 0 && offLeft > 0 && !autolavamountain.get()){
            cookie++;
            if (cookie==munscher.get()){
                cookieyaw=mc.player.getYRot();
                if (mouseT.get())mc.player.setYRot(mc.player.getYRot()+90);
                if (!mouseT.get()){
                    if (wasfacing==Direction.NORTH){
                        wasfacing=Direction.EAST;
                    } else if (wasfacing==Direction.SOUTH){
                        wasfacing=Direction.WEST;
                    } else if (wasfacing==Direction.WEST){
                        wasfacing=Direction.NORTH;
                    } else if (wasfacing==Direction.EAST){
                        wasfacing=Direction.SOUTH;
                    }
                }
            }else if (cookie>=munscher.get()+munscher.get()){
                if (mouseT.get())mc.player.setYRot(mc.player.getYRot()-90);
                if (!mouseT.get()){
                    if (wasfacing==Direction.NORTH){
                        wasfacing=Direction.WEST;
                    } else if (wasfacing==Direction.SOUTH){
                        wasfacing=Direction.EAST;
                    } else if (wasfacing==Direction.WEST){
                        wasfacing=Direction.SOUTH;
                    } else if (wasfacing==Direction.EAST){
                        wasfacing=Direction.NORTH;
                    }
                }
                cookie=0;
            }
        }
        if (mc.options.keyShift.isDown() && mc.options.keyLeft.isDown() && delayLeft <= 0 && offLeft > 0 && !autolavamountain.get()){
            cookie++;
            if (cookie==munscher.get()){
                cookieyaw=mc.player.getYRot();
                if (mouseT.get())mc.player.setYRot(mc.player.getYRot()-90);
                if (!mouseT.get()){
                    if (wasfacing==Direction.NORTH){
                        wasfacing=Direction.WEST;
                    } else if (wasfacing==Direction.SOUTH){
                        wasfacing=Direction.EAST;
                    } else if (wasfacing==Direction.WEST){
                        wasfacing=Direction.SOUTH;
                    } else if (wasfacing==Direction.EAST){
                        wasfacing=Direction.NORTH;
                    }
                }
            }else if (cookie>=munscher.get()+munscher.get()){
                if (mouseT.get())mc.player.setYRot(mc.player.getYRot()+90);
                if (!mouseT.get()){
                    if (wasfacing==Direction.NORTH){
                        wasfacing=Direction.EAST;
                    } else if (wasfacing==Direction.SOUTH){
                        wasfacing=Direction.WEST;
                    } else if (wasfacing==Direction.WEST){
                        wasfacing=Direction.NORTH;
                    } else if (wasfacing==Direction.EAST){
                        wasfacing=Direction.SOUTH;
                    }
                }
                cookie=0;
            }
        }
        else if (!mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown() && cookie>=1){
            mc.player.setYRot(cookieyaw);
            cookieyaw=mc.player.getYRot();
            cookie=0;
        }
        if (pause){
            if (isthisfirstblock){
                highestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                lowestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                isthisfirstblock=false;
            }
            if (!isthisfirstblock &&mc.player.getY()<lowestblock.getY()){
                lowestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                seekground();
            }
            if (!isthisfirstblock && mc.player.getY()>highestblock.getY()+1){
                highestblock=mc.player.blockPosition().offset(new Vec3i(0,-1,0));
                seekground2();
            }}
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (pause && autolavamountain.get()) {
            if (wasfacingBOT==Direction.NORTH) mc.player.setYRot(180);
            if (wasfacingBOT==Direction.SOUTH) mc.player.setYRot(0);
            if (wasfacingBOT==Direction.WEST) mc.player.setYRot(90);
            if (wasfacingBOT==Direction.EAST) mc.player.setYRot(-90);
            mc.player.setNoGravity(true);
            if (mc.player.getY() >= limit.get()-4 | mc.player.getY() > lowestblock.getY()+botlimit.get()){
                autocasttimenow=true;
                search=true;
                seekground();
                search2=true;
                seekground2();
                mc.player.setPosRaw(mc.player.getX(), mc.player.getY()+1, mc.player.getZ());
                mc.player.setXRot(80);
                if (!Modules.get().get(AutoLavaCaster.class).isActive()) Modules.get().get(AutoLavaCaster.class).toggle();
                ChatUtils.sendMsg(Component.nullToEmpty("Activating AutoLavaCaster."));
                toggle();
            }
        }
        if (!pause) return;

        if (((mouseT.get() && mc.player.getXRot() <= 40 || autolavamountain.get())) || (!mouseT.get() && prevPitch <= 40)){
            if (delayLeft > 0) delayLeft--;
            else if ((!lagpause.get() || timeSinceLastTick < lag.get()) && delayLeft <= 0 && offLeft > 0 && (mc.player.getY() <= limit.get() &&  mc.player.getY() >= downlimit.get() && !autolavamountain.get() || mc.player.getY() <= limit.get()-4 && mc.player.getY() <= lowestblock.getY()+botlimit.get()+1 && autolavamountain.get())) {
                offLeft--;
                if ((lagpause.get() && timeSinceLastTick >= lag.get()) || isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance()) || !pause || !go) return;
                if ((mouseT.get() && mc.player.getMotionDirection()==Direction.NORTH) || (!mouseT.get() && wasfacing==Direction.NORTH)) {            //UP
                    if (mc.options.keyJump.isDown() && !autolavamountain.get()){
                        BlockPos un1 = playerPos.offset(new Vec3i(0,spcoffset.get()+2,0));
                        BlockPos un2 = playerPos.offset(new Vec3i(0,spcoffset.get()+1,-1));
                        BlockPos un3 = playerPos.offset(new Vec3i(0,spcoffset.get()+2,-1));
                        BlockPos un4 = playerPos.offset(new Vec3i(0,spcoffset.get()+3,-1));
                        BlockPos pos = playerPos.offset(new Vec3i(0,spcoffset.get(),-1));
                        if (mc.level.getBlockState(un1).canBeReplaced() && mc.level.getBlockState(un2).canBeReplaced() && mc.level.getBlockState(un3).canBeReplaced() && mc.level.getBlockState(un4).canBeReplaced() && mc.level.getFluidState(un1).isEmpty() && mc.level.getFluidState(un2).isEmpty() && mc.level.getFluidState(un3).isEmpty() && mc.level.getFluidState(un4).isEmpty() && !mc.level.getBlockState(un1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(un2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(un3).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(un4).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(un2)){
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX(),mc.player.getY()+1+spcoffset.get(),mc.player.getZ()-1);
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()){
                                if (mouseT.get())mc.player.setXRot(75);
                                if (!mouseT.get())prevPitch=75;
                            }
                        }
                    } else {
                        BlockPos un1 = playerPos.offset(new Vec3i(0,2,0));
                        BlockPos un2 = playerPos.offset(new Vec3i(0,1,-1));
                        BlockPos un3 = playerPos.offset(new Vec3i(0,2,-1));
                        BlockPos un4 = playerPos.offset(new Vec3i(0,3,-1));
                        BlockPos pos = playerPos.offset(new Vec3i(0,0,-1));
                        if (mc.level.getBlockState(un1).canBeReplaced() && mc.level.getBlockState(un2).canBeReplaced() && mc.level.getBlockState(un3).canBeReplaced() && mc.level.getBlockState(un4).canBeReplaced() && mc.level.getFluidState(un1).isEmpty() && mc.level.getFluidState(un2).isEmpty() && mc.level.getFluidState(un3).isEmpty() && mc.level.getFluidState(un4).isEmpty() && !mc.level.getBlockState(un1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(un2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(un3).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(un4).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(un2)){
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX(),mc.player.getY()+1,mc.player.getZ()-1);
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()){
                                if (mouseT.get())mc.player.setXRot(75);
                                if (!mouseT.get())prevPitch=75;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMotionDirection()==Direction.EAST) || (!mouseT.get() && wasfacing==Direction.EAST)) {            //UP
                    if (mc.options.keyJump.isDown() && !autolavamountain.get()){
                        BlockPos ue1 = playerPos.offset(new Vec3i(0,spcoffset.get()+2,0));
                        BlockPos ue2 = playerPos.offset(new Vec3i(+1,spcoffset.get()+1,0));
                        BlockPos ue3 = playerPos.offset(new Vec3i(+1,spcoffset.get()+2,0));
                        BlockPos ue4 = playerPos.offset(new Vec3i(+1,spcoffset.get()+3,0));
                        BlockPos pos = playerPos.offset(new Vec3i(1,spcoffset.get(),0));
                        if (mc.level.getBlockState(ue1).canBeReplaced() && mc.level.getBlockState(ue2).canBeReplaced() && mc.level.getBlockState(ue3).canBeReplaced() && mc.level.getBlockState(ue4).canBeReplaced() && mc.level.getFluidState(ue1).isEmpty() && mc.level.getFluidState(ue2).isEmpty() && mc.level.getFluidState(ue3).isEmpty() && mc.level.getFluidState(ue4).isEmpty() && !mc.level.getBlockState(ue1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ue2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ue3).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ue4).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(ue2)){
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX()+1,mc.player.getY()+1+spcoffset.get(),mc.player.getZ());
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()){
                                if (mouseT.get())mc.player.setXRot(75);
                                if (!mouseT.get())prevPitch=75;
                            }
                        }
                    } else {
                        BlockPos ue1 = playerPos.offset(new Vec3i(0,2,0));
                        BlockPos ue2 = playerPos.offset(new Vec3i(+1,1,0));
                        BlockPos ue3 = playerPos.offset(new Vec3i(+1,2,0));
                        BlockPos ue4 = playerPos.offset(new Vec3i(+1,3,0));
                        BlockPos pos = playerPos.offset(new Vec3i(1,0,0));
                        if (mc.level.getBlockState(ue1).canBeReplaced() && mc.level.getBlockState(ue2).canBeReplaced() && mc.level.getBlockState(ue3).canBeReplaced() && mc.level.getBlockState(ue4).canBeReplaced() && mc.level.getFluidState(ue1).isEmpty() && mc.level.getFluidState(ue2).isEmpty() && mc.level.getFluidState(ue3).isEmpty() && mc.level.getFluidState(ue4).isEmpty() && !mc.level.getBlockState(ue1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ue2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ue3).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ue4).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(ue2)){
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX()+1,mc.player.getY()+1,mc.player.getZ());
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()){
                                if (mouseT.get())mc.player.setXRot(75);
                                if (!mouseT.get())prevPitch=75;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMotionDirection()==Direction.SOUTH) || (!mouseT.get() && wasfacing==Direction.SOUTH)) {            //UP
                    if (mc.options.keyJump.isDown() && !autolavamountain.get()){
                        BlockPos us1 = playerPos.offset(new Vec3i(0,spcoffset.get()+2,0));
                        BlockPos us2 = playerPos.offset(new Vec3i(0,spcoffset.get()+1,+1));
                        BlockPos us3 = playerPos.offset(new Vec3i(0,spcoffset.get()+2,+1));
                        BlockPos us4 = playerPos.offset(new Vec3i(0,spcoffset.get()+3,+1));
                        BlockPos pos = playerPos.offset(new Vec3i(0,spcoffset.get(),1));
                        if (mc.level.getBlockState(us1).canBeReplaced() && mc.level.getBlockState(us2).canBeReplaced() && mc.level.getBlockState(us3).canBeReplaced() && mc.level.getBlockState(us4).canBeReplaced() && mc.level.getFluidState(us1).isEmpty() && mc.level.getFluidState(us2).isEmpty() && mc.level.getFluidState(us3).isEmpty() && mc.level.getFluidState(us4).isEmpty() && !mc.level.getBlockState(us1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(us2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(us3).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(us4).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(us2)){
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX(),mc.player.getY()+1+spcoffset.get(),mc.player.getZ()+1);
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()){
                                if (mouseT.get())mc.player.setXRot(75);
                                if (!mouseT.get())prevPitch=75;
                            }
                        }
                    } else {
                        BlockPos us1 = playerPos.offset(new Vec3i(0,2,0));
                        BlockPos us2 = playerPos.offset(new Vec3i(0,1,+1));
                        BlockPos us3 = playerPos.offset(new Vec3i(0,2,+1));
                        BlockPos us4 = playerPos.offset(new Vec3i(0,3,+1));
                        BlockPos pos = playerPos.offset(new Vec3i(0,0,1));
                        if (mc.level.getBlockState(us1).canBeReplaced() && mc.level.getBlockState(us2).canBeReplaced() && mc.level.getBlockState(us3).canBeReplaced() && mc.level.getBlockState(us4).canBeReplaced() && mc.level.getFluidState(us1).isEmpty() && mc.level.getFluidState(us2).isEmpty() && mc.level.getFluidState(us3).isEmpty() && mc.level.getFluidState(us4).isEmpty() && !mc.level.getBlockState(us1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(us2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(us3).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(us4).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(us2)){
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX(),mc.player.getY()+1,mc.player.getZ()+1);
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()){
                                if (mouseT.get())mc.player.setXRot(75);
                                if (!mouseT.get())prevPitch=75;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMotionDirection()==Direction.WEST) || (!mouseT.get() && wasfacing==Direction.WEST)) {            //UP
                    if (mc.options.keyJump.isDown() && !autolavamountain.get()){
                        BlockPos uw1 = playerPos.offset(new Vec3i(0,spcoffset.get()+2,0));
                        BlockPos uw2 = playerPos.offset(new Vec3i(-1,spcoffset.get()+1,0));
                        BlockPos uw3 = playerPos.offset(new Vec3i(-1,spcoffset.get()+2,0));
                        BlockPos uw4 = playerPos.offset(new Vec3i(-1,spcoffset.get()+3,0));
                        BlockPos pos = playerPos.offset(new Vec3i(-1,spcoffset.get(),0));
                        if (mc.level.getBlockState(uw1).canBeReplaced() && mc.level.getBlockState(uw2).canBeReplaced() && mc.level.getBlockState(uw3).canBeReplaced() && mc.level.getBlockState(uw4).canBeReplaced() && mc.level.getFluidState(uw1).isEmpty() && mc.level.getFluidState(uw2).isEmpty() && mc.level.getFluidState(uw3).isEmpty() && mc.level.getFluidState(uw4).isEmpty() && !mc.level.getBlockState(uw1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(uw2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(uw3).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(uw4).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(uw2)){
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX()-1,mc.player.getY()+1+spcoffset.get(),mc.player.getZ());
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()){
                                if (mouseT.get())mc.player.setXRot(75);
                                if (!mouseT.get())prevPitch=75;
                            }
                        }
                    }else {
                        BlockPos uw1 = playerPos.offset(new Vec3i(0,2,0));
                        BlockPos uw2 = playerPos.offset(new Vec3i(-1,1,0));
                        BlockPos uw3 = playerPos.offset(new Vec3i(-1,2,0));
                        BlockPos uw4 = playerPos.offset(new Vec3i(-1,3,0));
                        BlockPos pos = playerPos.offset(new Vec3i(-1,0,0));
                        if (mc.level.getBlockState(uw1).canBeReplaced() && mc.level.getBlockState(uw2).canBeReplaced() && mc.level.getBlockState(uw3).canBeReplaced() && mc.level.getBlockState(uw4).canBeReplaced() && mc.level.getFluidState(uw1).isEmpty() && mc.level.getFluidState(uw2).isEmpty() && mc.level.getFluidState(uw3).isEmpty() && mc.level.getFluidState(uw4).isEmpty() && !mc.level.getBlockState(uw1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(uw2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(uw3).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(uw4).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(uw2)){
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX()-1,mc.player.getY()+1,mc.player.getZ());
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()){
                                if (mouseT.get())mc.player.setXRot(75);
                                if (!mouseT.get())prevPitch=75;
                            }
                        }
                    }
                }
                if (mc.player.getY() >= limit.get()-1 && InvertUpDir.get() && !autolavamountain.get()){
                    if (mouseT.get())mc.player.setXRot(75);
                    if (!mouseT.get())prevPitch=75;
                }
            } else if (mc.player.getY() <= downlimit.get() && !InvertDownDir.get()|| mc.player.getY() >= limit.get() && !InvertUpDir.get() && !autolavamountain.get()|| mc.player.getY() >= lowestblock.getY()+botlimit.get()+1 && autolavamountain.get()|| delayLeft <= 0 && offLeft <= 0) {
                delayLeft = delay.get();
                offLeft = offTime.get();
            }
        } else if ((mouseT.get() && mc.player.getXRot() > 40 && !autolavamountain.get()) || (!mouseT.get() && prevPitch > 40)){
            if (delayLeft > 0) delayLeft--;
            else if ((!lagpause.get() || timeSinceLastTick < lag.get()) && delayLeft <= 0 && offLeft > 0 && mc.player.getY() <= limit.get() && mc.player.getY() >= downlimit.get()) {
                offLeft--;
                if (mc.player == null || mc.level == null) {toggle(); return;}
                if ((lagpause.get() && timeSinceLastTick >= lag.get()) || isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance()) || !pause || !go) return;
                if ((mouseT.get() && mc.player.getMotionDirection()==Direction.NORTH) || (!mouseT.get() && wasfacing==Direction.NORTH)) {            //DOWN
                    if (mc.options.keyJump.isDown()){
                        BlockPos dn1 = playerPos.offset(new Vec3i(0,-spcoffset.get()-1,-1));
                        BlockPos dn2 = playerPos.offset(new Vec3i(0,-spcoffset.get(),-1));
                        BlockPos dn3 = playerPos.offset(new Vec3i(0,-spcoffset.get()+1,-1));
                        BlockPos pos = playerPos.offset(new Vec3i(0,-spcoffset.get()-2,-1));
                        if (mc.level.getBlockState(dn1).canBeReplaced() && mc.level.getBlockState(dn2).canBeReplaced() && mc.level.getBlockState(dn3).canBeReplaced() && mc.level.getFluidState(dn1).isEmpty() && mc.level.getFluidState(dn2).isEmpty() && mc.level.getFluidState(dn3).isEmpty() && !mc.level.getBlockState(dn1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(dn2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(dn3).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(dn2)) {
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX(),mc.player.getY()-1-spcoffset.get(),mc.player.getZ()-1);
                        } else {if (InvertDownDir.get()) mc.player.setXRot(35);}
                    } else {
                        BlockPos dn1 = playerPos.offset(new Vec3i(0,-1,-1));
                        BlockPos dn2 = playerPos.offset(new Vec3i(0,0,-1));
                        BlockPos dn3 = playerPos.offset(new Vec3i(0,1,-1));
                        BlockPos pos = playerPos.offset(new Vec3i(0,-2,-1));
                        if (mc.level.getBlockState(dn1).canBeReplaced() && mc.level.getBlockState(dn2).canBeReplaced() && mc.level.getBlockState(dn3).canBeReplaced() && mc.level.getFluidState(dn1).isEmpty() && mc.level.getFluidState(dn2).isEmpty() && mc.level.getFluidState(dn3).isEmpty() && !mc.level.getBlockState(dn1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(dn2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(dn3).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(dn2)) {
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX(),mc.player.getY()-1,mc.player.getZ()-1);
                        } else {
                            if (InvertDownDir.get()){
                                if (mouseT.get())mc.player.setXRot(35);
                                if (!mouseT.get())prevPitch=35;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMotionDirection()==Direction.EAST) || (!mouseT.get() && wasfacing==Direction.EAST)) {            //DOWN
                    if (mc.options.keyJump.isDown()){
                        BlockPos de1 = playerPos.offset(new Vec3i(1,-spcoffset.get()-1,0));
                        BlockPos de2 = playerPos.offset(new Vec3i(1,-spcoffset.get(),0));
                        BlockPos de3 = playerPos.offset(new Vec3i(1,-spcoffset.get()+1,0));
                        BlockPos pos = playerPos.offset(new Vec3i(1,-spcoffset.get()-2,0));
                        if (mc.level.getBlockState(de1).canBeReplaced() && mc.level.getBlockState(de2).canBeReplaced() && mc.level.getBlockState(de3).canBeReplaced() && mc.level.getFluidState(de1).isEmpty() && mc.level.getFluidState(de2).isEmpty() && mc.level.getFluidState(de3).isEmpty() && !mc.level.getBlockState(de1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(de2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(de3).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(de2)) {
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX()+1,mc.player.getY()-1-spcoffset.get(),mc.player.getZ());
                        } else {
                            if (InvertDownDir.get()){
                                if (mouseT.get())mc.player.setXRot(35);
                                if (!mouseT.get())prevPitch=35;
                            }
                        }
                    } else {
                        BlockPos de1 = playerPos.offset(new Vec3i(1,-1,0));
                        BlockPos de2 = playerPos.offset(new Vec3i(1,0,0));
                        BlockPos de3 = playerPos.offset(new Vec3i(1,1,0));
                        BlockPos pos = playerPos.offset(new Vec3i(1,-2,0));
                        if (mc.level.getBlockState(de1).canBeReplaced() && mc.level.getBlockState(de2).canBeReplaced() && mc.level.getBlockState(de3).canBeReplaced() && mc.level.getFluidState(de1).isEmpty() && mc.level.getFluidState(de2).isEmpty() && mc.level.getFluidState(de3).isEmpty() && !mc.level.getBlockState(de1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(de2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(de3).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(de2)) {
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX()+1,mc.player.getY()-1,mc.player.getZ());
                        } else {
                            if (InvertDownDir.get()){
                                if (mouseT.get())mc.player.setXRot(35);
                                if (!mouseT.get())prevPitch=35;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMotionDirection()==Direction.SOUTH) || (!mouseT.get() && wasfacing==Direction.SOUTH)) {            //DOWN
                    if (mc.options.keyJump.isDown()){
                        BlockPos ds1 = playerPos.offset(new Vec3i(0,-spcoffset.get()-1,1));
                        BlockPos ds2 = playerPos.offset(new Vec3i(0,-spcoffset.get(),1));
                        BlockPos ds3 = playerPos.offset(new Vec3i(0,-spcoffset.get()+1,1));
                        BlockPos pos = playerPos.offset(new Vec3i(0,-spcoffset.get()-2,1));
                        if (mc.level.getBlockState(ds1).canBeReplaced() && mc.level.getBlockState(ds2).canBeReplaced() && mc.level.getBlockState(ds3).canBeReplaced() && mc.level.getFluidState(ds1).isEmpty() && mc.level.getFluidState(ds2).isEmpty() && mc.level.getFluidState(ds3).isEmpty() && !mc.level.getBlockState(ds1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ds2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ds3).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(ds2)) {
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX(),mc.player.getY()-1- spcoffset.get(),mc.player.getZ()+1);
                        } else {
                            if (InvertDownDir.get()){
                                if (mouseT.get())mc.player.setXRot(35);
                                if (!mouseT.get())prevPitch=35;
                            }
                        }
                    } else {
                        BlockPos ds1 = playerPos.offset(new Vec3i(0,-1,1));
                        BlockPos ds2 = playerPos.offset(new Vec3i(0,0,1));
                        BlockPos ds3 = playerPos.offset(new Vec3i(0,1,1));
                        BlockPos pos = playerPos.offset(new Vec3i(0,-2,1));
                        if (mc.level.getBlockState(ds1).canBeReplaced() && mc.level.getBlockState(ds2).canBeReplaced() && mc.level.getBlockState(ds3).canBeReplaced() && mc.level.getFluidState(ds1).isEmpty() && mc.level.getFluidState(ds2).isEmpty() && mc.level.getFluidState(ds3).isEmpty() && !mc.level.getBlockState(ds1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ds2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(ds3).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(ds2)) {
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX(),mc.player.getY()-1,mc.player.getZ()+1);
                        } else {
                            if (InvertDownDir.get()){
                                if (mouseT.get())mc.player.setXRot(35);
                                if (!mouseT.get())prevPitch=35;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMotionDirection()==Direction.WEST) || (!mouseT.get() && wasfacing==Direction.WEST)) {            //DOWN
                    if (mc.options.keyJump.isDown()){
                        BlockPos dw1 = playerPos.offset(new Vec3i(-1,-spcoffset.get()-1,0));
                        BlockPos dw2 = playerPos.offset(new Vec3i(-1,-spcoffset.get(),0));
                        BlockPos dw3 = playerPos.offset(new Vec3i(-1,-spcoffset.get()+1,0));
                        BlockPos pos = playerPos.offset(new Vec3i(-1,-spcoffset.get()-2,0));
                        if (mc.level.getBlockState(dw1).canBeReplaced() && mc.level.getBlockState(dw2).canBeReplaced() && mc.level.getBlockState(dw3).canBeReplaced() && mc.level.getFluidState(dw1).isEmpty() && mc.level.getFluidState(dw2).isEmpty() && mc.level.getFluidState(dw3).isEmpty() && !mc.level.getBlockState(dw1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(dw2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(dw3).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(dw2)) {
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX()-1,mc.player.getY()-1-spcoffset.get(),mc.player.getZ());
                        } else {
                            if (InvertDownDir.get()){
                                if (mouseT.get())mc.player.setXRot(35);
                                if (!mouseT.get())prevPitch=35;
                            }
                        }
                    }else {
                        BlockPos dw1 = playerPos.offset(new Vec3i(-1,-1,0));
                        BlockPos dw2 = playerPos.offset(new Vec3i(-1,0,0));
                        BlockPos dw3 = playerPos.offset(new Vec3i(-1,1,0));
                        BlockPos pos = playerPos.offset(new Vec3i(-1,-2,0));
                        if (mc.level.getBlockState(dw1).canBeReplaced() && mc.level.getBlockState(dw2).canBeReplaced() && mc.level.getBlockState(dw3).canBeReplaced() && mc.level.getFluidState(dw1).isEmpty() && mc.level.getFluidState(dw2).isEmpty() && mc.level.getFluidState(dw3).isEmpty() && !mc.level.getBlockState(dw1).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(dw2).is(Blocks.POWDER_SNOW) && !mc.level.getBlockState(dw3).is(Blocks.POWDER_SNOW) && mc.level.getWorldBorder().isWithinBounds(dw2)) {
                            if (mc.level.getBlockState(pos).canBeReplaced()){
                                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            mc.player.setPos(mc.player.getX()-1,mc.player.getY()-1,mc.player.getZ());
                        } else {
                            if (InvertDownDir.get()){
                                if (mouseT.get())mc.player.setXRot(35);
                                if (!mouseT.get())prevPitch=35;
                            }
                        }
                    }
                }
                if (mc.player.getY() <= downlimit.get()+1 && InvertDownDir.get()){
                    if (mouseT.get())mc.player.setXRot(35);
                    if (!mouseT.get())prevPitch=35;
                }
            } else if (mc.player.getY() <= downlimit.get() || mc.player.getY() >= limit.get() || delayLeft <= 0 && offLeft <= 0) {
                delayLeft = delay.get();
                offLeft = offTime.get();
            }
        }
        PlayerUtils.centerPlayer();
    }
    private void seekground() {
        if (!(mc.level.getBlockState(lowestblock.offset(new Vec3i(0,-1,0))).getBlock() ==Blocks.AIR)) groundY=lowestblock.getY();
        else {
            for (lowblockY = -2; lowblockY > -319;) {
                BlockPos lowpos1= lowestblock.offset(new Vec3i(0, lowblockY,0));
                if (mc.level.getBlockState(lowpos1).getBlock()==Blocks.AIR && search) {
                    groundY=lowpos1.getY();
                }
                if (!(mc.level.getBlockState(lowpos1).getBlock()==Blocks.AIR)) search=false;
                lowblockY--;
            }
        }
    }
    private void seekground2() {
        if (!(mc.level.getBlockState(highestblock.offset(new Vec3i(0,-1,0))).getBlock() ==Blocks.AIR)) groundY2=highestblock.getY();
        else {
            for (highblockY = -2; highblockY > -319;) {
                BlockPos lowpos1= highestblock.offset(new Vec3i(0, highblockY,0));
                if (mc.level.getBlockState(lowpos1).getBlock()==Blocks.AIR && search2) {
                    groundY2=lowpos1.getY();
                }
                if (!(mc.level.getBlockState(lowpos1).getBlock()==Blocks.AIR)) search2=false;
                highblockY--;
            }
        }
    }
    private void lavamountainingredients() {
        FindItemResult findLava = InvUtils.findInHotbar(Items.LAVA_BUCKET);
        FindItemResult findWater = InvUtils.findInHotbar(Items.WATER_BUCKET);
        if (!findLava.found() || !findWater.found()) {
            error("Put a Lava and Water Bucket in your hotbar for the bot to work.");
            toggle();
        }
    }
    private void cascadingpileof() {
        FindItemResult findResult = InvUtils.findInHotbar(block -> !isInvalidBlock(block));
        if (!findResult.found() || findResult.slot() < 0 || findResult.slot() > 8) return;
        mc.player.getInventory().setSelectedSlot(findResult.slot());
    }

    private boolean isInvalidBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return true;
        if (stack.getItem() instanceof BedItem) return true;
        if (stack.getItem() instanceof SolidBucketItem) return true;
        if (stack.getItem() instanceof ScaffoldingBlockItem) return true;
        if (stack.getItem() instanceof DoubleHighBlockItem) return true;
        if (stack.getItem() instanceof StandingAndWallBlockItem) return true;
        if (stack.getItem() instanceof PlaceOnWaterBlockItem) return true;
        Block block = blockItem.getBlock();
        return block instanceof VegetationBlock
                || block instanceof TorchBlock
                || block instanceof DiodeBlock
                || block instanceof RedStoneWireBlock
                || block instanceof FenceBlock
                || block instanceof WallBlock
                || block instanceof FenceGateBlock
                || block instanceof FallingBlock
                || block instanceof BaseRailBlock
                || block instanceof SignBlock
                || block instanceof BellBlock
                || block instanceof CarpetBlock
                || block instanceof ConduitBlock
                || block instanceof CoralFanBlock
                || block instanceof CoralWallFanBlock
                || block instanceof BaseCoralFanBlock
                || block instanceof BaseCoralWallFanBlock
                || block instanceof TripWireHookBlock
                || block instanceof PointedDripstoneBlock
                || block instanceof TripWireBlock
                || block instanceof SnowLayerBlock
                || block instanceof PressurePlateBlock
                || block instanceof FaceAttachedHorizontalDirectionalBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof AmethystClusterBlock
                || block instanceof BuddingAmethystBlock
                || block instanceof ChorusFlowerBlock
                || block instanceof ChorusPlantBlock
                || block instanceof LanternBlock
                || block instanceof CandleBlock
                || block instanceof TntBlock
                || block instanceof CakeBlock
                || block instanceof WebBlock
                || block instanceof SugarCaneBlock
                || block instanceof SporeBlossomBlock
                || block instanceof KelpBlock
                || block instanceof GlowLichenBlock
                || block instanceof CactusBlock
                || block instanceof BambooStalkBlock
                || block instanceof FlowerPotBlock
                || block instanceof LadderBlock
                || skippableBlox.get().contains(block);
    }
}