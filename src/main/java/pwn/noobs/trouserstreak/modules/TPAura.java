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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class TPAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTP = settings.createGroup("Teleport Options");
    private final SettingGroup totem = settings.createGroup("Totem Bypass (PAPER ONLY)");

    private final Setting<Boolean> swing = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("swing arm")
                    .defaultValue(true)
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
    private final Setting<Boolean> requireCooldown = sgGeneral.add(new BoolSetting.Builder()
            .name("Require Full Cooldown")
            .description("Only attack when the weapon cooldown is at 100%. Leave off for mace or high-frequency aura use.")
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
    private final Setting<Boolean> skipCollisionCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("BoatNoclip skip collision check")
            .description("If BoatNoclip is on and you are in a boat skip collision checks for blocks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> goUp = sgTP.add(
            new BoolSetting.Builder()
                    .name("Clip up")
                    .description("Clips upward to do a Mace Smash and get around obstacles. There isn't enough packets for this in vanilla mode.")
                    .defaultValue(true)
                    .visible(() -> mode.get() == Mode.Paper)
                    .build()
    );
    private final Setting<Integer> packets = sgTP.add(
            new IntSetting.Builder()
                    .name("# spam packets to send (VANILLA)")
                    .description("How many packets to send before actual movements.")
                    .defaultValue(4)
                    .min(1)
                    .sliderRange(1,5)
                    .visible(() -> mode.get() == Mode.Vanilla)
                    .build()
    );
    private final Setting<Double> Distance = sgTP.add(
            new DoubleSetting.Builder()
                    .name("Max Distance (VANILLA)")
                    .description("Maximum range.")
                    .defaultValue(22.0)
                    .min(1.0)
                    .sliderRange(1.0,22.0)
                    .visible(() -> mode.get() == Mode.Vanilla)
                    .onChanged(v -> { if (mode.get() == Mode.Vanilla) maxDistance = v; })
                    .build()
    );
    private final Setting<Integer> paperpackets = sgTP.add(
            new IntSetting.Builder()
                    .name("# spam packets to send (PAPER)")
                    .description("How many packets to send before actual movements.")
                    .defaultValue(8)
                    .min(1)
                    .sliderRange(1,20)
                    .visible(() -> mode.get() == Mode.Paper)
                    .build()
    );
    private final Setting<Double> paperDistance = sgTP.add(
            new DoubleSetting.Builder()
                    .name("Max Distance (PAPER)")
                    .description("Maximum range.")
                    .defaultValue(49.0)
                    .min(1.0)
                    .sliderRange(1.0,99.0)
                    .visible(() -> mode.get() == Mode.Paper)
                    .onChanged(v -> { if (mode.get() == Mode.Paper) maxDistance = v; })
                    .build()
    );
    private final Setting<Double> offsethorizontal = sgTP.add(
            new DoubleSetting.Builder()
                    .name("Horizontal Offset")
                    .description("How much to offset the player after teleports.")
                    .defaultValue(0.05)
                    .min(0.001)
                    .sliderMax(0.99)
                    .build()
    );
    private final Setting<Double> offsetY = sgTP.add(
            new DoubleSetting.Builder()
                    .name("Y Offset")
                    .description("How much to offset the player after teleports.")
                    .defaultValue(0.01)
                    .min(0.001)
                    .sliderMax(0.99)
                    .build()
    );
    private final Setting<Boolean> attackSpam = totem.add(new BoolSetting.Builder()
            .name("Bypass totems (Mace)")
            .description("Teleport pathway is simplified for this to work. Settings example: 49 distance, 8 spam packets, 2 attacks, 3 follow-up height.")
            .defaultValue(false)
            .build());
    private final Setting<Integer> attacks = totem.add(new IntSetting.Builder()
            .name("# of Attacks")
            .description("This many attacks. 3 attacks may not work due to packet limits")
            .defaultValue(2)
            .sliderRange(1, 3)
            .min(0)
            .build()
    );
    private final Setting<Integer> followupHeight = totem.add(new IntSetting.Builder()
            .name("Follow-up Height")
            .description("Fall height for attacks 2 and 3 in Clip Up mode. 2 blocks is the minimum to kill through Absorption IV after a totem pops.")
            .defaultValue(3)
            .sliderRange(2, 15)
            .min(2)
            .max(15)
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
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        // mode has no onChanged hook so catch switches here, only update if actually changed
        if (mode.get() == Mode.Vanilla) {
            if (maxDistance != Distance.get()) maxDistance = Distance.get();
        } else {
            if (maxDistance != paperDistance.get()) maxDistance = paperDistance.get();
        }

        entityAttackTicks++;
        if (entityAttackTicks > entityAttackDelay.get()) {
            hitEntity();
            entityAttackTicks = 0;
        }
    }

    private Entity findClosestTarget() {
        Entity best = null;
        double bestValue = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidListTarget(entity)) continue;
            // bug in original: friends were skipped when "Attack Friends" was ON. corrected — skip when it's OFF
            if (!friends.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) continue;

            double value;
            if (targetPriority.get() == TargetPriority.LowestHealth && entity instanceof LivingEntity living) {
                value = living.getHealth();
            } else {
                value = entity.squaredDistanceTo(mc.player);
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
        Entity playerentity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        return entity.isAlive()
                && playerentity.distanceTo(entity) <= maxDistance;
    }
    public void hitEntity() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        Entity target = findClosestTarget();
        if (target instanceof PlayerEntity player && player.isBlocking()) return;

        if (target == null || !isValidTarget(target)) {
            entityAttackTicks = 0;
            return;
        }

        if (requireCooldown.get() && mc.player.getAttackCooldownProgress(1f) < 1.0f) return;

        boolean holdingMace = mc.player.getMainHandStack().getItem() == Items.MACE
                || mc.player.getOffHandStack().getItem() == Items.MACE;
        boolean doGoUp = mode.get() == Mode.Paper && goUp.get() && holdingMace;
        if (mode.get() == Mode.Paper && goUp.get() && !holdingMace) {
            if (chatFeedback) error("Clip up is enabled but you are not holding a Mace.");
        }

        Vec3d startPos = entity.getEntityPos();
        Vec3d targetPos = target.getEntityPos();

        double yOffset = mc.player.getVehicle() != null
                ? target.getBoundingBox().maxY + 0.3
                : targetPos.y;

        Vec3d insideTarget = new Vec3d(targetPos.x, yOffset, targetPos.z);
        double actualDist = startPos.distanceTo(targetPos);

        Vec3d finalPos = !invalid(insideTarget)
                ? insideTarget
                : findNearestPos(insideTarget);
        if (finalPos == null) return;

        Vec3d highPos = startPos.add(0, maxDistance, 0);
        Vec3d abovetarget = finalPos.add(0, maxDistance, 0);

        // highPos and abovetarget are only sent when doGoUp is active, so only validate them then.
        // the original checked them unconditionally which caused non-goUp attacks to silently fail
        // any time there happened to be a ceiling or unloaded chunk 49+ blocks above the player.
        if (invalid(finalPos) ||
                (doGoUp && (invalid(highPos) || invalid(abovetarget) || !hasClearPath(highPos, abovetarget)))) {
            if (chatFeedback) {
                if (doGoUp && !hasClearPath(highPos, abovetarget)) {
                    error("Path blocked between clip positions.");
                } else {
                    error("At least one of the teleports are invalid.");
                }
            }
            return;
        }

        if (actualDist > maxDistance - 0.5) return;

        int amountOfPackets = mode.get() == Mode.Vanilla ? packets.get() : paperpackets.get();
        for (int i2 = 0; i2 < amountOfPackets; i2++) {
            if (mc.player.hasVehicle()) mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
            else mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        }

        int attackCount = attacks.get();
        if (!attackSpam.get() || mode.get() == Mode.Vanilla) attackCount = 1;

        for (int i = 0; i < attackCount; i++) {
            // re-prime movement credit before each follow-up — after the previous hit's finalPos
            // packet Paper resets movement tolerance to baseline, so the next UP gets rejected
            // without this. double-totem bypass was failing at exactly this point.
            if (i > 0 && attackSpam.get()) {
                if (mc.player.hasVehicle()) mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                else mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
            }

            int blocks = (i == 0) ? (int)maxDistance : followupHeight.get();

            // cap to world height — above the build limit invalid() returns unexpected results.
            // without this the bypass silently fails in the Nether where the ceiling is at Y=127.
            if (mc.world != null) {
                int worldTop = mc.world.getTopYInclusive() - 1;
                if (finalPos.y + blocks > worldTop) {
                    int cappedBlocks = (int)(worldTop - finalPos.y);
                    if (cappedBlocks < 2) {
                        if (chatFeedback) error("Not enough vertical space above target for totem bypass (need 2 blocks, have " + cappedBlocks + ").");
                        break;
                    }
                    blocks = cappedBlocks;
                }
            }

            Vec3d progressiveAboveTarget = finalPos.add(0, blocks, 0);

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

            if (swing.get()) {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            if (attackCount == 1 && doGoUp) {
                sendMove(entity, progressiveAboveTarget);
                sendMove(entity, highPos);
            }

            // for single attack, go home inside the loop as before.
            // for totem bypass, queue all hits first then go home once — otherwise the home packet
            // between hits gives auto-totem time to swap a second totem in before hit 2 lands.
            if (attackCount == 1) {
                sendMove(entity, startPos);
            }
        }

        if (attackCount > 1) {
            sendMove(entity, startPos);
        }

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
