//Written By etianll
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BaseCoralFanBlock;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.ConduitBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.HeavyCoreBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class InstaSafetyBox extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("mode")
            .description("the shape of the safety box")
            .defaultValue(Modes.Sphere)
            .build());
    private final Setting<List<Block>> skippableBlox = sgGeneral.add(new BlockListSetting.Builder()
            .name("Blocks to not use")
            .description("Do not use these blocks for building.")
            .build()
    );
    private final Setting<Double> spherereach = sgGeneral.add(new DoubleSetting.Builder()
            .name("Sphere Range")
            .description("Your Range, in blocks.")
            .defaultValue(1)
            .sliderRange(1,5)
            .min (1)
            .visible(() -> mode.get() == Modes.Sphere)
            .build()
    );
    private final Setting<Integer> boxreach = sgGeneral.add(new IntSetting.Builder()
            .name("Box Range")
            .description("Your Range, in blocks.")
            .defaultValue(1)
            .sliderRange(1,4)
            .min (1)
            .visible(() -> mode.get() == Modes.Box)
            .build()
    );
    private final Setting<Integer> blockpertick = sgGeneral.add(new IntSetting.Builder()
            .name("Blocks per Tick")
            .description("How many blocks to place per tick.")
            .defaultValue(4)
            .sliderRange(1,10)
            .min (1)
            .build()
    );
    private final Setting<Integer> tickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("TickDelay")
            .description("Delays placement by this many ticks.")
            .defaultValue(1)
            .sliderRange(0, 10)
            .build()
    );
    public final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate Player")
            .description("Rotates the player to the direction of the blocks being placed.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("Swing Arm")
            .description("Swings arm or not.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> hard = sgGeneral.add(new BoolSetting.Builder()
            .name("Always choose hardest block")
            .description("Always chooses the hardest block even if you are already holding a valid block.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> toggle = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle on blocks placed")
            .description("Toggles module when all blocks have been attempted to be placed once.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
            .name("Center player")
            .description("Centers the player or not.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> sneaky = sgGeneral.add(new BoolSetting.Builder()
            .name("Sneak on slabs")
            .description("Makes the player shorter so you can safety box properly when standing on a slab.")
            .defaultValue(true)
            .build()
    );
    private int ticks;
    private boolean playerneedstosneak = false;
    private double reach = 0;

    public InstaSafetyBox() {
        super(Trouser.Main, "InstaSafetyBox", "Makes you safe by building box.");
    }
    @Override
    public void onDeactivate() {
        if (playerneedstosneak)mc.options.keyShift.setDown(false);
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mode.get()==Modes.Sphere) reach=spherereach.get();
        else if (mode.get()==Modes.Box) reach=boxreach.get();
        if (center.get()) PlayerUtils.centerPlayer();
        if (mc.player != null && sneaky.get() && mc.player.onGround() && mc.player.getY() >= Math.floor(mc.player.getY()) + 0.2) {
            mc.options.keyShift.setDown(true);
            playerneedstosneak = true;
        }
        if (ticks >= tickdelay.get()) {
            // Create a list of all the blocks within the specified range
            List<BlockPos> blocks = new ArrayList<>();

            for (int x = (int) (mc.player.getBlockX() - Math.round(Math.ceil(reach))); x <= mc.player.getBlockX() + Math.round(Math.ceil(reach)); x++) {
                for (int y = (int) (mc.player.getBlockY() - Math.round(Math.ceil(reach))); y <= (mc.player.getBlockY()+1) + Math.round(Math.ceil(reach)); y++) {
                    for (int z = (int) (mc.player.getBlockZ() - Math.round(Math.ceil(reach))); z <= mc.player.getBlockZ() + Math.round(Math.ceil(reach)); z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        Vec3 playerPos1 = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()).getCenter();
                        Vec3 playerPos2 = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY()+1, mc.player.getBlockZ()).getCenter();
                        double distance1 = playerPos1.distanceTo(blockPos.getCenter());
                        double distance2 = playerPos2.distanceTo(blockPos.getCenter());
                        switch (mode.get()) {
                            case Sphere -> {
                                if (distance1 <= reach || distance2 <= reach) {
                                    if (!blocks.contains(blockPos) && !blockPos.equals(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ())) && blockPos != new BlockPos(mc.player.getBlockX(), mc.player.getBlockY()+1, mc.player.getBlockZ()) && mc.level.getBlockState(blockPos).getBlock().defaultBlockState().canBeReplaced()) {
                                        blocks.add(blockPos);
                                    }
                                }
                            }
                            case Box -> {
                                if (!blocks.contains(blockPos) && !blockPos.equals(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ())) && blockPos != new BlockPos(mc.player.getBlockX(), mc.player.getBlockY()+1, mc.player.getBlockZ()) && mc.level.getBlockState(blockPos).getBlock().defaultBlockState().canBeReplaced()) {
                                    blocks.add(blockPos);
                                }
                            }
                        }
                    }
                }
            }

            // Sort the blocks by distance from the player
            blocks.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(mc.player.position())));

            int count = 0;
            for (BlockPos blockPos : blocks) {
                if (count >= blockpertick.get() || mc.gameMode == null) {
                    break;
                }
                if (hard.get() || isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) cascadingpileof();

                if (!Objects.equals(blockPos, new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ())) && blockPos != new BlockPos(mc.player.getBlockX(), mc.player.getBlockY()+1, mc.player.getBlockZ()) && mc.level.getBlockState(blockPos).getBlock().defaultBlockState().canBeReplaced() && !isInvalidBlock(mc.player.getMainHandItem().getItem().getDefaultInstance())) {
                    if (rotate.get())Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos));
                    if (swing.get())mc.player.swing(InteractionHand.MAIN_HAND);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(blockPos), Direction.DOWN, blockPos, false));
                    count++;
                }
                if (count >= blocks.size() && toggle.get()) {
                    toggle();
                }
            }

            ticks = 0;
        }
        ticks++;
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
        }
        if (event.screen instanceof DeathScreen) {
            toggle();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }
    private void cascadingpileof() {
        List<ItemStack> validBlocks = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem && !isInvalidBlock(stack)) {
                validBlocks.add(stack);
            }
        }

        if (validBlocks.isEmpty()) {
            return;
        }

        // Find the hardest block
        ItemStack hardestBlock = validBlocks.stream()
                .max(Comparator.comparingDouble(stack -> {
                    Block block = ((BlockItem) stack.getItem()).getBlock();
                    return block.defaultDestroyTime() < 0 ? Double.MAX_VALUE : block.defaultDestroyTime();
                }))
                .orElse(null);

        mc.player.getInventory().setSelectedSlot(mc.player.getInventory().findSlotMatchingItem(hardestBlock));
    }
    private boolean isInvalidBlock(ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem)
                || stack.getItem() instanceof BedItem
                || stack.getItem() instanceof SolidBucketItem
                || stack.getItem() instanceof ScaffoldingBlockItem
                || stack.getItem() instanceof DoubleHighBlockItem
                || stack.getItem() instanceof StandingAndWallBlockItem
                || stack.getItem() instanceof PlaceOnWaterBlockItem
                || ((BlockItem) stack.getItem()).getBlock() instanceof VegetationBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TorchBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof DiodeBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof RedStoneWireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WallBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceGateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FallingBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseRailBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SignBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BellBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CarpetBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ConduitBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseCoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseCoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripWireHookBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PointedDripstoneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripWireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PressurePlateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FaceAttachedHorizontalDirectionalBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AmethystClusterBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BuddingAmethystBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusFlowerBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusPlantBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LanternBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CandleBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TntBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CakeBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WebBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SugarCaneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SporeBlossomBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof KelpBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof GlowLichenBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CactusBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BambooStalkBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FlowerPotBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LadderBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SlabBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TrapDoorBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof HeavyCoreBlock
                || skippableBlox.get().contains(((BlockItem) stack.getItem()).getBlock());
    }
    public enum Modes {
        Sphere, Box
    }
}