//Written By etianll
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.modules.addon.TrouserModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class InstaSafetyBox extends TrouserModule {
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
            .sliderRange(1, 5)
            .min(1)
            .visible(() -> mode.get() == Modes.Sphere)
            .build()
    );
    private final Setting<Integer> boxreach = sgGeneral.add(new IntSetting.Builder()
            .name("Box Range")
            .description("Your Range, in blocks.")
            .defaultValue(1)
            .sliderRange(1, 4)
            .min(1)
            .visible(() -> mode.get() == Modes.Box)
            .build()
    );
    private final Setting<Integer> blockpertick = sgGeneral.add(new IntSetting.Builder()
            .name("Blocks per Tick")
            .description("How many blocks to place per tick.")
            .defaultValue(4)
            .sliderRange(1, 10)
            .min(1)
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
        super("InstaSafetyBox", "Makes you safe by building box.");
    }

    @Override
    public void onDeactivate() {
        if (playerneedstosneak) mc.options.sneakKey.setPressed(false);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mode.get() == Modes.Sphere) reach = spherereach.get();
        else if (mode.get() == Modes.Box) reach = boxreach.get();
        if (center.get()) PlayerUtils.centerPlayer();
        if (mc.player != null && sneaky.get() && mc.player.isOnGround() && mc.player.getY() >= Math.floor(mc.player.getY()) + 0.2) {
            mc.options.sneakKey.setPressed(true);
            playerneedstosneak = true;
        }
        if (ticks >= tickdelay.get()) {
            // Create a list of all the blocks within the specified range
            List<BlockPos> blocks = new ArrayList<>();

            for (int x = (int) (mc.player.getBlockX() - Math.round(Math.ceil(reach))); x <= mc.player.getBlockX() + Math.round(Math.ceil(reach)); x++) {
                for (int y = (int) (mc.player.getBlockY() - Math.round(Math.ceil(reach))); y <= (mc.player.getBlockY() + 1) + Math.round(Math.ceil(reach)); y++) {
                    for (int z = (int) (mc.player.getBlockZ() - Math.round(Math.ceil(reach))); z <= mc.player.getBlockZ() + Math.round(Math.ceil(reach)); z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        Vec3d playerPos1 = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()).toCenterPos();
                        Vec3d playerPos2 = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY() + 1, mc.player.getBlockZ()).toCenterPos();
                        double distance1 = playerPos1.distanceTo(blockPos.toCenterPos());
                        double distance2 = playerPos2.distanceTo(blockPos.toCenterPos());
                        switch (mode.get()) {
                            case Sphere -> {
                                if (distance1 <= reach || distance2 <= reach) {
                                    if (!blocks.contains(blockPos) && !blockPos.equals(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ())) && blockPos != new BlockPos(mc.player.getBlockX(), mc.player.getBlockY() + 1, mc.player.getBlockZ()) && mc.world.getBlockState(blockPos).getBlock().getDefaultState().isReplaceable()) {
                                        blocks.add(blockPos);
                                    }
                                }
                            }
                            case Box -> {
                                if (!blocks.contains(blockPos) && !blockPos.equals(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ())) && blockPos != new BlockPos(mc.player.getBlockX(), mc.player.getBlockY() + 1, mc.player.getBlockZ()) && mc.world.getBlockState(blockPos).getBlock().getDefaultState().isReplaceable()) {
                                    blocks.add(blockPos);
                                }
                            }
                        }
                    }
                }
            }

            // Sort the blocks by distance from the player
            blocks.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(mc.player.getPos())));

            int count = 0;
            for (BlockPos blockPos : blocks) {
                if (count >= blockpertick.get() || mc.interactionManager == null) {
                    break;
                }
                if (hard.get() || isInvalidBlock(mc.player.getMainHandStack().getItem().getDefaultStack()))
                    cascadingpileof();

                if (!Objects.equals(blockPos, new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ())) && blockPos != new BlockPos(mc.player.getBlockX(), mc.player.getBlockY() + 1, mc.player.getBlockZ()) && mc.world.getBlockState(blockPos).getBlock().getDefaultState().isReplaceable() && !isInvalidBlock(mc.player.getMainHandStack().getItem().getDefaultStack())) {
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos));
                    if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(blockPos), Direction.DOWN, blockPos, false));
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
            ItemStack stack = mc.player.getInventory().getStack(i);
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
                    return block.getHardness() < 0 ? Double.MAX_VALUE : block.getHardness();
                }))
                .orElse(null);

        mc.player.getInventory().selectedSlot = mc.player.getInventory().getSlotWithStack(hardestBlock);
    }

    private boolean isInvalidBlock(ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem)
                || stack.getItem() instanceof BedItem
                || stack.getItem() instanceof PowderSnowBucketItem
                || stack.getItem() instanceof ScaffoldingItem
                || stack.getItem() instanceof TallBlockItem
                || stack.getItem() instanceof VerticallyAttachableBlockItem
                || stack.getItem() instanceof PlaceableOnWaterItem
                || ((BlockItem) stack.getItem()).getBlock() instanceof PlantBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TorchBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AbstractRedstoneGateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof RedstoneWireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WallBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceGateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FallingBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AbstractRailBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AbstractSignBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BellBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CarpetBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ConduitBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof DeadCoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof DeadCoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripwireHookBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PointedDripstoneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripwireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PressurePlateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WallMountedBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AmethystClusterBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BuddingAmethystBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusFlowerBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusPlantBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LanternBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CandleBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TntBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CakeBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CobwebBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SugarCaneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SporeBlossomBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof KelpBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof GlowLichenBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CactusBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BambooBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FlowerPotBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LadderBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SlabBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TrapdoorBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof HeavyCoreBlock
                || skippableBlox.get().contains(((BlockItem) stack.getItem()).getBlock());
    }

    public enum Modes {
        Sphere, Box
    }
}