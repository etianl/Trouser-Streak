package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3i;
import pwn.noobs.trouserstreak.Trouser;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import pwn.noobs.trouserstreak.utils.BEntityUtils;
import pwn.noobs.trouserstreak.utils.BPlayerUtils;
import pwn.noobs.trouserstreak.utils.BWorldUtils;
import pwn.noobs.trouserstreak.utils.PositionUtils;


/**
 * @Author majorsopa
 * https://github.com/majorsopa
 * @Author evaan
 * https://github.com/evaan
 * @Author etianll
 * https://github.com/etianl
 */
public class AutoStaircaseFly extends Module {
    public enum CenterMode {
        Center,
        Snap,
        None
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("AirPlace")
        .description("AirPlace")
        .defaultValue(true)
        .build()
    );

    private final Setting<CenterMode> centerMode = sgGeneral.add(new EnumSetting.Builder<CenterMode>()
        .name("center")
        .description("How AutoStaircase should center you.")
        .defaultValue(CenterMode.Center)
        .build()
    );
    private final Setting<Double> up = sgGeneral.add(new DoubleSetting.Builder()
            .name("UpwardVelocity")
            .description("Your velocity going upward, for fine tuning.")
            .defaultValue(1.1)
            .min(0.1)
            .sliderMax(2.0)
            .build());
    private final Setting<Double> fwd = sgGeneral.add(new DoubleSetting.Builder()
            .name("ForwardVelocity")
            .description("Your velocity going forward, for fine tuning.")
            .defaultValue(0.64)
            .min(0.1)
            .sliderMax(2.0)
            .build()
    );

    public final Setting<Boolean> timer = sgGeneral.add(new BoolSetting.Builder()
            .name("Timer")
            .description("Timer on/off")
            .defaultValue(false)
            .build()
    );

    public final Setting<Integer> StairTimer = sgGeneral.add(new IntSetting.Builder()
            .name("TimerMultiplier")
            .description("The multiplier value for Timer.")
            .defaultValue(2)
            .min(1)
            .sliderMax(10)
            .visible(() -> timer.get())
            .build()
    );

    public final Setting<Boolean> akick = sgGeneral.add(new BoolSetting.Builder()
            .name("AntiKick")
            .description("AntiKick on/off")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The amount of delay, in ticks, between toggles in normal mode.")
            .defaultValue(15)
            .range(1, 5000)
            .sliderMax(60)
            .visible(() -> akick.get())
            .build()
    );
    private final Setting<Integer> offTime = sgGeneral.add(new IntSetting.Builder()
            .name("off-time")
            .description("The amount of delay, in ticks, that Flight is toggled off for in normal mode.")
            .defaultValue(5)
            .range(1, 20)
            .visible(() -> akick.get())
            .build()
    );

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
            .name("Build Limit")
            .description("sets the height at which the stairs stop")
            .range(-64, 319)
            .defaultValue(319)
            .min(-64)
            .sliderMax(319)
            .build());

    private boolean resetTimer;

    public AutoStaircaseFly () {
        super(Trouser.Main, "AutoStaircaseFly", "Make stairs while flying!");
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
        mc.options.jumpKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // East
        WButton east = table.add(theme.button("East")).expandX().minWidth(100).widget();
        east.action = () ->
                mc.player.setYaw(270);
        mc.options.jumpKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // South
        WButton south = table.add(theme.button("South")).expandX().minWidth(100).widget();
        south.action = () ->
                mc.player.setYaw(360);
        mc.options.jumpKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // West
        WButton west = table.add(theme.button("West")).expandX().minWidth(100).widget();
        west.action = () ->
                mc.player.setYaw(90);
        mc.options.jumpKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        return table;
    }

    @Override
    public void onActivate() {
        resetTimer = false;
        ticksPassed = 0;
        blocksPlaced = 0;

        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }
        mc.player.setVelocity(0,0,0);
        mc.options.jumpKey.setPressed(true);

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());
    }

    @Override
    public void onDeactivate() {
        mc.player.setVelocity(0,0,0);
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        resetTimer = true;
        mc.options.jumpKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
        BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
        if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
            mc.options.forwardKey.setPressed(false);
            if (!airPlace.getDefaultValue()) mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos.down()), Direction.DOWN, pos, false));
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);}
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (timer.get()) {
            if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.AIR) {
                resetTimer = false;
                Modules.get().get(Timer.class).setOverride(StairTimer.get());
            } else if (!resetTimer) {
                Modules.get().get(Timer.class).setOverride(Timer.OFF);
                resetTimer = true;
            }
        }
        if (mc.player.getMainHandStack().isEmpty()) {
            mc.player.setVelocity(0,0,0);
        }
        if (mc.options.backKey.isPressed()){
            mc.player.setVelocity(0,0,0);
        }
        if(mc.player.getY() >= limit.get()){
            mc.player.setVelocity(0,0,0);
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (akick.get() && delayLeft > 0) delayLeft--;

        else if (akick.get() && delayLeft <= 0 && offLeft > 0) {
            offLeft--;
        mc.options.backKey.setPressed(true);

        } else if (akick.get() && delayLeft <= 0 && offLeft <= 0) {
            mc.options.backKey.setPressed(false);
            delayLeft = delay.get();
            offLeft = offTime.get();
        }
    }
    private long lastModifiedTime = 0;
    private double lastY = Double.MAX_VALUE;
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerMoveC2SPacket packet) || akick.get()) return;

        long currentTime = System.currentTimeMillis();
        double currentY = packet.getY(Double.MAX_VALUE);
        if (currentY != Double.MAX_VALUE) {
            // maximum time we can be "floating" is 80 ticks, so 4 seconds max
            if (currentTime - lastModifiedTime > 1000
                    && lastY != Double.MAX_VALUE
                    && mc.world.getBlockState(mc.player.getBlockPos().down()).isAir()) {
                // actual check is for >= -0.03125D but we have to do a bit more than that
                // probably due to compression or some shit idk
                ((PlayerMoveC2SPacketAccessor) packet).setY(lastY - 0.03130D);
                lastModifiedTime = currentTime;
            } else {
                lastY = currentY;
            }
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        if (mc.player == null || mc.world == null) {toggle(); return;}
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
        BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
        switch (mc.player.getMovementDirection()) {
            case NORTH -> mc.player.setVelocity(0,up.get(),-fwd.get());
            case EAST -> mc.player.setVelocity(fwd.get(),up.get(),0);
            case SOUTH -> mc.player.setVelocity(0,up.get(),fwd.get());
            case WEST -> mc.player.setVelocity(-fwd.get(),up.get(),0);
            default -> {
            }
        }
        if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
            mc.options.forwardKey.setPressed(false);
            if (!airPlace.getDefaultValue()) mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos.down()), Direction.DOWN, pos, false));
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        if (!mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
            mc.options.jumpKey.setPressed(true);
                ticksPassed = 0;
                blocksPlaced = 0;

                centered = false;
                playerPos = BEntityUtils.playerPos(mc.player);

                if (centerMode.get() != CenterMode.None) {
                    if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
                    else PlayerUtils.centerPlayer();
                }

                dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        }
    }
    private void unpress() {
        setPressed(mc.options.forwardKey, false);
        setPressed(mc.options.backKey, false);
        setPressed(mc.options.leftKey, false);
        setPressed(mc.options.rightKey, false);
    }
    private void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }
}
