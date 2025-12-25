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

public class SpearKill extends Module {
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
            .build());
    public final Setting<Double> maxrange = sgGeneral.add(new DoubleSetting.Builder()
            .name("Max Targeting Range")
            .description("How far in blocks that entities can still be targeted.")
            .defaultValue(256)
            .min(0)
            .sliderRange(0,512)
            .build()
    );
    private final Setting<Boolean> spearcheattargetPlayers = sgGeneral.add(new BoolSetting.Builder()
            .name("TargetOnlyPlayers")
            .description("Only lock onto Player entities. False only targets LivingEntity.")
            .defaultValue(false)
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
            .build());
    public final Setting<Double> stopDistance = sgLunge.add(new DoubleSetting.Builder()
            .name("Stop Distance")
            .description("Distance between your hitbox and the entities hitbox to attempt to stop at.")
            .defaultValue(2)
            .min(0)
            .sliderRange(0,10)
            .visible(() -> stop.get() && mode.get() == Mode.Lunge)
            .build()
    );
    private final Setting<Integer> minDelayTicks = sgBlink.add(new IntSetting.Builder()
            .name("Min Delay Ticks")
            .description("Minimum ticks before flush (for damage calculation to start)")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 20)
            .visible(() -> mode.get() == Mode.Blink)
            .build());

    private final Setting<Integer> maxDelayTicks = sgBlink.add(new IntSetting.Builder()
            .name("Max Delay Ticks")
            .description("Maximum ticks to delay. Flush happens at this point.")
            .defaultValue(15)
            .min(5)
            .sliderRange(5, 40)
            .visible(() -> mode.get() == Mode.Blink)
            .build());

    private final Setting<Double> minDistance = sgBlink.add(new DoubleSetting.Builder()
            .name("Min Distance")
            .description("Minimum movement distance before flush (blocks)")
            .defaultValue(3.0)
            .min(0.5)
            .sliderRange(0.5, 10.0)
            .visible(() -> mode.get() == Mode.Blink)
            .build());

    private final Setting<Boolean> autoSprint = sgBlink.add(new BoolSetting.Builder()
            .name("Auto Sprint")
            .description("Automatically sprint while charging for faster movement")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Blink)

            .build());

    private final Setting<Boolean> blinkAimbot = sgBlink.add(new BoolSetting.Builder()
            .name("AimBot")
            .description("Automatically aim at nearest target")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Blink)
            .build());

    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private boolean isBlinking = false;
    private boolean isFlushing = false;
    private int blinkTimer = 0;
    private Vec3d startPos = null;
    private boolean wasCharging = false;
    private Entity blinkTarget = null;
    private Entity crosshairTarget;

    public SpearKill() {
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
        blinkTimer = 0;
        startPos = null;
        wasCharging = false;
        blinkTarget = null;
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
                if (spearcheattargetPlayers.get() && !(crosshairTarget instanceof PlayerEntity)) return;

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

        if (currentlyCharging && blinkAimbot.get()) {
            if (blinkTarget == null || !blinkTarget.isAlive()) {
                blinkTarget = target();
            }

            if (blinkTarget != null && blinkTarget instanceof LivingEntity) {
                Vec3d playerPos = mc.player.getEyePos();
                Vec3d targetPos = blinkTarget.getBoundingBox().getCenter();
                Vec3d toTarget = targetPos.subtract(playerPos).normalize();

                float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
                float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));

                mc.player.setYaw(yaw);
                mc.player.setHeadYaw(yaw);
                mc.player.setPitch(pitch);
            }
        } else if (!currentlyCharging) {
            blinkTarget = null;
        }

        if (currentlyCharging && autoSprint.get() && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }

        if (currentlyCharging && !wasCharging) {
            startBlink();
        }

        if (!currentlyCharging && wasCharging) {
            if (isBlinking) {
                doFlush("Released");
            }
            blinkTarget = null;
        }

        wasCharging = currentlyCharging;

        if (isBlinking) {
            blinkTimer++;

            Vec3d currentPos = mc.player.getEntityPos();
            Vec3d currentLookDir = mc.player.getRotationVec(1.0f);
            double distance = startPos != null ? startPos.distanceTo(currentPos) : 0;

            Vec3d movement = startPos != null ? currentPos.subtract(startPos) : Vec3d.ZERO;
            double movementLength = movement.length();

            // Check look direction alignment with movement
            // Server uses: lookDir.dot(motion) - we need movement to align with look
            double lookMovementDot = 0;
            if (movementLength > 0.1) {
                Vec3d movementNorm = movement.normalize();
                lookMovementDot = currentLookDir.dotProduct(movementNorm);
            }

            // If look direction changed too much from movement direction, restart blink
            // (moving backwards or sideways relative to look = bad for damage)
            if (blinkTimer > 3 && movementLength > 0.5 && lookMovementDot < 0.3) {
                // Movement doesn't align with look direction - restart
                flushPackets();  // Send what we have
                startBlink();    // Restart fresh
                return;
            }

            // Check if we should flush
            boolean shouldFlush = false;
            String reason = "";

            // Max delay reached - must flush
            if (blinkTimer >= maxDelayTicks.get()) {
                shouldFlush = true;
                reason = "Max delay";
            }
            // Min delay passed AND good distance moved AND good alignment
            else if (blinkTimer >= minDelayTicks.get() && distance >= minDistance.get() && lookMovementDot >= 0.5) {
                // Check if we have a target in range
                LivingEntity target = findNearestTarget();
                if (target != null && mc.player.distanceTo(target) <= 5.0) {
                    shouldFlush = true;
                    reason = String.format("Target (align:%.1f)", lookMovementDot);
                }
            }

            if (shouldFlush && currentlyCharging) {
                doFlush(reason);
                // Restart blink for continuous damage
                startBlink();
            }
        }
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

        if (mode.get() == Mode.Blink && isBlinking) {
            synchronized (packets) {
                packets.clear();
            }
            startPos = mc.player.getEntityPos();
            blinkTimer = 0;
        }
    }
    private int getReadyTicks(Item item) {
        if (item == Items.WOODEN_SPEAR) return 14;
        else if (item == Items.STONE_SPEAR || item == Items.GOLDEN_SPEAR) return 13;
        else if (item == Items.COPPER_SPEAR) return 12;
        else if (item == Items.IRON_SPEAR) return 11;
        else if (item == Items.DIAMOND_SPEAR) return 9;
        else if (item == Items.NETHERITE_SPEAR) return 7;
        else return 14;
    }
    private void startBlink() {
        isBlinking = true;
        blinkTimer = 0;
        startPos = mc.player.getEntityPos();
        synchronized (packets) {
            packets.clear();
        }
    }
    private void doFlush(String reason) {
        if (!isBlinking) return;

        flushPackets();

        isBlinking = false;
        blinkTimer = 0;
        startPos = null;
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

            // Blink mode: Send only the final position packet
            // Server sees: large movement from delayed position to current position
            // This creates high velocity = high damage
            PlayerMoveC2SPacket endPacket = new PlayerMoveC2SPacket.Full(
                    currentPos.x,
                    currentPos.y,
                    currentPos.z,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    true,
                    mc.player.horizontalCollision
            );
            mc.player.networkHandler.sendPacket(endPacket);

            packets.clear();
            isFlushing = false;
        }
    }
    private LivingEntity findNearestTarget() {
        if (mc.player == null || mc.world == null) return null;

        Vec3d lookDir = mc.player.getRotationVec(1.0f);
        double searchRange = 6.0;

        Box searchBox = mc.player.getBoundingBox().expand(searchRange);

        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.isDead() || living.getHealth() <= 0) continue;

            // Check if roughly in front of player (wider angle for better detection)
            Vec3d toTarget = entity.getEntityPos().subtract(mc.player.getEntityPos()).normalize();
            double dot = lookDir.dotProduct(toTarget);
            if (dot < 0.3) continue;  // ~72 degree cone

            double dist = mc.player.distanceTo(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = living;
            }
        }

        return nearest;
    }
    private Entity target() {
        if (mc.player == null || mc.world == null) return null;
        if (mc.crosshairTarget instanceof EntityHitResult hit) {
            if (!spearcheattargetPlayers.get() || hit.getEntity() instanceof PlayerEntity) return hit.getEntity();
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

            if (spearcheattargetPlayers.get() && !(e instanceof PlayerEntity)) continue;

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
}