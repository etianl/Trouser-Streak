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
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
    public final Setting<Double> StairTimer = sgGeneral.add(new DoubleSetting.Builder()
            .name("Timer")
            .description("The multiplier value for Staircase speed.")
            .defaultValue(1)
            .min(1)
            .sliderMax(30)
            .build()
    );
    private boolean resetTimer;
    public AutoStaircase() {
        super(Trouser.Main, "AutoStaircase", "Make stairs!");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(0,0,0);
        resetTimer = false;
        PlayerUtils.centerPlayer();
        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;
        BlockPos pos = mc.player.blockPosition().offset(0,-1,0);
        if (mc.level.getBlockState(pos).canBeReplaced()) {;
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
            mc.player.swing(InteractionHand.MAIN_HAND);}
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
        mc.options.keyUp.setDown(false);
        mc.options.keyJump.setDown(false);
        resetTimer = true;
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) {toggle(); return;}
            if (mc.player.getMainHandItem().getItem() instanceof BlockItem) {
                resetTimer = false;
                Modules.get().get(Timer.class).setOverride(StairTimer.get());
            } else if (!resetTimer) {
                resetTimer = true;
                Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
        if (mc.player.getMainHandItem().isEmpty()) {
            mc.options.keyUp.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyJump.setDown(false);
            PlayerUtils.centerPlayer();
        }
        if (mc.options.keyRight.isDown())
            mc.options.keyRight.setDown(false);
        if (mc.options.keyLeft.isDown())
            mc.options.keyLeft.setDown(false);
        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;
        switch (mc.player.getMotionDirection()) {
            case NORTH ->
                    mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ() - view.get()));
            case EAST ->
                    mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(mc.player.getX() + view.get(), mc.player.getY(), mc.player.getZ()));
            case SOUTH ->
                    mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ() + view.get()));
            case WEST ->
                    mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(mc.player.getX() - view.get(), mc.player.getY(), mc.player.getZ()));
            default -> {
            }
        }
        if (!mc.player.onGround())return;
        if (mc.options.keyDown.isDown()){
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.player.setDeltaMovement(0,0,0);
        }
        if(mc.player.getY() >= limit.get()){
            mc.options.keyUp.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyJump.setDown(false);
            PlayerUtils.centerPlayer();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        if (mc.player == null || mc.level == null) {toggle(); return;}
        if (!mc.player.onGround() || !(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;
        BlockPos pos = mc.player.blockPosition().relative(mc.player.getMotionDirection());
        if (mc.level.getBlockState(pos).canBeReplaced()) {
            mc.options.keyUp.setDown(false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        if (!mc.level.getBlockState(pos).canBeReplaced()) {
            mc.options.keyUp.setDown(true);
            mc.options.keyJump.setDown(true);
            PlayerUtils.centerPlayer();
        }
        if (mc.player.getMainHandItem().isEmpty()) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
        }
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {toggle();}
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {toggle();}
}