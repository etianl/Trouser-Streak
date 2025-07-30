//Written by etianll using some code from MeteorClient ClickTP as well as Airplace
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
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
        mc.player.setVelocity(0,0.01,0);
        if (!mc.options.sneakKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()+0.1,mc.player.getZ());
        } //this line here prevents you dying for realz
        else if (mc.options.sneakKey.isPressed()) {
            mc.options.sneakKey.setPressed(false);
            mc.player.setPos(mc.player.getX(),mc.player.getY()+(downrange.get()+1),mc.player.getZ());
        } //this line here prevents you dying for realz
    }
    //making absolutely sure there is no velocity and that this is setPos movement only
    @EventHandler
    private void onKeyEvent(KeyEvent event) {
        if (mc.player != null && mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed() || mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()){
            mc.player.setVelocity(0,0,0);
            mc.player.setMovementSpeed(0);
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        if (mc.player == null) return;
        mc.player.setVelocity(0,0,0);
        mc.player.setMovementSpeed(0);
    }
    @EventHandler
    private void onTick(TickEvent event) {
        if (mc.player == null) return;
        mc.player.setVelocity(0,0,0);
        mc.player.setMovementSpeed(0);
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        mc.player.setVelocity(0,0,0);
        mc.player.setMovementSpeed(0);
        if (Acceleration.get() && Range < range.get() && (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.sneakKey.isPressed() || mc.options.jumpKey.isPressed())){
            Range=Math.min(range.get(), Range + acceleration.get());
        } else if (Acceleration.get() && !mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed() && !mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed() && !mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()){
            Range=-1;
        } else if (!Acceleration.get()) Range=range.get();

        if (Acceleration.get() && upRange < uprange.get() && (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.sneakKey.isPressed() || mc.options.jumpKey.isPressed())){
            upRange=Math.min(uprange.get(), upRange + upacceleration.get());
        } else if (Acceleration.get() && !mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed() && !mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed() && !mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()){
            upRange=-1;
        } else if (!Acceleration.get()) upRange=uprange.get();

        if (Acceleration.get() && downRange < downrange.get() && (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.sneakKey.isPressed() || mc.options.jumpKey.isPressed())){
            downRange=Math.min(downrange.get(), downRange + downacceleration.get());
        } else if (Acceleration.get() && !mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed() && !mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed() && !mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()){
            downRange=-1;
        } else if (!Acceleration.get()) downRange=downrange.get();
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket && mc.player != null){
            ((PlayerMoveC2SPacketAccessor) event.packet).meteor$setOnGround(true);
            mc.player.setVelocity(0,0,0);
            mc.player.setMovementSpeed(0);
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        BlockPos playerPos = mc.player.getBlockPos();
        mc.player.setVelocity(0,0,0);
        mc.player.setMovementSpeed(0);

        BlockPos playerPos1 = mc.player.getBlockPos();
        BlockPos pos123 = playerPos1.add(BlockPos.ofFloored(0,-0.65,0));
        if (mc.world.getBlockState(pos123).isReplaceable() && akick.get() && delayLeft > 0) delayLeft--;
        if (mc.world.getBlockState(pos123).isReplaceable() && akick.get() && delayLeft <= 0 && offLeft > 0) {
            offLeft--;

            mc.player.setMovementSpeed(0);
            mc.player.setVelocity(0,0,0);
            mc.player.setPos(mc.player.getX(),mc.player.getY()-0.1,mc.player.getZ());
        } else if (akick.get() && delayLeft <= 0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }

        if (mode.get() == Modes.WASDFly && mc.options.forwardKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos pos12 = playerPos.add(new Vec3i(0,0,-Range));
                if (mc.world.getBlockState(pos12).isReplaceable() && mc.world.getBlockState(pos12).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
                }
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos pos13 = playerPos.add(new Vec3i(0,0,Range));
                if (mc.world.getBlockState(pos13).isReplaceable() && mc.world.getBlockState(pos13).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
                }
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos pos14 = playerPos.add(new Vec3i(Range,0,0));
                if (mc.world.getBlockState(pos14).isReplaceable() && mc.world.getBlockState(pos14).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos pos15 = playerPos.add(new Vec3i(-Range,0,0));
                if (mc.world.getBlockState(pos15).isReplaceable() && mc.world.getBlockState(pos15).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
                }
            }
        }

        if (mode.get() != Modes.Normal && mc.options.backKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos pos16 = playerPos.add(new Vec3i(0,0,Range));
                if (mc.world.getBlockState(pos16).isReplaceable() && mc.world.getBlockState(pos16).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
                }
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos pos17 = playerPos.add(new Vec3i(0,0,-Range));
                if (mc.world.getBlockState(pos17).isReplaceable() && mc.world.getBlockState(pos17).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
                }
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos pos18 = playerPos.add(new Vec3i(-Range,0,0));
                if (mc.world.getBlockState(pos18).isReplaceable() && mc.world.getBlockState(pos18).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos pos19 = playerPos.add(new Vec3i(Range,0,0));
                if (mc.world.getBlockState(pos19).isReplaceable() && mc.world.getBlockState(pos19).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
                }
            }
        }

        if (mode.get() == Modes.WASDFly && mc.options.leftKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos pos20 = playerPos.add(new Vec3i(0,0,-Range));
                if (mc.world.getBlockState(pos20).isReplaceable() && mc.world.getBlockState(pos20).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos pos21 = playerPos.add(new Vec3i(0,0,Range));
                if (mc.world.getBlockState(pos21).isReplaceable() && mc.world.getBlockState(pos21).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos pos22 = playerPos.add(new Vec3i(Range,0,0));
                if (mc.world.getBlockState(pos22).isReplaceable() && mc.world.getBlockState(pos22).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
                }
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos pos23 = playerPos.add(new Vec3i(-Range,0,0));
                if (mc.world.getBlockState(pos23).isReplaceable() && mc.world.getBlockState(pos23).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
                }
            }
        }

        if (mode.get() == Modes.WASDFly && mc.options.rightKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos pos24 = playerPos.add(new Vec3i(0,0,-Range));
                if (mc.world.getBlockState(pos24).isReplaceable() && mc.world.getBlockState(pos24).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos pos25 = playerPos.add(new Vec3i(0,0,Range));
                if (mc.world.getBlockState(pos25).isReplaceable() && mc.world.getBlockState(pos25).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos pos26 = playerPos.add(new Vec3i(Range,0,0));
                if (mc.world.getBlockState(pos26).isReplaceable() && mc.world.getBlockState(pos26).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
                }
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos pos27 = playerPos.add(new Vec3i(-Range,0,0));
                if (mc.world.getBlockState(pos27).isReplaceable() && mc.world.getBlockState(pos27).getBlock() != Blocks.LAVA){
                    mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
                }
            }
        }

        if (mc.options.jumpKey.isPressed()){
            //attempt to prevent clipping through ceiling
            BlockPos pos6 = playerPos.add(new Vec3i(0,1,0));
            BlockPos pos7 = playerPos.add(new Vec3i(0,2,0));
            BlockPos pos8 = playerPos.add(new Vec3i(0,3,0));
            BlockPos pos9 = playerPos.add(new Vec3i(0,4,0));
            BlockPos pos10 = playerPos.add(new Vec3i(0,5,0));
            BlockPos pos11 = playerPos.add(new Vec3i(0,6,0));
            if (mc.world.getBlockState(pos6).isReplaceable() && mc.world.getBlockState(pos7).isReplaceable() && mc.world.getBlockState(pos8).isReplaceable() && mc.world.getBlockState(pos9).isReplaceable() && mc.world.getBlockState(pos10).isReplaceable() && mc.world.getBlockState(pos11).isReplaceable() && mc.world.getBlockState(pos6).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos7).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos8).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos9).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos10).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos11).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY()+upRange,mc.player.getZ());
            }

        } else if (mode.get() == Modes.WASDFly && mc.options.jumpKey.isPressed() && mc.options.backKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()+upRange,mc.player.getZ());
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                mc.player.setPos(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                mc.player.setPos(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
            }
        } else if (mode.get() == Modes.WASDFly && mc.options.jumpKey.isPressed() && mc.options.forwardKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()+upRange,mc.player.getZ());
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                mc.player.setPos(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                mc.player.setPos(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
            }
        }

        if (mc.options.sneakKey.isPressed()){
            //attempt to prevent clipping through ground
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            BlockPos pos1 = playerPos.add(new Vec3i(0,-2,0));
            BlockPos pos2 = playerPos.add(new Vec3i(0,-3,0));
            BlockPos pos3 = playerPos.add(new Vec3i(0,-4,0));
            BlockPos pos4 = playerPos.add(new Vec3i(0,-5,0));
            BlockPos pos5 = playerPos.add(new Vec3i(0,-6,0));
            if (mc.world.getBlockState(pos).isReplaceable() && mc.world.getBlockState(pos1).isReplaceable() && mc.world.getBlockState(pos2).isReplaceable() && mc.world.getBlockState(pos3).isReplaceable() && mc.world.getBlockState(pos4).isReplaceable() && mc.world.getBlockState(pos5).isReplaceable() && mc.world.getBlockState(pos).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos1).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos2).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos3).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos4).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos5).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY()-downRange,mc.player.getZ());
            }
        } else if (mode.get() == Modes.WASDFly && mc.options.sneakKey.isPressed() && mc.options.backKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()-downRange,mc.player.getZ());
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                mc.player.setPos(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                mc.player.setPos(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
            }
        } else if (mode.get() == Modes.WASDFly && mc.options.sneakKey.isPressed() && mc.options.forwardKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()-downRange,mc.player.getZ());
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-Range);
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+Range);
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                mc.player.setPos(mc.player.getX()+Range,mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                mc.player.setPos(mc.player.getX()-Range,mc.player.getY(),mc.player.getZ());
            }
        }

        if (mode.get() == Modes.Normal && mc.options.forwardKey.isPressed()) {
            HitResult hitResult = mc.getCameraEntity().raycast(Range, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK || hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float yaw = mc.player.getYaw();
                double yawRad = Math.toRadians(yaw);
                double teleportRange = Range;

                double newX = playerX - teleportRange * Math.sin(yawRad);
                double newY = playerY; // Keep Y coordinate unchanged
                double newZ = playerZ + teleportRange * Math.cos(yawRad);

                mc.player.setPosition(newX, newY, newZ);
            }
        }
        if (mode.get() == Modes.Normal && mc.options.backKey.isPressed()) {
            HitResult hitResult = mc.getCameraEntity().raycast(Range, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK || hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float yaw = mc.player.getYaw();
                double yawRad = Math.toRadians(yaw);
                double teleportRange = Range;

                double newX = playerX + teleportRange * Math.sin(yawRad);
                double newY = playerY; // Keep Y coordinate unchanged
                double newZ = playerZ - teleportRange * Math.cos(yawRad);

                mc.player.setPosition(newX, newY, newZ);
            }
        }
        if (mode.get() == Modes.Normal && mc.options.rightKey.isPressed()) {
            HitResult hitResult = mc.getCameraEntity().raycast(Range, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK || hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float yaw = mc.player.getYaw();
                double yawRad = Math.toRadians(yaw);
                double teleportRange = Range;

                double newX = playerX - teleportRange * Math.cos(yawRad);
                double newY = playerY; // Keep Y coordinate unchanged
                double newZ = playerZ - teleportRange * Math.sin(yawRad);

                mc.player.setPosition(newX, newY, newZ);
            }
        }
        if (mode.get() == Modes.Normal && mc.options.leftKey.isPressed()) {
            HitResult hitResult = mc.getCameraEntity().raycast(Range, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK || hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                float yaw = mc.player.getYaw();
                double yawRad = Math.toRadians(yaw);
                double teleportRange = Range;

                double newX = playerX + teleportRange * Math.cos(yawRad);
                double newY = playerY; // Keep Y coordinate unchanged
                double newZ = playerZ + teleportRange * Math.sin(yawRad);

                mc.player.setPosition(newX, newY, newZ);
            }
        }

        if (mode.get() == Modes.Normal && mc.options.jumpKey.isPressed()) {
            //attempt to prevent clipping through ceiling
            BlockPos pos6 = playerPos.add(new Vec3i(0,1,0));
            BlockPos pos7 = playerPos.add(new Vec3i(0,2,0));
            BlockPos pos8 = playerPos.add(new Vec3i(0,3,0));
            BlockPos pos9 = playerPos.add(new Vec3i(0,4,0));
            BlockPos pos10 = playerPos.add(new Vec3i(0,5,0));
            BlockPos pos11 = playerPos.add(new Vec3i(0,6,0));
            if (mc.world.getBlockState(pos6).isReplaceable() && mc.world.getBlockState(pos7).isReplaceable() && mc.world.getBlockState(pos8).isReplaceable() && mc.world.getBlockState(pos9).isReplaceable() && mc.world.getBlockState(pos10).isReplaceable() && mc.world.getBlockState(pos11).isReplaceable() && mc.world.getBlockState(pos6).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos7).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos8).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos9).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos10).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos11).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY()+upRange,mc.player.getZ());
            }
        }
        if (mode.get() == Modes.Normal && mc.options.sneakKey.isPressed()) {
            //attempt to prevent clipping through ground
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            BlockPos pos1 = playerPos.add(new Vec3i(0,-2,0));
            BlockPos pos2 = playerPos.add(new Vec3i(0,-3,0));
            BlockPos pos3 = playerPos.add(new Vec3i(0,-4,0));
            BlockPos pos4 = playerPos.add(new Vec3i(0,-5,0));
            BlockPos pos5 = playerPos.add(new Vec3i(0,-6,0));
            if (mc.world.getBlockState(pos).isReplaceable() && mc.world.getBlockState(pos1).isReplaceable() && mc.world.getBlockState(pos2).isReplaceable() && mc.world.getBlockState(pos3).isReplaceable() && mc.world.getBlockState(pos4).isReplaceable() && mc.world.getBlockState(pos5).isReplaceable() && mc.world.getBlockState(pos).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos1).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos2).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos3).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos4).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos5).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY()-downRange,mc.player.getZ());
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