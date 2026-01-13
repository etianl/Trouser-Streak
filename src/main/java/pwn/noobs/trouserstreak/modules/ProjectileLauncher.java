//Made by etianl based upon suggestions from [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import pwn.noobs.trouserstreak.Trouser;
import net.minecraft.entity.Entity;

import java.util.Arrays;
import java.util.Collections;

public class ProjectileLauncher extends Module {
    public static ProjectileLauncher INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Mode {
        Vanilla,
        Paper
    }
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Compatibility Mode")
            .description("Vanilla = 22 blocks, Paper = more blocks")
            .defaultValue(Mode.Vanilla)
            .build()
    );
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
    private final Setting<Integer> packets = sgGeneral.add(new IntSetting.Builder()
            .name("# spam packets (VANILLA)")
            .defaultValue(4)
            .min(1)
            .sliderRange(1,5)
            .visible(() -> mode.get() == Mode.Vanilla)
            .build()
    );
    private final Setting<Double> Distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("Max Distance (VANILLA)")
            .defaultValue(21.9)
            .min(1.0)
            .sliderRange(1.0,21.9)
            .visible(() -> mode.get() == Mode.Vanilla)
            .build()
    );

    private final Setting<Integer> paperpackets = sgGeneral.add(new IntSetting.Builder()
            .name("# spam packets (PAPER)")
            .defaultValue(7)
            .min(1)
            .sliderRange(1,20)
            .visible(() -> mode.get() == Mode.Paper)
            .build()
    );
    private final Setting<Double> paperDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("Max Distance (PAPER)")
            .defaultValue(59.0)
            .min(1.0)
            .sliderRange(1.0,169.0)
            .visible(() -> mode.get() == Mode.Paper)
            .build()
    );

    private final Setting<Double> offsethorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("Horizontal Offset")
            .defaultValue(0.05)
            .min(0)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Double> offsetY = sgGeneral.add(new DoubleSetting.Builder()
            .name("Y Offset")
            .defaultValue(0.001)
            .min(0)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Boolean> fallbackDirections = sgGeneral.add(new BoolSetting.Builder()
            .name("Horizontal Fallback Direction")
            .description("Use flat horizontal reverse if angled fails (ceiling/floor scenarios).")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> preventfalldamage = sgGeneral.add(new BoolSetting.Builder()
            .name("Prevent fall damage")
            .description("If the amount of downward distance between teleports is greater than three use fallback direction instead. Unless in a boat.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> skipCollisionCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("BoatNoclip skip collision check")
            .description("If BoatNoclip is on and you are in a boat skip collision checks for blocks.")
            .defaultValue(true)
            .build()
    );
    private final SettingGroup sgArrowFire = settings.createGroup("Arrow Fire");
    private final Setting<Boolean> arrowFire = sgArrowFire.add(new BoolSetting.Builder()
            .name("Bow Machinegun")
            .description("Automatically fires arrows while holding bow.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> chargeTicks = sgArrowFire.add(new IntSetting.Builder()
            .name("charge-ticks")
            .description("Ticks to charge bow before firing.")
            .defaultValue(20)
            .min(4)
            .sliderRange(4,60)
            .visible(arrowFire::get)
            .build()
    );

    private double maxDistance;
    private volatile Vec3d startPos = Vec3d.ZERO;
    private volatile Vec3d reversePos = Vec3d.ZERO;

    public ProjectileLauncher() {
        super(Trouser.Main, "ProjectileLauncher", "Teleports backward on item use (pearl/potion) to safe furthest position.");
        INSTANCE = this;
    }

    private boolean isChargingBow = false;
    private int bowChargeTimer = 0;
    private boolean wasHoldingBow = false;

    @EventHandler
    private void onTick(TickEvent.Post event) {
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

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;

        if (event.packet instanceof PlayerInteractItemC2SPacket packet) {
            ItemStack stack = packet.getHand() == Hand.MAIN_HAND
                    ? mc.player.getMainHandStack()
                    : mc.player.getOffHandStack();

            Item item = stack.getItem();
            if (isValidProjectile(item)) {
                executeReverseTeleport();
            } else if ((item instanceof BowItem && projectileItems.get().contains(Items.BOW)) || (item instanceof TridentItem && projectileItems.get().contains(Items.TRIDENT))){
                isChargingBow = true;
            }
            return;
        }

        if (isChargingBow && event.packet instanceof PlayerActionC2SPacket packet &&
                packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
            executeReverseTeleport();
            isChargingBow = false;
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

    private void executeReverseTeleport() {
        if (mc.player == null || mc.world == null) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        maxDistance = mode.get() == Mode.Vanilla ? Distance.get() : paperDistance.get();

        startPos = entity.getEntityPos();
        Vec3d lookDir = mc.player.getRotationVec(1.0f);
        Vec3d reverseDir = new Vec3d(-lookDir.x, -lookDir.y, -lookDir.z).normalize();

        reversePos = findFurthestSafePos(startPos, reverseDir, maxDistance);
        if (entity != mc.player.getVehicle() && preventfalldamage.get() && reversePos != null && (reversePos.y - startPos.y > 2.99)) {
            if (chatFeedback)info("Blocked downward teleport (%.1f blocks) to prevent fall damage.", reversePos.y - startPos.y);
            reversePos = null;
        }
        if (fallbackDirections.get() && reversePos == null) {
            Vec3d horizontalDir = new Vec3d(reverseDir.x, 0, reverseDir.z).normalize();
            reversePos = findFurthestSafePos(startPos, horizontalDir, maxDistance);

            if (reversePos == null) {
                Vec3d backwardDir = Vec3d.fromPolar(0, entity.getYaw() + 180).normalize();
                backwardDir = new Vec3d(backwardDir.x, 0, backwardDir.z);
                reversePos = findFurthestSafePos(startPos, backwardDir, maxDistance);
            }
        }
        if (reversePos == null) return;

        int packetCount = mode.get() == Mode.Vanilla ? packets.get() : paperpackets.get();

        for (int i = 0; i < packetCount; i++) {
            if (entity == mc.player) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true,mc.player.horizontalCollision));
            } else {
                mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(entity));
            }
        }

        sendMove(entity, reversePos);
        Vec3d offset = getOffset(startPos);
        sendMove(entity, offset);
        entity.setPosition(offset);

        if (chatFeedback)info("Reverse teleported %s blocks behind.", startPos.distanceTo(reversePos));
    }

    private Vec3d findFurthestSafePos(Vec3d start, Vec3d direction, double maxDist) {
        Vec3d bestPos = null;

        for (double dist = maxDist; dist >= 0.5; dist -= 0.5) {
            Vec3d testPos = start.add(direction.multiply(dist));

            if (hasClearPath(start, testPos)) {
                bestPos = testPos;
                break;
            }
        }

        return bestPos;
    }

    private void sendMove(Entity entity, Vec3d pos) {
        if (mc.getNetworkHandler() == null) return;
        if (entity == mc.player) {
            PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false, false);
            ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(movepacket);
        } else {
            mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(pos, entity.getYaw(), entity.getPitch(), false));
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