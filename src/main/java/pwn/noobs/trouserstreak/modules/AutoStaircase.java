package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

/**
 * @Author majorsopa
 * https://github.com/majorsopa
 * @Author evaan
 * https://github.com/evaan
 * @Author etianll
 * https://github.com/etianl
 */

public class AutoStaircase extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Double> StairTimer = sgGeneral.add(new DoubleSetting.Builder()
            .name("Timer")
            .description("The multiplier value for Staircase speed.")
            .defaultValue(1)
            .min(1)
            .sliderMax(30)
            .build()
    );
    private final Setting<Double> view = sgGeneral.add(new DoubleSetting.Builder()
            .name("ViewAngle")
            .description("Angle of your view")
            .defaultValue(1)
            .min(0.1)
            .sliderMax(30)
            .build());
    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
            .name("Build Limit")
            .description("sets the height at which the stairs stop")
            .sliderRange(-64, 319)
            .defaultValue(319)
            .build()
    );
    private boolean resetTimer;

    public AutoStaircase() {
        super(Trouser.Main, "AutoStaircase", "Make stairs!");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        mc.player.setVelocity(0, 0, 0);
        resetTimer = false;
        PlayerUtils.centerPlayer();
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
        BlockPos pos = mc.player.getBlockPos().add(0, -1, 0);
        if (mc.world.getBlockState(pos).isReplaceable()) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        if (Modules.get().get(Flight.class).isActive()) {
            Modules.get().get(Flight.class).toggle();
        }
        if (Modules.get().get(FlightAntikick.class).isActive()) {
            Modules.get().get(FlightAntikick.class).toggle();
        }
        if (Modules.get().get(TPFly.class).isActive()) {
            Modules.get().get(TPFly.class).toggle();
        }
    }

    @Override
    public void onDeactivate() {
        mc.options.forwardKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        resetTimer = true;
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        if (mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) {
            resetTimer = false;
            Modules.get().get(Timer.class).setOverride(StairTimer.get());
        } else if (!resetTimer) {
            resetTimer = true;
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
        if (mc.player.getMainHandStack().isEmpty()) {
            mc.options.forwardKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            PlayerUtils.centerPlayer();
        }
        if (mc.options.rightKey.isPressed())
            mc.options.rightKey.setPressed(false);
        if (mc.options.leftKey.isPressed())
            mc.options.leftKey.setPressed(false);
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem)) return;
        switch (mc.player.getMovementDirection()) {
            case NORTH ->
                    mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ() - view.get()));
            case EAST ->
                    mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(mc.player.getX() + view.get(), mc.player.getY(), mc.player.getZ()));
            case SOUTH ->
                    mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ() + view.get()));
            case WEST ->
                    mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(mc.player.getX() - view.get(), mc.player.getY(), mc.player.getZ()));
            default -> {
            }
        }
        if (!mc.player.isOnGround()) return;
        if (mc.options.backKey.isPressed()) {
            mc.options.forwardKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.player.setVelocity(0, 0, 0);
        }
        if (mc.player.getY() >= limit.get()) {
            mc.options.forwardKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            PlayerUtils.centerPlayer();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        if (!mc.player.isOnGround() || !(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem))
            return;
        BlockPos pos = mc.player.getBlockPos().offset(mc.player.getMovementDirection());
        if (mc.world.getBlockState(pos).isReplaceable()) {
            mc.options.forwardKey.setPressed(false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(pos), Direction.DOWN, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        if (!mc.world.getBlockState(pos).isReplaceable()) {
            mc.options.forwardKey.setPressed(true);
            mc.options.jumpKey.setPressed(true);
            PlayerUtils.centerPlayer();
        }
        if (mc.player.getMainHandStack().isEmpty()) {
            mc.options.forwardKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }
}