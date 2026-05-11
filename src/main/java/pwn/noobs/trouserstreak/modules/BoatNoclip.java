package pwn.noobs.trouserstreak.modules;

import pwn.noobs.trouserstreak.Trouser;
import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BoatNoclip extends Module {
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgFlight = settings.createGroup("Flight");
    private final SettingGroup warning = settings.createGroup("Horizontal speeds over 5-6 will rubberband inside blocks.");

    private final Setting<Boolean> speed = sgSpeed.add(new BoolSetting.Builder()
            .name("speed")
            .defaultValue(true)
            .build()
    );
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

    @Override
    public void onDeactivate() {
        if (mc.player != null && mc.player.getVehicle() instanceof AbstractBoat boat) {
            boat.noPhysics = false;
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player != null && mc.player.getVehicle() instanceof AbstractBoat boat) {
            insideBlock = isInsideBlock(boat);
            boat.noPhysics = true;
            if (sentPacket) {
                ServerboundMoveVehiclePacket packet = ServerboundMoveVehiclePacket.fromEntity(boat);
                ((IVec3) packet.position()).meteor$setY(lastPacketY);
                mc.player.connection.send(packet);
                sentPacket = false;
            }
        }
        delayLeft--;
    }

    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        if (!(event.entity instanceof AbstractBoat entity)) return;
        if (entity.getControllingPassenger() != mc.player) return;
        entity.noPhysics = true;

        double velX = entity.getDeltaMovement().x;
        double velY = 0;
        double velZ = entity.getDeltaMovement().z;

        insideBlock = isInsideBlock(entity);

        if (speed.get()) {
            double appliedSpeed = insideBlock
                    ? horizontalSpeedInsideBlocks.get()
                    : horizontalSpeed.get();

            Vec3 vel = PlayerUtils.getHorizontalVelocity(appliedSpeed);
            velX = vel.x;
            velZ = vel.z;
        }

        if (mc.screen == null && Input.isPressed(mc.options.keyJump)) velY += verticalSpeed.get() / 20;
        if (mc.screen == null && Input.isPressed(mc.options.keySprint)) velY -= verticalSpeed.get() / 20;
        else velY -= fallSpeed.get() / 20;

        entity.setYRot(mc.player.getYRot());
        ((IVec3) event.movement).meteor$set(velX, velY, velZ);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMoveVehiclePacket packet)) return;
        if (!antiKick.get()) return;
        if (!(mc.player.getVehicle() instanceof AbstractBoat)) return;

        double currentY = packet.position().y;

        if (delayLeft <= 0 && !sentPacket && shouldFlyDown(currentY) && EntityUtils.isOnAir(mc.player.getVehicle())) {
            ((IVec3) packet.position()).meteor$setY(lastPacketY - 0.03130D);
            sentPacket = true;
            delayLeft = delay.get();
        }
        lastPacketY = currentY;
    }

    private boolean shouldFlyDown(double currentY) {
        if (currentY >= lastPacketY) return true;
        return lastPacketY - currentY < 0.03130D;
    }

    private boolean isInsideBlock(Entity entity) {
        if (entity == null || mc.level == null) return false;

        AABB box = entity.getBoundingBox()
                .contract(0.0, 0.05, 0.0)
                .inflate(0.5, 0.0, 0.5);

        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getCollisionShape(mc.level, pos, CollisionContext.empty());
                    if (shape.isEmpty()) continue;

                    if (entity.getBoundingBox().intersects(shape.bounds().move(pos))) {
                        return true;
                    }
                }
            }
        }

        for (Entity other : mc.level.getEntities(entity, box)) {
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