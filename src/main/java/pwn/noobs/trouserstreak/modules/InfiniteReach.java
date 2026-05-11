//original written by [agreed](https://github.com/aisiaiiad), handling for stuff other than attacking by etianl.
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;

public class InfiniteReach extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgPacket = settings.createGroup("Packet Settings.");
    private final Setting<Boolean> nonofall = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable NoFall While Teleporting")
            .description("Prevents fall damage when teleporting.")
            .defaultValue(true)
            .build()
    );
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
    private final Setting<Boolean> miningPacket = sgPacket.add(new BoolSetting.Builder()
            .name("Send Mining packet.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> blockAttackDelay = sgPacket.add(new IntSetting.Builder()
            .name("mining-packet-delay")
            .description("Ticks between mining packets.")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .visible(miningPacket::get)
            .build()
    );
    private final Setting<Boolean> itemUsePacket = sgPacket.add(new BoolSetting.Builder()
            .name("Send Item Use packet.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> itemUseDelay = sgPacket.add(new IntSetting.Builder()
            .name("item-use-packet-delay")
            .description("Ticks between item uses on blocks.")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .visible(itemUsePacket::get)
            .build()
    );
    private final Setting<Boolean> attackPacket = sgPacket.add(new BoolSetting.Builder()
            .name("Send Attack packet.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> entityAttackDelay = sgPacket.add(new IntSetting.Builder()
            .name("attack-packet-delay")
            .description("Ticks between entity attacks.")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .visible(attackPacket::get)
            .build()
    );
    private double maxDistance;
    private boolean wasNoFallEnabled = false;
    private boolean noFallToggled = false;
    public Entity hoveredTarget = null;
    private int blockAttackTicks = 0;
    private boolean canBlockAttack = true;
    private int itemUseTicks = 0;
    private boolean canItemUse = true;
    private int entityAttackTicks = 0;
    private boolean canEntityAttack = true;
    private volatile Vec3 startPos = Vec3.ZERO;
    private volatile Vec3 finalPos = Vec3.ZERO;
    private volatile Vec3 aboveself = Vec3.ZERO;
    private volatile Vec3 abovetarget = Vec3.ZERO;
    private volatile Vec3 blockfinalPos = Vec3.ZERO;
    private volatile Vec3 blockaboveself = Vec3.ZERO;
    private volatile Vec3 blockabovetarget = Vec3.ZERO;
    public InfiniteReach() {
        super(Trouser.Main, "infinite-reach", "Gives you super long arms. Lets you Mace Smash at long range in Paper servers.");
    }
    @Override
    public void onActivate() {
        if (mode.get() == Mode.Vanilla) maxDistance = Distance.get();
        else maxDistance = paperDistance.get();
        wasNoFallEnabled = false;
        noFallToggled = false;
        hoveredTarget = null;
        blockAttackTicks = 0;
        canBlockAttack = true;
        itemUseTicks = 0;
        canItemUse = true;
        entityAttackTicks = 0;
        canEntityAttack = true;
        startPos = Vec3.ZERO;
        finalPos = Vec3.ZERO;
        aboveself = Vec3.ZERO;
        abovetarget = Vec3.ZERO;
        blockfinalPos = Vec3.ZERO;
        blockaboveself = Vec3.ZERO;
        blockabovetarget = Vec3.ZERO;
    }
    @Override
    public void onDeactivate() {
        if (noFallToggled && wasNoFallEnabled) {
            Modules.get().get(NoFall.class).toggle();
        }
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
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

        if (mode.get() == Mode.Vanilla) maxDistance = Distance.get();
        else maxDistance = paperDistance.get();

        Vec3 cameraPos = mc.player.getEyePosition(1.0f);
        Vec3 rotation = mc.player.getViewVector(1.0f);
        Vec3 endVec = cameraPos.add(rotation.scale(maxDistance));

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player, cameraPos, endVec,
                mc.player.getBoundingBox().inflate(maxDistance),
                e -> e.isAlive() && e.isAttackable() && !e.isInvulnerable() && e != mc.player,
                maxDistance * maxDistance
        );

        BlockHitResult blockHit = null;
        if (entityHit == null) {
            HitResult rawHit = mc.getCameraEntity().pick(maxDistance, 0, false);
            if (rawHit instanceof BlockHitResult) {
                if (!mc.level.getBlockState(((BlockHitResult) rawHit).getBlockPos()).isAir()) blockHit = (BlockHitResult) rawHit;
            }
        }

        if (entityHit != null) {
            hoveredTarget = entityHit.getEntity();
            double x = Mth.lerp(event.tickDelta, hoveredTarget.xOld, hoveredTarget.getX()) - hoveredTarget.getX();
            double y = Mth.lerp(event.tickDelta, hoveredTarget.yOld, hoveredTarget.getY()) - hoveredTarget.getY();
            double z = Mth.lerp(event.tickDelta, hoveredTarget.zOld, hoveredTarget.getZ()) - hoveredTarget.getZ();

            AABB box = hoveredTarget.getBoundingBox();
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
                ? mc.player.position()
                : mc.player.getVehicle().position();
        if (blockHit != null){
            Vec3 blocktargetPos = blockHit.getBlockPos().getCenter();
            AABB blockBox = new AABB(blockHit.getBlockPos());

            BlockPos targetActionPos = blockHit.getBlockPos().relative(blockHit.getDirection());
            AABB playerBox = mc.player.getBoundingBox();
            AABB targetActionPosBox = new AABB(targetActionPos);

            if (playerBox.intersects(targetActionPosBox)) return;

            Vec3 diff = startPos.subtract(blocktargetPos);
            double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
            double targetUp = flatUp + diff.y;

            double yOffset = mc.player.getVehicle() != null
                    ? blockBox.maxY + 0.3
                    : blocktargetPos.y;

            Vec3 insideTargetBlock = new Vec3(blocktargetPos.x, yOffset, blocktargetPos.z);

            blockfinalPos = findNearestPosBLOCK(insideTargetBlock, targetActionPos, blockHit);
            if (blockfinalPos == null) return;

            blockaboveself = startPos.add(0, maxDistance, 0);
            blockabovetarget = blockfinalPos.add(0, targetUp, 0);
        } else {
            blockfinalPos = blockaboveself = blockabovetarget = null;
        }

        if (hoveredTarget != null){
            Vec3 targetPos = hoveredTarget.position();
            Vec3 diff = startPos.subtract(targetPos);

            double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
            double targetUp = flatUp + diff.y;
            double yOffset = mc.player.getVehicle() != null
                    ? hoveredTarget.getBoundingBox().maxY + 0.3
                    : targetPos.y;

            Vec3 insideTarget = new Vec3(targetPos.x, yOffset, targetPos.z);

            finalPos = !invalid(insideTarget)
                    ? insideTarget
                    : findNearestPos(insideTarget);
            if (finalPos == null) return;
            aboveself = startPos.add(0, maxDistance, 0);
            abovetarget = finalPos.add(0, targetUp, 0);
        } else {
            finalPos = aboveself = abovetarget = null;
        }

        boolean attackPressed = mc.options.keyAttack.isDown();
        boolean usePressed = mc.options.keyUse.isDown();
        boolean usingItem = mc.player.isUsingItem();

        if (!attackPressed && !usePressed) {
            if (nonofall.get()){
                if (noFallToggled) {
                    if (wasNoFallEnabled) {
                        Modules.get().get(NoFall.class).toggle();
                    }
                    noFallToggled = false;
                }
            }
            return;
        }
        BlockHitResult bhr = null;
        if (mc.hitResult instanceof BlockHitResult target) bhr = target;
        if (entityHit != null && attackPressed && canEntityAttack && attackPacket.get()) {
            canEntityAttack = false; entityAttackTicks = 0;
            if (nonofall.get()) donofallstuff();
            hitEntity(hoveredTarget, true);
        } else if (entityHit != null && usePressed && canItemUse && itemUsePacket.get()) {
            canItemUse = false; itemUseTicks = 0;
            if (nonofall.get()) donofallstuff();
            hitEntity(hoveredTarget, false);
        } else if (entityHit == null && attackPressed && canBlockAttack && miningPacket.get()) {
            canBlockAttack = false; blockAttackTicks = 0;
            if (nonofall.get()) donofallstuff();
            hitBlock(blockHit, true);
        } else if (entityHit == null && usePressed && canItemUse && !usingItem && itemUsePacket.get()) {
            canItemUse = false; itemUseTicks = 0;
            if (bhr != null && mc.level.getBlockState(bhr.getBlockPos()).isAir()){
                if (nonofall.get()) donofallstuff();
                hitBlock(blockHit, false);
            }
        }
    }
    private void donofallstuff(){
        if (!noFallToggled) {
            wasNoFallEnabled = Modules.get().get(NoFall.class).isActive();
            if (wasNoFallEnabled) {
                Modules.get().get(NoFall.class).toggle();
                noFallToggled = true;
            }
        }
    }
    private void hitBlock(BlockHitResult bhr, Boolean attackpressed) {
        if (mc.player == null || mc.getConnection() == null || mc.level == null) return;
        if (mc.level.getChunk(bhr.getBlockPos()) == null) return;
        if (startPos == null || blockfinalPos == null || blockaboveself == null || blockabovetarget == null) return;
        Entity entity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;

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
            if (mc.player.isPassenger()) mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(mc.player.getVehicle()));
            else mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
        }

        if (mode.get() == Mode.Paper && goUp.get()) {
            sendMove(entity, blockaboveself);
            sendMove(entity, blockabovetarget);
        }
        sendMove(entity, blockfinalPos);
        if (!phoneHome.get()) entity.setPos(blockfinalPos);

        if (attackpressed){
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, bhr.getBlockPos(), bhr.getDirection()
            ));
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, bhr.getBlockPos(), bhr.getDirection()
            ));
        } else {
            mc.getConnection().send(new ServerboundUseItemOnPacket(
                    InteractionHand.MAIN_HAND, bhr, 0
            ));
        }

        if (swing.get()) {
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        if (phoneHome.get()) {
            if (mode.get() == Mode.Paper && goUp.get()) {
                sendMove(entity, blockabovetarget.add(0, 0.01, 0));
                sendMove(entity, blockaboveself.add(0, 0.01, 0));
            }
            sendMove(entity, startPos);
            Vec3 offset = getOffset(startPos);
            sendMove(entity, offset);
            entity.setPos(offset);
        }
    }
    public void hitEntity(Entity target, Boolean attackpressed) {
        if (mc.player == null || mc.getConnection() == null) return;
        if (onlyMace.get() && mc.player.getMainHandItem().getItem() != Items.MACE) return;
        if (onlyMace.get() && target instanceof Player player && player.isBlocking()) return;
        if (startPos == null || finalPos == null || aboveself == null || abovetarget == null) return;
        Entity entity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;

        double actualDistance = startPos.distanceTo(target.position());
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
            if (mc.player.isPassenger()) mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(mc.player.getVehicle()));
            else mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
        }

        if (mode.get() == Mode.Paper && goUp.get()){
            sendMove(entity, aboveself);
            sendMove(entity, abovetarget);
        }
        sendMove(entity, finalPos);
        if (!phoneHome.get()) entity.setPos(finalPos);

        if (attackpressed)mc.getConnection().send(new ServerboundAttackPacket(target.getId()));
        else mc.getConnection().send(new ServerboundInteractPacket(target.getId(), mc.player.getUsedItemHand(), target.position(), true));

        if (swing.get()) {
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        if (phoneHome.get()){
            if (mode.get() == Mode.Paper && goUp.get()){
                sendMove(entity, abovetarget.add(0, 0.01, 0));
                sendMove(entity, aboveself.add(0, 0.01, 0));
            }
            sendMove(entity, startPos);
            Vec3 offset = getOffset(startPos);
            sendMove(entity, offset);
            entity.setPos(offset);
        }
    }
    private Vec3 findNearestPos(Vec3 desired) {
        if (!invalid(desired)) return desired;

        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    Vec3 test = desired.add(dx, dy, dz);
                    if (invalid(test)) continue;

                    double dist = test.distanceToSqr(desired);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = test;
                    }
                }
            }
        }
        return best;
    }
    private Vec3 findNearestPosBLOCK(Vec3 preferredInsideTarget, BlockPos actionBlockPos, BlockHitResult blockHit) {
        if (!invalid(preferredInsideTarget) && !BlockPos.containing(preferredInsideTarget).equals(actionBlockPos)) {
            return preferredInsideTarget;
        }

        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    Vec3 test = preferredInsideTarget.add(dx * 0.4, dy * 0.4, dz * 0.4);

                    if (BlockPos.containing(test).equals(actionBlockPos)) continue;

                    if (invalid(test)) continue;

                    double dist = test.distanceToSqr(preferredInsideTarget);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = test;
                    }
                }
            }
        }

        if (best == null) {
            for (Direction dir : Direction.values()) {
                if (dir == blockHit.getDirection()) continue;

                Vec3 fallback = preferredInsideTarget.add(
                        dir.getStepX() * 0.5,
                        dir.getStepY() * 0.5,
                        dir.getStepZ() * 0.5
                );

                if (!invalid(fallback) && !BlockPos.containing(fallback).equals(actionBlockPos)) {
                    return fallback;
                }
            }
        }
        return best;
    }
    private void sendMove(Entity entity, Vec3 pos) {
        if (mc.getConnection() == null) return;
        if (entity == mc.player) {
            ServerboundMovePlayerPacket movepacket = new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, false, false);
            ((IServerboundMovePlayerPacket) movepacket).meteor$setTag(1337);
            mc.player.connection.send(movepacket);
        } else {
            mc.getConnection().send(new ServerboundMoveVehiclePacket(pos, mc.player.getVehicle().getYRot(), mc.player.getVehicle().getXRot(), false));
        }
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
}