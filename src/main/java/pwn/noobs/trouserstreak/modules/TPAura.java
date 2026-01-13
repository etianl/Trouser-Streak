//made by etianl based on code from the original InfiniteReach by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class TPAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup totem = settings.createGroup("Totem Bypass (Mace)");

    private final Setting<Boolean> swing = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("swing arm")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .defaultValue(EntityType.PLAYER)
            .build()
    );
    public final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("Attack Friends")
            .defaultValue(false)
            .build()
    );
    public enum Mode {
        Vanilla,
        Paper
    }
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Compatibility Mode")
            .description("Vanilla = 22 blocks reach, Paper = possible 99 blocks reach, able to send more packets")
            .defaultValue(Mode.Vanilla)
            .build()
    );
    private final Setting<Boolean> goUp = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("Clip up")
                    .description("Clips upward to do a Mace Smash and get around obstacles. There isn't enough packets for this in vanilla mode.")
                    .defaultValue(true)
                    .visible(() -> mode.get() == Mode.Paper)
                    .build()
    );
    private final Setting<Integer> packets = sgGeneral.add(
            new IntSetting.Builder()
                    .name("# spam packets to send (VANILLA)")
                    .description("How many packets to send before actual movements.")
                    .defaultValue(4)
                    .min(1)
                    .sliderRange(1,5)
                    .visible(() -> mode.get() == Mode.Vanilla)
                    .build()
    );
    private final Setting<Double> Distance = sgGeneral.add(
            new DoubleSetting.Builder()
                    .name("Max Distance (VANILLA)")
                    .description("Maximum range.")
                    .defaultValue(22.0)
                    .min(1.0)
                    .sliderRange(1.0,22.0)
                    .visible(() -> mode.get() == Mode.Vanilla)
                    .build()
    );
    private final Setting<Integer> paperpackets = sgGeneral.add(
            new IntSetting.Builder()
                    .name("# spam packets to send (PAPER)")
                    .description("How many packets to send before actual movements.")
                    .defaultValue(7)
                    .min(1)
                    .sliderRange(1,10)
                    .visible(() -> mode.get() == Mode.Paper)
                    .build()
    );
    private final Setting<Double> paperDistance = sgGeneral.add(
            new DoubleSetting.Builder()
                    .name("Max Distance (PAPER)")
                    .description("Maximum range.")
                    .defaultValue(59.0)
                    .min(1.0)
                    .sliderRange(1.0,99.0)
                    .visible(() -> mode.get() == Mode.Paper)
                    .build()
    );
    private final Setting<Integer> entityAttackDelay = sgGeneral.add(
            new IntSetting.Builder()
                    .name("attack-delay")
                    .description("Ticks between entity attacks.")
                    .defaultValue(0)
                    .min(0)
                    .sliderMax(20)
                    .build()
    );
    private final Setting<Integer> attacks = sgGeneral.add(new IntSetting.Builder()
            .name("# of Attacks")
            .description("This many teleport attacks per delay.")
            .defaultValue(1)
            .sliderRange(1, 10)
            .min(0)
            .build()
    );
    private final Setting<Double> offsethorizontal = sgGeneral.add(
            new DoubleSetting.Builder()
                    .name("Horizontal Offset")
                    .description("How much to offset the player after teleports.")
                    .defaultValue(0.05)
                    .min(0.01)
                    .sliderMax(0.99)
                    .build()
    );
    private final Setting<Double> offsetY = sgGeneral.add(
            new DoubleSetting.Builder()
                    .name("Y Offset")
                    .description("How much to offset the player after teleports.")
                    .defaultValue(0.01)
                    .min(0.01)
                    .sliderMax(0.99)
                    .build()
    );
    private final Setting<Boolean> skipCollisionCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("BoatNoclip skip collision check")
            .description("If BoatNoclip is on and you are in a boat skip collision checks for blocks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> randomizeHeight = totem.add(new BoolSetting.Builder()
            .name("Randomize height")
            .description("Randomizes the fall height below the set limit.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> deviation = totem.add(new IntSetting.Builder()
            .name("Height deviation")
            .description("Max blocks to subtract from max height (e.g. 40)")
            .defaultValue(40)
            .sliderRange(1, 99)
            .min(1)
            .max(99)
            .visible(randomizeHeight::get)
            .build()
    );
    private double maxDistance;
    private int entityAttackTicks = 0;

    public TPAura() {
        super(Trouser.Main, "TPAura", "Teleport to entities to attack them.");
    }
    @Override
    public void onActivate() {
        if (mode.get() == Mode.Vanilla) maxDistance = Distance.get();
        else maxDistance = paperDistance.get();
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        if (mode.get() == Mode.Vanilla) maxDistance = Distance.get();
        else maxDistance = paperDistance.get();
        entityAttackTicks++;
        if (entityAttackTicks>entityAttackDelay.get()){
            for (int i = 0; i < attacks.get(); i++) {
                hitEntity();
            }
            entityAttackTicks = 0;
        }
    }

    private Entity findClosestTarget() {
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidListTarget(entity)) continue;
            if (friends.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) continue;
            double dist = entity.squaredDistanceTo(mc.player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }

        return closest;
    }
    private boolean isValidListTarget(Entity entity) {
        return entities.get().contains(entity.getType())
                && entity.isAlive()
                && entity.isAttackable()
                && !entity.isInvulnerable()
                && entity != mc.player;
    }
    private boolean isValidTarget(Entity entity) {
        Entity playerentity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        return entity.isAlive()
                && playerentity.distanceTo(entity) <= maxDistance;
    }

    public void hitEntity() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        Entity target = findClosestTarget();
        if (target instanceof PlayerEntity player && player.isBlocking()) return;
        Vec3d startPos;
        Vec3d finalPos;
        Vec3d aboveself;
        Vec3d abovetarget;
        if (target == null || !isValidTarget(target)) {
            entityAttackTicks = 0;
            return;
        }
        startPos = entity.getEntityPos();
        Vec3d targetPos = target.getEntityPos();
        Vec3d diff = startPos.subtract(targetPos);

        double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
        double targetUp = flatUp + diff.y;

        double yOffset = mc.player.getVehicle() != null
                ? target.getBoundingBox().maxY + 0.3
                : targetPos.y;

        Vec3d insideTarget = new Vec3d(targetPos.x, yOffset, targetPos.z);

        double actualDist = startPos.distanceTo(targetPos);

        finalPos = !invalid(insideTarget)
                ? insideTarget
                : findNearestPos(insideTarget);
        if (finalPos == null) return;
        aboveself = startPos.add(0, maxDistance, 0);
        abovetarget = finalPos.add(0, targetUp, 0);
        if (randomizeHeight.get()) {
            int randomSubtract = (int) (Math.random() * (deviation.get() + 1));
            aboveself = aboveself.add(0,-randomSubtract,0);
            abovetarget = abovetarget.add(0,-randomSubtract,0);
        }

        if (startPos == null || finalPos == null || aboveself == null || abovetarget == null) return;
        if (actualDist > maxDistance - 0.5) return;

        if (invalid(finalPos) ||
                (mode.get() == Mode.Paper && goUp.get() && (!hasClearPath(aboveself, abovetarget) || invalid(aboveself) || invalid(abovetarget)))) {
            if (chatFeedback) {
                if (!hasClearPath(aboveself, abovetarget)) {
                    error("Path blocked between clip positions.");
                } else {
                    error("At least one of the teleports are invalid.");
                }
            }
            return;
        }
        int amountOfPackets = mode.get() == Mode.Vanilla ? packets.get() : paperpackets.get();
        for (int i = 0; i < amountOfPackets; i++) {
            if (mc.player.hasVehicle()) mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
            else mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        }

        if (mode.get() == Mode.Paper && goUp.get()){
            sendMove(entity, aboveself);
            sendMove(entity, abovetarget);
        }
        sendMove(entity, finalPos);

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));

        if (swing.get()) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        if (mode.get() == Mode.Paper && goUp.get()){
            sendMove(entity, abovetarget.add(0,  0.01, 0));
            sendMove(entity, aboveself.add(0, 0.01, 0));
        }
        sendMove(entity, startPos);
        Vec3d offset = getOffset(startPos);
        sendMove(entity, offset);
        entity.setPosition(offset);
    }

    private Vec3d findNearestPos(Vec3d desired) {
        if (!invalid(desired)) return desired;

        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    Vec3d test = desired.add(dx, dy, dz);
                    if (invalid(test)) continue;

                    double dist = test.squaredDistanceTo(desired);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = test;
                    }
                }
            }
        }
        return best;
    }
    private void sendMove(Entity entity, Vec3d pos) {
        if (mc.getNetworkHandler() == null) return;
        if (entity == mc.player) {
            PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false, false);
            ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(movepacket);
        } else {
            mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(pos, mc.player.getVehicle().getYaw(), mc.player.getVehicle().getPitch(), false));
        }
    }
    private Vec3d getOffset(Vec3d base) {
        double dx = offsethorizontal.get();
        double dy = offsetY.get();

        Vec3d[] shuffledOffsets = new Vec3d[] {
                base.add( dx, dy,  0),
                base.add(-dx, dy,  0),
                base.add( 0, dy,  dx),
                base.add( 0, dy, -dx),
                base.add( dx, dy,  dx),
                base.add(-dx, dy,  -dx),
                base.add(-dx, dy,  dx),
                base.add(dx, dy,  -dx)
        };

        Collections.shuffle(Arrays.asList(shuffledOffsets));

        for (Vec3d pos : shuffledOffsets) {
            if (!invalid(pos)) return pos;
        }

        Vec3d noHorizontal = base.add(0, dy, 0);
        if (!invalid(noHorizontal)) return noHorizontal;

        return base;
    }

    private boolean invalid(Vec3d pos) {
        if (mc.world == null) return true;
        if (mc.world.getChunk(BlockPos.ofFloored(pos)) == null) return true;

        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;

        Box targetBox = entity.getBoundingBox().offset(
                pos.x - entity.getX(),
                pos.y - entity.getY(),
                pos.z - entity.getZ()
        );
        Module boatNoclip = Modules.get().get(BoatNoclip.class);
        if (skipCollisionCheck.get() && entity != mc.player && boatNoclip != null && boatNoclip.isActive()) {
            for (BlockPos bp : BlockPos.iterate(
                    BlockPos.ofFloored(targetBox.minX, targetBox.minY, targetBox.minZ),
                    BlockPos.ofFloored(targetBox.maxX, targetBox.maxY, targetBox.maxZ)
            )) {
                BlockState state = mc.world.getBlockState(bp);
                if (state.isOf(Blocks.LAVA)) {
                    return true;
                }
            }
        } else {
            for (BlockPos bp : BlockPos.iterate(
                    BlockPos.ofFloored(targetBox.minX, targetBox.minY, targetBox.minZ),
                    BlockPos.ofFloored(targetBox.maxX, targetBox.maxY, targetBox.maxZ)
            )) {
                BlockState state = mc.world.getBlockState(bp);
                if (state.isOf(Blocks.LAVA) || !state.getCollisionShape(mc.world, bp).isEmpty()) {
                    return true;
                }
            }
        }

        for (Entity e : mc.world.getOtherEntities(entity, targetBox)) {
            if (e.isCollidable(entity)) return true;
        }

        return false;
    }

    private boolean hasClearPath(Vec3d start, Vec3d end) {
        if (invalid(start) || invalid(end)) return false;

        int steps = Math.max(10, (int)(start.distanceTo(end) * 2.5));

        for (int i = 1; i < steps; i++) {
            double t = i / (double)steps;
            Vec3d sample = start.lerp(end, t);

            if (invalid(sample)) return false;
        }
        return true;
    }
}