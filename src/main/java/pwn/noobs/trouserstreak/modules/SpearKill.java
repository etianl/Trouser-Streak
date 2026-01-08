//Written by etianl (Lunge mode) and Kimtaeho (Blink mode)
//Kimtaeho: https://github.com/needitem
//etianl: https://github.com/etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
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

public class SpearKill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBlink = settings.createGroup("Blink Options");
    private final SettingGroup sgLunge = settings.createGroup("Lunge Options");
    private final SettingGroup sgBlinkLunge = settings.createGroup("Blink Lunge Options");


    public enum Mode {
        Lunge,
        Blink
    }

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Mode")
            .description("Lunge=velocity boost, Blink=packet delay.")
            .defaultValue(Mode.Lunge)
            .build()
    );

    public final Setting<Double> maxrange = sgGeneral.add(new DoubleSetting.Builder()
            .name("Max Targeting Range")
            .description("How far in blocks that entities can still be targeted.")
            .defaultValue(256)
            .min(0)
            .sliderRange(0, 512)
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
    private final Setting<Boolean> blinkLunge = sgBlinkLunge.add(new BoolSetting.Builder()
            .name("Blink+Lunge")
            .description("Combine Blink mode with velocity based lunging.")
            .defaultValue(false)
            .visible(() -> mode.get() == Mode.Blink)
            .build()
    );

    private final Setting<Double> blinkLungeStrength = sgBlinkLunge.add(new DoubleSetting.Builder()
            .name("Lunge Strength")
            .description("Velocity applied towards target")
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 2.0)
            .visible(() -> mode.get() == Mode.Blink && blinkLunge.get())
            .build()
    );

    private final Setting<Integer> blinkLungeTicks = sgBlinkLunge.add(new IntSetting.Builder()
            .name("Lunge Delay")
            .description("Ticks to charge before lunging")
            .defaultValue(15)
            .min(1)
            .sliderRange(1, 30)
            .visible(() -> mode.get() == Mode.Blink && blinkLunge.get())
            .build()
    );
    private final Setting<Double> flushRange = sgBlink.add(new DoubleSetting.Builder()
            .name("Flush Range")
            .description("Distance to target when flush occurs (blocks)")
            .defaultValue(3.0)
            .min(1.0)
            .sliderRange(1.0, 10.0)
            .visible(() -> mode.get() == Mode.Blink)
            .build());

    private final Setting<Double> maxflushRange = sgBlink.add(new DoubleSetting.Builder()
            .name("Force Flush Distance")
            .description("Distance to force a flush")
            .defaultValue(9.5)
            .min(1.0)
            .sliderRange(1.0, 20.0)
            .visible(() -> mode.get() == Mode.Blink && !blinkLunge.get())
            .build());

    private final Setting<Boolean> blinkAimbot = sgBlink.add(new BoolSetting.Builder()
            .name("Aimbot")
            .description("Lock on to target")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Blink)
            .build()
    );

    private final Setting<Double> blinkDistanceBoost = sgBlink.add(new DoubleSetting.Builder()
            .name("Distance Boost")
            .description("Extra blocks to add to start position (extends the travel distance)")
            .defaultValue(0.0)
            .min(0.0)
            .sliderRange(0.0, 10.0)
            .visible(() -> mode.get() == Mode.Blink)
            .build()
    );

    public final Setting<Double> getLungeStrength = sgLunge.add(new DoubleSetting.Builder()
            .name("SpearVelocity")
            .description("The amount of velocity applied to the player, in the direction of the target.")
            .defaultValue(5)
            .min(0)
            .sliderRange(1, 10)
            .visible(() -> mode.get() == Mode.Lunge)
            .build()
    );

    private final Setting<Boolean> stop = sgLunge.add(new BoolSetting.Builder()
            .name("Stop on target")
            .description("Stops the lunge when you reach the target.")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Lunge)
            .build()
    );

    public final Setting<Double> stopDistance = sgLunge.add(new DoubleSetting.Builder()
            .name("Stop Distance")
            .description("Distance between your hitbox and the entities hitbox to attempt to stop at.")
            .defaultValue(2)
            .min(0)
            .sliderRange(0, 10)
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

    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private boolean isBlinking = false;
    private boolean isFlushing = false;
    private Vec3d startPos = null;
    private boolean wasCharging = false;
    private double lastTargetDistance = Double.MAX_VALUE;
    private boolean wasApproaching = false;
    private Entity killtarget;
    private int blinkChargeTicks = 0;
    private int flushCooldown = 0;

    public SpearKill() {
        super(Trouser.Main, "SpearKill", "Increases spear damage! Lunge mode uses velocity and Blink mode delays packets. Thank you to Kimtaeho for Blink mode!");
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
        lastTargetDistance = Double.MAX_VALUE;
        wasApproaching = false;
        killtarget = null;
        blinkChargeTicks = 0;
        flushCooldown = 0;
    }

    private boolean isHoldingSpear() {
        if (mc.player == null) return false;
        String itemName = mc.player.getMainHandStack().getItem().toString().toLowerCase();
        return itemName.contains("spear");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        boolean currentlyCharging = isHoldingSpear() && mc.player.isUsingItem();

        if (mode.get() == Mode.Lunge) {
            if (currentlyCharging) {
                if (killtarget == null) killtarget = target();
                if (killtarget != null && !killtarget.isAlive()) killtarget = null;
                if (killtarget == null || !(killtarget instanceof LivingEntity)) return;
                if (!isValidTarget(killtarget)) return;
                LUNGE();
            } else {
                killtarget = null;
            }
            return;
        }

        if (currentlyCharging) {
            blinkChargeTicks++;
            if (killtarget == null || !killtarget.isAlive() || !canSeeTarget(killtarget)) {
                killtarget = target();
                lastTargetDistance = killtarget != null ? mc.player.distanceTo(killtarget) : Double.MAX_VALUE;
                wasApproaching = false;
            }
        } else {
            blinkChargeTicks = 0;
            killtarget = null;
            lastTargetDistance = Double.MAX_VALUE;
            wasApproaching = false;
        }

        if (currentlyCharging && blinkAimbot.get() && killtarget != null) {
            rotateToTarget(killtarget);
        }

        // Blink Lunge: Launch towards target after charge delay
        // Stop lunging after flush to allow re-approach for next hit
        if (currentlyCharging && blinkLunge.get() && killtarget != null && flushCooldown == 0) {
            if (blinkChargeTicks >= blinkLungeTicks.get()) {
                rotateToTarget(killtarget);
                Vec3d viewDir = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
                mc.player.setSprinting(true);
                mc.player.setVelocity(viewDir.multiply(blinkLungeStrength.get()));
            }
        }

        // Decrease flush cooldown
        if (flushCooldown > 0) {
            flushCooldown--;
        }

        if (currentlyCharging && !wasCharging) {
            startBlink();
        }

        if (!currentlyCharging && wasCharging) {
            if (isBlinking) {
                if (killtarget != null) {
                    rotateToTarget(killtarget);
                }
                flushPackets();
                isBlinking = false;
                startPos = null;
            }
        }

        wasCharging = currentlyCharging;

        if (isBlinking && killtarget != null && currentlyCharging) {
            double currentDistance = mc.player.distanceTo(killtarget);
            boolean isApproaching = currentDistance < lastTargetDistance;
            boolean shouldFlush = false;

            if (currentDistance <= flushRange.get()) {
                shouldFlush = true;
            } else if (wasApproaching && !isApproaching && currentDistance < 8.0) {
                shouldFlush = true;
            } else if (!blinkLunge.get() && startPos != null && new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(startPos) >= maxflushRange.get()) {
                // Only force flush when NOT using blink lunge
                flushPackets();
                startBlink();
            }

            if (shouldFlush) {
                rotateToTarget(killtarget);
                flushPackets();

                // Set cooldown to stop lunge temporarily after flush
                if (blinkLunge.get()) {
                    flushCooldown = blinkLungeTicks.get();  // Wait same amount as lunge delay before lunging again
                }

                // Restart blink for continuous damage
                isBlinking = true;
                startPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                synchronized (packets) {
                    packets.clear();
                }
                lastTargetDistance = mc.player.distanceTo(killtarget);
                wasApproaching = false;
            } else {
                lastTargetDistance = currentDistance;
                wasApproaching = isApproaching;
            }
        }
    }

    private void LUNGE() {
        int readyTicks = getReadyTicks(mc.player.getMainHandStack().getItem());
        rotateToTarget(killtarget);
        if (mc.player.getItemUseTime() > readyTicks) {
            if (stop.get()) {
                Box playerBox = mc.player.getBoundingBox().expand(stopDistance.get());
                Box targetBox = killtarget.getBoundingBox();
                if (!playerBox.intersects(targetBox)) {
                    double lungeSpeed = getLungeStrength.get();
                    Vec3d viewDir = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
                    mc.player.setSprinting(true);
                    mc.player.setVelocity(viewDir.multiply(lungeSpeed));
                } else {
                    killtarget = null;
                    mc.player.setVelocity(0, 0, 0);
                    mc.player.setSprinting(false);
                }
            } else {
                double lungeSpeed = getLungeStrength.get();
                Vec3d viewDir = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
                mc.player.setSprinting(true);
                mc.player.setVelocity(viewDir.multiply(lungeSpeed));
            }
        }
    }

    private void rotateToTarget(Entity target) {
        if (mc.player == null || target == null) return;
        Vec3d playerPos = mc.player.getEyePos();
        Box box = target.getBoundingBox();

        // Calculate height difference
        double targetCenterY = box.getCenter().y;
        double heightDiff = targetCenterY - playerPos.y;

        // Adjust aim point based on height difference
        // If target is above: aim lower (towards their feet)
        // If target is below: aim higher (towards their head)
        // Scale: more height diff = more offset
        double targetY;
        double boxHeight = box.maxY - box.minY;

        if (Math.abs(heightDiff) < 1.0) {
            // Same level - aim at center
            targetY = targetCenterY;
        } else if (heightDiff > 0) {
            // Target above - aim at lower part
            // More height diff = aim lower
            double offset = Math.min(heightDiff / 5.0, 0.4); // max 40% down from center
            targetY = targetCenterY - (boxHeight * offset);
        } else {
            // Target below - aim at upper part
            double offset = Math.min(-heightDiff / 5.0, 0.4); // max 40% up from center
            targetY = targetCenterY + (boxHeight * offset);
        }

        Vec3d targetPos = new Vec3d(box.getCenter().x, targetY, box.getCenter().z);
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
            startPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            lastTargetDistance = killtarget != null ? mc.player.distanceTo(killtarget) : Double.MAX_VALUE;
            wasApproaching = false;
        }
    }

    private int getReadyTicks(Item item) {
        String name = item.toString().toLowerCase();
        int value = 14;
        if (name.contains("wooden")) value = 14;
        else if (name.contains("stone") || name.contains("golden")) value = 13;
        else if (name.contains("copper")) value = 12;
        else if (name.contains("iron")) value = 11;
        else if (name.contains("diamond")) value = 9;
        else if (name.contains("netherite")) value = 7;
        return Math.round(value * (getreadyticksModifier.get() / 100.0f));
    }

    private void startBlink() {
        isBlinking = true;
        startPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        synchronized (packets) {
            packets.clear();
        }
        lastTargetDistance = killtarget != null ? mc.player.distanceTo(killtarget) : Double.MAX_VALUE;
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
            Vec3d currentPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            double distance = startPos != null ? startPos.distanceTo(currentPos) : 0;
            if (distance < flushRange.get()) {
                packets.clear();
                isFlushing = false;
                return;
            }

            // Calculate boosted start position if boost is enabled
            // Raycast to find max valid position (avoid walls)
            Vec3d sendStartPos = startPos;
            double boost = blinkDistanceBoost.get();
            if (boost > 0 && startPos != null) {
                Vec3d direction = currentPos.subtract(startPos);
                Vec3d horizontalDir = new Vec3d(direction.x, 0, direction.z).normalize();
                if (horizontalDir.length() > 0.01) {
                    // Target position with full boost
                    Vec3d targetPos = startPos.subtract(horizontalDir.multiply(boost));

                    // Raycast from startPos backwards to check for walls
                    HitResult hit = mc.world.raycast(new RaycastContext(
                            startPos, targetPos,
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            mc.player
                    ));

                    if (hit.getType() == HitResult.Type.MISS) {
                        // No wall, use full boost
                        sendStartPos = targetPos;
                    } else {
                        // Wall found, use position just before wall
                        sendStartPos = hit.getPos().add(horizontalDir.multiply(0.5));
                    }
                }
            }

            if (sendStartPos != null) {
                PlayerMoveC2SPacket startPacket = new PlayerMoveC2SPacket.Full(
                        sendStartPos.x, sendStartPos.y, sendStartPos.z,
                        mc.player.getYaw(), mc.player.getPitch(), false, false
                );
                mc.player.networkHandler.sendPacket(startPacket);
            }
            if (chatFeedback) {
                double totalDist = sendStartPos != null ? sendStartPos.distanceTo(currentPos) : distance;
                info("Flush: %.1f blocks (actual=%.1f, boost=%.1f)", totalDist, distance, boost);
            }
            PlayerMoveC2SPacket endPacket = new PlayerMoveC2SPacket.Full(
                    currentPos.x, currentPos.y, currentPos.z,
                    mc.player.getYaw(), mc.player.getPitch(),
                    mc.player.isOnGround(), mc.player.horizontalCollision
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
                eyePos, targetCenter,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player
        ));
        if (result.getType() == HitResult.Type.MISS) return true;
        return eyePos.distanceTo(result.getPos()) >= eyePos.distanceTo(targetCenter) - 0.5;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null) return false;
        EntityType<?> type = entity.getType();
        boolean inList = targetEntities.get().contains(type);
        if (targetListMode.get() == TargetListMode.Whitelist) {
            return inList || targetEntities.get().isEmpty();
        } else {
            return !inList;
        }
    }

    private boolean chatFeedback = true;
}