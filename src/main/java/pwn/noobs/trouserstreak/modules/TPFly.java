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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.events.OffGroundSpeedEvent;

public class TPFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("mode")
            .description("the mode")
            .defaultValue(Modes.WASDFly)
            .build());
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("The maximum distance you can teleport.")
            .defaultValue(5)
            .min(1)
            .sliderMax(7)
            .build()
    );
    private final Setting<Integer> upspeed = sgGeneral.add(new IntSetting.Builder()
            .name("UpRange")
            .description("UpwardRange")
            .defaultValue(4)
            .min(1)
            .sliderMax(6)
            .build()
    );
    private final Setting<Integer> downspeed = sgGeneral.add(new IntSetting.Builder()
            .name("DownRange")
            .description("DownwardRange")
            .defaultValue(4)
            .min(1)
            .sliderMax(6)
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
            .visible(() -> akick.get())
            .build()
    );
    private final Setting<Integer> offTime = sgGeneral.add(new IntSetting.Builder()
            .name("off-time")
            .description("The amount of delay, in ticks, that TPFly is toggled off.")
            .defaultValue(3)
            .sliderRange(0, 200)
            .visible(() -> akick.get())
            .build()
    );

    public TPFly() {
        super(Trouser.Main, "TPFly", "Teleports you to flyyyyyyyyy!");
    }
    private int delayLeft = delay.get();
    private int offLeft = offTime.get();
    @Override
    public void onDeactivate() {
        mc.player.setVelocity(0,0.01,0);
        if (!mc.options.sneakKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()+0.1,mc.player.getZ());
        } //this line here prevents you dying for realz
        else if (mc.options.sneakKey.isPressed()) {
            mc.options.sneakKey.setPressed(false);
            mc.player.setPos(mc.player.getX(),mc.player.getY()+(downspeed.get()+1),mc.player.getZ());
        } //this line here prevents you dying for realz
    }
    //making absolutely sure there is no velocity and that this is setPos movement only
    @EventHandler
    private void onKeyEvent(KeyEvent event) {
        if (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed() || mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()){
            mc.player.setVelocity(0,0,0);
            mc.player.setMovementSpeed(0);
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        mc.player.setVelocity(0,0,0);
        mc.player.setMovementSpeed(0);
    }
    @EventHandler
    private void onTick(TickEvent event) {
        mc.player.setVelocity(0,0,0);
        mc.player.setMovementSpeed(0);
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.player.setVelocity(0,0,0);
        mc.player.setMovementSpeed(0);
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket)
            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        BlockPos playerPos = mc.player.getBlockPos();
        mc.player.setVelocity(0,0,0);
        mc.player.setMovementSpeed(0);
        if (mode.get() == Modes.WASDFly && mc.options.forwardKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos pos12 = playerPos.add(new Vec3i(0,0,-range.get()));
                if (!mc.world.getBlockState(pos12).getMaterial().isSolid() && mc.world.getBlockState(pos12).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
                }
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos pos13 = playerPos.add(new Vec3i(0,0,range.get()));
                if (!mc.world.getBlockState(pos13).getMaterial().isSolid() && mc.world.getBlockState(pos13).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
                }
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos pos14 = playerPos.add(new Vec3i(range.get(),0,0));
                if (!mc.world.getBlockState(pos14).getMaterial().isSolid() && mc.world.getBlockState(pos14).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos pos15 = playerPos.add(new Vec3i(-range.get(),0,0));
                if (!mc.world.getBlockState(pos15).getMaterial().isSolid() && mc.world.getBlockState(pos15).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
                }
            }
        }

        if (mc.options.backKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos pos16 = playerPos.add(new Vec3i(0,0,range.get()));
                if (!mc.world.getBlockState(pos16).getMaterial().isSolid() && mc.world.getBlockState(pos16).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
                }
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos pos17 = playerPos.add(new Vec3i(0,0,-range.get()));
                if (!mc.world.getBlockState(pos17).getMaterial().isSolid() && mc.world.getBlockState(pos17).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
                }
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos pos18 = playerPos.add(new Vec3i(-range.get(),0,0));
                if (!mc.world.getBlockState(pos18).getMaterial().isSolid() && mc.world.getBlockState(pos18).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos pos19 = playerPos.add(new Vec3i(range.get(),0,0));
                if (!mc.world.getBlockState(pos19).getMaterial().isSolid() && mc.world.getBlockState(pos19).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
                }
            }
        }

        if (mc.options.leftKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos pos20 = playerPos.add(new Vec3i(0,0,-range.get()));
                if (!mc.world.getBlockState(pos20).getMaterial().isSolid() && mc.world.getBlockState(pos20).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos pos21 = playerPos.add(new Vec3i(0,0,range.get()));
                if (!mc.world.getBlockState(pos21).getMaterial().isSolid() && mc.world.getBlockState(pos21).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos pos22 = playerPos.add(new Vec3i(range.get(),0,0));
                if (!mc.world.getBlockState(pos22).getMaterial().isSolid() && mc.world.getBlockState(pos22).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
                }
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos pos23 = playerPos.add(new Vec3i(-range.get(),0,0));
                if (!mc.world.getBlockState(pos23).getMaterial().isSolid() && mc.world.getBlockState(pos23).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
                }
            }
        }

        if (mc.options.rightKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos pos24 = playerPos.add(new Vec3i(0,0,-range.get()));
                if (!mc.world.getBlockState(pos24).getMaterial().isSolid() && mc.world.getBlockState(pos24).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos pos25 = playerPos.add(new Vec3i(0,0,range.get()));
                if (!mc.world.getBlockState(pos25).getMaterial().isSolid() && mc.world.getBlockState(pos25).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
                }
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos pos26 = playerPos.add(new Vec3i(range.get(),0,0));
                if (!mc.world.getBlockState(pos26).getMaterial().isSolid() && mc.world.getBlockState(pos26).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
                }
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos pos27 = playerPos.add(new Vec3i(-range.get(),0,0));
                if (!mc.world.getBlockState(pos27).getMaterial().isSolid() && mc.world.getBlockState(pos27).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
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
            if (!mc.world.getBlockState(pos6).getMaterial().isSolid() && !mc.world.getBlockState(pos7).getMaterial().isSolid() && !mc.world.getBlockState(pos8).getMaterial().isSolid() && !mc.world.getBlockState(pos9).getMaterial().isSolid() && !mc.world.getBlockState(pos10).getMaterial().isSolid() && !mc.world.getBlockState(pos11).getMaterial().isSolid() && mc.world.getBlockState(pos6).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos7).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos8).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos9).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos10).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos11).getBlock() != Blocks.LAVA){
            mc.player.setPos(mc.player.getX(),mc.player.getY()+upspeed.get(),mc.player.getZ());
            }

        } else if (mc.options.jumpKey.isPressed() && mc.options.backKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()+upspeed.get(),mc.player.getZ());
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
            }
        } else if (mode.get() == Modes.WASDFly && mc.options.jumpKey.isPressed() && mc.options.forwardKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()+upspeed.get(),mc.player.getZ());
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
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
            if (!mc.world.getBlockState(pos).getMaterial().isSolid() && !mc.world.getBlockState(pos1).getMaterial().isSolid() && !mc.world.getBlockState(pos2).getMaterial().isSolid() && !mc.world.getBlockState(pos3).getMaterial().isSolid() && !mc.world.getBlockState(pos4).getMaterial().isSolid() && !mc.world.getBlockState(pos5).getMaterial().isSolid() && mc.world.getBlockState(pos).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos1).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos2).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos3).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos4).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos5).getBlock() != Blocks.LAVA){
            mc.player.setPos(mc.player.getX(),mc.player.getY()-downspeed.get(),mc.player.getZ());
            }
        } else if (mc.options.sneakKey.isPressed() && mc.options.backKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()-downspeed.get(),mc.player.getZ());
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
            }
        } else if (mode.get() == Modes.WASDFly && mc.options.sneakKey.isPressed() && mc.options.forwardKey.isPressed()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()-downspeed.get(),mc.player.getZ());
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
            }
        }

        if (mode.get() == Modes.PointandFly && mc.options.forwardKey.isPressed()) {
            HitResult hitResult = mc.getCameraEntity().raycast(range.get(), 0, false);

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                Direction side = ((BlockHitResult) hitResult).getSide();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, Hand.MAIN_HAND, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                BlockState state = mc.world.getBlockState(pos);

                VoxelShape shape = state.getCollisionShape(mc.world, pos);
                if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

                double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

                mc.player.setPosition(pos.getX() + 0.5 + side.getOffsetX(), pos.getY() + height, pos.getZ() + 0.5 + side.getOffsetZ());
            }
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                Direction side = ((BlockHitResult) hitResult).getSide();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, Hand.MAIN_HAND, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                BlockState state = mc.world.getBlockState(pos);

                VoxelShape shape = state.getCollisionShape(mc.world, pos);
                if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

                double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

                mc.player.setPosition(pos.getX() + 0.5 + side.getOffsetX(), pos.getY() + height, pos.getZ() + 0.5 + side.getOffsetZ());
            }
        }
        if (mode.get() == Modes.PointandFly && mc.options.jumpKey.isPressed()) {
            //attempt to prevent clipping through ceiling
            BlockPos pos6 = playerPos.add(new Vec3i(0,1,0));
            BlockPos pos7 = playerPos.add(new Vec3i(0,2,0));
            BlockPos pos8 = playerPos.add(new Vec3i(0,3,0));
            BlockPos pos9 = playerPos.add(new Vec3i(0,4,0));
            BlockPos pos10 = playerPos.add(new Vec3i(0,5,0));
            BlockPos pos11 = playerPos.add(new Vec3i(0,6,0));
            if (!mc.world.getBlockState(pos6).getMaterial().isSolid() && !mc.world.getBlockState(pos7).getMaterial().isSolid() && !mc.world.getBlockState(pos8).getMaterial().isSolid() && !mc.world.getBlockState(pos9).getMaterial().isSolid() && !mc.world.getBlockState(pos10).getMaterial().isSolid() && !mc.world.getBlockState(pos11).getMaterial().isSolid() && mc.world.getBlockState(pos6).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos7).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos8).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos9).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos10).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos11).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY()+upspeed.get(),mc.player.getZ());
            }
        }
        if (mode.get() == Modes.PointandFly && mc.options.sneakKey.isPressed()) {
            //attempt to prevent clipping through ground
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            BlockPos pos1 = playerPos.add(new Vec3i(0,-2,0));
            BlockPos pos2 = playerPos.add(new Vec3i(0,-3,0));
            BlockPos pos3 = playerPos.add(new Vec3i(0,-4,0));
            BlockPos pos4 = playerPos.add(new Vec3i(0,-5,0));
            BlockPos pos5 = playerPos.add(new Vec3i(0,-6,0));
            if (!mc.world.getBlockState(pos).getMaterial().isSolid() && !mc.world.getBlockState(pos1).getMaterial().isSolid() && !mc.world.getBlockState(pos2).getMaterial().isSolid() && !mc.world.getBlockState(pos3).getMaterial().isSolid() && !mc.world.getBlockState(pos4).getMaterial().isSolid() && !mc.world.getBlockState(pos5).getMaterial().isSolid() && mc.world.getBlockState(pos).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos1).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos2).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos3).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos4).getBlock() != Blocks.LAVA && mc.world.getBlockState(pos5).getBlock() != Blocks.LAVA){
                mc.player.setPos(mc.player.getX(),mc.player.getY()-downspeed.get(),mc.player.getZ());
            }
        }
        if (akick.get() && delayLeft > 0) delayLeft--;
        BlockPos playerPos1 = mc.player.getBlockPos();
        BlockPos pos1 = playerPos1.add(BlockPos.ofFloored(0,-0.65,0));
        if (!mc.world.getBlockState(pos1).isAir()){
            mc.player.setMovementSpeed(0);
            mc.player.setVelocity(0,0,0);
        }
        else if (akick.get() && delayLeft <= 0 && offLeft > 0) {
            offLeft--;
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            if (mc.world.getBlockState(pos).isAir())
                mc.player.setMovementSpeed(0);
                mc.player.setVelocity(0,0,0);
                mc.player.setPos(mc.player.getX(),mc.player.getY()-0.1,mc.player.getZ());
        } else if (akick.get() && delayLeft <= 0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }
    }
    //making absolutely sure there is no velocity and that this is setPos movement only
    @EventHandler
    private void onOffGroundSpeed(OffGroundSpeedEvent event) {
        event.speed = 0;
    }
    public enum Modes {
        PointandFly, WASDFly
    }
}