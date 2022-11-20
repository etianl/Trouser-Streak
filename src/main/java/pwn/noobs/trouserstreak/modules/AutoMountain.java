package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3i;
import pwn.noobs.trouserstreak.Trouser;
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
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import java.lang.Math;



/**
 * @Author majorsopa
 * https://github.com/majorsopa
 * @Author evaan
 * https://github.com/evaan
 * @Author etianll
 * https://github.com/etianl
 */
public class AutoMountain extends Module {
    public enum CenterMode {
        Center,
        Snap,
        None
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<CenterMode> centerMode = sgGeneral.add(new EnumSetting.Builder<CenterMode>()
        .name("center")
        .description("How AutoStaircase should center you.")
        .defaultValue(CenterMode.Center)
        .build()
    );
    private final Setting<Integer> spc = sgGeneral.add(new IntSetting.Builder()
            .name("VerticalSpacing")
            .description("Space between stairs placed vertically, for adjusting steepness of the mountain.")
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

    public final Setting<Boolean> akick = sgGeneral.add(new BoolSetting.Builder()
            .name("PacketAntiKick")
            .description("AntiKick on/off")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("Pause")
            .description("The amount of delay in ticks, when pausing")
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
            .name("Build Limit")
            .description("sets the Y level at which the stairs stop")
            .range(-64, 318)
            .defaultValue(318)
            .build());


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
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // Up
        WButton up = table.add(theme.button("Up")).expandX().minWidth(100).widget();
        up.action = () ->
                mc.player.setPitch(35);
        mc.player.setMovementSpeed(0);
        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        table.row();

        // Down
        WButton down = table.add(theme.button("Down")).expandX().minWidth(100).widget();
        down.action = () ->
                mc.player.setPitch(75);
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
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
        mc.player.setVelocity(0,0,0);
        ticksPassed = 0;
        blocksPlaced = 0;

        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }
        BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
        if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);}
            mc.player.setPos(mc.player.getX(),Math.round(mc.player.getY()),mc.player.getZ());
        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());
        if (Modules.get().get(TrouserFlight.class).isActive()) {
            Modules.get().get(TrouserFlight.class).toggle();
        }
        if (Modules.get().get(TPFly.class).isActive()) {
            Modules.get().get(TPFly.class).toggle();
        }
        if (mc.player.getPitch() >= 40){
            mc.player.setPitch(75);
        }
        if (mc.player.getPitch() <= 40){
            mc.player.setPitch(35);
        }
        if (mc.player.getMovementDirection() == Direction.NORTH){
            mc.player.setYaw(180);
        }
        if (mc.player.getMovementDirection() == Direction.EAST){
            mc.player.setYaw(270);
        }
        if (mc.player.getMovementDirection() == Direction.SOUTH){
            mc.player.setYaw(360);
        }
        if (mc.player.getMovementDirection() == Direction.WEST){
            mc.player.setYaw(90);
        }
    }

    @Override
    public void onDeactivate() {
        mc.player.setVelocity(0,0.01,0);
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        resetTimer = true;
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
        BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
        if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);}
        mc.player.setPos(mc.player.getX(),mc.player.getY()+0.05,mc.player.getZ());//this line here prevents you dying for realz
    }
    @EventHandler
    private void onKeyEvent(KeyEvent event) {
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
        mc.player.setVelocity(0,0,0);
        if (mc.options.useKey.isPressed()){
            Modules.get().get(AutoMountain.class).toggle();
        }
        if (timer.get()) {
            if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.AIR) {
                resetTimer = false;
                Modules.get().get(Timer.class).setOverride(StairTimer.get());
            } else if (!resetTimer) {
                Modules.get().get(Timer.class).setOverride(Timer.OFF);
                resetTimer = true;
            }
        }
            mc.player.setPos(mc.player.getX(),Math.round(mc.player.getY())+0.05,mc.player.getZ());//this line here prevents you dying for realz
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
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
    private double lastPacketY = Double.MAX_VALUE;
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerMoveC2SPacket packet) || akick.get()) return;

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
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        if (mc.player.getPitch() <= 40){
        if (delayLeft > 0) delayLeft--;

        else if (mc.player.getY() <= limit.get() && delayLeft <= 0 && offLeft > 0) {
            offLeft--;
            if (mc.player == null || mc.world == null) {toggle(); return;}
            if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
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
        } else if (mc.player.getY() >= limit.get() || delayLeft <= 0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
            if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
            mc.player.setVelocity(0,0,0);
            ticksPassed = 0;
            blocksPlaced = 0;

            centered = false;
            playerPos = BEntityUtils.playerPos(mc.player);

            if (centerMode.get() != CenterMode.None) {
                if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
                else PlayerUtils.centerPlayer();
            }
        }
    } else if (mc.player.getPitch() >= 40){
            if (delayLeft > 0) delayLeft--;

            else if (mc.player.getY() <= limit.get() && delayLeft <= 0 && offLeft > 0) {
                offLeft--;
                if (mc.player == null || mc.world == null) {toggle(); return;}
                if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
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
            } else if (mc.player.getY() >= limit.get() || delayLeft <= 0 && offLeft <= 0) {
                delayLeft = delay.get();
                offLeft = offTime.get();
                if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
                BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
                if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
                mc.player.setVelocity(0,0,0);
                ticksPassed = 0;
                blocksPlaced = 0;

                centered = false;
                playerPos = BEntityUtils.playerPos(mc.player);

                if (centerMode.get() != CenterMode.None) {
                    if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
                    else PlayerUtils.centerPlayer();
                }
            }
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
