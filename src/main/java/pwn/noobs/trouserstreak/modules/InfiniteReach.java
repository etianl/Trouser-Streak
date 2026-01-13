//original written by [agreed](https://github.com/aisiaiiad), handling for stuff other than attacking by etianl.
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;

public class InfiniteReach extends Module {
    public static InfiniteReach INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing arm")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> phoneHome = sgGeneral.add(new BoolSetting.Builder()
            .name("Home Teleport")
            .description("Brings you back home so you never knew you teleported.")
            .defaultValue(true)
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
    private final Setting<Boolean> goUp = sgGeneral.add(new BoolSetting.Builder()
            .name("Clip up")
            .description("Clips upward to do a Mace Smash and get around obstacles. There isn't enough packets for this in vanilla mode.")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Paper)
            .build()
    );
    private final Setting<Integer> packets = sgGeneral.add(new IntSetting.Builder()
            .name("# spam packets to send (VANILLA)")
            .description("How many packets to send before actual movements.")
            .defaultValue(4)
            .min(1)
            .sliderRange(1,5)
            .visible(() -> mode.get() == Mode.Vanilla)
            .build()
    );
    private final Setting<Double> Distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("Max Distance (VANILLA)")
            .description("Maximum range.")
            .defaultValue(22.0)
            .min(1.0)
            .sliderRange(1.0,22.0)
            .visible(() -> mode.get() == Mode.Vanilla)
            .build()
    );
    private final Setting<Integer> paperpackets = sgGeneral.add(new IntSetting.Builder()
            .name("# spam packets to send (PAPER)")
            .description("How many packets to send before actual movements.")
            .defaultValue(7)
            .min(1)
            .sliderRange(1,10)
            .visible(() -> mode.get() == Mode.Paper)
            .build()
    );
    private final Setting<Double> paperDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("Max Distance (PAPER)")
            .description("Maximum range.")
            .defaultValue(59.0)
            .min(1.0)
            .sliderRange(1.0,99.0)
            .visible(() -> mode.get() == Mode.Paper)
            .build()
    );
    private final Setting<Double> offsethorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("Horizontal Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.05)
            .min(0.01)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Double> offsetY = sgGeneral.add(new DoubleSetting.Builder()
            .name("Y Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.01)
            .min(0.01)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Boolean> onlyMace = sgGeneral.add(new BoolSetting.Builder()
            .name("Only When Mace")
            .description("Only execute the reach attack if you are holding a mace and the target is not blocking.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> skipCollisionCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("BoatNoclip skip collision check")
            .description("If BoatNoclip is on and you are in a boat skip collision checks for blocks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> renderentity = sgRender.add(new BoolSetting.Builder()
            .name("Render Entity Box")
            .description("Render Box around target Entity.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("entity-side-color")
            .description("")
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .visible(() -> renderentity.get())
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("entity-line-color")
            .description("")
            .defaultValue(new SettingColor(255, 0, 0, 120))
            .visible(() -> renderentity.get())
            .build()
    );
    private final Setting<Boolean> renderblock = sgRender.add(new BoolSetting.Builder()
            .name("Render Block Box")
            .description("Render Box around target Block.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> bsideColor = sgRender.add(new ColorSetting.Builder()
            .name("block-side-color")
            .description("")
            .defaultValue(new SettingColor(255, 0, 255, 40))
            .visible(() -> renderblock.get())
            .build()
    );
    private final Setting<SettingColor> blineColor = sgRender.add(new ColorSetting.Builder()
            .name("block-line-color")
            .description("")
            .defaultValue(new SettingColor(255, 0, 255, 120))
            .visible(() -> renderblock.get())
            .build()
    );
    private final SettingGroup sgDelay = settings.createGroup("Delay");

    private final Setting<Integer> blockAttackDelay = sgDelay.add(new IntSetting.Builder()
            .name("mining-packet-delay")
            .description("Ticks between mining packets.")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .build()
    );

    private final Setting<Integer> itemUseDelay = sgDelay.add(new IntSetting.Builder()
            .name("item-use-delay")
            .description("Ticks between item uses on blocks.")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .build()
    );

    private final Setting<Integer> entityAttackDelay = sgDelay.add(new IntSetting.Builder()
            .name("attack-delay")
            .description("Ticks between entity attacks.")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .build()
    );

    public Entity hoveredTarget;
    private double maxDistance;
    private int blockAttackTicks = 0;
    private boolean canBlockAttack = true;
    private int itemUseTicks = 0;
    private boolean canItemUse = true;
    private int entityAttackTicks = 0;
    private boolean canEntityAttack = true;
    private volatile Vec3d startPos = Vec3d.ZERO;
    private volatile Vec3d finalPos = Vec3d.ZERO;
    private volatile Vec3d aboveself = Vec3d.ZERO;
    private volatile Vec3d abovetarget = Vec3d.ZERO;
    private volatile Vec3d blockfinalPos = Vec3d.ZERO;
    private volatile Vec3d blockaboveself = Vec3d.ZERO;
    private volatile Vec3d blockabovetarget = Vec3d.ZERO;
    public InfiniteReach() {
        super(Trouser.Main, "infinite-reach", "Gives you super long arms. Lets you Mace Smash at long range in Paper servers.");
        INSTANCE = this;
    }
    @Override
    public void onActivate() {
        if (mode.get() == Mode.Vanilla) maxDistance = Distance.get();
        else maxDistance = paperDistance.get();
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!canBlockAttack) {
            blockAttackTicks++;
            if (blockAttackTicks >= blockAttackDelay.get()) {
                canBlockAttack = true;
                blockAttackTicks = 0;
            }
        }
        if (!canItemUse) {
            itemUseTicks++;
            if (itemUseTicks >= itemUseDelay.get()) {
                canItemUse = true;
                itemUseTicks = 0;
            }
        }
        if (!canEntityAttack) {
            entityAttackTicks++;
            if (entityAttackTicks >= entityAttackDelay.get()) {
                canEntityAttack = true;
                entityAttackTicks = 0;
            }
        }
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        if (mode.get() == Mode.Vanilla) maxDistance = Distance.get();
        else maxDistance = paperDistance.get();

        Vec3d cameraPos = mc.player.getCameraPosVec(1.0f);
        Vec3d rotation = mc.player.getRotationVec(1.0f);
        Vec3d endVec = cameraPos.add(rotation.multiply(maxDistance));

        EntityHitResult entityHit = ProjectileUtil.raycast(
                mc.player, cameraPos, endVec,
                mc.player.getBoundingBox().expand(maxDistance),
                e -> e.isAlive() && e.isAttackable() && !e.isInvulnerable() && e != mc.player,
                maxDistance * maxDistance
        );

        BlockHitResult blockHit = null;
        if (entityHit == null) {
            HitResult rawHit = mc.getCameraEntity().raycast(maxDistance, 0, false);
            if (rawHit instanceof BlockHitResult) {
                if (!mc.world.getBlockState(((BlockHitResult) rawHit).getBlockPos()).isAir()) blockHit = (BlockHitResult) rawHit;
            }
        }

        if (entityHit != null) {
            hoveredTarget = entityHit.getEntity();
            double x = MathHelper.lerp(event.tickDelta, hoveredTarget.lastRenderX, hoveredTarget.getX()) - hoveredTarget.getX();
            double y = MathHelper.lerp(event.tickDelta, hoveredTarget.lastRenderY, hoveredTarget.getY()) - hoveredTarget.getY();
            double z = MathHelper.lerp(event.tickDelta, hoveredTarget.lastRenderZ, hoveredTarget.getZ()) - hoveredTarget.getZ();

            Box box = hoveredTarget.getBoundingBox();
            if (renderentity.get()) {
                event.renderer.box(x + box.minX, y + box.minY, z + box.minZ,
                        x + box.maxX, y + box.maxY, z + box.maxZ,
                        sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
            }
        } else {
            hoveredTarget = null;
        }

        if (hoveredTarget == null && blockHit == null) {
            startPos = finalPos = aboveself = abovetarget = null;
            blockfinalPos = blockaboveself = blockabovetarget = null;
            return;
        }

        if (entityHit == null && renderblock.get()) {
            event.renderer.box(blockHit.getBlockPos(), bsideColor.get(), blineColor.get(), ShapeMode.Both, 0);
        }

        startPos = mc.player.getVehicle() == null
                ? mc.player.getEntityPos()
                : mc.player.getVehicle().getEntityPos();
        if (blockHit != null){
            Vec3d blocktargetPos = blockHit.getBlockPos().toCenterPos();
            Box blockBox = new Box(blockHit.getBlockPos());

            BlockPos targetActionPos = blockHit.getBlockPos().offset(blockHit.getSide());
            Box playerBox = mc.player.getBoundingBox();
            Box targetActionPosBox = new Box(targetActionPos);

            if (playerBox.intersects(targetActionPosBox)) return;

            Vec3d diff = startPos.subtract(blocktargetPos);
            double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
            double targetUp = flatUp + diff.y;

            double yOffset = mc.player.getVehicle() != null
                    ? blockBox.maxY + 0.3
                    : blocktargetPos.y;

            Vec3d insideTargetBlock = new Vec3d(blocktargetPos.x, yOffset, blocktargetPos.z);

            blockfinalPos = findNearestPosBLOCK(insideTargetBlock, targetActionPos, blockHit);
            if (blockfinalPos == null) return;

            blockaboveself = startPos.add(0, maxDistance, 0);
            blockabovetarget = blockfinalPos.add(0, targetUp, 0);
        } else {
            blockfinalPos = blockaboveself = blockabovetarget = null;
        }

        if (hoveredTarget != null){
            Vec3d targetPos = hoveredTarget.getEntityPos();
            Vec3d diff = startPos.subtract(targetPos);

            double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
            double targetUp = flatUp + diff.y;
            double yOffset = mc.player.getVehicle() != null
                    ? hoveredTarget.getBoundingBox().maxY + 0.3
                    : targetPos.y;

            Vec3d insideTarget = new Vec3d(targetPos.x, yOffset, targetPos.z);

            finalPos = !invalid(insideTarget)
                    ? insideTarget
                    : findNearestPos(insideTarget);
            if (finalPos == null) return;
            aboveself = startPos.add(0, maxDistance, 0);
            abovetarget = finalPos.add(0, targetUp, 0);
        } else {
            finalPos = aboveself = abovetarget = null;
        }

        boolean attackPressed = mc.options.attackKey.isPressed();
        boolean usePressed = mc.options.useKey.isPressed();
        boolean usingItem = mc.player.isUsingItem();

        if (!attackPressed && !usePressed) return;
        BlockHitResult bhr = null;
        if (mc.crosshairTarget instanceof BlockHitResult target) bhr = target;
        if (entityHit != null && attackPressed && canEntityAttack) {
            canEntityAttack = false; entityAttackTicks = 0;
            hitEntity(hoveredTarget, true);
        } else if (entityHit != null && usePressed && canItemUse) {
            canItemUse = false; itemUseTicks = 0;
            hitEntity(hoveredTarget, false);
        } else if (entityHit == null && attackPressed && canBlockAttack) {
            canBlockAttack = false; blockAttackTicks = 0;
            hitBlock(blockHit, true);
        } else if (entityHit == null && usePressed && canItemUse && !usingItem) {
            canItemUse = false; itemUseTicks = 0;
            if (bhr != null && mc.world.getBlockState(bhr.getBlockPos()).isAir())hitBlock(blockHit, false);
        }
    }

    private void hitBlock(BlockHitResult bhr, Boolean attackpressed) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.world == null) return;
        if (mc.world.getChunk(bhr.getBlockPos()) == null) return;
        if (startPos == null || blockfinalPos == null || blockaboveself == null || blockabovetarget == null) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;

        if (invalid(blockfinalPos) ||
                (mode.get() == Mode.Paper && goUp.get() && (!hasClearPath(blockaboveself, blockabovetarget) || invalid(blockaboveself) || invalid(blockabovetarget)))) {
            if (chatFeedback) {
                if (!hasClearPath(blockaboveself, blockabovetarget)) {
                    error("Path blocked between clip positions.");
                } else {
                    error("At least one of the teleports are invalid.");
                }
            }
            return;
        }

        int amountOfPackets = mode.get() == Mode.Vanilla ? packets.get() : paperpackets.get();
        for (int i = 0; i < amountOfPackets; i++) {
            if (mc.player.hasVehicle()) mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
            else mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        }

        if (mode.get() == Mode.Paper && goUp.get()) {
            sendMove(entity, blockaboveself);
            sendMove(entity, blockabovetarget);
        }
        sendMove(entity, blockfinalPos);
        if (!phoneHome.get()) entity.setPosition(blockfinalPos);

        if (attackpressed){
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bhr.getBlockPos(), bhr.getSide()
            ));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bhr.getBlockPos(), bhr.getSide()
            ));
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                    Hand.MAIN_HAND, bhr, 0
            ));
        }

        if (swing.get()) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        if (phoneHome.get()) {
            if (mode.get() == Mode.Paper && goUp.get()) {
                sendMove(entity, blockabovetarget.add(0, 0.01, 0));
                sendMove(entity, blockaboveself.add(0, 0.01, 0));
            }
            sendMove(entity, startPos);
            Vec3d offset = getOffset(startPos);
            sendMove(entity, offset);
            entity.setPosition(offset);
        }
    }
    public void hitEntity(Entity target, Boolean attackpressed) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (onlyMace.get() && mc.player.getMainHandStack().getItem() != Items.MACE) return;
        if (onlyMace.get() && target instanceof PlayerEntity player && player.isBlocking()) return;
        if (startPos == null || finalPos == null || aboveself == null || abovetarget == null) return;
        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;

        double actualDistance = startPos.distanceTo(target.getEntityPos());
        if (actualDistance > maxDistance - 0.5) {
            return;
        }
        if (invalid(finalPos) ||
                (mode.get() == Mode.Paper && goUp.get() && (!hasClearPath(aboveself, abovetarget) || invalid(aboveself) || invalid(abovetarget)))) {
            if (chatFeedback) {
                if (!hasClearPath(aboveself, abovetarget)) {
                    error("Vertical path blocked between clip positions.");
                } else {
                    error("At least one of the teleports are invalid.");
                }
            }
            return;
        }

        int amountOfPackets = mode.get() == Mode.Vanilla ? packets.get() : paperpackets.get();
        for (int i = 0; i < amountOfPackets; i++) {
            if (mc.player.hasVehicle()) mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
            else mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        }

        if (mode.get() == Mode.Paper && goUp.get()){
            sendMove(entity, aboveself);
            sendMove(entity, abovetarget);
        }
        sendMove(entity, finalPos);
        if (!phoneHome.get()) entity.setPosition(finalPos);

        if (attackpressed)mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        else mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(target, mc.player.isSneaking(), mc.player.getActiveHand()));

        if (swing.get()) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        if (phoneHome.get()){
            if (mode.get() == Mode.Paper && goUp.get()){
                sendMove(entity, abovetarget.add(0, 0.01, 0));
                sendMove(entity, aboveself.add(0, 0.01, 0));
            }
            sendMove(entity, startPos);
            Vec3d offset = getOffset(startPos);
            sendMove(entity, offset);
            entity.setPosition(offset);
        }
    }
    private Vec3d findNearestPos(Vec3d desired) {
        if (!invalid(desired)) return desired;

        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
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
    private Vec3d findNearestPosBLOCK(Vec3d preferredInsideTarget, BlockPos actionBlockPos, BlockHitResult blockHit) {
        if (!invalid(preferredInsideTarget) && !BlockPos.ofFloored(preferredInsideTarget).equals(actionBlockPos)) {
            return preferredInsideTarget;
        }

        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    Vec3d test = preferredInsideTarget.add(dx * 0.4, dy * 0.4, dz * 0.4);

                    if (BlockPos.ofFloored(test).equals(actionBlockPos)) continue;

                    if (invalid(test)) continue;

                    double dist = test.squaredDistanceTo(preferredInsideTarget);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = test;
                    }
                }
            }
        }

        if (best == null) {
            for (Direction dir : Direction.values()) {
                if (dir == blockHit.getSide()) continue;

                Vec3d fallback = preferredInsideTarget.add(
                        dir.getOffsetX() * 0.5,
                        dir.getOffsetY() * 0.5,
                        dir.getOffsetZ() * 0.5
                );

                if (!invalid(fallback) && !BlockPos.ofFloored(fallback).equals(actionBlockPos)) {
                    return fallback;
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