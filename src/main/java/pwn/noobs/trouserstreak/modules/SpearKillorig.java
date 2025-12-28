package pwn.noobs.trouserstreak.modules;

/**
 * A note from Kimtaeho (https://github.com/needitem):
 *
 * Blink:
 * - Delays position packets while charging spear
 * - Flushes single end-position packet = server sees huge movement
 * - Server calculates: velocity = currentPos - lastKnownPos
 *
 * How spear damage works:
 * - damage = baseDamage + floor(relativeVelocity * damageMultiplier)
 * - relativeVelocity = playerLookDir.dot(playerMotion) - playerLookDir.dot(targetMotion)
 */

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SpearKillorig extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLunge = settings.createGroup("Lunge Options");
    private final SettingGroup sgBlink = settings.createGroup("Blink Options");

    public enum Mode {
        Lunge,      // Set velocity directly
        Blink       // Packet delay method
    }
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Mode")
            .description("Blink=packet delay, Lunge=velocity boost.")
            .defaultValue(Mode.Lunge)
            .build()
    );
    public final Setting<Double> maxrange = sgGeneral.add(new DoubleSetting.Builder()
            .name("Max Targeting Range")
            .description("How far in blocks that entities can still be targeted.")
            .defaultValue(256)
            .min(0)
            .sliderRange(0,512)
            .build()
    );
    public enum TargetListMode {
        Whitelist,
        Blacklist
    }
    private final Setting<TargetListMode> targetListMode = sgGeneral.add(new EnumSetting.Builder<TargetListMode>()
            .name("Target List Mode")
            .description("Whitelist = only target these entities, Blacklist = don't target these entities")
            .defaultValue(TargetListMode.Blacklist)
            .build());

    private final Setting<Set<EntityType<?>>> targetEntities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("Target Entities")
            .description("Entities to whitelist/blacklist")
            .onlyAttackable()
            .build());
    public final Setting<Double> getLungeStrength = sgLunge.add(new DoubleSetting.Builder()
            .name("SpearVelocity")
            .description("The amount of velocity applied to the player, in the direction of the target.")
            .defaultValue(5)
            .min(0)
            .sliderRange(1,10)
            .visible(() -> mode.get() == Mode.Lunge)
            .build()
    );
    private final Setting<Boolean> stop = sgLunge.add(new BoolSetting.Builder()
            .name("Stop on target")
            .description("Stops the lunge when you reach the target. Can help with reliability of the spear kill.")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Lunge)
            .build()
    );
    public final Setting<Double> stopDistance = sgLunge.add(new DoubleSetting.Builder()
            .name("Stop Distance")
            .description("Distance between your hitbox and the entities hitbox to attempt to stop at.")
            .defaultValue(2)
            .min(0)
            .sliderRange(0,10)
            .visible(() -> stop.get() && mode.get() == Mode.Lunge)
            .build()
    );
    private final Setting<Integer> getreadyticksModifier = sgLunge.add(new IntSetting.Builder()
            .name("Lunge Delay Modifier")
            .description("Wait % amount of time until spear is ready before lunge.")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 100)
            .visible(() -> mode.get() == Mode.Lunge)
            .build());
    private final Setting<Double> flushRange = sgBlink.add(new DoubleSetting.Builder()
            .name("Flush Range")
            .description("Distance to target when flush occurs (blocks)")
            .defaultValue(4.0)
            .min(1.0)
            .sliderRange(1.0, 10.0)
            .visible(() -> mode.get() == Mode.Blink)
            .build());

    private final Setting<Boolean> blinkLunge = sgBlink.add(new BoolSetting.Builder()
            .name("Lunge")
            .description("Launch towards target like Lunge mode (works while flying)")
            .defaultValue(false)
            .visible(() -> mode.get() == Mode.Blink)
            .build());

    private final Setting<Double> blinkLungeStrength = sgBlink.add(new DoubleSetting.Builder()
            .name("Lunge Strength")
            .description("Velocity applied towards target")
            .defaultValue(2.0)
            .min(0.1)
            .sliderRange(0.1, 8.0)
            .visible(() -> mode.get() == Mode.Blink && blinkLunge.get())
            .build());

    private final Setting<Integer> blinkLungeTicks = sgBlink.add(new IntSetting.Builder()
            .name("Lunge Delay")
            .description("Ticks to charge before lunging")
            .defaultValue(15)
            .min(1)
            .sliderRange(1, 30)
            .visible(() -> mode.get() == Mode.Blink && blinkLunge.get())
            .build());

    private final Setting<Boolean> blinkAimbot = sgBlink.add(new BoolSetting.Builder()
            .name("Aimbot")
            .description("Automatically aim at nearest target")
            .defaultValue(false)
            .visible(() -> mode.get() == Mode.Blink)
            .build());

    private final Setting<Double> blinkAimRange = sgBlink.add(new DoubleSetting.Builder()
            .name("Aim Range")
            .description("Maximum range to search for targets")
            .defaultValue(64.0)
            .min(5.0)
            .sliderRange(5.0, 256.0)
            .visible(() -> mode.get() == Mode.Blink && blinkAimbot.get())
            .build());

    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private boolean isBlinking = false;
    private boolean isFlushing = false;
    private Vec3d startPos = null;
    private boolean wasCharging = false;
    private Entity blinkTarget = null;
    private double lastTargetDistance = Double.MAX_VALUE;
    private boolean wasApproaching = false;
    private int blinkChargeTicks = 0;
    private Entity crosshairTarget;

    public SpearKillorig() {
        super(Trouser.Main, "SpearKill", "Increases spear damage! Lunge mode uses velocity and Blink mode delays packets allowing normal movement. Thank you to Kimtaeho for Blink mode!");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @Override
    public void onDeactivate() {
        flushPackets();
        resetState();
    }

    private void resetState() {
        synchronized (packets) {
            packets.clear();
        }
        isBlinking = false;
        isFlushing = false;
        startPos = null;
        wasCharging = false;
        blinkTarget = null;
        lastTargetDistance = Double.MAX_VALUE;
        wasApproaching = false;
        blinkChargeTicks = 0;
        crosshairTarget = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        Item spearItem = mc.player.getActiveItem().getItem();
        boolean currentlyCharging = mc.player.getActiveItem().getUseAction() == UseAction.SPEAR;
        int readyTicks = getReadyTicks(spearItem);

        if (mode.get() == Mode.Lunge) {
            if (currentlyCharging) {
                if (crosshairTarget == null) crosshairTarget = target();
                if (crosshairTarget != null && !crosshairTarget.isAlive()) crosshairTarget = null;
                if (crosshairTarget == null || !(crosshairTarget instanceof LivingEntity)) return;
                if (!isValidTarget(crosshairTarget)) return;

                Vec3d playerPos = mc.player.getEyePos();
                Vec3d targetPos = crosshairTarget.getBoundingBox().getCenter();
                Vec3d toTarget = targetPos.subtract(playerPos).normalize();

                float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
                float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));

                mc.player.setYaw(yaw);
                mc.player.setHeadYaw(yaw);
                mc.player.setPitch(pitch);
                if (mc.player.getItemUseTime() > readyTicks) {
                    if (stop.get()) {
                        Box playerBox = mc.player.getBoundingBox().expand(stopDistance.get());
                        Box targetBox = crosshairTarget.getBoundingBox();

                        if (!playerBox.intersects(targetBox)) {
                            double lungeSpeed = getLungeStrength.get();
                            Vec3d viewDir = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
                            mc.player.setSprinting(true);
                            mc.player.setVelocity(viewDir.multiply(lungeSpeed));
                        } else {
                            crosshairTarget = null;
                            mc.player.setVelocity(0,0,0);
                            mc.player.setSprinting(false);
                        }
                    } else {
                        double lungeSpeed = getLungeStrength.get();
                        Vec3d viewDir = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
                        mc.player.setSprinting(true);
                        mc.player.setVelocity(viewDir.multiply(lungeSpeed));
                    }
                }
            } else {
                crosshairTarget = null;
            }
            return;
        }

        // Blink mode: Target-based flush with direction correction
        // Track charge ticks
        if (currentlyCharging) {
            blinkChargeTicks++;
        } else {
            blinkChargeTicks = 0;
        }

        // Find/update target (always needed for distance-based flush)
        if (currentlyCharging) {
            if (blinkTarget == null || !blinkTarget.isAlive() || !canSeeTarget(blinkTarget)) {
                blinkTarget = target();
                lastTargetDistance = blinkTarget != null ? mc.player.distanceTo(blinkTarget) : Double.MAX_VALUE;
                wasApproaching = false;
            }
        } else {
            blinkTarget = null;
            lastTargetDistance = Double.MAX_VALUE;
            wasApproaching = false;
        }

        // Aimbot functionality for Blink mode
        if (currentlyCharging && blinkAimbot.get() && blinkTarget != null) {
            // Aim at target (TrouserStreak style)
            Vec3d playerPos = mc.player.getEyePos();
            Vec3d targetPos = blinkTarget.getBoundingBox().getCenter();
            Vec3d toTarget = targetPos.subtract(playerPos).normalize();

            float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));

            mc.player.setYaw(yaw);
            mc.player.setHeadYaw(yaw);
            mc.player.setPitch(pitch);
        }

        // Blink Lunge: Launch towards target after charge delay (like Lunge mode)
        if (currentlyCharging && blinkLunge.get() && blinkTarget != null) {
            if (blinkChargeTicks >= blinkLungeTicks.get()) {
                // Calculate direction to target
                Vec3d playerPos = mc.player.getEyePos();
                Vec3d targetPos = blinkTarget.getBoundingBox().getCenter();
                Vec3d toTarget = targetPos.subtract(playerPos).normalize();

                // Aim at target
                float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
                float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));
                mc.player.setYaw(yaw);
                mc.player.setHeadYaw(yaw);
                mc.player.setPitch(pitch);

                // Apply velocity towards target (like Lunge mode)
                Vec3d viewDir = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
                mc.player.setSprinting(true);
                mc.player.setVelocity(viewDir.multiply(blinkLungeStrength.get()));
            }
        }

        // Charge started
        if (currentlyCharging && !wasCharging) {
            startBlink();
        }

        // Charge ended (released or interrupted)
        if (!currentlyCharging && wasCharging) {
            if (isBlinking) {
                // Rotate towards target before flush for max damage
                if (blinkTarget != null) {
                    rotateToTarget(blinkTarget);
                }
                flushPackets();
                isBlinking = false;
                startPos = null;
            }
        }

        wasCharging = currentlyCharging;

        // Distance-based flush logic
        if (isBlinking && blinkTarget != null && currentlyCharging) {
            double currentDistance = mc.player.distanceTo(blinkTarget);
            boolean isApproaching = currentDistance < lastTargetDistance;

            // Check flush conditions:
            // 1. Close enough to target (within flush range)
            // 2. Was approaching but now moving away (passed through target)
            boolean shouldFlush = false;

            if (currentDistance <= flushRange.get()) {
                // Close to target - flush now
                shouldFlush = true;
            } else if (wasApproaching && !isApproaching) {
                // Passed through/by target (was getting closer, now getting farther)
                shouldFlush = true;
            }

            if (shouldFlush) {
                // Rotate towards target for max damage
                rotateToTarget(blinkTarget);
                flushPackets();

                // Restart blink for continuous damage
                isBlinking = true;
                startPos = mc.player.getEntityPos();
                synchronized (packets) {
                    packets.clear();
                }

                // Reset distance tracking
                lastTargetDistance = mc.player.distanceTo(blinkTarget);
                wasApproaching = false;
            } else {
                lastTargetDistance = currentDistance;
                wasApproaching = isApproaching;
            }
        }
    }
    private void rotateToTarget(Entity target) {
        if (mc.player == null || target == null) return;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetPos = target.getBoundingBox().getCenter();
        Vec3d toTarget = targetPos.subtract(playerPos).normalize();

        float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));

        mc.player.setYaw(yaw);
        mc.player.setHeadYaw(yaw);
        mc.player.setPitch(pitch);
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!Utils.canUpdate()) return;
        if (!(event.packet instanceof PlayerMoveC2SPacket p)) return;

        // Blink mode packet handling
        if (mode.get() == Mode.Blink && isBlinking && !isFlushing) {
            event.cancel();
            synchronized (packets) {
                if (!packets.isEmpty()) {
                    PlayerMoveC2SPacket last = packets.get(packets.size() - 1);
                    if (isSamePacket(p, last)) return;
                }
                packets.add(p);
            }
        }
    }
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.world == null) return;
        if (!(event.packet instanceof PlayerPositionLookS2CPacket)) return;

        // Blink mode: Server position correction - reset blink state
        if (mode.get() == Mode.Blink && isBlinking) {
            synchronized (packets) {
                packets.clear();
            }
            startPos = mc.player.getEntityPos();
            lastTargetDistance = blinkTarget != null ? mc.player.distanceTo(blinkTarget) : Double.MAX_VALUE;
            wasApproaching = false;
        }
    }
    private int getReadyTicks(Item item) {
        int value = 14;
        if (item == Items.WOODEN_SPEAR) value = 14;
        else if (item == Items.STONE_SPEAR || item == Items.GOLDEN_SPEAR) value = 13;
        else if (item == Items.COPPER_SPEAR) value = 12;
        else if (item == Items.IRON_SPEAR) value = 11;
        else if (item == Items.DIAMOND_SPEAR) value = 9;
        else if (item == Items.NETHERITE_SPEAR) value = 7;

        return Math.round(value * (getreadyticksModifier.get() / 100.0f));
    }
    private void startBlink() {
        isBlinking = true;
        startPos = mc.player.getEntityPos();
        synchronized (packets) {
            packets.clear();
        }
        lastTargetDistance = blinkTarget != null ? mc.player.distanceTo(blinkTarget) : Double.MAX_VALUE;
        wasApproaching = false;
    }
    private boolean isSamePacket(PlayerMoveC2SPacket a, PlayerMoveC2SPacket b) {
        return a.isOnGround() == b.isOnGround()
                && a.getYaw(-1) == b.getYaw(-1)
                && a.getPitch(-1) == b.getPitch(-1)
                && a.getX(-1) == b.getX(-1)
                && a.getY(-1) == b.getY(-1)
                && a.getZ(-1) == b.getZ(-1);
    }
    private void flushPackets() {
        if (mc.player == null || mc.player.networkHandler == null) return;

        synchronized (packets) {
            if (packets.isEmpty()) return;

            isFlushing = true;

            Vec3d currentPos = mc.player.getEntityPos();
            double distance = startPos != null ? startPos.distanceTo(currentPos) : 0;

            // Ignore if distance is less than flush range (too close, not enough velocity)
            if (distance < flushRange.get()) {
                if (chatFeedback) {
                    info("Skipped: %.2f blocks (min: %.1f)", distance, flushRange.get());
                }
                packets.clear();
                isFlushing = false;
                return;
            }

            // Send start position first, then end position immediately after
            // Server sees: startPos -> currentPos in 1 tick = huge velocity
            if (startPos != null) {
                PlayerMoveC2SPacket startPacket = new PlayerMoveC2SPacket.Full(
                        startPos.x,
                        startPos.y,
                        startPos.z,
                        mc.player.getYaw(),
                        mc.player.getPitch(),
                        false,
                        false
                );
                mc.player.networkHandler.sendPacket(startPacket);
            }

            if (chatFeedback) {
                info("Flush: %.2f blocks", distance);
            }

            // Send final current position packet immediately after
            PlayerMoveC2SPacket endPacket = new PlayerMoveC2SPacket.Full(
                    currentPos.x,
                    currentPos.y,
                    currentPos.z,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
            );
            mc.player.networkHandler.sendPacket(endPacket);

            packets.clear();
            isFlushing = false;
        }
    }
    private Entity target() {
        if (mc.player == null || mc.world == null) return null;
        if (mc.crosshairTarget instanceof EntityHitResult hit) {
            if (isValidTarget(hit.getEntity())) return hit.getEntity();
        }

        double maxRange = maxrange.get();
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        HitResult blockHit = mc.world.raycast(new RaycastContext(eyePos,
                eyePos.add(lookVec.multiply(maxRange)), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player));
        double rayLength = blockHit.getType() == HitResult.Type.MISS ? maxRange :
                eyePos.distanceTo(blockHit.getPos());

        List<Entity> candidates = mc.world.getOtherEntities(mc.player,
                mc.player.getBoundingBox().stretch(lookVec.multiply(rayLength)),
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player);

        candidates.sort(Comparator.comparingDouble(e ->
                eyePos.squaredDistanceTo(e.getBoundingBox().getCenter())));

        double coneAngle = 0.999;
        for (Entity e : candidates) {
            double dist = eyePos.distanceTo(e.getBoundingBox().getCenter());
            if (dist > maxRange) break;

            if (!isValidTarget(e)) continue;

            if (!canSeeTarget(e)) continue;

            Vec3d toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();

            if (lookVec.dotProduct(toEntity) > coneAngle) {
                return e;
            }
        }
        return null;
    }
    private boolean canSeeTarget(Entity target) {
        if (mc.player == null || mc.world == null) return false;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetCenter = target.getBoundingBox().getCenter();

        HitResult result = mc.world.raycast(new RaycastContext(
                eyePos,
                targetCenter,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (result.getType() == HitResult.Type.MISS) return true;
        return eyePos.distanceTo(result.getPos()) >= eyePos.distanceTo(targetCenter) - 0.5;
    }
    private boolean isValidTarget(Entity entity) {
        if (entity == null) return false;

        EntityType<?> type = entity.getType();
        boolean inList = targetEntities.get().contains(type);

        if (targetListMode.get() == TargetListMode.Whitelist) {
            // Whitelist: only target if in list (or list is empty = target all)
            return inList || targetEntities.get().isEmpty();
        } else {
            // Blacklist: target if NOT in list
            return !inList;
        }
    }
    private Entity findBlinkTarget() {
        if (mc.player == null || mc.world == null) return null;

        double maxRange = blinkAimRange.get();
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        // Get all entities in range
        List<Entity> candidates = mc.world.getOtherEntities(mc.player,
                mc.player.getBoundingBox().expand(maxRange),
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player);

        // Sort by crosshair alignment (highest dot product = closest to crosshair)
        candidates.sort((a, b) -> {
            Vec3d toA = a.getBoundingBox().getCenter().subtract(eyePos).normalize();
            Vec3d toB = b.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dotA = lookVec.dotProduct(toA);
            double dotB = lookVec.dotProduct(toB);
            return Double.compare(dotB, dotA);  // Higher dot = more aligned = first
        });

        // Find first valid target that's within FOV
        for (Entity e : candidates) {
            double dist = eyePos.distanceTo(e.getBoundingBox().getCenter());
            if (dist > maxRange) continue;

            // Check target filter
            if (!isValidTarget(e)) continue;

            // Check if visible (no blocks in between)
            if (!canSeeTarget(e)) continue;

            Vec3d toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dot = lookVec.dotProduct(toEntity);

            // Must be in front of player (dot > 0 = less than 90 degrees)
            if (dot > 0) {
                return e;
            }
        }

        return null;
    }
}