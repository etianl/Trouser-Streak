package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
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
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.BEntityUtils;
import pwn.noobs.trouserstreak.utils.BPlayerUtils;


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

    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<Boolean> startPaused = sgGeneral.add(new BoolSetting.Builder()
            .name("Start Paused")
            .description("AutoMountain is Paused when module activated, for more control.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> spc = sgGeneral.add(new IntSetting.Builder()
            .name("VerticalSpacing")
            .description("Space between stairs placed vertically, for adjusting steepness of the mountain.")
            .defaultValue(1)
            .min(1)
            .sliderMax(5)
            .build());
    private final Setting<Integer> spcoffset = sgGeneral.add(new IntSetting.Builder()
            .name("OnDemandSpacing")
            .description("Press spacebar to adjust spacing as you build")
            .defaultValue(1)
            .min(1)
            .sliderMax(5)
            .build());

    public final Setting<Boolean> timer = sgGeneral.add(new BoolSetting.Builder()
            .name("Timer")
            .description("Timer on/off")
            .defaultValue(false)
            .build()
    );

    public final Setting<Double> StairTimer = sgGeneral.add(new DoubleSetting.Builder()
            .name("TimerMultiplier")
            .description("The multiplier value for Timer.")
            .defaultValue(2)
            .min(0.5)
            .sliderMax(10)
            .visible(() -> timer.get())
            .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("PauseForThisAmountOfTicks")
            .description("The amount of delay in ticks, when pausing. Useful if a server kicks you for too many packets.")
            .defaultValue(0)
            .sliderRange(0, 40)
            .build()
    );
    private final Setting<Integer> offTime = sgGeneral.add(new IntSetting.Builder()
            .name("TicksBetweenPause")
            .description("The amount of delay, in ticks, between pauses.")
            .defaultValue(15)
            .sliderRange(1, 60)
            .build()
    );

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
            .name("UpwardBuildLimit")
            .description("sets the Y level at which the stairs stop going up")
            .sliderRange(-64, 318)
            .defaultValue(318)
            .build());
    public final Setting<Boolean> InvertDir = sgGeneral.add(new BoolSetting.Builder()
            .name("InvertDir@Limit")
            .description("Inverts Direction from up to down, 1 block before you reach your set limit.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> swap = sgGeneral.add(new BoolSetting.Builder()
            .name("SwapStackonRunOut")
            .description("Swaps to another stack of blocks in your hotbar when you run out")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> lagpause = sgGeneral.add(new BoolSetting.Builder()
            .name("Pause if Server Lagging")
            .description("Pause Builder if server is lagging (prevents rare death when building downward)")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> lag = sgGeneral.add(new DoubleSetting.Builder()
            .name("How many seconds until pause")
            .description("Pause Builder if server is lagging for this many seconds (prevents rare death when building downward)")
            .sliderRange(0, 10)
            .defaultValue(1)
            .visible(() -> lagpause.get())
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

    private boolean resetTimer;

    public AutoMountain() {
        super(Trouser.Main, "AutoMountain", "Make Mountains!");
    }
    private int delayLeft = delay.get();
    private int offLeft = offTime.get();

    // Fields
    private BlockPos playerPos;
    private int ticksPassed;
    private int blocksPlaced;
    private boolean centered;

    Direction dir;
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();

        // North
        WButton north = table.add(theme.button("North")).expandX().minWidth(100).widget();
        north.action = () ->
                mc.player.setYaw(180);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // East
        WButton east = table.add(theme.button("East")).expandX().minWidth(100).widget();
        east.action = () ->
                mc.player.setYaw(270);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // South
        WButton south = table.add(theme.button("South")).expandX().minWidth(100).widget();
        south.action = () ->
                mc.player.setYaw(360);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // West
        WButton west = table.add(theme.button("West")).expandX().minWidth(100).widget();
        west.action = () ->
                mc.player.setYaw(90);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // Up
        WButton up = table.add(theme.button("Up")).expandX().minWidth(100).widget();
        up.action = () ->
                mc.player.setPitch(35);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // Down
        WButton down = table.add(theme.button("Down")).expandX().minWidth(100).widget();
        down.action = () ->
                mc.player.setPitch(75);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();


        return table;
    }
    public boolean pause = true;

    @Override
    public void onActivate() {
        mc.player.setPos(mc.player.getX(),Math.round(mc.player.getY()),mc.player.getZ());
        if (startPaused.get() == true){
        pause = false;
        error("Press UseKey (RightClick) to Build Stairs!");
        } else if (startPaused.get() == false){
            pause = true;
        }
        resetTimer = false;
        ticksPassed = 0;
        blocksPlaced = 0;

        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock) return;
        mc.player.setVelocity(0,0,0);
        BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
        if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);}
        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());
        if (Modules.get().get(TrouserFlight.class).isActive()) {
            Modules.get().get(TrouserFlight.class).toggle();
        }
        if (Modules.get().get(TPFly.class).isActive()) {
            Modules.get().get(TPFly.class).toggle();
        }
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        resetTimer = true;
        if (!pause==false){
        mc.player.setPos(mc.player.getX(),mc.player.getY()+0.2,mc.player.getZ());//this line here prevents you dying for realz
        mc.player.setVelocity(0,0.1,0);//this line here prevents you dying for realz
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock) return;
        BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
        if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);}}
    }
    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.useKey.isPressed()){
            pause = pause ? false : true;
            mc.player.setPos(mc.player.getX(),mc.player.getY()+0.2,mc.player.getZ());//this line here prevents you dying for realz
            mc.player.setVelocity(0,0.1,0);//this line here prevents you dying for realz
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
            resetTimer = true;
            if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock) return;
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
    }
    @EventHandler
    private void onKeyEvent(KeyEvent event) {
        if (!pause == true) return;
        if (mc.options.forwardKey.isPressed()){
            mc.player.setPitch(35);
        }
        if (mc.options.backKey.isPressed()){
            mc.player.setPitch(75);
        }
        if (mc.options.leftKey.isPressed()){
            mc.player.setYaw(mc.player.getYaw()-90);
        }
        if (mc.options.rightKey.isPressed()){
            mc.player.setYaw(mc.player.getYaw()+90);
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (!pause == true) return;
        ticksPassed = 0;
        blocksPlaced = 0;

        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());
        if (pause = true){
            if (Modules.get().get(TrouserFlight.class).isActive()) {
                Modules.get().get(TrouserFlight.class).toggle();
            }
            if (Modules.get().get(TPFly.class).isActive()) {
                Modules.get().get(TPFly.class).toggle();
            }
        }
        if (swap.get()){
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
            mc.player.getInventory().selectedSlot = 0;
            if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
                mc.player.getInventory().selectedSlot = 1;
                if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
                    mc.player.getInventory().selectedSlot = 2;
                    if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
                        mc.player.getInventory().selectedSlot = 3;
                        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
                            mc.player.getInventory().selectedSlot = 4;
                            if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
                                mc.player.getInventory().selectedSlot = 5;
                                if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
                                    mc.player.getInventory().selectedSlot = 6;
                                    if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
                                        mc.player.getInventory().selectedSlot = 7;
                                        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock){
                                            mc.player.getInventory().selectedSlot = 8;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }}
        mc.player.setVelocity(0,0,0);
        if (timer.get()) {
            if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.AIR) {
                resetTimer = false;
                Modules.get().get(Timer.class).setOverride(StairTimer.get());
            } else if (!resetTimer) {
                Modules.get().get(Timer.class).setOverride(Timer.OFF);
                resetTimer = true;
            }
        }
            mc.player.setPos(mc.player.getX(),Math.round(mc.player.getY())+0.25,mc.player.getZ());//this line here prevents you dying for realz
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        playerPos = BEntityUtils.playerPos(mc.player);
        if (!pause == true) return;
        ticksPassed = 0;
        blocksPlaced = 0;

        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);
        PlayerUtils.centerPlayer();

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());
    }
    private double lastPacketY = Double.MAX_VALUE;
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        //this here packet antikick from Flight prevents kick if left floating and also helps to ensure stairs building is correct
        boolean akick = true;
        if (!(event.packet instanceof PlayerMoveC2SPacket packet) || akick) return;

        double currentY = packet.getY(Double.MAX_VALUE);
        if (currentY != Double.MAX_VALUE) {
            // maximum time we can be "floating" is 80 ticks, so 4 seconds max
            if (this.delayLeft <= 0 && this.lastPacketY != Double.MAX_VALUE &&
                    shouldFlyDown(currentY, this.lastPacketY) && isEntityOnAir(mc.player)) {
                // actual check is for >= -0.03125D but we have to do a bit more than that
                // probably due to compression or some shit idk
                ((PlayerMoveC2SPacketAccessor) packet).setY(lastPacketY - 0.03130D);
            } else {
                lastPacketY = currentY;
            }
        }
    }
    private boolean shouldFlyDown(double currentY, double lastY) {
        if (currentY >= lastY) {
            return true;
        } else return lastY - currentY < 0.03130D;
    }
    private boolean isEntityOnAir(Entity entity) {
        return entity.world.getStatesInBox(entity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0)).allMatch(AbstractBlock.AbstractBlockState::isAir);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
        if (mc.options.jumpKey.isPressed()){
            if (mc.player.getPitch() <= 40){            //UP
                switch (mc.player.getMovementDirection()) {
                    case NORTH -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(0, +spcoffset.get()+spc.get()-1, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case SOUTH -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(0, +spcoffset.get()+spc.get()-1, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case EAST -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(1, +spcoffset.get()+spc.get()-1, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case WEST -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(-1, +spcoffset.get()+spc.get()-1, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    default -> {
                    }
                }
            }
            else if (mc.player.getPitch() >= 40){            //UP
                switch (mc.player.getMovementDirection()) {
                    case NORTH -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(0, -spcoffset.get()-spc.get()-1, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case SOUTH -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(0, -spcoffset.get()-spc.get()-1, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case EAST -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(1, -spcoffset.get()-spc.get()-1, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case WEST -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(-1, -spcoffset.get()-spc.get()-1, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    default -> {
                    }
                }
            }
        }
        else {
            if (mc.player.getPitch() <= 40) {            //UP
                switch (mc.player.getMovementDirection()) {
                    case NORTH -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(0, +spc.get() - 1, -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case SOUTH -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(0, +spc.get() - 1, 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case EAST -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(1, +spc.get() - 1, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case WEST -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(-1, +spc.get() - 1, 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    default -> {
                    }
                }
            } else if (mc.player.getPitch() >= 40) {            //UP
                switch (mc.player.getMovementDirection()) {
                    case NORTH -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(0, -1 - spc.get(), -1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case SOUTH -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(0, -1 - spc.get(), 1));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case EAST -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(1, -1 - spc.get(), 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    case WEST -> {
                        BlockPos pos1 = playerPos.add(new Vec3i(-1, -1 - spc.get(), 0));
                        event.renderer.box(pos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                    default -> {
                    }
                }
            }
        }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        float timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();

        if (lagpause.get() && timeSinceLastTick >= lag.get()) {
            if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock || !pause == true) return;
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }

        if (mc.player.getPitch() <= 40){
        if (delayLeft > 0) delayLeft--;

        else if ((!lagpause.get() || timeSinceLastTick <= lag.get()) && mc.player.getY() <= limit.get() && delayLeft <= 0 && offLeft > 0) {
            offLeft--;
            if (mc.player == null || mc.world == null) {toggle(); return;}
            if ((lagpause.get() && timeSinceLastTick >= lag.get()) || !(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock || !pause == true) return;
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            switch (mc.player.getMovementDirection()) {
                case NORTH -> {
                    BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                    BlockPos un1 = playerPos.add(new Vec3i(0,spc.get(),0));
                    BlockPos un2 = playerPos.add(new Vec3i(0,spc.get(),-1));
                    BlockPos un3 = playerPos.add(new Vec3i(0,spc.get()+1,-1));
                    if (mc.world.getBlockState(un1).isAir() && mc.world.getBlockState(un2).isAir() && mc.world.getBlockState(un3).isAir()){
                    mc.player.setPosition(mc.player.getX(),mc.player.getY()+spc.get(),mc.player.getZ()-1);}
                    else {}
                }
                case EAST -> {
                    BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                    BlockPos ue1 = playerPos.add(new Vec3i(0,spc.get(),0));
                    BlockPos ue2 = playerPos.add(new Vec3i(+1,spc.get(),0));
                    BlockPos ue3 = playerPos.add(new Vec3i(+1,spc.get()+1,0));
                    if (mc.world.getBlockState(ue1).isAir() && mc.world.getBlockState(ue2).isAir() && mc.world.getBlockState(ue3).isAir()){
                    mc.player.setPosition(mc.player.getX()+1,mc.player.getY()+spc.get(),mc.player.getZ());}
                    else {}
                }
                case SOUTH -> {
                    BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                    BlockPos us1 = playerPos.add(new Vec3i(0,spc.get(),0));
                    BlockPos us2 = playerPos.add(new Vec3i(0,spc.get(),+1));
                    BlockPos us3 = playerPos.add(new Vec3i(0,spc.get()+1,+1));
                    if (mc.world.getBlockState(us1).isAir() && mc.world.getBlockState(us2).isAir() && mc.world.getBlockState(us3).isAir()){
                    mc.player.setPosition(mc.player.getX(),mc.player.getY()+spc.get(),mc.player.getZ()+1);}
                    else {}
                }
                case WEST -> {
                    BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                    BlockPos uw1 = playerPos.add(new Vec3i(0,spc.get(),0));
                    BlockPos uw2 = playerPos.add(new Vec3i(-1,spc.get(),0));
                    BlockPos uw3 = playerPos.add(new Vec3i(-1,spc.get()+1,0));
                    if (mc.world.getBlockState(uw1).isAir() && mc.world.getBlockState(uw2).isAir() && mc.world.getBlockState(uw3).isAir()){
                    mc.player.setPosition(mc.player.getX()-1,mc.player.getY()+spc.get(),mc.player.getZ());}
                    else {}
                }
                default -> {
                }
            }
            if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            BlockPos playerPos = BEntityUtils.playerPos(mc.player);
            BlockPos nokillplz = playerPos.add(0,+spcoffset.get(),0);
            if (mc.world.getBlockState(nokillplz).isAir() && mc.options.jumpKey.isPressed()){
                mc.player.setPosition(mc.player.getX(),mc.player.getY()+spcoffset.get(),mc.player.getZ());
            }
            if (mc.player.getY() >= limit.get()-1 && InvertDir.get()){
                mc.player.setPitch(75);
            }
        } else if (mc.player.getY() >= limit.get() && !InvertDir.get()|| delayLeft <= 0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
            if ((lagpause.get() && timeSinceLastTick >= lag.get()) || !(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock || !pause == true) return;
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
            mc.player.setVelocity(0,0,0);
            ticksPassed = 0;
            blocksPlaced = 0;

            centered = false;
            playerPos = BEntityUtils.playerPos(mc.player);
            PlayerUtils.centerPlayer();
        }
    } else if (mc.player.getPitch() >= 40){
            if (delayLeft > 0) delayLeft--;

            else if ((!lagpause.get() || timeSinceLastTick <= lag.get()) && mc.player.getY() <= limit.get() && delayLeft <= 0 && offLeft > 0) {
                offLeft--;
                if (mc.player == null || mc.world == null) {toggle(); return;}
                if ((lagpause.get() && timeSinceLastTick >= lag.get()) || !(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock || !pause == true) return;
                BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
                switch (mc.player.getMovementDirection()) {
                    case NORTH -> {
                        BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                        BlockPos dn1 = playerPos.add(new Vec3i(0,-(spc.get()-1),-1));
                        BlockPos dn2 = playerPos.add(new Vec3i(0,-spc.get(),-1));
                        BlockPos dn3 = playerPos.add(new Vec3i(0,0,-1));
                        BlockPos dn4 = playerPos.add(new Vec3i(0,spc.get(),-1));
                        if (mc.world.getBlockState(dn1).isAir() && mc.world.getBlockState(dn2).isAir() && mc.world.getBlockState(dn3).isAir() && mc.world.getBlockState(dn4).isAir()) {
                        mc.player.setPosition(mc.player.getX(),mc.player.getY()-spc.get(),mc.player.getZ()-1);}
                        else {}
                    }
                    case EAST -> {
                        BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                        BlockPos de1 = playerPos.add(new Vec3i(1,-(spc.get()-1),0));
                        BlockPos de2 = playerPos.add(new Vec3i(1,-spc.get(),0));
                        BlockPos de3 = playerPos.add(new Vec3i(1,0,0));
                        BlockPos de4 = playerPos.add(new Vec3i(1,spc.get(),0));
                        if (mc.world.getBlockState(de1).isAir() && mc.world.getBlockState(de2).isAir() && mc.world.getBlockState(de3).isAir() && mc.world.getBlockState(de4).isAir()) {
                        mc.player.setPosition(mc.player.getX()+1,mc.player.getY()-spc.get(),mc.player.getZ());}
                        else {}
                    }
                    case SOUTH -> {
                        BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                        BlockPos ds1 = playerPos.add(new Vec3i(0,-(spc.get()-1),1));
                        BlockPos ds2 = playerPos.add(new Vec3i(0,-spc.get(),1));
                        BlockPos ds3 = playerPos.add(new Vec3i(0,0,1));
                        BlockPos ds4 = playerPos.add(new Vec3i(0,spc.get(),1));
                        if (mc.world.getBlockState(ds1).isAir() && mc.world.getBlockState(ds2).isAir() && mc.world.getBlockState(ds3).isAir() && mc.world.getBlockState(ds4).isAir()) {
                        mc.player.setPosition(mc.player.getX(),mc.player.getY()-spc.get(),mc.player.getZ()+1);}
                        else {}
                    }
                    case WEST -> {
                        BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                        BlockPos dw1 = playerPos.add(new Vec3i(-1,-(spc.get()-1),0));
                        BlockPos dw2 = playerPos.add(new Vec3i(-1,-spc.get(),0));
                        BlockPos dw3 = playerPos.add(new Vec3i(-1,0,0));
                        BlockPos dw4 = playerPos.add(new Vec3i(-1,spc.get(),0));
                        if (mc.world.getBlockState(dw1).isAir() && mc.world.getBlockState(dw2).isAir() && mc.world.getBlockState(dw3).isAir() && mc.world.getBlockState(dw4).isAir()) {
                        mc.player.setPosition(mc.player.getX()-1,mc.player.getY()-spc.get(),mc.player.getZ());}
                        else {}
                    }
                    default -> {
                    }
                }
                if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos nokillplz1 = playerPos.add(new Vec3i(0,-spcoffset.get(),0));
                if (mc.world.getBlockState(nokillplz1).isAir() && mc.options.jumpKey.isPressed()){
                    mc.player.setPosition(mc.player.getX(),mc.player.getY()-spcoffset.get(),mc.player.getZ());
                }
            } else if (mc.player.getY() >= limit.get() || delayLeft <= 0 && offLeft <= 0) {
                delayLeft = delay.get();
                offLeft = offTime.get();
                if ((lagpause.get() && timeSinceLastTick >= lag.get()) || !(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof WallStandingBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SnowBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TntBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CobwebBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BambooBlock || !pause == true) return;
                BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
                if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
                mc.player.setVelocity(0,0,0);
                ticksPassed = 0;
                blocksPlaced = 0;

                centered = false;
                playerPos = BEntityUtils.playerPos(mc.player);
                PlayerUtils.centerPlayer();
            }
        }
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {toggle();}
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {toggle();}
}