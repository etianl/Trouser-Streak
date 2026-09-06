//Written by etianl (Lunge mode) and Kimtaeho (Blink mode)
//Kimtaeho: https://github.com/needitem
//etianl: https://github.com/etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;

public class SpearKill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBlink = settings.createGroup("Blink Options");
    private final SettingGroup sgLunge = settings.createGroup("Lunge Options");
    private final SettingGroup sgBlinkLunge = settings.createGroup("Blink Lunge Options");

    private final Setting<Boolean> nonofall = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable NoFall While Charging")
            .description("Prevents fall damage when lunging downward quickly.")
            .defaultValue(true)
            .build()
    );
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
            .build()
    );
    private final Setting<Set<EntityType<?>>> targetEntities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("Target Entities")
            .description("Entities to whitelist/blacklist")
            .onlyAttackable()
            .build()
    );
    private final Setting<Boolean> ignorefriends = sgGeneral.add(new BoolSetting.Builder()
            .name("Ignore Friends")
            .description("Do not Spear Kill friends.")
            .defaultValue(true)
            .build()
    );

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

    public enum lungeMode {
        DirectionBased,
        FromAbove,
        Auto_FromAboveFirst
    }
    private final Setting<lungeMode> LungeDirectionMode = sgLunge.add(new EnumSetting.Builder<lungeMode>()
            .name("Lunge Direction")
            .description("DirectionBased = toward look direction, FromAbove = Stab from above, Auto_FromAboveFirst = Stab from above, unless the above position or path to target from above position is invalid.")
            .defaultValue(lungeMode.DirectionBased)
            .build()
    );
    private final Setting<Double> aboveHeight = sgLunge.add(new DoubleSetting.Builder()
            .name("Above Height")
            .description("Blocks above target center.")
            .defaultValue(10.0)
            .min(5)
            .sliderRange(5, 50)
            .visible(() -> mode.get() == Mode.Lunge && (LungeDirectionMode.get() == lungeMode.FromAbove || LungeDirectionMode.get() == lungeMode.Auto_FromAboveFirst))
            .build()
    );
    private final Setting<Double> aboveHeightdistance = sgLunge.add(new DoubleSetting.Builder()
            .name("Above Height Trigger Distance")
            .description("If within this distance of the above target position, start the lunge toward the target.")
            .defaultValue(3.0)
            .min(1)
            .sliderRange(1, 10)
            .visible(() -> mode.get() == Mode.Lunge && (LungeDirectionMode.get() == lungeMode.FromAbove || LungeDirectionMode.get() == lungeMode.Auto_FromAboveFirst))
            .build()
    );
    private final Setting<Boolean> checkdistanceinvalid = sgLunge.add(new BoolSetting.Builder()
            .name("Trigger Distance Validition")
            .description("Checks if the area around the above position is valid with using a radius of Above Height Trigger Distance.")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Lunge && LungeDirectionMode.get() == lungeMode.Auto_FromAboveFirst)
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

    private final List<ServerboundMovePlayerPacket> packets = new ArrayList<>();
    private boolean isBlinking = false;
    private boolean isFlushing = false;
    private Vec3 startPos = null;
    private boolean wasCharging = false;
    private double lastTargetDistance = Double.MAX_VALUE;
    private boolean wasApproaching = false;
    private Entity killtarget;
    private int blinkChargeTicks = 0;
    private int flushCooldown = 0;
    private boolean firstPhase = false;
    private Vec3 aboveTargetPos = null;
    private boolean wasNoFallEnabled = false;
    private boolean noFallToggled = false;

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
        firstPhase = false;
        aboveTargetPos = null;
        if (noFallToggled && wasNoFallEnabled) {
            Modules.get().get(NoFall.class).toggle();
        }
        noFallToggled = false;
        wasNoFallEnabled = false;
    }

    private boolean isUsingSpear() {
        if (mc.player == null) return false;
        String itemName = mc.player.getUseItem().getItem().toString().toLowerCase();
        return itemName.contains("spear");
    }

    private boolean currentlyCharging = false;
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        currentlyCharging = isUsingSpear();

        if (nonofall.get()){
            if (currentlyCharging && !noFallToggled) {
                wasNoFallEnabled = Modules.get().get(NoFall.class).isActive();
                if (wasNoFallEnabled) {
                    Modules.get().get(NoFall.class).toggle();
                    noFallToggled = true;
                }
            } else if (!currentlyCharging && noFallToggled) {
                if (wasNoFallEnabled) {
                    Modules.get().get(NoFall.class).toggle();
                }
                noFallToggled = false;
            }
        }

        if (mode.get() == Mode.Lunge) return;

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
            } else if (!blinkLunge.get() && startPos != null && new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(startPos) >= maxflushRange.get()) {
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
                startPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
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
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;

        if (mode.get() == Mode.Lunge) {
            if (currentlyCharging) {
                if (killtarget == null) killtarget = target();
                if (killtarget != null && !killtarget.isAlive()) {
                    if (stop.get()) {
                        mc.player.setDeltaMovement(0, 0, 0);
                        mc.player.setSprinting(false);
                    }
                    killtarget = null;
                    firstPhase = false;
                    aboveTargetPos = null;
                }
                if (killtarget == null || !(killtarget instanceof LivingEntity)) return;
                if (!isValidTarget(killtarget)) return;
                LUNGE();
            } else {
                killtarget = null;
                firstPhase = false;
                aboveTargetPos = null;
            }
            return;
        }

        if (currentlyCharging && blinkAimbot.get() && killtarget != null) {
            rotateToTarget(killtarget);
        }

        // Blink Lunge: Launch towards target after charge delay
        // Stop lunging after flush to allow re-approach for next hit
        if (currentlyCharging && blinkLunge.get() && killtarget != null && flushCooldown == 0) {
            if (blinkChargeTicks >= blinkLungeTicks.get()) {
                rotateToTarget(killtarget);
                Vec3 viewDir = Vec3.directionFromRotation(mc.player.getXRot(), mc.player.getYRot());
                mc.player.setSprinting(true);
                mc.player.setDeltaMovement(viewDir.scale(blinkLungeStrength.get()));
            }
        }
    }

    private void LUNGE() {
        int readyTicks = mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? getReadyTicks(mc.player.getMainHandItem().getItem())
                : getReadyTicks(mc.player.getOffhandItem().getItem());

        rotateToTarget(killtarget);

        if (mc.player.getTicksUsingItem() > readyTicks) {
            AABB playerBox = mc.player.getBoundingBox().inflate(stopDistance.get());
            AABB targetBox = killtarget.getBoundingBox();
            boolean atTarget = playerBox.intersects(targetBox);

            if (atTarget) {
                if (stop.get()) {
                    killtarget = null;
                    mc.player.setDeltaMovement(0, 0, 0);
                    mc.player.setSprinting(false);
                }
                firstPhase = false;
                aboveTargetPos = null;
                return;
            }

            double lungeSpeed = getLungeStrength.get();
            Vec3 viewDir;

            if (killtarget == null) return;
            switch (LungeDirectionMode.get()) {
                case DirectionBased -> {
                    Vec3 targetDir = killtarget.getBoundingBox().getCenter().subtract(mc.player.position()).normalize();
                    viewDir = targetDir;

                    mc.player.setSprinting(true);
                    mc.player.setDeltaMovement(viewDir.scale(lungeSpeed));
                }
                case FromAbove -> {
                    if (!firstPhase || aboveTargetPos == null) {
                        Vec3 targetCenter = killtarget.getBoundingBox().getCenter();
                        aboveTargetPos = new Vec3(targetCenter.x, targetCenter.y + aboveHeight.get(), targetCenter.z);
                        firstPhase = true;
                    }

                    Vec3 playerPos = mc.player.position();
                    double distToAbove = playerPos.distanceTo(aboveTargetPos);

                    if (distToAbove < aboveHeightdistance.get()) {
                        Vec3 targetDir = killtarget.getBoundingBox().getCenter().subtract(playerPos).normalize();
                        viewDir = targetDir;
                        firstPhase = false;
                    } else {
                        Vec3 aboveDir = aboveTargetPos.subtract(playerPos).normalize();
                        viewDir = aboveDir;
                    }

                    mc.player.setSprinting(true);
                    mc.player.setDeltaMovement(viewDir.scale(lungeSpeed));
                }
                case Auto_FromAboveFirst -> {
                    if (!firstPhase || aboveTargetPos == null) {
                        Vec3 targetCenter = killtarget.getBoundingBox().getCenter();
                        aboveTargetPos = new Vec3(targetCenter.x, targetCenter.y + aboveHeight.get(), targetCenter.z);
                        firstPhase = true;
                    }

                    Vec3 playerPos = mc.player.position();
                    double distToAbove = playerPos.distanceTo(aboveTargetPos);

                    boolean FromAbovePathValid = isFromAbovePathValid(aboveTargetPos, killtarget);

                    if (!FromAbovePathValid) {
                        Vec3 targetDir = killtarget.getBoundingBox().getCenter().subtract(playerPos).normalize();
                        viewDir = targetDir;
                        firstPhase = false;
                    } else if (distToAbove < aboveHeightdistance.get()) {
                        Vec3 targetDir = killtarget.getBoundingBox().getCenter().subtract(playerPos).normalize();
                        viewDir = targetDir;
                        firstPhase = false;
                    } else {
                        Vec3 aboveDir = aboveTargetPos.subtract(playerPos).normalize();
                        viewDir = aboveDir;
                    }

                    mc.player.setSprinting(true);
                    mc.player.setDeltaMovement(viewDir.scale(lungeSpeed));
                }
            }
        }
    }
    private boolean isFromAbovePathValid(Vec3 abovePos, Entity target) {
        if (mc.level == null || abovePos == null) return false;

        Vec3 targetCenter = target.getBoundingBox().getCenter();

        if (invalid(abovePos)) {
            return false;
        }

        if (checkdistanceinvalid.get()) {
            double checkDistance = aboveHeightdistance.get();
            int radius = (int)checkDistance;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Vec3 testPos = abovePos.add(x, y, z);
                        if (testPos.distanceTo(abovePos) <= checkDistance && invalid(testPos)) {
                            return false;
                        }
                    }
                }
            }
        }

        int pathSteps = Math.max(10, (int)(abovePos.distanceTo(targetCenter) * 2.5));
        for (int i = 1; i < pathSteps; i++) {
            double t = i / (double)pathSteps;
            Vec3 sample = abovePos.lerp(targetCenter, t);
            if (invalid(sample)) {
                return false;
            }
        }

        return true;
    }

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    private final Map<Vec3, Boolean> positionCache = new HashMap<>();
    private boolean invalid(Vec3 pos) {
        if (mc.level == null) return true;

        double clampedY = Mth.clamp(pos.y, mc.level.getMinY(), mc.level.getMaxY() - 1);
        if (clampedY != pos.y) return true;

        BlockPos floored = BlockPos.containing(pos);
        int chunkX = floored.getX() >> 4;
        int chunkZ = floored.getZ() >> 4;
        if (mc.level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return true;

        if (positionCache.containsKey(pos)) return positionCache.get(pos);

        Entity entity = mc.player;
        Vec3 delta = pos.subtract(entity.position());
        AABB box = entity.getBoundingBox().move(delta);

        mutablePos.set(floored);
        for (int x = -1; x <= 1; x++) {
            mutablePos.setX(floored.getX() + x);
            for (int y = -1; y <= 1; y++) {
                mutablePos.setY(floored.getY() + y);
                for (int z = -1; z <= 1; z++) {
                    mutablePos.setZ(floored.getZ() + z);
                    BlockState state = mc.level.getBlockState(mutablePos);
                    if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                            || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE)
                            || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)) {
                        positionCache.put(pos, true);
                        return true;
                    }
                }
            }
        }

        for (Entity e : mc.level.getEntities(entity, box)) {
            if (e.canBeCollidedWith(entity)) {
                positionCache.put(pos, true);
                return true;
            }
        }

        boolean collides = mc.level.getBlockCollisions(entity, box).iterator().hasNext();
        positionCache.put(pos, collides);
        return collides;
    }
    private void rotateToTarget(Entity target) {
        if (mc.player == null || target == null) return;
        Vec3 playerPos = mc.player.getEyePosition();
        AABB box = target.getBoundingBox();

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

        Vec3 targetPos = new Vec3(box.getCenter().x, targetY, box.getCenter().z);
        Vec3 toTarget = targetPos.subtract(playerPos).normalize();
        float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));
        mc.player.setYRot(yaw);
        mc.player.setYHeadRot(yaw);
        mc.player.setXRot(pitch);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!Utils.canUpdate()) return;
        if (!(event.packet instanceof ServerboundMovePlayerPacket p)) return;
        if (mode.get() == Mode.Blink && isBlinking && !isFlushing) {
            event.cancel();
            synchronized (packets) {
                if (!packets.isEmpty()) {
                    ServerboundMovePlayerPacket last = packets.get(packets.size() - 1);
                    if (isSamePacket(p, last)) return;
                }
                packets.add(p);
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.level == null) return;
        if (!(event.packet instanceof ClientboundPlayerPositionPacket)) return;
        if (mode.get() == Mode.Blink && isBlinking) {
            synchronized (packets) {
                packets.clear();
            }
            startPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
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
        startPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        synchronized (packets) {
            packets.clear();
        }
        lastTargetDistance = killtarget != null ? mc.player.distanceTo(killtarget) : Double.MAX_VALUE;
        wasApproaching = false;
    }

    private boolean isSamePacket(ServerboundMovePlayerPacket a, ServerboundMovePlayerPacket b) {
        return a.isOnGround() == b.isOnGround()
                && a.getYRot(-1) == b.getYRot(-1)
                && a.getXRot(-1) == b.getXRot(-1)
                && a.getX(-1) == b.getX(-1)
                && a.getY(-1) == b.getY(-1)
                && a.getZ(-1) == b.getZ(-1);
    }

    private void flushPackets() {
        if (mc.player == null || mc.player.connection == null) return;
        synchronized (packets) {
            if (packets.isEmpty()) return;
            isFlushing = true;
            Vec3 currentPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            double distance = startPos != null ? startPos.distanceTo(currentPos) : 0;
            if (distance < flushRange.get()) {
                packets.clear();
                isFlushing = false;
                return;
            }

            // Calculate boosted start position if boost is enabled
            // Raycast to find max valid position (avoid walls)
            Vec3 sendStartPos = startPos;
            double boost = blinkDistanceBoost.get();
            if (boost > 0 && startPos != null) {
                Vec3 direction = currentPos.subtract(startPos);
                Vec3 horizontalDir = new Vec3(direction.x, 0, direction.z).normalize();
                if (horizontalDir.length() > 0.01) {
                    // Target position with full boost
                    Vec3 targetPos = startPos.subtract(horizontalDir.scale(boost));

                    // Raycast from startPos backwards to check for walls
                    HitResult hit = mc.level.clip(new ClipContext(
                            startPos, targetPos,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            mc.player
                    ));

                    if (hit.getType() == HitResult.Type.MISS) {
                        // No wall, use full boost
                        sendStartPos = targetPos;
                    } else {
                        // Wall found, use position just before wall
                        sendStartPos = hit.getLocation().add(horizontalDir.scale(0.5));
                    }
                }
            }

            if (sendStartPos != null) {
                ServerboundMovePlayerPacket startPacket = new ServerboundMovePlayerPacket.PosRot(
                        sendStartPos.x, sendStartPos.y, sendStartPos.z,
                        mc.player.getYRot(), mc.player.getXRot(), false, false
                );
                mc.player.connection.send(startPacket);
            }
            if (chatFeedback) {
                double totalDist = sendStartPos != null ? sendStartPos.distanceTo(currentPos) : distance;
                info("Flush: %.1f blocks (actual=%.1f, boost=%.1f)", totalDist, distance, boost);
            }
            ServerboundMovePlayerPacket endPacket = new ServerboundMovePlayerPacket.PosRot(
                    currentPos.x, currentPos.y, currentPos.z,
                    mc.player.getYRot(), mc.player.getXRot(),
                    mc.player.onGround(), mc.player.horizontalCollision
            );
            mc.player.connection.send(endPacket);
            packets.clear();
            isFlushing = false;
        }
    }

    private Entity target() {
        if (mc.player == null || mc.level == null) return null;
        if (mc.hitResult instanceof EntityHitResult hit) {
            if (isValidTarget(hit.getEntity())) return hit.getEntity();
        }
        double maxRange = maxrange.get();
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        HitResult blockHit = mc.level.clip(new ClipContext(eyePos,
                eyePos.add(lookVec.scale(maxRange)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player));
        double rayLength = blockHit.getType() == HitResult.Type.MISS ? maxRange :
                eyePos.distanceTo(blockHit.getLocation());
        List<Entity> candidates = mc.level.getEntities(mc.player,
                mc.player.getBoundingBox().expandTowards(lookVec.scale(rayLength)),
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player);
        candidates.sort(Comparator.comparingDouble(e ->
                eyePos.distanceToSqr(e.getBoundingBox().getCenter())));
        double coneAngle = 0.999;
        for (Entity e : candidates) {
            double dist = eyePos.distanceTo(e.getBoundingBox().getCenter());
            if (dist > maxRange) break;
            if (!isValidTarget(e)) continue;
            if (!canSeeTarget(e)) continue;
            Vec3 toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();
            if (lookVec.dot(toEntity) > coneAngle) {
                return e;
            }
        }
        return null;
    }

    private boolean canSeeTarget(Entity target) {
        if (mc.player == null || mc.level == null) return false;
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        HitResult result = mc.level.clip(new ClipContext(
                eyePos, targetCenter,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player
        ));
        if (result.getType() == HitResult.Type.MISS) return true;
        return eyePos.distanceTo(result.getLocation()) >= eyePos.distanceTo(targetCenter) - 0.5;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null) return false;

        if (entity instanceof Player player && ignorefriends.get() && Friends.get().isFriend(player)) {
            return false;
        }

        EntityType<?> type = entity.getType();
        boolean inList = targetEntities.get().contains(type);
        if (targetListMode.get() == TargetListMode.Whitelist) {
            return inList || targetEntities.get().isEmpty();
        } else {
            return !inList;
        }
    }
}