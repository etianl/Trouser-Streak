//made by etianl based on code from the original InfiniteReach by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class TPAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTP = settings.createGroup("Teleport Options");
    private final SettingGroup totem = settings.createGroup("Totem Bypass (PAPER ONLY)");

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing arm")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> rotateToTarget = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate to Target")
            .description("Sends a look packet aimed at the target before attacking. Helps hit registration on servers that check facing direction.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> entityAttackDelay = sgGeneral.add(new IntSetting.Builder()
            .name("attack-delay")
            .description("Ticks between entity attacks.")
            .defaultValue(5)
            .min(0)
            .sliderMax(20)
            .build()
    );
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .defaultValue(EntityType.PLAYER)
            .build()
    );
    public final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("Ignore Friends")
            .defaultValue(false)
            .build()
    );
    public enum TargetPriority {
        Closest,
        LowestHealth
    }
    private final Setting<TargetPriority> targetPriority = sgGeneral.add(new EnumSetting.Builder<TargetPriority>()
            .name("Target Priority")
            .description("Closest = nearest entity. Lowest Health = entity with least HP remaining.")
            .defaultValue(TargetPriority.Closest)
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
    private final Setting<Boolean> skipCollisionCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("BoatNoclip skip collision check")
            .description("If BoatNoclip is on and you are in a boat skip collision checks for blocks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> goUp = sgTP.add(new BoolSetting.Builder()
            .name("Clip up")
            .description("Clips upward to do a Mace Smash and get around obstacles. There isn't enough packets for this in vanilla mode.")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Paper)
            .build()
    );
    private final Setting<Integer> packets = sgTP.add(new IntSetting.Builder()
            .name("# spam packets to send (VANILLA)")
            .description("How many packets to send before actual movements.")
            .defaultValue(4)
            .min(1)
            .sliderRange(1,5)
            .visible(() -> mode.get() == Mode.Vanilla)
            .build()
    );
    private final Setting<Double> Distance = sgTP.add(new DoubleSetting.Builder()
            .name("Max Distance (VANILLA)")
            .description("Maximum range.")
            .defaultValue(22.0)
            .min(1.0)
            .sliderRange(1.0,22.0)
            .visible(() -> mode.get() == Mode.Vanilla)
            .onChanged(v -> { if (mode.get() == Mode.Vanilla) maxDistance = v; })
            .build()
    );
    private final Setting<Integer> paperpackets = sgTP.add(new IntSetting.Builder()
            .name("# spam packets to send (PAPER)")
            .description("How many packets to send before actual movements.")
            .defaultValue(6)
            .min(1)
            .sliderRange(1,20)
            .visible(() -> mode.get() == Mode.Paper)
            .onChanged(v -> { if (mode.get() == Mode.Paper) maxDistance = v; })
            .build()
    );
    private final Setting<Double> paperDistance = sgTP.add(new DoubleSetting.Builder()
            .name("Max Distance (PAPER)")
            .description("Maximum range.")
            .defaultValue(49.0)
            .min(1.0)
            .sliderRange(1.0,99.0)
            .visible(() -> mode.get() == Mode.Paper)
            .build()
    );
    private final Setting<Double> offsethorizontal = sgTP.add(new DoubleSetting.Builder()
            .name("Horizontal Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.05)
            .min(0.001)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Double> offsetY = sgTP.add(new DoubleSetting.Builder()
            .name("Y Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.01)
            .min(0.001)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Boolean> attackSpam = totem.add(new BoolSetting.Builder()
            .name("Bypass totems (Mace)")
            .description("Teleport pathway is simplified for this to work. Settings example: 49 distance, 6 spam packet, 2 attack, 9 height increase.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> attacks = totem.add(new IntSetting.Builder()
            .name("# of Attacks")
            .description("This many attacks. 3 attacks may not work due to packet limits")
            .defaultValue(2)
            .sliderRange(1, 3)
            .min(0)
            .build()
    );
    private final Setting<Integer> increase = totem.add(new IntSetting.Builder()
            .name("Height Increase")
            .description("Blocks to add to fall height for each follow-up attack. Each hit must deal more damage than the last to beat invulnerability frames.")
            .defaultValue(9)
            .sliderRange(1, 100)
            .min(1)
            .max(100)
            .build()
    );

    private double maxDistance;
    private int entityAttackTicks = 0;

    public TPAura() {
        super(Trouser.Main, "TPAura", "Teleport to entities to attack them.");
    }
    @Override
    public void onActivate() {
        maxDistance = mode.get() == Mode.Vanilla ? Distance.get() : paperDistance.get();
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

        maxDistance = mode.get() == Mode.Vanilla ? Distance.get() : paperDistance.get();
        entityAttackTicks++;
        if (entityAttackTicks > entityAttackDelay.get()){
            hitEntity();
            entityAttackTicks = 0;
        }
    }

    private Entity findClosestTarget() {
        Entity best = null;
        double bestValue = Double.MAX_VALUE;
        double maxDistSq = maxDistance * maxDistance;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.distanceToSqr(mc.player) > maxDistSq) continue;
            if (!isValidListTarget(entity)) continue;
            if (friends.get() && entity instanceof Player && Friends.get().isFriend((Player) entity)) continue;

            double value;
            if (targetPriority.get() == TargetPriority.LowestHealth && entity instanceof LivingEntity living) {
                value = living.getHealth();
            } else {
                value = entity.distanceToSqr(mc.player);
            }

            if (value < bestValue) {
                bestValue = value;
                best = entity;
            }
        }

        return best;
    }
    private boolean isValidListTarget(Entity entity) {
        return entities.get().contains(entity.getType())
                && entity.isAlive()
                && entity.isAttackable()
                && !entity.isInvulnerable()
                && entity != mc.player;
    }
    private boolean isValidTarget(Entity entity) {
        Entity playerentity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;
        return entity.isAlive()
                && playerentity.distanceTo(entity) <= maxDistance;
    }
    public void hitEntity() {
        if (mc.player == null || mc.getConnection() == null) return;
        Entity entity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;
        Entity target = findClosestTarget();
        if (target instanceof Player player && player.isBlocking()) return;

        if (target == null || !isValidTarget(target)) {
            entityAttackTicks = 0;
            return;
        }

        Vec3 startPos = entity.position();
        Vec3 targetPos = target.position();

        double actualDist = startPos.distanceTo(targetPos);
        if (actualDist > maxDistance - 0.5) return;

        double yOffset = mc.player.getVehicle() != null
                ? target.getBoundingBox().maxY + 0.3
                : target.getBoundingBox().getCenter().y;

        Vec3 insideTarget = new Vec3(targetPos.x, yOffset, targetPos.z);

        Vec3 finalPos = !invalid(insideTarget)
                ? insideTarget
                : findNearestPos(insideTarget);
        if (finalPos == null) return;

        Vec3 highPos = startPos.add(0, maxDistance, 0);
        Vec3 abovetarget = finalPos.add(0, maxDistance, 0);
        boolean doGoUp = mode.get() == Mode.Paper && goUp.get();

        boolean aboveTargetInvalid = doGoUp && (attackSpam.get()
                ? (mc.level == null || abovetarget.y > mc.level.getMaxY() - 1)
                : (invalid(highPos) || invalid(abovetarget) || !hasClearPath(highPos, abovetarget)));
        if (invalid(finalPos) || aboveTargetInvalid) {
            if (chatFeedback) {
                if (doGoUp && !attackSpam.get() && !hasClearPath(highPos, abovetarget)) {
                    error("Path blocked between clip positions.");
                } else {
                    error("At least one of the teleports are invalid.");
                }
            }
            return;
        }

        int amountOfPackets = mode.get() == Mode.Vanilla ? packets.get() : paperpackets.get();
        for (int i2 = 0; i2 < amountOfPackets; i2++) {
            if (mc.player.isPassenger()) mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(mc.player.getVehicle()));
            else mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision));
        }

        int attackCount = attacks.get();
        if (!attackSpam.get() || mode.get() == Mode.Vanilla) attackCount = 1;

        int currentHeight = (int) maxDistance;
        for (int i = 0; i < attackCount; i++) {
            int blocks = (i == 0) ? (int)maxDistance : currentHeight;

            if (attackSpam.get() && mc.level != null) {
                int worldTop = mc.level.getMaxY() - 1;
                if (finalPos.y + blocks > worldTop) {
                    blocks = (int)(worldTop - finalPos.y);
                    if (blocks < 1) break;
                }
            }

            Vec3 progressiveAboveTarget = finalPos.add(0, blocks, 0);

            if (doGoUp && invalid(progressiveAboveTarget)) {
                if (chatFeedback) error("Invalid progressive height positions at " + blocks + " blocks.");
                break;
            }

            if (attackCount > 1 && chatFeedback && ((doGoUp && !hasClearPath(progressiveAboveTarget, finalPos)) || !hasClearPath(startPos, finalPos))) {
                error("Path blocked between clip positions.");
                break;
            }

            if (attackCount == 1 && doGoUp) {
                sendMove(entity, highPos);
            }

            if (doGoUp) {
                sendMove(entity, progressiveAboveTarget);
            }

            sendMove(entity, finalPos);

            if (rotateToTarget.get()) {
                Vec3 toTarget = target.getBoundingBox().getCenter().subtract(mc.player.getEyePosition()).normalize();
                float yaw = (float)(Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
                float pitch = (float)-Math.toDegrees(Math.asin(Mth.clamp(toTarget.y, -1.0, 1.0)));
                ServerboundMovePlayerPacket rotPacket = new ServerboundMovePlayerPacket.Rot(yaw, pitch, false, mc.player.horizontalCollision);
                ((IServerboundMovePlayerPacket) rotPacket).meteor$setTag(1337);
                mc.player.connection.send(rotPacket);
            }

            if (swing.get()) {
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                mc.player.swing(InteractionHand.MAIN_HAND);
            }

            mc.getConnection().send(new ServerboundAttackPacket(target.getId()));

            if (attackCount == 1 && doGoUp) {
                sendMove(entity, progressiveAboveTarget);
                sendMove(entity, highPos);
            }

            if (attackCount == 1) {
                sendMove(entity, startPos);
            }

            currentHeight += increase.get();
        }

        if (attackCount > 1) {
            sendMove(entity, startPos);
        }

        Vec3 offset = getOffset(startPos);
        sendMove(entity, offset);
        entity.setPos(offset);
    }
    private Vec3 findNearestPos(Vec3 desired) {
        if (!invalid(desired)) return desired;

        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    Vec3 test = desired.add(dx, dy, dz);
                    if (invalid(test)) continue;

                    double dist = test.distanceToSqr(desired);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = test;
                    }
                }
            }
        }
        return best;
    }
    private void sendMove(Entity entity, Vec3 pos) {
        if (mc.getConnection() == null) return;
        if (entity == mc.player) {
            ServerboundMovePlayerPacket movepacket = new ServerboundMovePlayerPacket.PosRot(pos,mc.player.getYRot(),mc.player.getXRot(), false, mc.player.horizontalCollision);
            ((IServerboundMovePlayerPacket) movepacket).meteor$setTag(1337);
            mc.player.connection.send(movepacket);
        } else {
            mc.getConnection().send(new ServerboundMoveVehiclePacket(pos, mc.player.getVehicle().getYRot(), mc.player.getVehicle().getXRot(), false));
        }
    }
    private Vec3 getOffset(Vec3 base) {
        double dx = offsethorizontal.get();
        double dy = offsetY.get();

        Vec3[] shuffledOffsets = new Vec3[] {
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

        for (Vec3 pos : shuffledOffsets) {
            if (!invalid(pos)) return pos;
        }

        Vec3 noHorizontal = base.add(0, dy, 0);
        if (!invalid(noHorizontal)) return noHorizontal;

        return base;
    }
    private boolean invalid(Vec3 pos) {
        if (mc.level == null) return true;
        if (mc.level.getChunk(BlockPos.containing(pos)) == null) return true;

        Entity entity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;

        AABB targetBox = entity.getBoundingBox().move(
                pos.x - entity.getX(),
                pos.y - entity.getY(),
                pos.z - entity.getZ()
        );
        Module boatNoclip = Modules.get().get(BoatNoclip.class);
        if (skipCollisionCheck.get() && entity != mc.player && boatNoclip != null && boatNoclip.isActive()) {
            for (BlockPos bp : BlockPos.betweenClosed(
                    BlockPos.containing(targetBox.minX, targetBox.minY, targetBox.minZ),
                    BlockPos.containing(targetBox.maxX, targetBox.maxY, targetBox.maxZ)
            )) {
                BlockState state = mc.level.getBlockState(bp);
                if (state.is(Blocks.LAVA)) {
                    return true;
                }
            }
        } else {
            for (BlockPos bp : BlockPos.betweenClosed(
                    BlockPos.containing(targetBox.minX, targetBox.minY, targetBox.minZ),
                    BlockPos.containing(targetBox.maxX, targetBox.maxY, targetBox.maxZ)
            )) {
                BlockState state = mc.level.getBlockState(bp);
                if (state.is(Blocks.LAVA) || !state.getCollisionShape(mc.level, bp).isEmpty()) {
                    return true;
                }
            }
        }

        for (Entity e : mc.level.getEntities(entity, targetBox)) {
            if (e.canBeCollidedWith(entity)) return true;
        }

        return false;
    }
    private boolean hasClearPath(Vec3 start, Vec3 end) {
        if (invalid(start) || invalid(end)) return false;

        int steps = Math.max(10, (int)(start.distanceTo(end) * 2.5));

        for (int i = 1; i < steps; i++) {
            double t = i / (double)steps;
            Vec3 sample = start.lerp(end, t);

            if (invalid(sample)) return false;
        }
        return true;
    }
}