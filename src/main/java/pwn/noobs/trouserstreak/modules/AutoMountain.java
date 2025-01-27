package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
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
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
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
    public static boolean autocasttimenow = false;
    public static BlockPos lowestblock = new BlockPos(666, 666, 666);
    public static BlockPos highestblock = new BlockPos(666, 666, 666);
    public static int groundY;
    public static int groundY2;
    public static boolean isthisfirstblock;
    public static Direction wasfacingBOT;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> autolavamountain = sgGeneral.add(new BoolSetting.Builder()
            .name("MountainMakerBot")
            .description("Starts casting on your stairs with AutoLavaCaster when you reach your build limit. Not intended for use in a closed in space.")
            .defaultValue(false)
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
    private final Setting<Integer> munscher = sgTimings.add(new IntSetting.Builder()
            .name("DiagonalSwitchDelay")
            .description("Delays switching direction by this many ticks when building diagonally.")
            .min(1)
            .sliderRange(1, 10)
            .defaultValue(1)
            .visible(() -> !autolavamountain.get())
            .build());
    private final Setting<Integer> botlimit = sgBuild.add(new IntSetting.Builder()
            .name("Mountain Height")
            .description("Builds stairs up to this many blocks from your starting Y level")
            .min(3)
            .sliderRange(3, 380)
            .defaultValue(30)
            .visible(autolavamountain::get)
            .build());
    private final Setting<Integer> downlimit = sgBuild.add(new IntSetting.Builder()
            .name("DownwardBuildLimit")
            .description("sets the Y level at which the stairs stop going down")
            .sliderRange(-64, 318)
            .defaultValue(-64)
            .visible(() -> !autolavamountain.get())
            .build());
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
    public final Setting<Boolean> swap = sgGeneral.add(new BoolSetting.Builder()
            .name("SwapStackonRunOut")
            .description("Swaps to another stack of blocks in your hotbar when you run out")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> disabledisconnect = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable On Disconnect")
            .description("Toggles the Module off when you disconnect.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> lowYrst = sgGeneral.add(new BoolSetting.Builder()
            .name("ResetLowestBlockOnACTIVATE")
            .description("UNCHECK for proper timings for AutoLavaCaster's UseLastLowestBlockfromAutoMountain timing mode if NOT clicking to pause. LOWEST BLOCK ONLY RESET IF AutoLavaCaster is used or button here is pressed.")
            .defaultValue(true)
            .build()
    );
    private final SettingGroup sgBuild = settings.createGroup("Build Options");
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
    private final SettingGroup sgTimings = settings.createGroup("Timings");
    public final Setting<Double> StairTimer = sgTimings.add(new DoubleSetting.Builder()
            .name("TimerMultiplier")
            .description("The multiplier value for Timer.")
            .defaultValue(1)
            .sliderRange(0.1, 10)
            .build()
    );
    public final Setting<Boolean> delayakick = sgTimings.add(new BoolSetting.Builder()
            .name("PauseBasedAntiKick")
            .description("Helps if you're flying, or sending too many packets.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> delay = sgTimings.add(new IntSetting.Builder()
            .name("PauseForThisAmountOfTicks")
            .description("The amount of delay in ticks, when pausing. Useful if you're flying, or sending too many packets.")
            .min(1)
            .defaultValue(5)
            .sliderRange(0, 100)
            .visible(delayakick::get)
            .build()
    );
    private int delayLeft = delay.get();
    private final Setting<Integer> offTime = sgTimings.add(new IntSetting.Builder()
            .name("TicksBetweenPause")
            .description("The amount of delay, in ticks, between pauses.")
            .min(1)
            .defaultValue(20)
            .sliderRange(1, 200)
            .visible(delayakick::get)
            .build()
    );
    private int offLeft = offTime.get();
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
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<List<Block>> skippableBlox = sgGeneral.add(new BlockListSetting.Builder()
            .name("Blocks to not use")
            .description("Do not use these blocks for mountains.")
            .build()
    );
    private final Setting<Integer> spd = sgTimings.add(new IntSetting.Builder()
            .name("PlacementTickDelay")
            .description("Delay block placement to slow down the builder and to help SwapStackOnRunOut option.")
            .min(1)
            .sliderRange(1, 10)
            .defaultValue(1)
            .build());
    private final Setting<Integer> limit = sgBuild.add(new IntSetting.Builder()
            .name("UpwardBuildLimit")
            .description("sets the Y level at which the stairs stop going up")
            .sliderRange(-64, 318)
            .defaultValue(318)
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
    private boolean pause = false;
    private boolean resetTimer;
    private float timeSinceLastTick;
    private BlockPos playerPos;
    private BlockPos renderplayerPos;
    private int cookie = 0;
    private int speed = 0;
    private boolean go = true;
    private float cookieyaw;
    private boolean search = true;
    private boolean search2 = true;
    private int lowblockY = -1;
    private int highblockY = -1;
    private Direction wasfacing;
    private int prevPitch;
    public AutoMountain() {
        super(Trouser.Main, "AutoMountain", "Make Mountains!");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton rstlowblock = table.add(theme.button("Reset Lowest Block")).expandX().minWidth(100).widget();
        rstlowblock.action = () -> {
            lowestblock = new BlockPos(666, 666, 666);
            isthisfirstblock = true;
        };
        table.row();
        return table;
    }

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
        if (lowYrst.get() || autolavamountain.get()) isthisfirstblock = true;
        groundY = 0;
        groundY2 = 0;
        lowblockY = -1;
        highblockY = -1;
        if (startPaused.get()) {
            pause = false;
            if (autolavamountain.get())
                ChatUtils.sendMsg(Text.of("Press UseKey (RightClick) to Build a Mountain! Please wait while the bot works."));
            else ChatUtils.sendMsg(Text.of("Press UseKey (RightClick) to Build Stairs!"));
        } else if (!startPaused.get()) {
            mc.player.setPos(mc.player.getX(), Math.ceil(mc.player.getY()), mc.player.getZ());
            wasfacing = mc.player.getHorizontalFacing();
            prevPitch = Math.round(mc.player.getPitch());
            if (swap.get()) {
                cascadingpileof();
            }
            if (autolavamountain.get()) {
                wasfacingBOT = mc.player.getHorizontalFacing();
                lavamountainingredients();
            }
            mc.player.setVelocity(0, 0, 0);
            PlayerUtils.centerPlayer();
            pause = true;
            if (autolavamountain.get())
                ChatUtils.sendMsg(Text.of("Building a Mountain! Please wait while the bot works."));
        }
        resetTimer = false;
        playerPos = mc.player.getBlockPos();
        renderplayerPos = mc.player.getBlockPos();
        if (startPaused.get() || isInvalidBlock(mc.player.getInventory().getMainHandStack().getItem().getDefaultStack()))
            return;
        BlockPos pos = playerPos.add(new Vec3i(0, -1, 0));
        if (mc.world.getBlockState(pos).isReplaceable()) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null) return;
        mc.player.setNoGravity(false);
        if (isthisfirstblock) {
            highestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
            lowestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
            isthisfirstblock = false;
        }
        if (pause = true) {
            if (!isthisfirstblock && mc.player.getY() < lowestblock.getY())
                lowestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
            if (!isthisfirstblock && mc.player.getY() > highestblock.getY() + 1)
                highestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
        }
        search = true;
        seekground();
        search2 = true;
        seekground2();
        speed = 0;
        resetTimer = true;
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        if (isInvalidBlock(mc.player.getInventory().getMainHandStack().getItem().getDefaultStack())) return;
        if (!pause) {
            BlockPos pos = playerPos.add(new Vec3i(0, -1, 0));
            if (mc.world.getBlockState(pos).isReplaceable()) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && mc.player != null) {
            if (mc.options.jumpKey.isPressed() && !autolavamountain.get()) {
                if ((mouseT.get() && mc.player.getPitch() <= 40) || (!mouseT.get() && prevPitch <= 40)) {            //UP
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.NORTH) || (!mouseT.get() && wasfacing == Direction.NORTH)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(0, +spcoffset.get(), -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.SOUTH) || (!mouseT.get() && wasfacing == Direction.SOUTH)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(0, +spcoffset.get(), 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.EAST) || (!mouseT.get() && wasfacing == Direction.EAST)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(1, +spcoffset.get(), 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.WEST) || (!mouseT.get() && wasfacing == Direction.WEST)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(-1, +spcoffset.get(), 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                } else if ((mouseT.get() && mc.player.getPitch() > 40) || (!mouseT.get() && prevPitch > 40)) {            //DOWN
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.NORTH) || (!mouseT.get() && wasfacing == Direction.NORTH)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(0, -spcoffset.get() - 2, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.SOUTH) || (!mouseT.get() && wasfacing == Direction.SOUTH)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(0, -spcoffset.get() - 2, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.EAST) || (!mouseT.get() && wasfacing == Direction.EAST)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(1, -spcoffset.get() - 2, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.WEST) || (!mouseT.get() && wasfacing == Direction.WEST)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(-1, -spcoffset.get() - 2, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                }
            } else if (!mc.options.jumpKey.isPressed() || autolavamountain.get()) {
                if (((mouseT.get() && mc.player.getPitch() <= 40) || autolavamountain.get()) || (!mouseT.get() && prevPitch <= 40 && !autolavamountain.get())) {            //UP
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.NORTH) || (!mouseT.get() && wasfacing == Direction.NORTH)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(0, 0, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        if (autolavamountain.get() && !pause) {
                            BlockPos pos2 = renderplayerPos.add(new Vec3i(0, botlimit.get() - 1, -botlimit.get()));
                            BlockPos pos3 = renderplayerPos.add(new Vec3i(0, 1, -2));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        } else if (autolavamountain.get() && pause) {
                            BlockPos pos2 = lowestblock.add(new Vec3i(0, botlimit.get(), -botlimit.get()));
                            BlockPos pos3 = renderplayerPos.add(new Vec3i(0, 1, -2));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.SOUTH) || (!mouseT.get() && wasfacing == Direction.SOUTH)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(0, 0, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        if (autolavamountain.get() && !pause) {
                            BlockPos pos2 = renderplayerPos.add(new Vec3i(0, botlimit.get() - 1, botlimit.get()));
                            BlockPos pos3 = renderplayerPos.add(new Vec3i(0, 1, 2));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        } else if (autolavamountain.get() && pause) {
                            BlockPos pos2 = lowestblock.add(new Vec3i(0, botlimit.get(), botlimit.get()));
                            BlockPos pos3 = renderplayerPos.add(new Vec3i(0, 1, 2));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.EAST) || (!mouseT.get() && wasfacing == Direction.EAST)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(1, 0, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        if (autolavamountain.get() && !pause) {
                            BlockPos pos3 = renderplayerPos.add(new Vec3i(2, 1, 0));
                            BlockPos pos2 = renderplayerPos.add(new Vec3i(botlimit.get(), botlimit.get() - 1, 0));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        } else if (autolavamountain.get() && pause) {
                            BlockPos pos3 = renderplayerPos.add(new Vec3i(2, 1, 0));
                            BlockPos pos2 = lowestblock.add(new Vec3i(botlimit.get(), botlimit.get(), 0));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.WEST) || (!mouseT.get() && wasfacing == Direction.WEST)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(-1, 0, -0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        if (autolavamountain.get() && !pause) {
                            BlockPos pos3 = renderplayerPos.add(new Vec3i(-2, 1, 0));
                            BlockPos pos2 = renderplayerPos.add(new Vec3i(-botlimit.get(), botlimit.get() - 1, 0));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        } else if (autolavamountain.get() && pause) {
                            BlockPos pos3 = renderplayerPos.add(new Vec3i(-2, 1, 0));
                            BlockPos pos2 = lowestblock.add(new Vec3i(-botlimit.get(), botlimit.get(), 0));
                            event.renderer.box(pos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            event.renderer.box(pos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                } else if ((mouseT.get() && mc.player.getPitch() > 40) || (!mouseT.get() && prevPitch > 40)) {            //DOWN
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.NORTH) || (!mouseT.get() && wasfacing == Direction.NORTH)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(0, -2, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.SOUTH) || (!mouseT.get() && wasfacing == Direction.SOUTH)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(0, -2, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.EAST) || (!mouseT.get() && wasfacing == Direction.EAST)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(1, -2, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    if ((mouseT.get() && mc.player.getMovementDirection() == Direction.WEST) || (!mouseT.get() && wasfacing == Direction.WEST)) {
                        BlockPos pos1 = renderplayerPos.add(new Vec3i(-1, -2, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.useKey.isPressed()) {
            if (pause) {
                BlockPos pos = playerPos.add(new Vec3i(0, -1, 0));
                if (mc.world.getBlockState(pos).isReplaceable()) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
            if (!pause) mc.player.setPos(mc.player.getX(), Math.ceil(mc.player.getY()), mc.player.getZ());
            pause = !pause;
            mc.player.setVelocity(0, 0, 0);
            cookie = 0;
            speed = 0;
            resetTimer = true;
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
            if (isInvalidBlock(mc.player.getInventory().getMainHandStack().getItem().getDefaultStack())) return;
            if (isthisfirstblock) {
                highestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
                lowestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
                isthisfirstblock = false;
            }
            if (!isthisfirstblock && mc.player.getY() < lowestblock.getY()) {
                lowestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
                seekground();
            }
            if (!isthisfirstblock && mc.player.getY() > highestblock.getY() + 1) {
                highestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
                seekground2();
            }
        }
    }

    @EventHandler
    private void onKeyEvent(KeyEvent event) {
        if (!pause) return;
        if (!autolavamountain.get()) {
            if (mc.options.forwardKey.isPressed()) {
                if (mouseT.get()) mc.player.setPitch(35);
                if (!mouseT.get()) prevPitch = 35;
            }
            if (mc.options.backKey.isPressed()) {
                if (mouseT.get()) mc.player.setPitch(75);
                if (!mouseT.get()) prevPitch = 75;
            }
            if ((lagpause.get() && timeSinceLastTick >= lag.get()) || isInvalidBlock(mc.player.getInventory().getMainHandStack().getItem().getDefaultStack()) || !pause)
                return;
            if (mc.options.leftKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                if (mouseT.get()) mc.player.setYaw(mc.player.getYaw() - 90);
                if (!mouseT.get()) {
                    if (wasfacing == Direction.NORTH) {
                        wasfacing = Direction.WEST;
                        return;
                    }
                    if (wasfacing == Direction.SOUTH) {
                        wasfacing = Direction.EAST;
                        return;
                    }
                    if (wasfacing == Direction.WEST) {
                        wasfacing = Direction.SOUTH;
                        return;
                    }
                    if (wasfacing == Direction.EAST) {
                        wasfacing = Direction.NORTH;
                        return;
                    }
                }
            }
            if (mc.options.rightKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                if (mouseT.get()) mc.player.setYaw(mc.player.getYaw() + 90);
                if (!mouseT.get()) {
                    if (wasfacing == Direction.NORTH) {
                        wasfacing = Direction.EAST;
                        return;
                    }
                    if (wasfacing == Direction.SOUTH) {
                        wasfacing = Direction.WEST;
                        return;
                    }
                    if (wasfacing == Direction.WEST) {
                        wasfacing = Direction.NORTH;
                        return;
                    }
                    if (wasfacing == Direction.EAST) {
                        wasfacing = Direction.SOUTH;
                    }
                }
            }
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket)
            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        playerPos = mc.player.getBlockPos();
        if (mc.player.getY() % 1 != 0 && !pause) {
            renderplayerPos = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY() + 1, mc.player.getBlockZ());
        } else renderplayerPos = mc.player.getBlockPos();
        timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();

        if (speed < spd.get()) {
            go = false;
            speed++;
        }
        if (speed >= spd.get()) {
            go = true;
            speed = 0;
        }
        if (!pause) {
            wasfacing = mc.player.getHorizontalFacing();
            prevPitch = Math.round(mc.player.getPitch());
            if (autolavamountain.get()) {
                wasfacingBOT = mc.player.getHorizontalFacing();
                isthisfirstblock = true;
                lavamountainingredients();
            }
            mc.player.setNoGravity(false);
            search = true;
            search2 = true;
        }
        if (!pause) return;
        if (swap.get()) {
            cascadingpileof();
        }
        if (autolavamountain.get()) {
            if (wasfacingBOT == Direction.NORTH) mc.player.setYaw(180);
            if (wasfacingBOT == Direction.SOUTH) mc.player.setYaw(0);
            if (wasfacingBOT == Direction.WEST) mc.player.setYaw(90);
            if (wasfacingBOT == Direction.EAST) mc.player.setYaw(-90);
        }
        if (!delayakick.get()) {
            offLeft = 666666666;
            delayLeft = 0;
        } else if (delayakick.get() && offLeft > offTime.get()) {
            offLeft = offTime.get();
        }
        mc.player.setVelocity(0, 0, 0);
        PlayerUtils.centerPlayer();
        mc.player.setPos(mc.player.getX(), Math.round(mc.player.getY()) + 0.25, mc.player.getZ());
        if (Modules.get().get(Flight.class).isActive()) {
            Modules.get().get(Flight.class).toggle();
        }
        if (Modules.get().get(FlightAntikick.class).isActive()) {
            Modules.get().get(FlightAntikick.class).toggle();
        }
        if (Modules.get().get(TPFly.class).isActive()) {
            Modules.get().get(TPFly.class).toggle();
        }
        if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.AIR) {
            resetTimer = false;
            Modules.get().get(Timer.class).setOverride(StairTimer.get());
        } else if (!resetTimer) {
            resetTimer = true;
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
        if ((lagpause.get() && timeSinceLastTick >= lag.get()) || isInvalidBlock(mc.player.getInventory().getMainHandStack().getItem().getDefaultStack()) || !pause || !go)
            return;
        if (mc.options.sneakKey.isPressed() && mc.options.rightKey.isPressed() && delayLeft <= 0 && offLeft > 0 && !autolavamountain.get()) {
            cookie++;
            if (cookie == munscher.get()) {
                cookieyaw = mc.player.getYaw();
                if (mouseT.get()) mc.player.setYaw(mc.player.getYaw() + 90);
                if (!mouseT.get()) {
                    if (wasfacing == Direction.NORTH) {
                        wasfacing = Direction.EAST;
                    } else if (wasfacing == Direction.SOUTH) {
                        wasfacing = Direction.WEST;
                    } else if (wasfacing == Direction.WEST) {
                        wasfacing = Direction.NORTH;
                    } else if (wasfacing == Direction.EAST) {
                        wasfacing = Direction.SOUTH;
                    }
                }
            } else if (cookie >= munscher.get() + munscher.get()) {
                if (mouseT.get()) mc.player.setYaw(mc.player.getYaw() - 90);
                if (!mouseT.get()) {
                    if (wasfacing == Direction.NORTH) {
                        wasfacing = Direction.WEST;
                    } else if (wasfacing == Direction.SOUTH) {
                        wasfacing = Direction.EAST;
                    } else if (wasfacing == Direction.WEST) {
                        wasfacing = Direction.SOUTH;
                    } else if (wasfacing == Direction.EAST) {
                        wasfacing = Direction.NORTH;
                    }
                }
                cookie = 0;
            }
        }
        if (mc.options.sneakKey.isPressed() && mc.options.leftKey.isPressed() && delayLeft <= 0 && offLeft > 0 && !autolavamountain.get()) {
            cookie++;
            if (cookie == munscher.get()) {
                cookieyaw = mc.player.getYaw();
                if (mouseT.get()) mc.player.setYaw(mc.player.getYaw() - 90);
                if (!mouseT.get()) {
                    if (wasfacing == Direction.NORTH) {
                        wasfacing = Direction.WEST;
                    } else if (wasfacing == Direction.SOUTH) {
                        wasfacing = Direction.EAST;
                    } else if (wasfacing == Direction.WEST) {
                        wasfacing = Direction.SOUTH;
                    } else if (wasfacing == Direction.EAST) {
                        wasfacing = Direction.NORTH;
                    }
                }
            } else if (cookie >= munscher.get() + munscher.get()) {
                if (mouseT.get()) mc.player.setYaw(mc.player.getYaw() + 90);
                if (!mouseT.get()) {
                    if (wasfacing == Direction.NORTH) {
                        wasfacing = Direction.EAST;
                    } else if (wasfacing == Direction.SOUTH) {
                        wasfacing = Direction.WEST;
                    } else if (wasfacing == Direction.WEST) {
                        wasfacing = Direction.NORTH;
                    } else if (wasfacing == Direction.EAST) {
                        wasfacing = Direction.SOUTH;
                    }
                }
                cookie = 0;
            }
        } else if (!mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed() && cookie >= 1) {
            mc.player.setYaw(cookieyaw);
            cookieyaw = mc.player.getYaw();
            cookie = 0;
        }
        if (pause) {
            if (isthisfirstblock) {
                highestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
                lowestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
                isthisfirstblock = false;
            }
            if (!isthisfirstblock && mc.player.getY() < lowestblock.getY()) {
                lowestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
                seekground();
            }
            if (!isthisfirstblock && mc.player.getY() > highestblock.getY() + 1) {
                highestblock = mc.player.getBlockPos().add(new Vec3i(0, -1, 0));
                seekground2();
            }
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (pause && autolavamountain.get()) {
            if (wasfacingBOT == Direction.NORTH) mc.player.setYaw(180);
            if (wasfacingBOT == Direction.SOUTH) mc.player.setYaw(0);
            if (wasfacingBOT == Direction.WEST) mc.player.setYaw(90);
            if (wasfacingBOT == Direction.EAST) mc.player.setYaw(-90);
            mc.player.setNoGravity(true);
            if (mc.player.getY() >= limit.get() - 4 | mc.player.getY() > lowestblock.getY() + botlimit.get()) {
                autocasttimenow = true;
                search = true;
                seekground();
                search2 = true;
                seekground2();
                mc.player.setPos(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ());
                mc.player.setPitch(80);
                if (!Modules.get().get(AutoLavaCaster.class).isActive())
                    Modules.get().get(AutoLavaCaster.class).toggle();
                ChatUtils.sendMsg(Text.of("Activating AutoLavaCaster."));
                toggle();
            }
        }
        if (!pause) return;

        if (((mouseT.get() && mc.player.getPitch() <= 40 || autolavamountain.get())) || (!mouseT.get() && prevPitch <= 40)) {
            if (delayLeft > 0) delayLeft--;
            else if ((!lagpause.get() || timeSinceLastTick < lag.get()) && delayLeft <= 0 && offLeft > 0 && (mc.player.getY() <= limit.get() && mc.player.getY() >= downlimit.get() && !autolavamountain.get() || mc.player.getY() <= limit.get() - 4 && mc.player.getY() <= lowestblock.getY() + botlimit.get() + 1 && autolavamountain.get())) {
                offLeft--;
                if (mc.player == null || mc.world == null) {
                    toggle();
                    return;
                }
                if ((lagpause.get() && timeSinceLastTick >= lag.get()) || isInvalidBlock(mc.player.getInventory().getMainHandStack().getItem().getDefaultStack()) || !pause || !go)
                    return;
                if ((mouseT.get() && mc.player.getMovementDirection() == Direction.NORTH) || (!mouseT.get() && wasfacing == Direction.NORTH)) {            //UP
                    if (mc.options.jumpKey.isPressed() && !autolavamountain.get()) {
                        BlockPos un1 = playerPos.add(new Vec3i(0, spcoffset.get() + 2, 0));
                        BlockPos un2 = playerPos.add(new Vec3i(0, spcoffset.get() + 1, -1));
                        BlockPos un3 = playerPos.add(new Vec3i(0, spcoffset.get() + 2, -1));
                        BlockPos un4 = playerPos.add(new Vec3i(0, spcoffset.get() + 3, -1));
                        BlockPos pos = playerPos.add(new Vec3i(0, spcoffset.get(), -1));
                        if (mc.world.getBlockState(un1).isReplaceable() && mc.world.getBlockState(un2).isReplaceable() && mc.world.getBlockState(un3).isReplaceable() && mc.world.getBlockState(un4).isReplaceable() && mc.world.getFluidState(un1).isEmpty() && mc.world.getFluidState(un2).isEmpty() && mc.world.getFluidState(un3).isEmpty() && mc.world.getFluidState(un4).isEmpty() && !mc.world.getBlockState(un1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(un2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(un3).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(un4).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(un2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1 + spcoffset.get(), mc.player.getZ() - 1);
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()) {
                                if (mouseT.get()) mc.player.setPitch(75);
                                if (!mouseT.get()) prevPitch = 75;
                            }
                        }
                    } else {
                        BlockPos un1 = playerPos.add(new Vec3i(0, 2, 0));
                        BlockPos un2 = playerPos.add(new Vec3i(0, 1, -1));
                        BlockPos un3 = playerPos.add(new Vec3i(0, 2, -1));
                        BlockPos un4 = playerPos.add(new Vec3i(0, 3, -1));
                        BlockPos pos = playerPos.add(new Vec3i(0, 0, -1));
                        if (mc.world.getBlockState(un1).isReplaceable() && mc.world.getBlockState(un2).isReplaceable() && mc.world.getBlockState(un3).isReplaceable() && mc.world.getBlockState(un4).isReplaceable() && mc.world.getFluidState(un1).isEmpty() && mc.world.getFluidState(un2).isEmpty() && mc.world.getFluidState(un3).isEmpty() && mc.world.getFluidState(un4).isEmpty() && !mc.world.getBlockState(un1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(un2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(un3).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(un4).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(un2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ() - 1);
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()) {
                                if (mouseT.get()) mc.player.setPitch(75);
                                if (!mouseT.get()) prevPitch = 75;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMovementDirection() == Direction.EAST) || (!mouseT.get() && wasfacing == Direction.EAST)) {            //UP
                    if (mc.options.jumpKey.isPressed() && !autolavamountain.get()) {
                        BlockPos ue1 = playerPos.add(new Vec3i(0, spcoffset.get() + 2, 0));
                        BlockPos ue2 = playerPos.add(new Vec3i(+1, spcoffset.get() + 1, 0));
                        BlockPos ue3 = playerPos.add(new Vec3i(+1, spcoffset.get() + 2, 0));
                        BlockPos ue4 = playerPos.add(new Vec3i(+1, spcoffset.get() + 3, 0));
                        BlockPos pos = playerPos.add(new Vec3i(1, spcoffset.get(), 0));
                        if (mc.world.getBlockState(ue1).isReplaceable() && mc.world.getBlockState(ue2).isReplaceable() && mc.world.getBlockState(ue3).isReplaceable() && mc.world.getBlockState(ue4).isReplaceable() && mc.world.getFluidState(ue1).isEmpty() && mc.world.getFluidState(ue2).isEmpty() && mc.world.getFluidState(ue3).isEmpty() && mc.world.getFluidState(ue4).isEmpty() && !mc.world.getBlockState(ue1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ue2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ue3).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ue4).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(ue2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX() + 1, mc.player.getY() + 1 + spcoffset.get(), mc.player.getZ());
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()) {
                                if (mouseT.get()) mc.player.setPitch(75);
                                if (!mouseT.get()) prevPitch = 75;
                            }
                        }
                    } else {
                        BlockPos ue1 = playerPos.add(new Vec3i(0, 2, 0));
                        BlockPos ue2 = playerPos.add(new Vec3i(+1, 1, 0));
                        BlockPos ue3 = playerPos.add(new Vec3i(+1, 2, 0));
                        BlockPos ue4 = playerPos.add(new Vec3i(+1, 3, 0));
                        BlockPos pos = playerPos.add(new Vec3i(1, 0, 0));
                        if (mc.world.getBlockState(ue1).isReplaceable() && mc.world.getBlockState(ue2).isReplaceable() && mc.world.getBlockState(ue3).isReplaceable() && mc.world.getBlockState(ue4).isReplaceable() && mc.world.getFluidState(ue1).isEmpty() && mc.world.getFluidState(ue2).isEmpty() && mc.world.getFluidState(ue3).isEmpty() && mc.world.getFluidState(ue4).isEmpty() && !mc.world.getBlockState(ue1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ue2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ue3).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ue4).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(ue2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX() + 1, mc.player.getY() + 1, mc.player.getZ());
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()) {
                                if (mouseT.get()) mc.player.setPitch(75);
                                if (!mouseT.get()) prevPitch = 75;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMovementDirection() == Direction.SOUTH) || (!mouseT.get() && wasfacing == Direction.SOUTH)) {            //UP
                    if (mc.options.jumpKey.isPressed() && !autolavamountain.get()) {
                        BlockPos us1 = playerPos.add(new Vec3i(0, spcoffset.get() + 2, 0));
                        BlockPos us2 = playerPos.add(new Vec3i(0, spcoffset.get() + 1, +1));
                        BlockPos us3 = playerPos.add(new Vec3i(0, spcoffset.get() + 2, +1));
                        BlockPos us4 = playerPos.add(new Vec3i(0, spcoffset.get() + 3, +1));
                        BlockPos pos = playerPos.add(new Vec3i(0, spcoffset.get(), 1));
                        if (mc.world.getBlockState(us1).isReplaceable() && mc.world.getBlockState(us2).isReplaceable() && mc.world.getBlockState(us3).isReplaceable() && mc.world.getBlockState(us4).isReplaceable() && mc.world.getFluidState(us1).isEmpty() && mc.world.getFluidState(us2).isEmpty() && mc.world.getFluidState(us3).isEmpty() && mc.world.getFluidState(us4).isEmpty() && !mc.world.getBlockState(us1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(us2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(us3).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(us4).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(us2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1 + spcoffset.get(), mc.player.getZ() + 1);
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()) {
                                if (mouseT.get()) mc.player.setPitch(75);
                                if (!mouseT.get()) prevPitch = 75;
                            }
                        }
                    } else {
                        BlockPos us1 = playerPos.add(new Vec3i(0, 2, 0));
                        BlockPos us2 = playerPos.add(new Vec3i(0, 1, +1));
                        BlockPos us3 = playerPos.add(new Vec3i(0, 2, +1));
                        BlockPos us4 = playerPos.add(new Vec3i(0, 3, +1));
                        BlockPos pos = playerPos.add(new Vec3i(0, 0, 1));
                        if (mc.world.getBlockState(us1).isReplaceable() && mc.world.getBlockState(us2).isReplaceable() && mc.world.getBlockState(us3).isReplaceable() && mc.world.getBlockState(us4).isReplaceable() && mc.world.getFluidState(us1).isEmpty() && mc.world.getFluidState(us2).isEmpty() && mc.world.getFluidState(us3).isEmpty() && mc.world.getFluidState(us4).isEmpty() && !mc.world.getBlockState(us1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(us2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(us3).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(us4).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(us2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ() + 1);
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()) {
                                if (mouseT.get()) mc.player.setPitch(75);
                                if (!mouseT.get()) prevPitch = 75;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMovementDirection() == Direction.WEST) || (!mouseT.get() && wasfacing == Direction.WEST)) {            //UP
                    if (mc.options.jumpKey.isPressed() && !autolavamountain.get()) {
                        BlockPos uw1 = playerPos.add(new Vec3i(0, spcoffset.get() + 2, 0));
                        BlockPos uw2 = playerPos.add(new Vec3i(-1, spcoffset.get() + 1, 0));
                        BlockPos uw3 = playerPos.add(new Vec3i(-1, spcoffset.get() + 2, 0));
                        BlockPos uw4 = playerPos.add(new Vec3i(-1, spcoffset.get() + 3, 0));
                        BlockPos pos = playerPos.add(new Vec3i(-1, spcoffset.get(), 0));
                        if (mc.world.getBlockState(uw1).isReplaceable() && mc.world.getBlockState(uw2).isReplaceable() && mc.world.getBlockState(uw3).isReplaceable() && mc.world.getBlockState(uw4).isReplaceable() && mc.world.getFluidState(uw1).isEmpty() && mc.world.getFluidState(uw2).isEmpty() && mc.world.getFluidState(uw3).isEmpty() && mc.world.getFluidState(uw4).isEmpty() && !mc.world.getBlockState(uw1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(uw2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(uw3).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(uw4).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(uw2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX() - 1, mc.player.getY() + 1 + spcoffset.get(), mc.player.getZ());
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()) {
                                if (mouseT.get()) mc.player.setPitch(75);
                                if (!mouseT.get()) prevPitch = 75;
                            }
                        }
                    } else {
                        BlockPos uw1 = playerPos.add(new Vec3i(0, 2, 0));
                        BlockPos uw2 = playerPos.add(new Vec3i(-1, 1, 0));
                        BlockPos uw3 = playerPos.add(new Vec3i(-1, 2, 0));
                        BlockPos uw4 = playerPos.add(new Vec3i(-1, 3, 0));
                        BlockPos pos = playerPos.add(new Vec3i(-1, 0, 0));
                        if (mc.world.getBlockState(uw1).isReplaceable() && mc.world.getBlockState(uw2).isReplaceable() && mc.world.getBlockState(uw3).isReplaceable() && mc.world.getBlockState(uw4).isReplaceable() && mc.world.getFluidState(uw1).isEmpty() && mc.world.getFluidState(uw2).isEmpty() && mc.world.getFluidState(uw3).isEmpty() && mc.world.getFluidState(uw4).isEmpty() && !mc.world.getBlockState(uw1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(uw2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(uw3).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(uw4).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(uw2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX() - 1, mc.player.getY() + 1, mc.player.getZ());
                        } else {
                            if (InvertUpDir.get() && !autolavamountain.get()) {
                                if (mouseT.get()) mc.player.setPitch(75);
                                if (!mouseT.get()) prevPitch = 75;
                            }
                        }
                    }
                }
                if (mc.player.getY() >= limit.get() - 1 && InvertUpDir.get() && !autolavamountain.get()) {
                    if (mouseT.get()) mc.player.setPitch(75);
                    if (!mouseT.get()) prevPitch = 75;
                }
            } else if (mc.player.getY() <= downlimit.get() && !InvertDownDir.get() || mc.player.getY() >= limit.get() && !InvertUpDir.get() && !autolavamountain.get() || mc.player.getY() >= lowestblock.getY() + botlimit.get() + 1 && autolavamountain.get() || delayLeft <= 0 && offLeft <= 0) {
                delayLeft = delay.get();
                offLeft = offTime.get();
            }
        } else if ((mouseT.get() && mc.player.getPitch() > 40 && !autolavamountain.get()) || (!mouseT.get() && prevPitch > 40)) {
            if (delayLeft > 0) delayLeft--;
            else if ((!lagpause.get() || timeSinceLastTick < lag.get()) && delayLeft <= 0 && offLeft > 0 && mc.player.getY() <= limit.get() && mc.player.getY() >= downlimit.get()) {
                offLeft--;
                if (mc.player == null || mc.world == null) {
                    toggle();
                    return;
                }
                if ((lagpause.get() && timeSinceLastTick >= lag.get()) || isInvalidBlock(mc.player.getInventory().getMainHandStack().getItem().getDefaultStack()) || !pause || !go)
                    return;
                if ((mouseT.get() && mc.player.getMovementDirection() == Direction.NORTH) || (!mouseT.get() && wasfacing == Direction.NORTH)) {            //DOWN
                    if (mc.options.jumpKey.isPressed()) {
                        BlockPos dn1 = playerPos.add(new Vec3i(0, -spcoffset.get() - 1, -1));
                        BlockPos dn2 = playerPos.add(new Vec3i(0, -spcoffset.get(), -1));
                        BlockPos dn3 = playerPos.add(new Vec3i(0, -spcoffset.get() + 1, -1));
                        BlockPos pos = playerPos.add(new Vec3i(0, -spcoffset.get() - 2, -1));
                        if (mc.world.getBlockState(dn1).isReplaceable() && mc.world.getBlockState(dn2).isReplaceable() && mc.world.getBlockState(dn3).isReplaceable() && mc.world.getFluidState(dn1).isEmpty() && mc.world.getFluidState(dn2).isEmpty() && mc.world.getFluidState(dn3).isEmpty() && !mc.world.getBlockState(dn1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(dn2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(dn3).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(dn2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX(), mc.player.getY() - 1 - spcoffset.get(), mc.player.getZ() - 1);
                        } else {
                            if (InvertDownDir.get()) mc.player.setPitch(35);
                        }
                    } else {
                        BlockPos dn1 = playerPos.add(new Vec3i(0, -1, -1));
                        BlockPos dn2 = playerPos.add(new Vec3i(0, 0, -1));
                        BlockPos dn3 = playerPos.add(new Vec3i(0, 1, -1));
                        BlockPos pos = playerPos.add(new Vec3i(0, -2, -1));
                        if (mc.world.getBlockState(dn1).isReplaceable() && mc.world.getBlockState(dn2).isReplaceable() && mc.world.getBlockState(dn3).isReplaceable() && mc.world.getFluidState(dn1).isEmpty() && mc.world.getFluidState(dn2).isEmpty() && mc.world.getFluidState(dn3).isEmpty() && !mc.world.getBlockState(dn1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(dn2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(dn3).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(dn2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ() - 1);
                        } else {
                            if (InvertDownDir.get()) {
                                if (mouseT.get()) mc.player.setPitch(35);
                                if (!mouseT.get()) prevPitch = 35;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMovementDirection() == Direction.EAST) || (!mouseT.get() && wasfacing == Direction.EAST)) {            //DOWN
                    if (mc.options.jumpKey.isPressed()) {
                        BlockPos de1 = playerPos.add(new Vec3i(1, -spcoffset.get() - 1, 0));
                        BlockPos de2 = playerPos.add(new Vec3i(1, -spcoffset.get(), 0));
                        BlockPos de3 = playerPos.add(new Vec3i(1, -spcoffset.get() + 1, 0));
                        BlockPos pos = playerPos.add(new Vec3i(1, -spcoffset.get() - 2, 0));
                        if (mc.world.getBlockState(de1).isReplaceable() && mc.world.getBlockState(de2).isReplaceable() && mc.world.getBlockState(de3).isReplaceable() && mc.world.getFluidState(de1).isEmpty() && mc.world.getFluidState(de2).isEmpty() && mc.world.getFluidState(de3).isEmpty() && !mc.world.getBlockState(de1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(de2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(de3).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(de2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX() + 1, mc.player.getY() - 1 - spcoffset.get(), mc.player.getZ());
                        } else {
                            if (InvertDownDir.get()) {
                                if (mouseT.get()) mc.player.setPitch(35);
                                if (!mouseT.get()) prevPitch = 35;
                            }
                        }
                    } else {
                        BlockPos de1 = playerPos.add(new Vec3i(1, -1, 0));
                        BlockPos de2 = playerPos.add(new Vec3i(1, 0, 0));
                        BlockPos de3 = playerPos.add(new Vec3i(1, 1, 0));
                        BlockPos pos = playerPos.add(new Vec3i(1, -2, 0));
                        if (mc.world.getBlockState(de1).isReplaceable() && mc.world.getBlockState(de2).isReplaceable() && mc.world.getBlockState(de3).isReplaceable() && mc.world.getFluidState(de1).isEmpty() && mc.world.getFluidState(de2).isEmpty() && mc.world.getFluidState(de3).isEmpty() && !mc.world.getBlockState(de1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(de2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(de3).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(de2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX() + 1, mc.player.getY() - 1, mc.player.getZ());
                        } else {
                            if (InvertDownDir.get()) {
                                if (mouseT.get()) mc.player.setPitch(35);
                                if (!mouseT.get()) prevPitch = 35;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMovementDirection() == Direction.SOUTH) || (!mouseT.get() && wasfacing == Direction.SOUTH)) {            //DOWN
                    if (mc.options.jumpKey.isPressed()) {
                        BlockPos ds1 = playerPos.add(new Vec3i(0, -spcoffset.get() - 1, 1));
                        BlockPos ds2 = playerPos.add(new Vec3i(0, -spcoffset.get(), 1));
                        BlockPos ds3 = playerPos.add(new Vec3i(0, -spcoffset.get() + 1, 1));
                        BlockPos pos = playerPos.add(new Vec3i(0, -spcoffset.get() - 2, 1));
                        if (mc.world.getBlockState(ds1).isReplaceable() && mc.world.getBlockState(ds2).isReplaceable() && mc.world.getBlockState(ds3).isReplaceable() && mc.world.getFluidState(ds1).isEmpty() && mc.world.getFluidState(ds2).isEmpty() && mc.world.getFluidState(ds3).isEmpty() && !mc.world.getBlockState(ds1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ds2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ds3).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(ds2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX(), mc.player.getY() - 1 - spcoffset.get(), mc.player.getZ() + 1);
                        } else {
                            if (InvertDownDir.get()) {
                                if (mouseT.get()) mc.player.setPitch(35);
                                if (!mouseT.get()) prevPitch = 35;
                            }
                        }
                    } else {
                        BlockPos ds1 = playerPos.add(new Vec3i(0, -1, 1));
                        BlockPos ds2 = playerPos.add(new Vec3i(0, 0, 1));
                        BlockPos ds3 = playerPos.add(new Vec3i(0, 1, 1));
                        BlockPos pos = playerPos.add(new Vec3i(0, -2, 1));
                        if (mc.world.getBlockState(ds1).isReplaceable() && mc.world.getBlockState(ds2).isReplaceable() && mc.world.getBlockState(ds3).isReplaceable() && mc.world.getFluidState(ds1).isEmpty() && mc.world.getFluidState(ds2).isEmpty() && mc.world.getFluidState(ds3).isEmpty() && !mc.world.getBlockState(ds1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ds2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(ds3).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(ds2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ() + 1);
                        } else {
                            if (InvertDownDir.get()) {
                                if (mouseT.get()) mc.player.setPitch(35);
                                if (!mouseT.get()) prevPitch = 35;
                            }
                        }
                    }
                }
                if ((mouseT.get() && mc.player.getMovementDirection() == Direction.WEST) || (!mouseT.get() && wasfacing == Direction.WEST)) {            //DOWN
                    if (mc.options.jumpKey.isPressed()) {
                        BlockPos dw1 = playerPos.add(new Vec3i(-1, -spcoffset.get() - 1, 0));
                        BlockPos dw2 = playerPos.add(new Vec3i(-1, -spcoffset.get(), 0));
                        BlockPos dw3 = playerPos.add(new Vec3i(-1, -spcoffset.get() + 1, 0));
                        BlockPos pos = playerPos.add(new Vec3i(-1, -spcoffset.get() - 2, 0));
                        if (mc.world.getBlockState(dw1).isReplaceable() && mc.world.getBlockState(dw2).isReplaceable() && mc.world.getBlockState(dw3).isReplaceable() && mc.world.getFluidState(dw1).isEmpty() && mc.world.getFluidState(dw2).isEmpty() && mc.world.getFluidState(dw3).isEmpty() && !mc.world.getBlockState(dw1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(dw2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(dw3).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(dw2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX() - 1, mc.player.getY() - 1 - spcoffset.get(), mc.player.getZ());
                        } else {
                            if (InvertDownDir.get()) {
                                if (mouseT.get()) mc.player.setPitch(35);
                                if (!mouseT.get()) prevPitch = 35;
                            }
                        }
                    } else {
                        BlockPos dw1 = playerPos.add(new Vec3i(-1, -1, 0));
                        BlockPos dw2 = playerPos.add(new Vec3i(-1, 0, 0));
                        BlockPos dw3 = playerPos.add(new Vec3i(-1, 1, 0));
                        BlockPos pos = playerPos.add(new Vec3i(-1, -2, 0));
                        if (mc.world.getBlockState(dw1).isReplaceable() && mc.world.getBlockState(dw2).isReplaceable() && mc.world.getBlockState(dw3).isReplaceable() && mc.world.getFluidState(dw1).isEmpty() && mc.world.getFluidState(dw2).isEmpty() && mc.world.getFluidState(dw3).isEmpty() && !mc.world.getBlockState(dw1).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(dw2).isOf(Blocks.POWDER_SNOW) && !mc.world.getBlockState(dw3).isOf(Blocks.POWDER_SNOW) && mc.world.getWorldBorder().contains(dw2)) {
                            if (mc.world.getBlockState(pos).isReplaceable()) {
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            mc.player.setPosition(mc.player.getX() - 1, mc.player.getY() - 1, mc.player.getZ());
                        } else {
                            if (InvertDownDir.get()) {
                                if (mouseT.get()) mc.player.setPitch(35);
                                if (!mouseT.get()) prevPitch = 35;
                            }
                        }
                    }
                }
                if (mc.player.getY() <= downlimit.get() + 1 && InvertDownDir.get()) {
                    if (mouseT.get()) mc.player.setPitch(35);
                    if (!mouseT.get()) prevPitch = 35;
                }
            } else if (mc.player.getY() <= downlimit.get() || mc.player.getY() >= limit.get() || delayLeft <= 0 && offLeft <= 0) {
                delayLeft = delay.get();
                offLeft = offTime.get();
            }
        }
        PlayerUtils.centerPlayer();
    }

    private void seekground() {
        if (!(mc.world.getBlockState(lowestblock.add(new Vec3i(0, -1, 0))).getBlock() == Blocks.AIR))
            groundY = lowestblock.getY();
        else {
            for (lowblockY = -2; lowblockY > -319; ) {
                BlockPos lowpos1 = lowestblock.add(new Vec3i(0, lowblockY, 0));
                if (mc.world.getBlockState(lowpos1).getBlock() == Blocks.AIR && search) {
                    groundY = lowpos1.getY();
                }
                if (!(mc.world.getBlockState(lowpos1).getBlock() == Blocks.AIR)) search = false;
                lowblockY--;
            }
        }
    }

    private void seekground2() {
        if (!(mc.world.getBlockState(highestblock.add(new Vec3i(0, -1, 0))).getBlock() == Blocks.AIR))
            groundY2 = highestblock.getY();
        else {
            for (highblockY = -2; highblockY > -319; ) {
                BlockPos lowpos1 = highestblock.add(new Vec3i(0, highblockY, 0));
                if (mc.world.getBlockState(lowpos1).getBlock() == Blocks.AIR && search2) {
                    groundY2 = lowpos1.getY();
                }
                if (!(mc.world.getBlockState(lowpos1).getBlock() == Blocks.AIR)) search2 = false;
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
        if (!findResult.found()) {
            return;
        }
        mc.player.getInventory().selectedSlot = findResult.slot();
    }

    private boolean isInvalidBlock(ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem)
                || stack.getItem() instanceof BedItem
                || stack.getItem() instanceof PowderSnowBucketItem
                || stack.getItem() instanceof ScaffoldingItem
                || stack.getItem() instanceof TallBlockItem
                || stack.getItem() instanceof VerticallyAttachableBlockItem
                || stack.getItem() instanceof PlaceableOnWaterItem
                || ((BlockItem) stack.getItem()).getBlock() instanceof PlantBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TorchBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AbstractRedstoneGateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof RedstoneWireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WallBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceGateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FallingBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AbstractRailBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AbstractSignBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BellBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CarpetBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ConduitBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof DeadCoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof DeadCoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripwireHookBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PointedDripstoneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripwireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SnowBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PressurePlateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WallMountedBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AmethystClusterBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BuddingAmethystBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusFlowerBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusPlantBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LanternBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CandleBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TntBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CakeBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CobwebBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SugarCaneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SporeBlossomBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof KelpBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof GlowLichenBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CactusBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BambooBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FlowerPotBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LadderBlock
                || skippableBlox.get().contains(((BlockItem) stack.getItem()).getBlock());
    }
}