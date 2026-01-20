//Made by etianl based upon some suggestions from [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.modules;

import io.netty.buffer.Unpooled;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AntiHunger;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
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
    private final SettingGroup sgTP = settings.createGroup("Teleport Options");
    private final SettingGroup sgArrowFire = settings.createGroup("Bow Options");
    private final SettingGroup sglegacy = settings.createGroup("Legacy Mode (Servers 1.20.6 and below)");
    public final Setting<Boolean> legacyMode = sglegacy.add(new BoolSetting.Builder()
            .name("Legacy Mode")
            .description("Original jitter based BowInstaKill that consumes hunger. DOES NOT WORK ON ALL PAPER SERVERS. May work on super old Paper versions.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Integer> multiplier = sglegacy.add(new IntSetting.Builder()
            .name("Multiplier")
            .description("Higher values make success more likely at the cost of more hunger.")
            .defaultValue(90)
            .sliderRange(1,300)
            .min(1)
            .visible(legacyMode::get)
            .build()
    );
    public enum tpMode {
        Reverse,
        Forward
    }
    private final Setting<tpMode> tpmode = sgGeneral.add(new EnumSetting.Builder<tpMode>()
            .name("TP Mode")
            .description("Send you backwards or forward for the extra velocity.")
            .defaultValue(tpMode.Reverse)
            .visible(() -> !legacyMode.get())
            .build()
    );
    public enum Mode {
        Vanilla,
        Paper
    }
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Compatibility Mode")
            .description("Vanilla = 22 blocks, Paper = more blocks")
            .defaultValue(Mode.Vanilla)
            .visible(() -> !legacyMode.get())
            .build()
    );
    private final ItemListSetting projectileItems = (ItemListSetting) sgGeneral.add(new ItemListSetting.Builder()
            .name("projectile-items")
            .description("Which items to apply extra velocity to.")
            .defaultValue(
                    Items.ENDER_PEARL,
                    Items.SPLASH_POTION,
                    Items.LINGERING_POTION,
                    Items.EXPERIENCE_BOTTLE,
                    Items.SNOWBALL,
                    Items.EGG,
                    Items.BOW,
                    Items.TRIDENT
            )
            .filter(item -> item == Items.ENDER_PEARL
                    || item == Items.SPLASH_POTION
                    || item == Items.LINGERING_POTION
                    || item == Items.EXPERIENCE_BOTTLE
                    || item == Items.SNOWBALL
                    || item == Items.EGG
                    || item == Items.BOW
                    || item == Items.TRIDENT
            )
            .build()
    );
    private final Setting<Boolean> skipCollisionCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("BoatNoclip skip collision check")
            .description("If BoatNoclip is on and you are in a boat skip collision checks for blocks.")
            .defaultValue(true)
            .visible(() -> !legacyMode.get())
            .build()
    );
    private final Setting<Double> minDistance = sgTP.add(new DoubleSetting.Builder()
            .name("Min Distance")
            .description("The teleport is only valid if it's this far away.")
            .defaultValue(2.0)
            .min(0.5)
            .sliderRange(0.5,21.9)
            .visible(() -> !legacyMode.get())
            .build()
    );
    private final Setting<Integer> packets = sgTP.add(new IntSetting.Builder()
            .name("# spam packets (VANILLA)")
            .defaultValue(4)
            .min(1)
            .sliderRange(1,5)
            .visible(() -> mode.get() == Mode.Vanilla && !legacyMode.get())
            .build()
    );
    private final Setting<Double> Distance = sgTP.add(new DoubleSetting.Builder()
            .name("Max Distance (VANILLA)")
            .defaultValue(21.9)
            .min(1.0)
            .sliderRange(1.0,21.9)
            .visible(() -> mode.get() == Mode.Vanilla && !legacyMode.get())
            .build()
    );

    private final Setting<Integer> paperpackets = sgTP.add(new IntSetting.Builder()
            .name("# spam packets (PAPER)")
            .defaultValue(15)
            .min(1)
            .sliderRange(1,17)
            .visible(() -> mode.get() == Mode.Paper && !legacyMode.get())
            .build()
    );
    private final Setting<Double> paperDistance = sgTP.add(new DoubleSetting.Builder()
            .name("Max Distance (PAPER)")
            .defaultValue(149.0)
            .min(1.0)
            .sliderRange(1.0,169.0)
            .visible(() -> mode.get() == Mode.Paper && !legacyMode.get())
            .build()
    );

    private final Setting<Double> offsethorizontal = sgTP.add(new DoubleSetting.Builder()
            .name("Horizontal Offset")
            .description("Offset you this much from the home position.")
            .defaultValue(0.05)
            .min(0)
            .sliderMax(0.99)
            .visible(() -> !legacyMode.get())
            .build()
    );
    private final Setting<Double> offsetY = sgTP.add(new DoubleSetting.Builder()
            .name("Y Offset")
            .description("Offset you this much from the home position.")
            .defaultValue(0.001)
            .min(0)
            .sliderMax(0.99)
            .visible(() -> !legacyMode.get())
            .build()
    );
    private final Setting<Boolean> fallbackDirections = sgTP.add(new BoolSetting.Builder()
            .name("Horizontal Fallback Direction")
            .description("Use flat horizontal reverse if angled fails (ceiling/floor scenarios). Use forward teleport if reverse fails and vice/versa.")
            .defaultValue(true)
            .visible(() -> !legacyMode.get())
            .build()
    );
    private final Setting<Boolean> oppositeFallbackDirections = sgTP.add(new BoolSetting.Builder()
            .name("Opposite Fallback Direction")
            .description("Use forward teleport if reverse fails and vice/versa.")
            .defaultValue(true)
            .visible(() -> !legacyMode.get())
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
            .description("Ticks to charge bow before firing. More ticks adds a little velocity.")
            .defaultValue(20)
            .min(4)
            .sliderRange(4,60)
            .visible(arrowFire::get)
            .build()
    );

    private double maxDistance;
    private volatile Vec3d startPos = Vec3d.ZERO;
    private volatile Vec3d teleportPos = Vec3d.ZERO;

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

        if (!holdingBow) {
            isChargingBow = false;
            bowChargeTimer = 0;
            wasHoldingBow = false;
            return;
        }

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
    private boolean sendingPacketToSend;
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (sendingPacketToSend) return;
        if (mc.player == null) return;
        Packet<?> packetToSend = event.packet;

        if (event.packet instanceof PlayerInteractItemC2SPacket packet) {
            ItemStack stack = packet.getHand() == Hand.MAIN_HAND
                    ? mc.player.getMainHandStack()
                    : mc.player.getOffHandStack();

            Item item = stack.getItem();
            if (isValidProjectile(item)) {
                if (!legacyMode.get()){
                    event.cancel();
                    executeTeleport(packetToSend);
                } else {
                    sendlegacypackets();
                }
            } else if ((item instanceof BowItem && projectileItems.get().contains(Items.BOW)) || (item instanceof TridentItem && projectileItems.get().contains(Items.TRIDENT))){
                isChargingBow = true;
            }
            return;
        }

        if (isChargingBow && event.packet instanceof PlayerActionC2SPacket packet &&
                packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
            if (!legacyMode.get()){
                event.cancel();
                executeTeleport(packetToSend);
            } else {
                sendlegacypackets();
            }
            isChargingBow = false;
        }
    }
    private void sendlegacypackets(){
        boolean antihungerWasEnabled = false;
        if (Modules.get().get(AntiHunger.class).isActive()){
            Modules.get().get(AntiHunger.class).toggle();
            antihungerWasEnabled = true;
        }
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        for (int i = 0; i < multiplier.get(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.000000001, mc.player.getZ(), true));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), false));
        }
        if (!mc.options.sprintKey.isPressed() || !mc.options.getSprintToggled().getValue()) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        if (antihungerWasEnabled) Modules.get().get(AntiHunger.class).toggle();
    }
    private boolean isValidProjectile(Item item) {
        return (item instanceof EnderPearlItem && projectileItems.get().contains(Items.ENDER_PEARL)) ||
                (item instanceof SplashPotionItem && projectileItems.get().contains(Items.SPLASH_POTION)) ||
                (item instanceof LingeringPotionItem && projectileItems.get().contains(Items.LINGERING_POTION)) ||
                (item instanceof ExperienceBottleItem && projectileItems.get().contains(Items.EXPERIENCE_BOTTLE)) ||
                (item instanceof SnowballItem && projectileItems.get().contains(Items.SNOWBALL)) ||
                (item instanceof EggItem && projectileItems.get().contains(Items.EGG));
    }
    private void executeTeleport(Packet<?> packetToSend) {
        if (mc.player == null || mc.world == null) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        maxDistance = mode.get() == Mode.Vanilla ? Distance.get() : paperDistance.get();

        boolean isReverseTeleport = tpmode.get() == tpMode.Reverse;

        startPos = entity.getPos();
        Vec3d forwardDir = mc.player.getRotationVec(1.0f);
        Vec3d reverseDir = new Vec3d(-forwardDir.x, -forwardDir.y, -forwardDir.z).normalize();

        if (tpmode.get() == tpMode.Reverse) teleportPos = findFurthestSafePos(startPos, reverseDir, maxDistance, isReverseTeleport);
        else if (tpmode.get() == tpMode.Forward) teleportPos = findFurthestSafePos(startPos, forwardDir, maxDistance, isReverseTeleport);

        if (teleportPos == null) {
            if (oppositeFallbackDirections.get() && tpmode.get() == tpMode.Reverse) {
                isReverseTeleport = false;
                teleportPos = findFurthestSafePos(startPos, forwardDir, maxDistance, isReverseTeleport);
            }
            else if (oppositeFallbackDirections.get() && tpmode.get() == tpMode.Forward) {
                isReverseTeleport = true;
                teleportPos = findFurthestSafePos(startPos, reverseDir, maxDistance, isReverseTeleport);
            }
            if (fallbackDirections.get() && teleportPos == null) {
                Vec3d horizontalDir = null;
                if (tpmode.get() == tpMode.Reverse){
                    horizontalDir = new Vec3d(reverseDir.x, 0, reverseDir.z).normalize();
                    isReverseTeleport = true;
                } else if (tpmode.get() == tpMode.Forward) {
                    horizontalDir = new Vec3d(forwardDir.x, 0, forwardDir.z).normalize();
                    isReverseTeleport = false;
                }
                if (horizontalDir != null) teleportPos = findFurthestSafePos(startPos, horizontalDir, maxDistance, isReverseTeleport);
                if (oppositeFallbackDirections.get() && teleportPos == null) {
                    if (tpmode.get() == tpMode.Reverse){
                        isReverseTeleport = false;
                        horizontalDir = new Vec3d(forwardDir.x, 0, forwardDir.z).normalize();
                    } else if (tpmode.get() == tpMode.Forward) {
                        isReverseTeleport = true;
                        horizontalDir = new Vec3d(reverseDir.x, 0, reverseDir.z).normalize();
                    }
                    if (horizontalDir != null) teleportPos = findFurthestSafePos(startPos, horizontalDir, maxDistance, isReverseTeleport);
                }
            }
        }
        if (teleportPos == null) {
            if (chatFeedback) error("No possible teleport positions found.");
            return;
        }

        int packetCount = mode.get() == Mode.Vanilla ? packets.get() : paperpackets.get();

        for (int i = 0; i < packetCount; i++) {
            if (entity == mc.player) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
            } else {
                mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(entity));
            }
        }

        sendMove(entity, teleportPos);
        entity.setPosition(teleportPos);
        if (!isReverseTeleport){
            sendingPacketToSend = true;
            mc.player.networkHandler.sendPacket(packetToSend);
            sendingPacketToSend = false;
        }
        sendMove(entity, startPos);
        entity.setPosition(startPos);
        if (isReverseTeleport){
            sendingPacketToSend = true;
            mc.player.networkHandler.sendPacket(packetToSend);
            sendingPacketToSend = false;
        }
        Vec3d offset = getOffset(startPos);
        sendMove(entity, offset);
        entity.setPosition(offset);

        if (chatFeedback)info("Teleported %s (%.1f blocks)", isReverseTeleport ? "REVERSE" : "FORWARD", startPos.distanceTo(teleportPos));
    }

    private Vec3d findFurthestSafePos(Vec3d start, Vec3d direction, double maxDist, boolean isReverseTeleport) {
        Vec3d bestPos = null;

        for (double dist = maxDist; dist >= minDistance.get(); dist -= 0.5) {
            Vec3d testPos = start.add(direction.multiply(dist));

            if (hasClearPath(start, testPos, isReverseTeleport)) {
                bestPos = testPos;
                break;
            }
        }

        return bestPos;
    }
    private void sendMove(Entity entity, Vec3d pos) {
        if (mc.getNetworkHandler() == null) return;
        if (entity == mc.player) {
            PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false);
            mc.player.networkHandler.sendPacket(movepacket);
        } else {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
            buf.writeFloat(entity.getYaw());
            buf.writeFloat(entity.getPitch());

            VehicleMoveC2SPacket packet = new VehicleMoveC2SPacket(buf);
            mc.getNetworkHandler().sendPacket(packet);
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
            if (e.isCollidable()) return true;
        }

        return false;
    }

    private boolean hasClearPath(Vec3d start, Vec3d end, boolean isReverseTeleport) {
        if (invalid(start) || invalid(end)) return false;

        int steps = Math.max(10, (int)(start.distanceTo(end) * 2.5));

        boolean checkLivingEntities = !isReverseTeleport;

        for (int i = 1; i < steps; i++) {
            double t = i / (double)steps;
            Vec3d sample = start.lerp(end, t);

            if (invalid(sample)) return false;

            if (checkLivingEntities) {
                Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
                Box sampleBox = entity.getBoundingBox().offset(
                        sample.x - entity.getX(),
                        sample.y - entity.getY(),
                        sample.z - entity.getZ()
                );

                for (Entity other : mc.world.getOtherEntities(entity, sampleBox, e ->
                        e.isAlive() && e.isLiving()
                )) {
                    return false;
                }
            }
        }
        return true;
    }
}