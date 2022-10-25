package pwn.noobs.trouserstreak.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PositionUtils {
    public static boolean allPlaced(List<BlockPos> posList) {
        for (BlockPos pos : posList) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.AIR) return false;
        }
        return true;
    }

    public static List<BlockPos> dynamicTopPos(PlayerEntity targetEntity, boolean predictMovement) {
        List<BlockPos> pos = new ArrayList<>();

        Box box = targetEntity.getBoundingBox().contract(0.001, 0, 0.001);
        if (predictMovement) {
            Vec3d v = targetEntity.getVelocity();
            box.offset(v.x, v.y, v.z);
        }

        // Head positions
        pos.add(new BlockPos(box.minX, box.minY + 2.5, box.minZ)); // North West
        pos.add(new BlockPos(box.maxX, box.minY + 2.5, box.minZ)); // North East
        pos.add(new BlockPos(box.maxX, box.minY + 2.5, box.maxZ)); // South East
        pos.add(new BlockPos(box.minX, box.minY + 2.5, box.maxZ)); // South West

        return pos;
    }

    public static List<BlockPos> dynamicHeadPos(PlayerEntity targetEntity, boolean predictMovement) {
        List<BlockPos> pos = new ArrayList<>();

        Box box = targetEntity.getBoundingBox().contract(0.001, 0, 0.001);
        if (predictMovement) {
            Vec3d v = targetEntity.getVelocity();
            box.offset(v.x, v.y, v.z);
        }

        // North
        pos.add(new BlockPos(box.minX, box.minY + 1.5, box.minZ - 1));
        pos.add(new BlockPos(box.maxX, box.minY + 1.5, box.minZ - 1));
        // East
        pos.add(new BlockPos(box.maxX + 1, box.minY + 1.5, box.minZ));
        pos.add(new BlockPos(box.maxX + 1, box.minY + 1.5, box.maxZ));
        // South
        pos.add(new BlockPos(box.maxX, box.minY + 1.5, box.maxZ + 1));
        pos.add(new BlockPos(box.minX, box.minY + 1.5, box.maxZ + 1));
        // West
        pos.add(new BlockPos(box.minX - 1, box.minY + 1.5, box.maxZ));
        pos.add(new BlockPos(box.minX - 1, box.minY + 1.5, box.minZ));

        return pos;
    }

    public static List<BlockPos> dynamicBottomPos(PlayerEntity targetEntity, boolean predictMovement) {
        List<BlockPos> pos = new ArrayList<>();

        Box box = targetEntity.getBoundingBox().contract(0.001, 0, 0.001);
        if (predictMovement) {
            Vec3d v = targetEntity.getVelocity();
            box.offset(v.x, v.y, v.z);
        }

        // Bottom positions
        pos.add(new BlockPos(box.minX, box.minY - 0.5, box.minZ)); // North West
        pos.add(new BlockPos(box.maxX, box.minY - 0.5, box.minZ)); // North East
        pos.add(new BlockPos(box.maxX, box.minY - 0.5, box.maxZ)); // South East
        pos.add(new BlockPos(box.minX, box.minY - 0.5, box.maxZ)); // South West

        return pos;
    }

    public static List<BlockPos> dynamicFeetPos(PlayerEntity targetEntity, boolean predictMovement) {
        List<BlockPos> pos = new ArrayList<>();

        Box box = targetEntity.getBoundingBox().contract(0.001, 0, 0.001);
        if (predictMovement) {
            Vec3d v = targetEntity.getVelocity();
            box.offset(v.x, v.y, v.z);
        }

        // Positions around (adding 0.4 to the y like isBurrowed check, just like roundblockpos)
        // North
        pos.add(new BlockPos(box.minX, box.minY + 0.4, box.minZ - 1));
        pos.add(new BlockPos(box.maxX, box.minY + 0.4, box.minZ - 1));
        // East
        pos.add(new BlockPos(box.maxX + 1, box.minY + 0.4, box.minZ));
        pos.add(new BlockPos(box.maxX + 1, box.minY + 0.4, box.maxZ));
        // South
        pos.add(new BlockPos(box.maxX, box.minY + 0.4, box.maxZ + 1));
        pos.add(new BlockPos(box.minX, box.minY + 0.4, box.maxZ + 1));
        // West
        pos.add(new BlockPos(box.minX - 1, box.minY + 0.4, box.maxZ));
        pos.add(new BlockPos(box.minX - 1, box.minY + 0.4, box.minZ));

        return pos;
    }

    public static List<BlockPos> dynamicRussianNorth(PlayerEntity targetEntity, boolean plus) {
        List<BlockPos> pos = new ArrayList<>();

        Box box = targetEntity.getBoundingBox().contract(0.001, 0, 0.001);
        // North
        pos.add(new BlockPos(box.minX, box.minY + 0.4, box.minZ - 1).north());
        pos.add(new BlockPos(box.maxX, box.minY + 0.4, box.minZ - 1).north());
        if (plus) {
            pos.add(new BlockPos(box.minX, box.minY + 0.4, box.minZ - 1).west());
            pos.add(new BlockPos(box.maxX, box.minY + 0.4, box.minZ - 1).east());
        }

        return pos;
    }

    public static List<BlockPos> dynamicRussianEast(PlayerEntity targetEntity, boolean plus) {
        List<BlockPos> pos = new ArrayList<>();

        Box box = targetEntity.getBoundingBox().contract(0.001, 0, 0.001);
        // East
        pos.add(new BlockPos(box.maxX + 1, box.minY + 0.4, box.minZ).east());
        pos.add(new BlockPos(box.maxX + 1, box.minY + 0.4, box.maxZ).east());
        if (plus) {
            pos.add(new BlockPos(box.maxX + 1, box.minY + 0.4, box.minZ).north());
            pos.add(new BlockPos(box.maxX + 1, box.minY + 0.4, box.maxZ).south());
        }

        return pos;
    }

    public static List<BlockPos> dynamicRussianSouth(PlayerEntity targetEntity, boolean plus) {
        List<BlockPos> pos = new ArrayList<>();

        Box box = targetEntity.getBoundingBox().contract(0.001, 0, 0.001);
        // South
        pos.add(new BlockPos(box.maxX, box.minY + 0.4, box.maxZ + 1).south());
        pos.add(new BlockPos(box.minX, box.minY + 0.4, box.maxZ + 1).south());
        if (plus) {
            pos.add(new BlockPos(box.maxX, box.minY + 0.4, box.maxZ + 1).east());
            pos.add(new BlockPos(box.minX, box.minY + 0.4, box.maxZ + 1).west());
        }

        return pos;
    }

    public static List<BlockPos> dynamicRussianWest(PlayerEntity targetEntity, boolean plus) {
        List<BlockPos> pos = new ArrayList<>();

        Box box = targetEntity.getBoundingBox().contract(0.001, 0, 0.001);
        // West
        pos.add(new BlockPos(box.minX - 1, box.minY + 0.4, box.maxZ).west());
        pos.add(new BlockPos(box.minX - 1, box.minY + 0.4, box.minZ).west());
        if (plus) {
            pos.add(new BlockPos(box.minX - 1, box.minY + 0.4, box.maxZ).south());
            pos.add(new BlockPos(box.minX - 1, box.minY + 0.4, box.minZ).north());
        }

        return pos;
    }

}
