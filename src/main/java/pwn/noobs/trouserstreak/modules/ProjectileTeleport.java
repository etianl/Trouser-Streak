//Written by [agreed](https://github.com/aisiaiiad), Bow Machinegun and some other options/features by etianl.
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isValidListTarget(entity)) continue;
            if (friends.get() && entity instanceof Player && Friends.get().isFriend((Player) entity)) continue;
            double dist = entity.distanceToSqr(mc.player);
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

        if (!arrowFire.get() || mc.player == null || mc.gameMode == null) return;

        boolean holdingBow = mc.player.getMainHandItem().getItem() instanceof BowItem
                || mc.player.getOffhandItem().getItem() instanceof BowItem;

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
        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                Direction.DOWN, mc.player.getInventory().getSelectedSlot()
        ));
    }
    private boolean executingInteract = false;
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        if (event.packet instanceof ServerboundUseItemPacket packet) {
            ItemStack stack = packet.getHand() == InteractionHand.MAIN_HAND
                    ? mc.player.getMainHandItem()
                    : mc.player.getOffhandItem();

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

        if (event.packet instanceof ServerboundPlayerActionPacket packet && !paction) {
            if (packet.getAction() != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return;

            Item item = mc.player.getMainHandItem().getItem();
            Item item2 = mc.player.getOffhandItem().getItem();
            if (!isAction(item) && !isAction(item2)) return;

            isChargingBow = false;
            event.cancel();
            action(packet.getAction());
        }
    }

    private boolean isValidProjectile(Item item) {
        return (item instanceof EnderpearlItem && projectileItems.get().contains(Items.ENDER_PEARL)) ||
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
        Entity playerentity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;
        return entity.isAlive()
                && playerentity.distanceTo(entity) <= maxDistance.get();
    }
    public void interact(InteractionHand hand) {
        if (target == null || !isValidTarget(target)) return;
        Entity entity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;
        Vec3 home = entity.position();

        if (mc.player.distanceToSqr(target) > maxDistance.get() * maxDistance.get()) return;

        AABB box = target.getBoundingBox();

        double x = box.getCenter().x;
        double y = box.maxY;
        double z = box.getCenter().z;

        Vec3 topPos = new Vec3(x, y, z);
        Vec3 aboveHome = home.add(0, teleportHeight.get(), 0);
        Vec3 aboveTarget = topPos.add(0, teleportHeight.get(), 0);
        Vec3 shotPos = topPos.add(0, targetoffset.get(), 0);
        Vec3 homeOffset = getOffset(home);

        if (!validatePath(aboveHome, aboveTarget, shotPos, aboveTarget, aboveHome, homeOffset)) {
            if (chatFeedback)error("One of the teleports are blocked.");
            return;
        }
        if (!hasClearPath(aboveHome, aboveTarget)) {
            if (chatFeedback)error("Vertical path blocked between clip positions.");
        }
        for (int i = 0; i < prePackets.get(); i++) {
            if (entity == mc.player) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true,mc.player.horizontalCollision));
            } else {
                mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
            }
        }

        moveTo(entity, aboveHome);
        moveTo(entity, aboveTarget);
        moveTo(entity, shotPos);

        float yaw = mc.player.getYRot();

        for (int i = 0; i < interactAmount.get(); i++) {
            mc.player.connection.send(new ServerboundUseItemPacket(hand, 0, yaw, 90.0f));
        }

        moveTo(entity, aboveTarget);
        moveTo(entity, aboveHome);
        moveTo(entity, home);
        moveTo(entity, homeOffset);

        entity.setPos(homeOffset);
    }

    public void action(ServerboundPlayerActionPacket.Action action) {
        if (target == null || !isValidTarget(target)) return;
        Entity entity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;
        Vec3 home = entity.position();

        if (mc.player.distanceToSqr(target) > maxDistance.get() * maxDistance.get()) return;

        AABB box = target.getBoundingBox();

        double x = box.getCenter().x;
        double y = box.maxY;
        double z = box.getCenter().z;

        Vec3 topPos = new Vec3(x, y, z);
        Vec3 aboveHome = home.add(0, teleportHeight.get(), 0);
        Vec3 aboveTarget = topPos.add(0, teleportHeight.get(), 0);
        Vec3 shotPos = topPos.add(0, targetoffset.get(), 0);
        Vec3 homeOffset = getOffset(home);

        if (!validatePath(aboveHome, aboveTarget, shotPos, aboveTarget, aboveHome, homeOffset)) {
            if (chatFeedback)error("One of the teleports are blocked.");
            return;
        }

        for (int i = 0; i < prePackets.get(); i++) {
            if (entity == mc.player) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true,mc.player.horizontalCollision));
            } else {
                mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
            }
        }

        moveTo(entity, aboveHome);
        moveTo(entity, aboveTarget);
        moveTo(entity, shotPos);

        paction = true;
        mc.player.connection.send(new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN, 0));
        paction = false;

        moveTo(entity, aboveTarget);
        moveTo(entity, aboveHome);
        moveTo(entity, home);
        moveTo(entity, homeOffset);
    }

    private void moveTo(Entity entity, Vec3 pos) {
        if (entity == mc.player) {
            ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, false, false);
            ((IServerboundMovePlayerPacket) packet).meteor$setTag(1337);
            mc.player.connection.send(packet);
        } else {
            mc.player.connection.send(new ServerboundMoveVehiclePacket(pos, entity.getYRot(), entity.getXRot(), false));
        }
        entity.setPos(pos);
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

    private boolean validatePath(Vec3... positions) {
        for (Vec3 pos : positions) {
            if (invalid(pos)) {
                return false;
            }
        }
        return true;
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
}