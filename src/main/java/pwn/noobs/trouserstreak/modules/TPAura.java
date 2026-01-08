//made by etianl based on code from the original InfiniteReach by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
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
                    .defaultValue(49.0)
                    .min(1.0)
                    .sliderRange(1.0,99.0)
                    .visible(() -> mode.get() == Mode.Paper)
                    .build()
    );
    private final Setting<Integer> entityAttackDelay = sgGeneral.add(
            new IntSetting.Builder()
                    .name("attack-delay")
                    .description("Ticks between entity attacks.")
                    .defaultValue(3)
                    .min(1)
                    .sliderMax(20)
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
    private double maxDistance;
    private int entityAttackTicks = 0;
    private boolean canEntityAttack = true;
    private Entity target = null;
    private volatile Vec3d startPos = Vec3d.ZERO;
    private volatile Vec3d finalPos = Vec3d.ZERO;
    private volatile Vec3d aboveself = Vec3d.ZERO;
    private volatile Vec3d abovetarget = Vec3d.ZERO;

    public TPAura() {
        super(Trouser.Main, "TPAura", "Teleport to entities to attack them.");
    }
    @Override
    public void onActivate() {
        if (mode.get() == Mode.Vanilla) maxDistance = Distance.get();
        else maxDistance = paperDistance.get();
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        target = findClosestTarget();
        if (target == null) {
            startPos = finalPos = aboveself = abovetarget = null;
            entityAttackTicks = 0;
            return;
        }
        startPos = mc.player.getVehicle() == null
                ? mc.player.getPos()
                : mc.player.getVehicle().getPos();
        Vec3d targetPos = target.getPos();
        Vec3d diff = startPos.subtract(targetPos);

        double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
        double targetUp = flatUp + diff.y;

        double yOffset = mc.player.getVehicle() != null
                ? target.getBoundingBox().maxY + 0.3
                : targetPos.y;

        Vec3d insideTarget = new Vec3d(targetPos.x, yOffset, targetPos.z);

        double actualDist = startPos.distanceTo(targetPos);
        if (actualDist > maxDistance - 0.5) return;

        finalPos = !invalid(insideTarget)
                ? insideTarget
                : findNearestPos(insideTarget);
        if (finalPos == null) return;
        aboveself = startPos.add(0, maxDistance, 0);
        abovetarget = finalPos.add(0, targetUp, 0);
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        if (mode.get() == Mode.Vanilla) maxDistance = Distance.get();
        else maxDistance = paperDistance.get();

        if (!canEntityAttack) {
            entityAttackTicks++;
            if (entityAttackTicks >= entityAttackDelay.get()) {
                canEntityAttack = true;
                entityAttackTicks = 0;
            }
            return;
        }

        if (!friends.get() && target instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) target)) return;

        if (target != null && isValidTarget(target)) {
            hitEntity(target, true);
            canEntityAttack = false;
        }
    }

    private Entity findClosestTarget() {
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidListTarget(entity)) continue;

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
        return entity.isAlive()
                && mc.player.distanceTo(entity) <= maxDistance;
    }

    public void hitEntity(Entity target, Boolean attackpressed) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (target instanceof PlayerEntity player && player.isBlocking()) return;
        if (startPos == null || finalPos == null || aboveself == null || abovetarget == null) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;

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

        if (attackpressed)mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        else mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(target, mc.player.isSneaking(), mc.player.getActiveHand()));

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
        if (mc.world.getChunk(BlockPos.ofFloored(pos)) == null) return true;
        Entity entity = mc.player.hasVehicle()
                ? mc.player.getVehicle()
                : mc.player;
        Box box = entity.getBoundingBox().offset(
                pos.x - entity.getX(),
                pos.y - entity.getY(),
                pos.z - entity.getZ()
        );
        BlockPos blockPos = BlockPos.ofFloored(pos);
        for (int x = blockPos.getX() - 1; x <= blockPos.getX() + 1; x++) {
            for (int y = blockPos.getY() - 1; y <= blockPos.getY() + 1; y++) {
                for (int z = blockPos.getZ() - 1; z <= blockPos.getZ() + 1; z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(checkPos);

                    if (state.isOf(Blocks.LAVA)) {
                        return true;
                    }
                }
            }
        }
        for (Entity e : mc.world.getOtherEntities(mc.player, box)) {
            if (e.isCollidable()) return true;
        }
        Vec3d delta = pos.subtract(entity.getPos());
        return mc.world.getBlockCollisions(entity, entity.getBoundingBox().offset(delta)).iterator().hasNext();
    }

    private boolean hasClearPath(Vec3d start, Vec3d end) {
        if (invalid(start) || invalid(end)) return false;
        Entity entity = mc.player.hasVehicle()
                ? mc.player.getVehicle()
                : mc.player;
        Box playerBox = entity.getBoundingBox();
        int steps = Math.max(10, (int)(start.distanceTo(end) * 2.5));

        for (int i = 1; i < steps; i++) {
            double t = i / (double)steps;
            Vec3d sample = start.lerp(end, t);

            Box sampleBox = playerBox.offset(sample.x - mc.player.getX(),
                    sample.y - mc.player.getY(),
                    sample.z - mc.player.getZ());

            if (!mc.world.isSpaceEmpty(sampleBox)) return false;
            if (mc.world.getOtherEntities(mc.player, sampleBox)
                    .stream().anyMatch(e -> e.isCollidable())) return false;
        }
        return true;
    }
}