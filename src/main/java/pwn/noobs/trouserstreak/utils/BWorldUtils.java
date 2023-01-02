package pwn.noobs.trouserstreak.utils;

import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BWorldUtils {

    public enum SwitchMode {
        Packet,
        Client,
        Both
    }

    public enum PlaceMode {
        Packet,
        Client,
        Both
    }

    public enum AirPlaceDirection {
        Up,
        Down
    }

    private static final Vec3d hitPos = new Vec3d(0, 0, 0);

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, SwitchMode switchMode, PlaceMode placeMode, boolean onlyAirplace, AirPlaceDirection airPlaceDirection, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (findItemResult.isOffhand()) {
            return place(blockPos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, mc.player.getInventory().selectedSlot, rotate, rotationPriority, switchMode, placeMode, onlyAirplace, airPlaceDirection, swingHand, checkEntities, swapBack);
        } else if (findItemResult.isHotbar()) {
            return place(blockPos, Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, findItemResult.slot(), rotate, rotationPriority, switchMode, placeMode, onlyAirplace, airPlaceDirection, swingHand, checkEntities, swapBack);
        }
        return false;
    }

    public static boolean place(BlockPos blockPos, Hand hand, int oldSlot, int targetSlot, boolean rotate, int rotationPriority, SwitchMode switchMode, PlaceMode placeMode, boolean onlyAirplace, AirPlaceDirection airPlaceDirection, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (targetSlot < 0 || targetSlot > 8) return false;
        if (!canPlace(blockPos, checkEntities)) return false;

        ((IVec3d) hitPos).set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);

        BlockPos neighbour;
        Direction side = getPlaceSide(blockPos);

        if (side == null || onlyAirplace) {
            if (airPlaceDirection == AirPlaceDirection.Up) side = Direction.UP;
            else side = Direction.DOWN;
            neighbour = blockPos;
        } else {
            neighbour = blockPos.offset(side.getOpposite());
            hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        }

        Direction s = side;

        if (rotate) Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), rotationPriority, () -> place(new BlockHitResult(hitPos, s, neighbour, false), hand, oldSlot, targetSlot, switchMode, placeMode, swingHand, swapBack));
        else place(new BlockHitResult(hitPos, s, neighbour, false), hand, oldSlot, targetSlot, switchMode, placeMode, swingHand, swapBack);

        return true;
    }

    private static void place(BlockHitResult blockHitResult, Hand hand, int oldSlot, int targetSlot, SwitchMode switchMode, PlaceMode placeMode, boolean swing, boolean swapBack) {

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));

        if (switchMode != SwitchMode.Client) mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(targetSlot));
        if (switchMode != SwitchMode.Packet) InvUtils.swap(targetSlot, swapBack);

        if (placeMode != PlaceMode.Client) mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, blockHitResult, 0));
        if (placeMode != PlaceMode.Packet) mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);

        if (swing) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        if (swapBack) {
            if (switchMode != SwitchMode.Client) mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            if (switchMode != SwitchMode.Packet) InvUtils.swapBack();
        }

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }

    public static boolean canPlace(BlockPos blockPos, boolean checkEntities) {
        // Check y level
        if (!World.isValid(blockPos)) return false;

        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        // Check if intersects entities
        return !checkEntities || mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent());
    }

    public static boolean canPlace(BlockPos blockPos) {
        return canPlace(blockPos, true);
    }

    public static Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            BlockState state = mc.world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            return side2;
        }

        return null;
    }

    public static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof AbstractButtonBlock
                || block instanceof AbstractPressurePlateBlock
                || block instanceof BlockWithEntity
                || block instanceof BedBlock
                || block instanceof FenceGateBlock
                || block instanceof DoorBlock
                || block instanceof NoteBlock
                || block instanceof TrapdoorBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof GrindstoneBlock
                || block instanceof StonecutterBlock;
    }

    public static void lookAtBlock(BlockPos blockToLookAt) {
        rotate(calculateLookFromPlayer(blockToLookAt.getX(), blockToLookAt.getY(), blockToLookAt.getZ(), mc.player));
    }

    public static void rotate(float yaw, float pitch) {
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    public static void rotate(double[] rotations) {
        mc.player.setYaw((float) rotations[0]);
        mc.player.setPitch((float) rotations[1]);
    }

    public static void snapPlayer(BlockPos lastPos) {
        double xPos = mc.player.getPos().x;
        double zPos = mc.player.getPos().z;

        if(Math.abs((lastPos.getX() + 0.5) - mc.player.getPos().x) >= 0.2) {
            int xDir = (lastPos.getX() + 0.5) - mc.player.getPos().x > 0 ? 1 : -1;
            xPos += 0.3 * xDir;
        }

        if(Math.abs((lastPos.getZ() + 0.5) - mc.player.getPos().z) >= 0.2) {
            int zDir = (lastPos.getZ() + 0.5) - mc.player.getPos().z > 0 ? 1 : -1;
            zPos += 0.3 * zDir;
        }

        mc.player.setVelocity(0, 0, 0);
        mc.player.setPosition(xPos, mc.player.getY(), zPos);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
    }

    // World Utils

    public static BlockPos roundBlockPos(Vec3d vec) {
        return new BlockPos(vec.x, (int) Math.round(vec.y), vec.z);
    }

    // Player Utils

    public static double getEyeY(PlayerEntity player) {
        return player.getY() + player.getEyeHeight(player.getPose());
    }

    public static double[] calculateLookFromPlayer(double x, double y, double z, PlayerEntity player) {
        return calculateAngle(new Vec3d(player.getX(), getEyeY(player), player.getZ()), new Vec3d(x,y,z));
    }

    // Math Utils

    public static double[] calculateAngle(Vec3d a, Vec3d b) {
        double
                x = a.x - b.x,
                y = a.y - b.y,
                z = a.z - b.z,
                d = Math.sqrt(x * x + y * y + z * z),
                pitch = Math.toDegrees(Math.asin(y / d)),
                yaw = Math.toDegrees(Math.atan2(z / d, x / d) + Math.PI / 2);

        return new double[] {
                yaw,
                pitch
        };
    }

    public static boolean doesBoxTouchBlock(Box box, Block block) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    if (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == block) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static void spawnLightning(double x, double y, double z) {
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);

        lightning.updatePosition(x, y, z);
        lightning.refreshPositionAfterTeleport(x, y, z);
        mc.world.addEntity(lightning.getId(), lightning);
    }
}
