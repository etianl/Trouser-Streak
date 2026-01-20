//Written by [agreed](https://github.com/aisiaiiad), Bow Machinegun and some other options/features by etianl.
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class ProjectileTeleport extends Module {
    private boolean paction = false;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgTP = settings.createGroup("Teleport Options");
    private final SettingGroup sgArrowFire = settings.createGroup("Bow Options");

    private final ItemListSetting projectileItems = sgGeneral.add(new ItemListSetting.Builder()
            .name("projectile-items")
            .description("Which items to apply extra velocity to.")
            .defaultValue(
                    Items.ENDER_PEARL,
                    Items.SPLASH_POTION,
                    Items.LINGERING_POTION,
                    Items.EXPERIENCE_BOTTLE,
                    Items.SNOWBALL,
                    Items.EGG,
                    Items.WIND_CHARGE,
                    Items.BOW,
                    Items.TRIDENT
            )
            .filter(item -> item == Items.ENDER_PEARL
                    || item == Items.SPLASH_POTION
                    || item == Items.LINGERING_POTION
                    || item == Items.EXPERIENCE_BOTTLE
                    || item == Items.SNOWBALL
                    || item == Items.EGG
                    || item == Items.WIND_CHARGE
                    || item == Items.BOW
                    || item == Items.TRIDENT
            )
            .build()
    );
    private final Setting<Integer> interactAmount = sgGeneral.add(
            new IntSetting.Builder()
                    .name("interact-amount")
                    .defaultValue(1)
                    .min(0)
                    .sliderMax(15)
                    .build()
    );
    private final Setting<Boolean> skipCollisionCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("BoatNoclip skip collision check")
            .description("If BoatNoclip is on and you are in a boat skip collision checks for blocks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(
            new EntityTypeListSetting.Builder()
                    .name("entities")
                    .description("Entities to target.")
                    .onlyAttackable()
                    .defaultValue(EntityType.PLAYER)
                    .build()
    );
    public final Setting<Boolean> friends = sgTargeting.add(new BoolSetting.Builder()
            .name("Attack Friends")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> prePackets = sgTP.add(
            new IntSetting.Builder()
                    .name("# spam packets")
                    .defaultValue(8)
                    .min(0)
                    .sliderMax(20)
                    .build()
    );
    private final Setting<Double> teleportHeight = sgTP.add(
        new DoubleSetting.Builder()
            .name("teleport-height")
                .description("Teleport you up to this high for increased velocity for projectiles.")
                .defaultValue(60.0)
            .min(5.0)
            .sliderMax(130.0)
            .build()
    );

    private final Setting<Double> maxDistance = sgTP.add(
            new DoubleSetting.Builder()
                    .name("max-distance")
                    .description("Teleport you up to this far to targets.")
                    .defaultValue(60.0)
                    .min(5.0)
                    .sliderMax(120.0)
                    .build()
    );
    private final Setting<Double> targetoffset = sgTP.add(
            new DoubleSetting.Builder()
                    .name("Above target offset")
                    .description("Offset you this much above the target.")
                    .defaultValue(0.01)
                    .min(0)
                    .sliderMax(10.0)
                    .build()
    );
    private final Setting<Double> offsethorizontal = sgTP.add(new DoubleSetting.Builder()
            .name("Horizontal Offset")
            .description("Offset you this much from the start position.")
            .defaultValue(0.05)
            .min(0.001)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Double> offsetY = sgTP.add(new DoubleSetting.Builder()
            .name("Y Offset")
            .description("Offset you this much from the start position.")
            .defaultValue(0.01)
            .min(0.001)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Boolean> arrowFire = sgArrowFire.add(new BoolSetting.Builder()
            .name("Bow Machinegun")
            .description("Automatically fires arrows while holding bow.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> chargeTicks = sgArrowFire.add(new IntSetting.Builder()
            .name("charge-ticks")
            .description("Ticks to charge bow before firing.")
            .defaultValue(4)
            .min(4)
            .sliderRange(4,60)
            .visible(arrowFire::get)
            .build()
    );

    public ProjectileTeleport() {
        super(Trouser.Main, "projectile-teleport", "Teleport to entities to launch a projectile at them with high velocity. Works best on Paper servers.");
    }
    private boolean isChargingBow = false;
    private int bowChargeTimer = 0;
    private boolean wasHoldingBow = false;
    private Entity target = null;
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
    @EventHandler
    private void onTick(TickEvent.Post event) {
        target = findClosestTarget();

        if (!arrowFire.get() || mc.player == null || mc.interactionManager == null) return;

        boolean holdingBow = mc.player.getMainHandStack().getItem() instanceof BowItem
                || mc.player.getOffHandStack().getItem() instanceof BowItem;

        if (holdingBow && !wasHoldingBow) {
            bowChargeTimer = 0;
        }

        if (isChargingBow && holdingBow) {
            bowChargeTimer++;
            if (bowChargeTimer >= chargeTicks.get()) {
                fireArrowTick();
                bowChargeTimer = 0;
            }
        } else {
            bowChargeTimer = 0;
        }

        wasHoldingBow = holdingBow;
    }
    private void fireArrowTick() {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                Direction.DOWN, mc.player.getInventory().selectedSlot
        ));
    }
    private boolean executingInteract = false;
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (event.packet instanceof PlayerInteractItemC2SPacket packet) {
            ItemStack stack = packet.getHand() == Hand.MAIN_HAND
                    ? mc.player.getMainHandStack()
                    : mc.player.getOffHandStack();

            Item item = stack.getItem();

            if (isValidProjectile(item)) {
                if (executingInteract) return;
                executingInteract = true;
                event.cancel();
                try {
                    interact(packet.getHand());
                } finally {
                    executingInteract = false;
                }
            } else if ((item instanceof BowItem && projectileItems.get().contains(Items.BOW))){
                isChargingBow = true;
            }
        }

        if (event.packet instanceof PlayerActionC2SPacket packet && !paction) {
            if (packet.getAction() != PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) return;

            Item item = mc.player.getMainHandStack().getItem();
            Item item2 = mc.player.getOffHandStack().getItem();
            if (!isAction(item) && !isAction(item2)) return;

            isChargingBow = false;
            event.cancel();
            action(packet.getAction());
        }
    }

    private boolean isValidProjectile(Item item) {
        return (item instanceof EnderPearlItem && projectileItems.get().contains(Items.ENDER_PEARL)) ||
                (item instanceof SplashPotionItem && projectileItems.get().contains(Items.SPLASH_POTION)) ||
                (item instanceof LingeringPotionItem && projectileItems.get().contains(Items.LINGERING_POTION)) ||
                (item instanceof ExperienceBottleItem && projectileItems.get().contains(Items.EXPERIENCE_BOTTLE)) ||
                (item instanceof SnowballItem && projectileItems.get().contains(Items.SNOWBALL)) ||
                (item instanceof WindChargeItem && projectileItems.get().contains(Items.WIND_CHARGE)) ||
                (item instanceof EggItem && projectileItems.get().contains(Items.EGG));
    }

    public boolean isAction(Item item) {
        return (item instanceof BowItem && projectileItems.get().contains(Items.BOW)) || (item instanceof TridentItem && projectileItems.get().contains(Items.TRIDENT));
    }
    private boolean isValidTarget(Entity entity) {
        Entity playerentity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        return entity.isAlive()
                && playerentity.distanceTo(entity) <= maxDistance.get();
    }
    public void interact(Hand hand) {
        if (target == null || !isValidTarget(target)) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        Vec3d home = entity.getEntityPos();

        if (mc.player.squaredDistanceTo(target) > maxDistance.get() * maxDistance.get()) return;

        Box box = target.getBoundingBox();

        double x = box.getCenter().x;
        double y = box.maxY;
        double z = box.getCenter().z;

        Vec3d topPos = new Vec3d(x, y, z);
        Vec3d aboveHome = home.add(0, teleportHeight.get(), 0);
        Vec3d aboveTarget = topPos.add(0, teleportHeight.get(), 0);
        Vec3d shotPos = topPos.add(0, targetoffset.get(), 0);
        Vec3d homeOffset = getOffset(home);

        if (!validatePath(aboveHome, aboveTarget, shotPos, aboveTarget, aboveHome, homeOffset)) {
            if (chatFeedback)error("One of the teleports are blocked.");
            return;
        }
        if (!hasClearPath(aboveHome, aboveTarget)) {
            if (chatFeedback)error("Vertical path blocked between clip positions.");
        }
        for (int i = 0; i < prePackets.get(); i++) {
            if (entity == mc.player) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true,mc.player.horizontalCollision));
            } else {
                mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(entity));
            }
        }

        moveTo(entity, aboveHome);
        moveTo(entity, aboveTarget);
        moveTo(entity, shotPos);

        float yaw = mc.player.getYaw();

        for (int i = 0; i < interactAmount.get(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(hand, 0, yaw, 90.0f));
        }

        moveTo(entity, aboveTarget);
        moveTo(entity, aboveHome);
        moveTo(entity, home);
        moveTo(entity, homeOffset);

        entity.setPosition(homeOffset);
    }

    public void action(PlayerActionC2SPacket.Action action) {
        if (target == null || !isValidTarget(target)) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        Vec3d home = entity.getEntityPos();

        if (mc.player.squaredDistanceTo(target) > maxDistance.get() * maxDistance.get()) return;

        Box box = target.getBoundingBox();

        double x = box.getCenter().x;
        double y = box.maxY;
        double z = box.getCenter().z;

        Vec3d topPos = new Vec3d(x, y, z);
        Vec3d aboveHome = home.add(0, teleportHeight.get(), 0);
        Vec3d aboveTarget = topPos.add(0, teleportHeight.get(), 0);
        Vec3d shotPos = topPos.add(0, targetoffset.get(), 0);
        Vec3d homeOffset = getOffset(home);

        if (!validatePath(aboveHome, aboveTarget, shotPos, aboveTarget, aboveHome, homeOffset)) {
            if (chatFeedback)error("One of the teleports are blocked.");
            return;
        }

        for (int i = 0; i < prePackets.get(); i++) {
            if (entity == mc.player) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true,mc.player.horizontalCollision));
            } else {
                mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(entity));
            }
        }

        moveTo(entity, aboveHome);
        moveTo(entity, aboveTarget);
        moveTo(entity, shotPos);

        paction = true;
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(action, BlockPos.ORIGIN, Direction.DOWN, 0));
        paction = false;

        moveTo(entity, aboveTarget);
        moveTo(entity, aboveHome);
        moveTo(entity, home);
        moveTo(entity, homeOffset);
    }

    private void moveTo(Entity entity, Vec3d pos) {
        if (entity == mc.player) {
            PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false, false);
            ((IPlayerMoveC2SPacket) packet).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(packet);
        } else {
            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(pos, entity.getYaw(), entity.getPitch(), false));
        }
        entity.setPosition(pos);
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

    private boolean validatePath(Vec3d... positions) {
        for (Vec3d pos : positions) {
            if (invalid(pos)) {
                return false;
            }
        }
        return true;
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
}