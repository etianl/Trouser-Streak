package pwn.noobs.trouserstreak.utils;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BEntityUtils {
    public static Entity deadEntity;
    public static boolean isDeathPacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 3) {
                deadEntity = packet.getEntity(mc.world);
                return deadEntity instanceof PlayerEntity;
            }
        }
        return false;
    }

    public static Direction rayTraceCheck(BlockPos pos, boolean forceReturn) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        Direction[] var3 = Direction.values();

        for (Direction direction : var3) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d((double) pos.getX() + 0.5D + (double) direction.getVector().getX() * 0.5D, (double) pos.getY() + 0.5D + (double) direction.getVector().getY() * 0.5D, (double) pos.getZ() + 0.5D + (double) direction.getVector().getZ() * 0.5D), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }

        if (forceReturn) {
            if ((double)pos.getY() > eyesPos.y) {
                return Direction.DOWN;
            } else {
                return Direction.UP;
            }
        } else {
            return null;
        }
    }

    public static int getBlockBreakingSpeed(BlockState block, BlockPos pos, int slot) {
        PlayerEntity player = mc.player;

        float f = (player.getInventory().getStack(slot)).getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            int i = EnchantmentHelper.get(player.getInventory().getStack(slot)).getOrDefault(Enchantments.EFFICIENCY, 0);
            if (i > 0) {
                f += (float)(i * i + 1);
            }
        }

        if (StatusEffectUtil.hasHaste(player)) {
            f *= 1.0F + (float)(StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }

        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k;
            switch(player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    k = 0.3F;
                    break;
                case 1:
                    k = 0.09F;
                    break;
                case 2:
                    k = 0.0027F;
                    break;
                case 3:
                default:
                    k = 8.1E-4F;
            }

            f *= k;
        }

        if (player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
            f /= 5.0F;
        }

        if (!player.isOnGround()) {
            f /= 5.0F;
        }

        float t = block.getHardness(mc.world, pos);
        if (t == -1.0F) {
            return 0;
        } else {
            return (int) Math.ceil(1 / (f / t / 30));
        }
    }

    public static Vec3d crystalEdgePos(EndCrystalEntity crystal) {
        Vec3d crystalPos = crystal.getPos();
        return new Vec3d(
                crystalPos.x < mc.player.getX() ? crystalPos.add(Math.min(1, mc.player.getX() - crystalPos.x), 0, 0).x : crystalPos.x > mc.player.getX() ? crystalPos.add(Math.max(-1, mc.player.getX() - crystalPos.x), 0, 0).x : crystalPos.x,
                crystalPos.y < mc.player.getY() ? crystalPos.add(0, Math.min(1, mc.player.getY() - crystalPos.y), 0).y : crystalPos.y,
                crystalPos.z < mc.player.getZ() ? crystalPos.add(0, 0, Math.min(1, mc.player.getZ() - crystalPos.z)).z : crystalPos.z > mc.player.getZ() ? crystalPos.add(0, 0, Math.max(-1, mc.player.getZ() - crystalPos.z)).z : crystalPos.z);
    }

    public static boolean isBedrock(BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
    }

    public enum BlastResistantType {
        Any, // Any blast resistant block
        Unbreakable, // Can't be mined
        Mineable, // You can mine the block
        NotAir // Doesn't matter as long it's not air
    }

    public static boolean isBlastResistant(BlockPos pos, BlastResistantType type) {
        Block block = mc.world.getBlockState(pos).getBlock();
        switch (type) {
            case Any, Mineable -> {
                return block == Blocks.OBSIDIAN
                    || block == Blocks.CRYING_OBSIDIAN
                    || block instanceof AnvilBlock
                    || block == Blocks.NETHERITE_BLOCK
                    || block == Blocks.ENDER_CHEST
                    || block == Blocks.RESPAWN_ANCHOR
                    || block == Blocks.ANCIENT_DEBRIS
                    || block == Blocks.ENCHANTING_TABLE
                    || (block == Blocks.BEDROCK && type == BlastResistantType.Any)
                    || (block == Blocks.END_PORTAL_FRAME && type == BlastResistantType.Any);
            }
            case Unbreakable -> {
                return block == Blocks.BEDROCK
                    || block == Blocks.END_PORTAL_FRAME;
            }
            case NotAir -> {
                return block != Blocks.AIR;
            }
        }
        return false;
    }

    public static List<BlockPos> getSurroundBlocks(PlayerEntity player) {
        if (player == null) return null;

        List<BlockPos> positions = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;

            BlockPos pos = playerPos(player).offset(direction);

            if (isBlastResistant(pos, BlastResistantType.Mineable)) { positions.add(pos); }
        }

        return positions;
    }

    public static BlockPos getCityBlock(PlayerEntity player) {
        List<BlockPos> posList = getSurroundBlocks(player);
        posList.sort(Comparator.comparingDouble(BPlayerUtils::distanceFromEye));
        return posList.isEmpty() ? null : posList.get(0);
    }

    public static BlockPos getTargetBlock(PlayerEntity player) {
        BlockPos finalPos = null;

        List<BlockPos> positions = getSurroundBlocks(player);
        List<BlockPos> myPositions = getSurroundBlocks(mc.player);

        if (positions == null) return null;

        for (BlockPos pos : positions) {

            if (myPositions != null && !myPositions.isEmpty() && myPositions.contains(pos)) continue;

            if (finalPos == null) {
                finalPos = pos;
                continue;
            }

            if (mc.player.squaredDistanceTo(Utils.vec3d(pos)) < mc.player.squaredDistanceTo(Utils.vec3d(finalPos))) {
                finalPos = pos;
            }
        }

        return finalPos;
    }

    public static String getName(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof PlayerEntity) return entity.getEntityName();
        return entity.getType().getName().getString();
    }

    public static BlockPos playerPos(PlayerEntity targetEntity) {
        return BWorldUtils.roundBlockPos(targetEntity.getPos());
    }

    public static boolean isTopTrapped(PlayerEntity targetEntity, BlastResistantType type) {
        return isBlastResistant(playerPos(targetEntity).add(0, 2, 0), type);
    }

    public static boolean isFaceSurrounded(PlayerEntity targetEntity, BlastResistantType type) {
        return isBlastResistant(playerPos(targetEntity).add(1, 1, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(-1, 1, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 1, 1), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 1, -1), type);
    }

    public static boolean isBothTrapped(PlayerEntity targetEntity, BlastResistantType type) {
        return isTopTrapped(targetEntity, type) && isFaceSurrounded(targetEntity, type);
    }

    public static boolean isAnyTrapped(PlayerEntity targetEntity, BlastResistantType type) {
        return isTopTrapped(targetEntity, type) || isFaceSurrounded(targetEntity, type);
    }

    public static boolean isSurrounded(PlayerEntity targetEntity, BlastResistantType type) {
        return isBlastResistant(playerPos(targetEntity).add(1, 0, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(-1, 0, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 0, 1), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 0, -1), type);
    }

    public static boolean isSurroundBroken(PlayerEntity targetEntity, BlastResistantType type) {
        return (!isBlastResistant(playerPos(targetEntity).add(1, 0, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(-1, 0, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 0, 1), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 0, -1), type))

                || (isBlastResistant(playerPos(targetEntity).add(1, 0, 0), type)
                && !isBlastResistant(playerPos(targetEntity).add(-1, 0, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 0, 1), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 0, -1), type))

                || (isBlastResistant(playerPos(targetEntity).add(1, 0, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(-1, 0, 0), type)
                && !isBlastResistant(playerPos(targetEntity).add(0, 0, 1), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 0, -1), type))

                || (isBlastResistant(playerPos(targetEntity).add(1, 0, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(-1, 0, 0), type)
                && isBlastResistant(playerPos(targetEntity).add(0, 0, 1), type)
                && !isBlastResistant(playerPos(targetEntity).add(0, 0, -1), type));
    }

    public static boolean isBurrowed(PlayerEntity targetEntity, BlastResistantType type) {
        BlockPos playerPos = BWorldUtils.roundBlockPos(new Vec3d(targetEntity.getX(), targetEntity.getY() + 0.4, targetEntity.getZ()));
        // Adding a 0.4 to the Y check since sometimes when the player moves around weirdly/ after chorusing they tend to clip into the block under them
        return isBlastResistant(playerPos, type);
    }

    public static boolean isWebbed(PlayerEntity targetEntity) {
        return BWorldUtils.doesBoxTouchBlock(targetEntity.getBoundingBox(), Blocks.COBWEB);
    }

    public static boolean isInHole(PlayerEntity targetEntity, boolean doubles, BlastResistantType type) {
        if (!Utils.canUpdate()) return false;

        BlockPos blockPos = playerPos(targetEntity);
        int air = 0;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;

            if (!isBlastResistant(blockPos.offset(direction), type)) {
                if (!doubles || direction == Direction.DOWN) return false;

                air++;

                for (Direction dir : Direction.values()) {
                    if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                    if (!isBlastResistant(blockPos.offset(direction).offset(dir), type)) {
                        return false;
                    }
                }
            }
        }

        return air < 2;
    }

    public static boolean isHelmet(Item item) {
        return item == Items.NETHERITE_HELMET
                || item == Items.DIAMOND_HELMET
                || item == Items.IRON_HELMET
                || item == Items.GOLDEN_HELMET
                || item == Items.CHAINMAIL_HELMET
                || item == Items.LEATHER_HELMET
                || item == Items.TURTLE_HELMET;
    }

    public static boolean isChestplate(Item item) {
        return item == Items.NETHERITE_CHESTPLATE
                || item == Items.DIAMOND_CHESTPLATE
                || item == Items.IRON_CHESTPLATE
                || item == Items.GOLDEN_CHESTPLATE
                || item == Items.CHAINMAIL_CHESTPLATE
                || item == Items.LEATHER_CHESTPLATE;
    }

    public static boolean isLeggings(Item item) {
        return item == Items.NETHERITE_LEGGINGS
                || item == Items.DIAMOND_LEGGINGS
                || item == Items.IRON_LEGGINGS
                || item == Items.GOLDEN_LEGGINGS
                || item == Items.CHAINMAIL_LEGGINGS
                || item == Items.LEATHER_LEGGINGS;
    }

    public static boolean isBoots(Item item) {
        return item == Items.NETHERITE_BOOTS
                || item == Items.DIAMOND_BOOTS
                || item == Items.IRON_BOOTS
                || item == Items.GOLDEN_BOOTS
                || item == Items.CHAINMAIL_BOOTS
                || item == Items.LEATHER_BOOTS;
    }
}
