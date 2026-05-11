//Written by etianll using some code from MeteorClient ClickTP as well as Airplace
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.events.OffGroundSpeedEvent;

public class TPFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("mode")
            .description("the mode")
            .defaultValue(Modes.Normal)
            .build());
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("The maximum distance you can teleport.")
            .defaultValue(5)
            .min(1)
            .sliderMax(7)
            .build()
    );
    private final Setting<Integer> uprange = sgGeneral.add(new IntSetting.Builder()
            .name("UpRange")
            .description("UpwardRange")
            .defaultValue(4)
            .min(1)
            .sliderMax(6)
            .build()
    );
    private final Setting<Integer> downrange = sgGeneral.add(new IntSetting.Builder()
            .name("DownRange")
            .description("DownwardRange")
            .defaultValue(4)
            .min(1)
            .sliderMax(6)
            .build()
    );
    public final Setting<Boolean> Acceleration = sgGeneral.add(new BoolSetting.Builder()
            .name("Acceleration")
            .description("Acceleration on/off")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> acceleration = sgGeneral.add(new IntSetting.Builder()
            .name("Acceleration")
            .description("The Acceleration.")
            .defaultValue(1)
            .min(1)
            .sliderMax(5)
            .visible(Acceleration::get)
            .build()
    );
    private final Setting<Integer> upacceleration = sgGeneral.add(new IntSetting.Builder()
            .name("UpAcceleration")
            .description("The upward Acceleration.")
            .defaultValue(1)
            .min(1)
            .sliderMax(5)
            .visible(Acceleration::get)
            .build()
    );
    private final Setting<Integer> downacceleration = sgGeneral.add(new IntSetting.Builder()
            .name("DownAcceleration")
            .description("The downward Acceleration.")
            .defaultValue(1)
            .min(1)
            .sliderMax(5)
            .visible(Acceleration::get)
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
            .description("The amount of delay, in ticks, between toggles.")
            .defaultValue(20)
            .sliderRange(0, 60)
            .visible(akick::get)
            .build()
    );
    private final Setting<Integer> offTime = sgGeneral.add(new IntSetting.Builder()
            .name("off-time")
            .description("The amount of delay, in ticks, that TPFly is toggled off.")
            .defaultValue(3)
            .sliderRange(0, 200)
            .visible(akick::get)
            .build()
    );

    public TPFly() {
        super(Trouser.Main, "TPFly", "Teleports you to flyyyyyyyyy!");
    }
    private int Range = -1;
    private int upRange = -1;
    private int downRange = -1;
    private int delayLeft = delay.get();
    private int offLeft = offTime.get();
    @Override
    public void onDeactivate() {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(0,0.01,0);
        if (!mc.options.keyShift.isDown()){
            mc.player.setPosRaw(mc.player.getX(),mc.player.getY()+0.1,mc.player.getZ());
        } //this line here prevents you dying for realz
        else if (mc.options.keyShift.isDown()) {
            mc.options.keyShift.setDown(false);
            mc.player.setPosRaw(mc.player.getX(),mc.player.getY()+(downrange.get()+1),mc.player.getZ());
        } //this line here prevents you dying for realz
    }
    //making absolutely sure there is no velocity and that this is setPos movement only
    @EventHandler
    private void onKeyEvent(KeyInputEvent event) {
        if (mc.player != null && mc.options.keyJump.isDown() || mc.options.keyShift.isDown() || mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()){
            mc.player.setDeltaMovement(0,0,0);
            mc.player.setSpeed(0);
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(0,0,0);
        mc.player.setSpeed(0);
    }
    @EventHandler
    private void onTick(TickEvent event) {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(0,0,0);
        mc.player.setSpeed(0);
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(0,0,0);
        mc.player.setSpeed(0);
        if (Acceleration.get() && Range < range.get() && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyRight.isDown() || mc.options.keyLeft.isDown() || mc.options.keyShift.isDown() || mc.options.keyJump.isDown())){
            Range=Math.min(range.get(), Range + acceleration.get());
        } else if (Acceleration.get() && !mc.options.keyUp.isDown() && !mc.options.keyDown.isDown() && !mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown() && !mc.options.keyShift.isDown() && !mc.options.keyJump.isDown()){
            Range=-1;
        } else if (!Acceleration.get()) Range=range.get();

        if (Acceleration.get() && upRange < uprange.get() && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyRight.isDown() || mc.options.keyLeft.isDown() || mc.options.keyShift.isDown() || mc.options.keyJump.isDown())){
            upRange=Math.min(uprange.get(), upRange + upacceleration.get());
        } else if (Acceleration.get() && !mc.options.keyUp.isDown() && !mc.options.keyDown.isDown() && !mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown() && !mc.options.keyShift.isDown() && !mc.options.keyJump.isDown()){
            upRange=-1;
        } else if (!Acceleration.get()) upRange=uprange.get();

        if (Acceleration.get() && downRange < downrange.get() && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyRight.isDown() || mc.options.keyLeft.isDown() || mc.options.keyShift.isDown() || mc.options.keyJump.isDown())){
            downRange=Math.min(downrange.get(), downRange + downacceleration.get());
        } else if (Acceleration.get() && !mc.options.keyUp.isDown() && !mc.options.keyDown.isDown() && !mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown() && !mc.options.keyShift.isDown() && !mc.options.keyJump.isDown()){
            downRange=-1;
        } else if (!Acceleration.get()) downRange=downrange.get();
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundMovePlayerPacket && mc.player != null){
            ((ServerboundMovePlayerPacketAccessor) event.packet).meteor$setOnGround(true);
            mc.player.setDeltaMovement(0,0,0);
            mc.player.setSpeed(0);
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        BlockPos playerPos = mc.player.blockPosition();
        mc.player.setDeltaMovement(0,0,0);
        mc.player.setSpeed(0);

        BlockPos playerPos1 = mc.player.blockPosition();
        BlockPos pos123 = playerPos1.offset(BlockPos.containing(0,-0.65,0));
        if (mc.level.getBlockState(pos123).canBeReplaced() && akick.get() && delayLeft > 0) delayLeft--;
        if (mc.level.getBlockState(pos123).canBeReplaced() && akick.get() && delayLeft <= 0 && offLeft > 0) {
            offLeft--;

            mc.player.setSpeed(0);
            mc.player.setDeltaMovement(0,0,0);
            mc.player.setPosRaw(mc.player.getX(),mc.player.getY()-0.1,mc.player.getZ());
        } else if (akick.get() && delayLeft <= 0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }

        if (mode.get() == Modes.WASDFly && mc.options.keyUp.isDown()){
            if (mc.player.getMotionDirection() == Direction.NORTH) {
                BlockPos pos12 = playerPos.offset(new Vec3i(0,0,-Range));
                if (mc.level.getBlockState(pos12).canBeReplaced() && mc.level.getBlockState(pos12).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
                }
            }
            if (mc.player.getMotionDirection() == Direction.SOUTH) {
                BlockPos pos13 = playerPos.offset(new Vec3i(0,0,Range));
                if (mc.level.getBlockState(pos13).canBeReplaced() && mc.level.getBlockState(pos13).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
                }
            }
            if (mc.player.getMotionDirection() == Direction.EAST) {
                BlockPos pos14 = playerPos.offset(new Vec3i(Range,0,0));
                if (mc.level.getBlockState(pos14).canBeReplaced() && mc.level.getBlockState(pos14).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMotionDirection() == Direction.WEST) {
                BlockPos pos15 = playerPos.offset(new Vec3i(-Range,0,0));
                if (mc.level.getBlockState(pos15).canBeReplaced() && mc.level.getBlockState(pos15).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
                }
            }
        }

        if (mode.get() != Modes.Normal && mc.options.keyDown.isDown()){
            if (mc.player.getMotionDirection() == Direction.NORTH) {
                BlockPos pos16 = playerPos.offset(new Vec3i(0,0,Range));
                if (mc.level.getBlockState(pos16).canBeReplaced() && mc.level.getBlockState(pos16).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
                }
            }
            if (mc.player.getMotionDirection() == Direction.SOUTH) {
                BlockPos pos17 = playerPos.offset(new Vec3i(0,0,-Range));
                if (mc.level.getBlockState(pos17).canBeReplaced() && mc.level.getBlockState(pos17).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
                }
            }
            if (mc.player.getMotionDirection() == Direction.EAST) {
                BlockPos pos18 = playerPos.offset(new Vec3i(-Range,0,0));
                if (mc.level.getBlockState(pos18).canBeReplaced() && mc.level.getBlockState(pos18).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMotionDirection() == Direction.WEST) {
                BlockPos pos19 = playerPos.offset(new Vec3i(Range,0,0));
                if (mc.level.getBlockState(pos19).canBeReplaced() && mc.level.getBlockState(pos19).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
                }
            }
        }

        if (mode.get() == Modes.WASDFly && mc.options.keyLeft.isDown()){
            if (mc.player.getMotionDirection() == Direction.NORTH) {
                BlockPos pos20 = playerPos.offset(new Vec3i(0,0,-Range));
                if (mc.level.getBlockState(pos20).canBeReplaced() && mc.level.getBlockState(pos20).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMotionDirection() == Direction.SOUTH) {
                BlockPos pos21 = playerPos.offset(new Vec3i(0,0,Range));
                if (mc.level.getBlockState(pos21).canBeReplaced() && mc.level.getBlockState(pos21).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMotionDirection() == Direction.EAST) {
                BlockPos pos22 = playerPos.offset(new Vec3i(Range,0,0));
                if (mc.level.getBlockState(pos22).canBeReplaced() && mc.level.getBlockState(pos22).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
                }
            }
            if (mc.player.getMotionDirection() == Direction.WEST) {
                BlockPos pos23 = playerPos.offset(new Vec3i(-Range,0,0));
                if (mc.level.getBlockState(pos23).canBeReplaced() && mc.level.getBlockState(pos23).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
                }
            }
        }

        if (mode.get() == Modes.WASDFly && mc.options.keyRight.isDown()){
            if (mc.player.getMotionDirection() == Direction.NORTH) {
                BlockPos pos24 = playerPos.offset(new Vec3i(0,0,-Range));
                if (mc.level.getBlockState(pos24).canBeReplaced() && mc.level.getBlockState(pos24).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMotionDirection() == Direction.SOUTH) {
                BlockPos pos25 = playerPos.offset(new Vec3i(0,0,Range));
                if (mc.level.getBlockState(pos25).canBeReplaced() && mc.level.getBlockState(pos25).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMotionDirection() == Direction.EAST) {
                BlockPos pos26 = playerPos.offset(new Vec3i(Range,0,0));
                if (mc.level.getBlockState(pos26).canBeReplaced() && mc.level.getBlockState(pos26).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
                }
            }
            if (mc.player.getMotionDirection() == Direction.WEST) {
                BlockPos pos27 = playerPos.offset(new Vec3i(-Range,0,0));
                if (mc.level.getBlockState(pos27).canBeReplaced() && mc.level.getBlockState(pos27).getBlock() != Blocks.LAVA){
                    mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
                }
            }
        }

        if (mc.options.keyJump.isDown()){
            //attempt to prevent clipping through ceiling
            BlockPos pos6 = playerPos.offset(new Vec3i(0,1,0));
            BlockPos pos7 = playerPos.offset(new Vec3i(0,2,0));
            BlockPos pos8 = playerPos.offset(new Vec3i(0,3,0));
            BlockPos pos9 = playerPos.offset(new Vec3i(0,4,0));
            BlockPos pos10 = playerPos.offset(new Vec3i(0,5,0));
            BlockPos pos11 = playerPos.offset(new Vec3i(0,6,0));
            if (mc.level.getBlockState(pos6).canBeReplaced() && mc.level.getBlockState(pos7).canBeReplaced() && mc.level.getBlockState(pos8).canBeReplaced() && mc.level.getBlockState(pos9).canBeReplaced() && mc.level.getBlockState(pos10).canBeReplaced() && mc.level.getBlockState(pos11).canBeReplaced() && mc.level.getBlockState(pos6).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos7).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos8).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos9).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos10).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos11).getBlock() != Blocks.LAVA){
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY()+upRange,mc.player.getZ());
            }

        } else if (mode.get() == Modes.WASDFly && mc.options.keyJump.isDown() && mc.options.keyDown.isDown()){
            mc.player.setPosRaw(mc.player.getX(),mc.player.getY()+upRange,mc.player.getZ());
            if (mc.player.getMotionDirection() == Direction.NORTH) {
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
            }
            if (mc.player.getMotionDirection() == Direction.SOUTH) {
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
            }
            if (mc.player.getMotionDirection() == Direction.EAST) {
                mc.player.setPosRaw(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMotionDirection() == Direction.WEST) {
                mc.player.setPosRaw(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
            }
        } else if (mode.get() == Modes.WASDFly && mc.options.keyJump.isDown() && mc.options.keyUp.isDown()){
            mc.player.setPosRaw(mc.player.getX(),mc.player.getY()+upRange,mc.player.getZ());
            if (mc.player.getMotionDirection() == Direction.NORTH) {
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
            }
            if (mc.player.getMotionDirection() == Direction.SOUTH) {
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
            }
            if (mc.player.getMotionDirection() == Direction.EAST) {
                mc.player.setPosRaw(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMotionDirection() == Direction.WEST) {
                mc.player.setPosRaw(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
            }
        }

        if (mc.options.keyShift.isDown()){
            //attempt to prevent clipping through ground
            BlockPos pos = playerPos.offset(new Vec3i(0,-1,0));
            BlockPos pos1 = playerPos.offset(new Vec3i(0,-2,0));
            BlockPos pos2 = playerPos.offset(new Vec3i(0,-3,0));
            BlockPos pos3 = playerPos.offset(new Vec3i(0,-4,0));
            BlockPos pos4 = playerPos.offset(new Vec3i(0,-5,0));
            BlockPos pos5 = playerPos.offset(new Vec3i(0,-6,0));
            if (mc.level.getBlockState(pos).canBeReplaced() && mc.level.getBlockState(pos1).canBeReplaced() && mc.level.getBlockState(pos2).canBeReplaced() && mc.level.getBlockState(pos3).canBeReplaced() && mc.level.getBlockState(pos4).canBeReplaced() && mc.level.getBlockState(pos5).canBeReplaced() && mc.level.getBlockState(pos).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos1).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos2).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos3).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos4).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos5).getBlock() != Blocks.LAVA){
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY()-downRange,mc.player.getZ());
            }
        } else if (mode.get() == Modes.WASDFly && mc.options.keyShift.isDown() && mc.options.keyDown.isDown()){
            mc.player.setPosRaw(mc.player.getX(),mc.player.getY()-downRange,mc.player.getZ());
            if (mc.player.getMotionDirection() == Direction.NORTH) {
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
            }
            if (mc.player.getMotionDirection() == Direction.SOUTH) {
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
            }
            if (mc.player.getMotionDirection() == Direction.EAST) {
                mc.player.setPosRaw(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMotionDirection() == Direction.WEST) {
                mc.player.setPosRaw(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
            }
        } else if (mode.get() == Modes.WASDFly && mc.options.keyShift.isDown() && mc.options.keyUp.isDown()){
            mc.player.setPosRaw(mc.player.getX(),mc.player.getY()-downRange,mc.player.getZ());
            if (mc.player.getMotionDirection() == Direction.NORTH) {
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
            }
            if (mc.player.getMotionDirection() == Direction.SOUTH) {
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
            }
            if (mc.player.getMotionDirection() == Direction.EAST) {
                mc.player.setPosRaw(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMotionDirection() == Direction.WEST) {
                mc.player.setPosRaw(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
            }
        }

        if (mode.get() == Modes.Normal && mc.options.keyUp.isDown()) {
            HitResult hitResult = mc.getCameraEntity().pick(Range, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK || hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();

                if (mc.level.getBlockState(pos).useWithoutItem(mc.level, mc.player, (BlockHitResult) hitResult) != InteractionResult.PASS) return;

                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float yaw = mc.player.getYRot();
                double yawRad = Math.toRadians(yaw);
                double teleportRange = Range;

                double newX = playerX - teleportRange * Math.sin(yawRad);
                double newY = playerY; // Keep Y coordinate unchanged
                double newZ = playerZ + teleportRange * Math.cos(yawRad);

                mc.player.setPos(newX, newY, newZ);
            }
        }
        if (mode.get() == Modes.Normal && mc.options.keyDown.isDown()) {
            HitResult hitResult = mc.getCameraEntity().pick(Range, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK || hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();

                if (mc.level.getBlockState(pos).useWithoutItem(mc.level, mc.player, (BlockHitResult) hitResult) != InteractionResult.PASS) return;

                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float yaw = mc.player.getYRot();
                double yawRad = Math.toRadians(yaw);
                double teleportRange = Range;

                double newX = playerX + teleportRange * Math.sin(yawRad);
                double newY = playerY; // Keep Y coordinate unchanged
                double newZ = playerZ - teleportRange * Math.cos(yawRad);

                mc.player.setPos(newX, newY, newZ);
            }
        }
        if (mode.get() == Modes.Normal && mc.options.keyRight.isDown()) {
            HitResult hitResult = mc.getCameraEntity().pick(Range, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK || hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                if (mc.level.getBlockState(pos).useWithoutItem(mc.level, mc.player, (BlockHitResult) hitResult) != InteractionResult.PASS) return;

                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float yaw = mc.player.getYRot();
                double yawRad = Math.toRadians(yaw);
                double teleportRange = Range;

                double newX = playerX - teleportRange * Math.cos(yawRad);
                double newY = playerY; // Keep Y coordinate unchanged
                double newZ = playerZ - teleportRange * Math.sin(yawRad);

                mc.player.setPos(newX, newY, newZ);
            }
        }
        if (mode.get() == Modes.Normal && mc.options.keyLeft.isDown()) {
            HitResult hitResult = mc.getCameraEntity().pick(Range, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK || hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();

                if (mc.level.getBlockState(pos).useWithoutItem(mc.level, mc.player, (BlockHitResult) hitResult) != InteractionResult.PASS) return;

                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float yaw = mc.player.getYRot();
                double yawRad = Math.toRadians(yaw);
                double teleportRange = Range;

                double newX = playerX + teleportRange * Math.cos(yawRad);
                double newY = playerY; // Keep Y coordinate unchanged
                double newZ = playerZ + teleportRange * Math.sin(yawRad);

                mc.player.setPos(newX, newY, newZ);
            }
        }

        if (mode.get() == Modes.Normal && mc.options.keyJump.isDown()) {
            //attempt to prevent clipping through ceiling
            BlockPos pos6 = playerPos.offset(new Vec3i(0,1,0));
            BlockPos pos7 = playerPos.offset(new Vec3i(0,2,0));
            BlockPos pos8 = playerPos.offset(new Vec3i(0,3,0));
            BlockPos pos9 = playerPos.offset(new Vec3i(0,4,0));
            BlockPos pos10 = playerPos.offset(new Vec3i(0,5,0));
            BlockPos pos11 = playerPos.offset(new Vec3i(0,6,0));
            if (mc.level.getBlockState(pos6).canBeReplaced() && mc.level.getBlockState(pos7).canBeReplaced() && mc.level.getBlockState(pos8).canBeReplaced() && mc.level.getBlockState(pos9).canBeReplaced() && mc.level.getBlockState(pos10).canBeReplaced() && mc.level.getBlockState(pos11).canBeReplaced() && mc.level.getBlockState(pos6).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos7).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos8).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos9).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos10).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos11).getBlock() != Blocks.LAVA){
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY()+upRange,mc.player.getZ());
            }
        }
        if (mode.get() == Modes.Normal && mc.options.keyShift.isDown()) {
            //attempt to prevent clipping through ground
            BlockPos pos = playerPos.offset(new Vec3i(0,-1,0));
            BlockPos pos1 = playerPos.offset(new Vec3i(0,-2,0));
            BlockPos pos2 = playerPos.offset(new Vec3i(0,-3,0));
            BlockPos pos3 = playerPos.offset(new Vec3i(0,-4,0));
            BlockPos pos4 = playerPos.offset(new Vec3i(0,-5,0));
            BlockPos pos5 = playerPos.offset(new Vec3i(0,-6,0));
            if (mc.level.getBlockState(pos).canBeReplaced() && mc.level.getBlockState(pos1).canBeReplaced() && mc.level.getBlockState(pos2).canBeReplaced() && mc.level.getBlockState(pos3).canBeReplaced() && mc.level.getBlockState(pos4).canBeReplaced() && mc.level.getBlockState(pos5).canBeReplaced() && mc.level.getBlockState(pos).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos1).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos2).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos3).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos4).getBlock() != Blocks.LAVA && mc.level.getBlockState(pos5).getBlock() != Blocks.LAVA){
                mc.player.setPosRaw(mc.player.getX(),mc.player.getY()-downRange,mc.player.getZ());
            }
        }
    }
    //making absolutely sure there is no velocity and that this is setPos movement only
    @EventHandler
    private void onOffGroundSpeed(OffGroundSpeedEvent event) {
        event.speed = 0;
    }
    public enum Modes {
        Normal, WASDFly
    }
}