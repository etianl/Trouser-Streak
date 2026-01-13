package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.LivingEntityMoveEvent;
import net.minecraft.block.AbstractBlock;
import pwn.noobs.trouserstreak.Trouser;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public class BoatNoclip extends Module {
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgFlight = settings.createGroup("Flight");

    private final Setting<Boolean> speed = sgSpeed.add(new BoolSetting.Builder()
            .name("speed")
            .defaultValue(true)
            .build()
    );

    private final SettingGroup warning = settings.createGroup("Horizontal speeds over 5-6 will rubberband inside blocks.");

    private final Setting<Double> horizontalSpeed = sgSpeed.add(new DoubleSetting.Builder()
            .name("horizontal-speed")
            .defaultValue(10)
            .min(0)
            .sliderMax(50)
            .visible(speed::get)
            .build()
    );

    private final Setting<Double> horizontalSpeedInsideBlocks = sgSpeed.add(new DoubleSetting.Builder()
            .name("horizontal-speed-inside-blocks")
            .defaultValue(5)
            .min(0)
            .sliderMax(50)
            .visible(speed::get)
            .build()
    );

    private final Setting<Double> verticalSpeed = sgSpeed.add(new DoubleSetting.Builder()
            .name("vertical-speed")
            .defaultValue(6)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Double> fallSpeed = sgFlight.add(new DoubleSetting.Builder()
            .name("fall-speed")
            .defaultValue(0)
            .min(0)
            .build()
    );

    private final Setting<Boolean> antiKick = sgFlight.add(new BoolSetting.Builder()
            .name("anti-fly-kick")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> delay = sgFlight.add(new IntSetting.Builder()
            .name("delay")
            .defaultValue(40)
            .min(1)
            .sliderMax(80)
            .visible(antiKick::get)
            .build()
    );

    private int delayLeft;
    private double lastPacketY = Double.MAX_VALUE;
    private boolean sentPacket;
    private boolean insideBlock;

    public BoatNoclip() {
        super(Trouser.Main, "boat-noclip", "Fly through anything using boats | Tested on 1.21.11 paper, might not work on older versions.");
    }

    @Override
    public void onActivate() {
        delayLeft = delay.get();
        sentPacket = false;
        lastPacketY = Double.MAX_VALUE;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player != null && mc.player.getVehicle() instanceof BoatEntity boat) {
            insideBlock = isInsideBlock(boat);
            if (sentPacket) {
                VehicleMoveC2SPacket packet = VehicleMoveC2SPacket.fromVehicle(boat);
                ((IVec3d) packet.position()).meteor$setY(lastPacketY);
                mc.player.networkHandler.sendPacket(packet);
                sentPacket = false;
            }
        }

        delayLeft--;
    }

    @EventHandler
    private void onEntityMove(LivingEntityMoveEvent event) {
        Entity entity = event.entity;
        if (!(entity instanceof BoatEntity)) return;
        if (entity.getControllingPassenger() != mc.player) return;

        double velX = entity.getVelocity().x;
        double velY = 0;
        double velZ = entity.getVelocity().z;

        insideBlock = isInsideBlock(entity);

        if (speed.get()) {
            double appliedSpeed = insideBlock
                    ? horizontalSpeedInsideBlocks.get()
                    : horizontalSpeed.get();

            Vec3d vel = PlayerUtils.getHorizontalVelocity(appliedSpeed);
            velX = vel.x;
            velZ = vel.z;
        }

        if (mc.currentScreen == null && Input.isPressed(mc.options.jumpKey)) velY += verticalSpeed.get() / 20;
        if (mc.currentScreen == null && Input.isPressed(mc.options.sprintKey)) velY -= verticalSpeed.get() / 20;
        else velY -= fallSpeed.get() / 20;

        entity.setYaw(mc.player.getYaw());
        ((IVec3d) event.movement).meteor$set(velX, velY, velZ);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof VehicleMoveC2SPacket packet)) return;
        if (!antiKick.get()) return;
        if (!(mc.player.getVehicle() instanceof BoatEntity)) return;

        double currentY = packet.position().y;

        if (delayLeft <= 0 && !sentPacket && shouldFlyDown(currentY) && isOnAir(mc.player.getVehicle())) {
            ((IVec3d) packet.position()).meteor$setY(lastPacketY - 0.03130D);
            sentPacket = true;
            delayLeft = delay.get();
        }

        lastPacketY = currentY;
    }
    private static boolean isOnAir(Entity entity) {
        return entity.getEntityWorld().getStatesInBox(entity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0)).allMatch(AbstractBlock.AbstractBlockState::isAir);
    }
    private boolean shouldFlyDown(double currentY) {
        if (currentY >= lastPacketY) return true;
        return lastPacketY - currentY < 0.03130D;
    }

    private boolean isInsideBlock(Entity entity) {
        if (entity == null || mc.world == null) return false;

        Box box = entity.getBoundingBox()
                .shrink(0.0, 0.05, 0.0)
                .expand(0.5, 0.0, 0.5);

        int minX = MathHelper.floor(box.minX);
        int minY = MathHelper.floor(box.minY);
        int minZ = MathHelper.floor(box.minZ);
        int maxX = MathHelper.floor(box.maxX);
        int maxY = MathHelper.floor(box.maxY);
        int maxZ = MathHelper.floor(box.maxZ);

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getCollisionShape(mc.world, pos, ShapeContext.absent());
                    if (shape.isEmpty()) continue;

                    if (entity.getBoundingBox().intersects(shape.getBoundingBox().offset(pos))) {
                        return true;
                    }
                }
            }
        }

        for (Entity other : mc.world.getOtherEntities(entity, box)) {
            if (other == mc.player) continue;
            if (other == entity.getVehicle()) continue;
            if (other.hasPassenger(entity) || entity.hasPassenger(other)) continue;
            if (!other.isAlive() || other.isRemoved()) continue;

            if (other.getBoundingBox().intersects(box)) {
                return true;
            }
        }

        return false;
    }
}