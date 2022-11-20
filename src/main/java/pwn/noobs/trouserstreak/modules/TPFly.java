//Written by etianll using some code from MeteorClient ClickTP as well as Airplace
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.BEntityUtils;

public class TPFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("mode")
            .description("the mode")
            .defaultValue(Modes.PointandFly)
            .build());

    private final Setting<Boolean> customRange = sgGeneral.add(new BoolSetting.Builder()
            .name("POINTANDFLY UseRange")
            .description("Use custom range for POINTANDFLY.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("The maximum distance you can teleport.")
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Integer> upspeed = sgGeneral.add(new IntSetting.Builder()
            .name("UpRange")
            .description("UpwardRange")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Integer> downspeed = sgGeneral.add(new IntSetting.Builder()
            .name("DownRange")
            .description("DownwardRange")
            .defaultValue(4)
            .min(0)
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
            .defaultValue(3)
            .range(0, 5000)
            .sliderMax(60)
            .visible(() -> akick.get())
            .build()
    );
    private final Setting<Integer> offTime = sgGeneral.add(new IntSetting.Builder()
            .name("off-time")
            .description("The amount of delay, in ticks, that Flight is toggled off.")
            .defaultValue(15)
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
        mc.player.setPos(mc.player.getX(),mc.player.getY()+0.25,mc.player.getZ());
        } //this line here prevents you dying for realz
        else if (mc.options.sneakKey.isPressed()) {
            mc.options.sneakKey.setPressed(false);
            mc.player.setPos(mc.player.getX(),mc.player.getY()+(downspeed.get()+1),mc.player.getZ());
        } //this line here prevents you dying for realz
    }
    //making absolutely sure there is no velocity and that this is setPos movement only
    @EventHandler
    private void onTick(TickEvent event) {
        mc.player.setVelocity(0,0,0);}
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.player.setVelocity(0,0,0);}
    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.player.setVelocity(0,0,0);
        switch (mc.player.getMovementDirection()) {
            case NORTH -> {}
            case EAST -> {}
            case SOUTH -> {}
            case WEST -> {}
            default -> {}
        }
        if (mode.get() == Modes.WASDFly && mc.options.forwardKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos12 = playerPos.add(new Vec3i(0,0,-range.get()));
                if (mc.world.getBlockState(pos12).isAir()){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
                } else if (!mc.world.getBlockState(pos12).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos13 = playerPos.add(new Vec3i(0,0,range.get()));
                if (mc.world.getBlockState(pos13).isAir()){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
                } else if (!mc.world.getBlockState(pos13).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos14 = playerPos.add(new Vec3i(range.get(),0,0));
                if (mc.world.getBlockState(pos14).isAir()){
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
                } else if (!mc.world.getBlockState(pos14).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos15 = playerPos.add(new Vec3i(-range.get(),0,0));
                if (mc.world.getBlockState(pos15).isAir()){
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
                } else if (!mc.world.getBlockState(pos15).isAir()){}
            }
        }
        if (mc.options.backKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos16 = playerPos.add(new Vec3i(0,0,range.get()));
                if (mc.world.getBlockState(pos16).isAir()){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
                } else if (!mc.world.getBlockState(pos16).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos17 = playerPos.add(new Vec3i(0,0,-range.get()));
                if (mc.world.getBlockState(pos17).isAir()){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
                } else if (!mc.world.getBlockState(pos17).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos18 = playerPos.add(new Vec3i(-range.get(),0,0));
                if (mc.world.getBlockState(pos18).isAir()){
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
                } else if (!mc.world.getBlockState(pos18).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos19 = playerPos.add(new Vec3i(range.get(),0,0));
                if (mc.world.getBlockState(pos19).isAir()){
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
                } else if (!mc.world.getBlockState(pos19).isAir()){}
            }
        }
        if (mc.options.leftKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos20 = playerPos.add(new Vec3i(0,0,-range.get()));
                if (mc.world.getBlockState(pos20).isAir()){
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
                } else if (!mc.world.getBlockState(pos20).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos21 = playerPos.add(new Vec3i(0,0,range.get()));
                if (mc.world.getBlockState(pos21).isAir()){
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
                } else if (!mc.world.getBlockState(pos21).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos22 = playerPos.add(new Vec3i(range.get(),0,0));
                if (mc.world.getBlockState(pos22).isAir()){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
                } else if (!mc.world.getBlockState(pos22).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos23 = playerPos.add(new Vec3i(-range.get(),0,0));
                if (mc.world.getBlockState(pos23).isAir()){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
                } else if (!mc.world.getBlockState(pos23).isAir()){}
            }
        }


        if (mc.options.rightKey.isPressed()){
            if (mc.player.getMovementDirection() == Direction.NORTH) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos24 = playerPos.add(new Vec3i(0,0,-range.get()));
                if (mc.world.getBlockState(pos24).isAir()){
                mc.player.setPos(mc.player.getX()+range.get(),mc.player.getY(),mc.player.getZ());
                } else if (!mc.world.getBlockState(pos24).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.SOUTH) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos25 = playerPos.add(new Vec3i(0,0,range.get()));
                if (mc.world.getBlockState(pos25).isAir()){
                mc.player.setPos(mc.player.getX()-range.get(),mc.player.getY(),mc.player.getZ());
                } else if (!mc.world.getBlockState(pos25).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.EAST) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos26 = playerPos.add(new Vec3i(range.get(),0,0));
                if (mc.world.getBlockState(pos26).isAir()){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+range.get());
                } else if (!mc.world.getBlockState(pos26).isAir()){}
            }
            if (mc.player.getMovementDirection() == Direction.WEST) {
                BlockPos playerPos = BEntityUtils.playerPos(mc.player);
                BlockPos pos27 = playerPos.add(new Vec3i(-range.get(),0,0));
                if (mc.world.getBlockState(pos27).isAir()){
                mc.player.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()-range.get());
                } else if (!mc.world.getBlockState(pos27).isAir()){}
            }
        }


        if (mc.options.jumpKey.isPressed()){
            //attempt to prevent clipping through ceiling
            BlockPos playerPos = BEntityUtils.playerPos(mc.player);
            BlockPos pos6 = playerPos.add(new Vec3i(0,1,0));
            BlockPos pos7 = playerPos.add(new Vec3i(0,2,0));
            BlockPos pos8 = playerPos.add(new Vec3i(0,3,0));
            BlockPos pos9 = playerPos.add(new Vec3i(0,4,0));
            BlockPos pos10 = playerPos.add(new Vec3i(0,5,0));
            BlockPos pos11 = playerPos.add(new Vec3i(0,6,0));
            if (mc.world.getBlockState(pos6).isAir() && mc.world.getBlockState(pos7).isAir() && mc.world.getBlockState(pos8).isAir() && mc.world.getBlockState(pos9).isAir() && mc.world.getBlockState(pos10).isAir() && mc.world.getBlockState(pos11).isAir()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()+upspeed.get(),mc.player.getZ());}
            else if (!mc.world.getBlockState(pos6).isAir() && mc.world.getBlockState(pos7).isAir() && mc.world.getBlockState(pos8).isAir() && mc.world.getBlockState(pos9).isAir() && mc.world.getBlockState(pos10).isAir() && mc.world.getBlockState(pos11).isAir())
            {}

        }
        else if (mc.options.jumpKey.isPressed() && mc.options.backKey.isPressed()){
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
        }
        else if (mode.get() == Modes.WASDFly && mc.options.jumpKey.isPressed() && mc.options.forwardKey.isPressed()){
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
            BlockPos playerPos = BEntityUtils.playerPos(mc.player);
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            BlockPos pos1 = playerPos.add(new Vec3i(0,-2,0));
            BlockPos pos2 = playerPos.add(new Vec3i(0,-3,0));
            BlockPos pos3 = playerPos.add(new Vec3i(0,-4,0));
            BlockPos pos4 = playerPos.add(new Vec3i(0,-5,0));
            BlockPos pos5 = playerPos.add(new Vec3i(0,-6,0));
            if (mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos1).isAir() && mc.world.getBlockState(pos2).isAir() && mc.world.getBlockState(pos3).isAir() && mc.world.getBlockState(pos4).isAir() && mc.world.getBlockState(pos5).isAir()){
            mc.player.setPos(mc.player.getX(),mc.player.getY()-downspeed.get(),mc.player.getZ());
            }
            else if (!mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos1).isAir() && mc.world.getBlockState(pos2).isAir() && mc.world.getBlockState(pos3).isAir() && mc.world.getBlockState(pos4).isAir() && mc.world.getBlockState(pos5).isAir()){}
        }
        else if (mc.options.sneakKey.isPressed() && mc.options.backKey.isPressed()){
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
        }
        else if (mode.get() == Modes.WASDFly && mc.options.sneakKey.isPressed() && mc.options.forwardKey.isPressed()){
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
            double r = customRange.get() ? range.get() : mc.interactionManager.getReachDistance();
            HitResult hitResult = mc.getCameraEntity().raycast(r, 0, false);

            if (hitResult.getType() == HitResult.Type.ENTITY && mc.player.interact(((EntityHitResult) hitResult).getEntity(), Hand.MAIN_HAND) != ActionResult.PASS) return;

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
        if (mode.get() == Modes.PointandFly && mc.options.forwardKey.isPressed() && mc.options.jumpKey.isPressed()) {
            double r = customRange.get() ? range.get() : mc.interactionManager.getReachDistance();
            HitResult hitResult = mc.getCameraEntity().raycast(r, 0, false);

            if (hitResult.getType() == HitResult.Type.ENTITY && mc.player.interact(((EntityHitResult) hitResult).getEntity(), Hand.MAIN_HAND) != ActionResult.PASS) return;

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                Direction side = ((BlockHitResult) hitResult).getSide();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, Hand.MAIN_HAND, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                BlockState state = mc.world.getBlockState(pos);

                VoxelShape shape = state.getCollisionShape(mc.world, pos);
                if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

                double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

                mc.player.setPosition(pos.getX() + 0.5 + side.getOffsetX(), pos.getY() + upspeed.get(), pos.getZ() + 0.5 + side.getOffsetZ());
            }
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                Direction side = ((BlockHitResult) hitResult).getSide();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, Hand.MAIN_HAND, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                BlockState state = mc.world.getBlockState(pos);

                VoxelShape shape = state.getCollisionShape(mc.world, pos);
                if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

                double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

                mc.player.setPosition(pos.getX() + 0.5 + side.getOffsetX(), pos.getY() + upspeed.get(), pos.getZ() + 0.5 + side.getOffsetZ());
            }
        }
        if (mode.get() == Modes.PointandFly && mc.options.forwardKey.isPressed() && mc.options.sneakKey.isPressed()) {
            double r = customRange.get() ? range.get() : mc.interactionManager.getReachDistance();
            HitResult hitResult = mc.getCameraEntity().raycast(r, 0, false);

            if (hitResult.getType() == HitResult.Type.ENTITY && mc.player.interact(((EntityHitResult) hitResult).getEntity(), Hand.MAIN_HAND) != ActionResult.PASS) return;

            if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                Direction side = ((BlockHitResult) hitResult).getSide();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, Hand.MAIN_HAND, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                BlockState state = mc.world.getBlockState(pos);

                VoxelShape shape = state.getCollisionShape(mc.world, pos);
                if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

                double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

                mc.player.setPosition(pos.getX() + 0.5 + side.getOffsetX(), pos.getY() - downspeed.get(), pos.getZ() + 0.5 + side.getOffsetZ());
            }
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                Direction side = ((BlockHitResult) hitResult).getSide();

                if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, Hand.MAIN_HAND, (BlockHitResult) hitResult) != ActionResult.PASS) return;

                BlockState state = mc.world.getBlockState(pos);

                VoxelShape shape = state.getCollisionShape(mc.world, pos);
                if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

                double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

                mc.player.setPosition(pos.getX() + 0.5 + side.getOffsetX(), pos.getY() - downspeed.get(), pos.getZ() + 0.5 + side.getOffsetZ());
            }
        }
        if (akick.get() && delayLeft > 0) delayLeft--;

        else if (akick.get() && delayLeft <= 0 && offLeft > 0) {
            offLeft--;
            BlockPos playerPos = BEntityUtils.playerPos(mc.player);
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            if (mc.world.getBlockState(pos).isAir())
                mc.player.setPos(mc.player.getX(),mc.player.getY()-0.1,mc.player.getZ());
        } else if (akick.get() && delayLeft <= 0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }
    }
    public enum Modes {
        PointandFly, WASDFly
    }
}
