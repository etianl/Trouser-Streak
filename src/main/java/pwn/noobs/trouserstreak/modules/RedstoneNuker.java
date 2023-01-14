/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BambooSaplingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class RedstoneNuker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final SettingGroup sgAutoTool = settings.createGroup("AutoTool");

    // General

    private final Setting<Shape> shape = sgGeneral.add(new EnumSetting.Builder<Shape>()
        .name("shape")
        .description("The shape of nuking algorithm.")
        .defaultValue(Shape.Sphere)
        .build()
    );

    private final Setting<RedstoneNuker.Mode> mode = sgGeneral.add(new EnumSetting.Builder<RedstoneNuker.Mode>()
        .name("mode")
        .description("The way the blocks are broken.")
        .defaultValue(Mode.All)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The break range.")
        .defaultValue(4)
        .min(0)
        .visible(() -> shape.get() != Shape.Cube)
        .build()
    );


    private final Setting<Integer> range_up = sgGeneral.add(new IntSetting.Builder()
        .name("up")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_down = sgGeneral.add(new IntSetting.Builder()
        .name("down")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_left = sgGeneral.add(new IntSetting.Builder()
        .name("left")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_right = sgGeneral.add(new IntSetting.Builder()
        .name("right")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_forward = sgGeneral.add(new IntSetting.Builder()
        .name("forward")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_back = sgGeneral.add(new IntSetting.Builder()
        .name("back")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between breaking blocks.")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to break per tick. Useful when insta mining.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<RedstoneNuker.SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<RedstoneNuker.SortMode>()
        .name("sort-mode")
        .description("The blocks you want to mine first.")
        .defaultValue(RedstoneNuker.SortMode.Closest)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client side.")
        .defaultValue(true)
        .build()
    );

    // Whitelist
    private final Setting<List<Block>> whitelist = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to remove.")
            .defaultValue(Blocks.REDSTONE_BLOCK, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.ACACIA_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE, Blocks.CRIMSON_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE,
                          Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.JUNGLE_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.MANGROVE_PRESSURE_PLATE, Blocks.OAK_PRESSURE_PLATE,
                          Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.SPRUCE_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE, Blocks.DAYLIGHT_DETECTOR, Blocks.REDSTONE_WIRE, Blocks.COMPARATOR,
                          Blocks.REPEATER, Blocks.DETECTOR_RAIL, Blocks.LEVER, Blocks.SCULK_SENSOR, Blocks.FIRE, Blocks.OBSERVER, Blocks.TRIPWIRE_HOOK, Blocks.TRIPWIRE)
            .filter(this::filterBlocks)
            .build()
    );

    // Rendering

    // Bounding box
    private final Setting<Boolean> enableRenderBounding = sgRender.add(new BoolSetting.Builder()
        .name("bounding-box")
        .description("Enable rendering bounding box for Cube and Uniform Cube.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeModeBox = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("nuke-box-mode")
        .description("How the shape for the bounding box is rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColorBox = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the bounding box.")
        .defaultValue(new SettingColor(16,106,144, 100))
        .build()
    );

    private final Setting<SettingColor> lineColorBox = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the bounding box.")
        .defaultValue(new SettingColor(16,106,144, 255))
        .build()
    );

    // Broken blocks

    private final Setting<Boolean> enableRenderBreaking = sgRender.add(new BoolSetting.Builder()
        .name("broken-blocks")
        .description("Enable rendering bounding box for Cube and Uniform Cube.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeModeBreak = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("nuke-block-mode")
        .description("How the shapes for broken blocks are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 80))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    //AutoTool
    private final Setting<EnchantPreference> prefer = sgAutoTool.add(new EnumSetting.Builder<EnchantPreference>()
            .name("prefer")
            .description("Either to prefer Silk Touch, Fortune, or none.")
            .defaultValue(EnchantPreference.None)
            .build()
    );

    private final Setting<Boolean> silkTouchForEnderChest = sgAutoTool.add(new BoolSetting.Builder()
            .name("silk-touch-for-ender-chest")
            .description("Mines Ender Chests only with the Silk Touch enchantment.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiBreak = sgAutoTool.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Stops you from breaking your tool.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> breakDurability = sgAutoTool.add(new IntSetting.Builder()
            .name("anti-break-percentage")
            .description("The durability percentage to stop using a tool.")
            .defaultValue(10)
            .range(1, 100)
            .sliderRange(1, 100)
            .visible(antiBreak::get)
            .build()
    );

    private final Setting<Boolean> switchBack = sgAutoTool.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches your hand to whatever was selected when releasing your attack key.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> switchDelay = sgAutoTool.add((new IntSetting.Builder()
            .name("switch-delay")
            .description("Delay in ticks before switching tools.")
            .defaultValue(0)
            .build()
    ));

    private boolean wasPressed;
    private boolean shouldSwitch;
    private int ticks;
    private int bestSlot;
    private final Pool<BlockPos.Mutable> blockPosPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private boolean firstBlock;
    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();

    private int timer;
    private int noBlockTimer;

    private BlockPos.Mutable pos1 = new BlockPos.Mutable(); // Rendering for cubes
    private BlockPos.Mutable pos2 = new BlockPos.Mutable();
    private Box box;
    int maxh = 0;
    int maxv = 0;


    public RedstoneNuker() {
        super(Trouser.Main, "RedstoneNuker", "Breaks redstone, to keep you safe when placing TNT.");
    }


    @Override
    public void onActivate() {
        firstBlock = true;
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        timer = 0;
        noBlockTimer = 0;
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (enableRenderBreaking.get()){
            // Broken block
            renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeModeBreak.get()));
        }

        if (enableRenderBounding.get()){
            // Render bounding box if cube and should break stuff
            if (shape.get() != Shape.Sphere && mode.get() != Mode.Smash) {
                box = new Box(pos1, pos2);
                event.renderer.box(box, sideColorBox.get(), lineColorBox.get(), shapeModeBox.get(), 0);
            }
        }

    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        // Update timer
        if (timer > 0) {
            timer--;
            return;
        }

        // Calculate some stuff
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        double rangeSq = Math.pow(range.get(), 2);

        if (shape.get() == Shape.UniformCube) range.set((double) Math.round(range.get()));

        // Some render stuff

        double pX_ = pX;
        double pZ_ = pZ;
        int r = (int) Math.round(range.get());

        if (shape.get() == Shape.UniformCube) {
            pX_ += 1; // weired position stuff
            pos1.set(pX_ - r, pY - r + 1, pZ - r+1); // down
            pos2.set(pX_ + r-1, pY + r, pZ + r); // up
        } else {
            int direction = Math.round((mc.player.getRotationClient().y % 360) / 90);
            direction = Math.floorMod(direction, 4);

            // direction == 1
            pos1.set(pX_ - (range_forward.get()), Math.ceil(pY) - range_down.get(), pZ_ - range_right.get()); // down
            pos2.set(pX_ + range_back.get()+1, Math.ceil(pY + range_up.get() + 1), pZ_ + range_left.get()+1); // up

            // Only change me if you want to mess with 3D rotations:
            if (direction == 2) {
                pX_ += 1;
                pZ_ += 1;
                pos1.set(pX_ - (range_left.get()+1), Math.ceil(pY) - range_down.get(), pZ_ - (range_forward.get()+1)); // down
                pos2.set(pX_ + range_right.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_back.get()); // up
            } else if (direction == 3) {
                pX_ += 1;
                pos1.set(pX_ - (range_back.get()+1), Math.ceil(pY) - range_down.get(), pZ_ - range_left.get()); // down
                pos2.set(pX_ + range_forward.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_right.get()+1); // up
            } else if (direction == 0) {
                pZ_ += 1;
                pX_ += 1;
                pos1.set(pX_ - (range_right.get()+1), Math.ceil(pY) - range_down.get(), pZ_ - (range_back.get()+1)); // down
                pos2.set(pX_ + range_left.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_forward.get()); // up
            }

            // get largest horizontal
            maxh = 1 + Math.max(Math.max(Math.max(range_back.get(),range_right.get()),range_forward.get()),range_left.get());
            maxv = 1 + Math.max(range_up.get(), range_down.get());
        }

        if (mode.get() == Mode.Flatten){
            pos1.setY((int) Math.floor(pY));
        }
        box = new Box(pos1, pos2);


        // Find blocks to break
        BlockIterator.register(Math.max((int) Math.ceil(range.get()+1), maxh), Math.max((int) Math.ceil(range.get()), maxv), (blockPos, blockState) -> {
            // Check for air, unbreakable blocks and distance
            boolean toofarSphere = Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > rangeSq;
            boolean toofarUniformCube = maxDist(Math.floor(pX), Math.floor(pY), Math.floor(pZ), blockPos.getX(), blockPos.getY(), blockPos.getZ()) >= range.get();
            boolean toofarCube = !box.contains(Vec3d.ofCenter(blockPos));

            if (!BlockUtils.canBreak(blockPos, blockState)
                || (toofarSphere && shape.get() == Shape.Sphere)
                || (toofarUniformCube && shape.get() == Shape.UniformCube)
                || (toofarCube && shape.get() == Shape.Cube))
                return;

            // Flatten
            if (mode.get() == Mode.Flatten && blockPos.getY() < Math.floor(mc.player.getY())) return;

            // Smash
            if (mode.get() == Mode.Smash && blockState.getHardness(mc.world, blockPos) != 0) return;

            // Check for selected
            if (!whitelist.get().contains(blockState.getBlock())) return;

            // Add block
            blocks.add(blockPosPool.get().set(blockPos));
        });

        // Break block if found
        BlockIterator.after(() -> {
            // Sort blocks

			if (sortMode.get() == SortMode.TopDown)
                blocks.sort(Comparator.comparingDouble(value -> -1*value.getY()));
            else if (sortMode.get() != SortMode.None)
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? 1 : -1)));

            // Check if some block was found
            if (blocks.isEmpty()) {
                // If no block was found for long enough then set firstBlock flag to true to not wait before breaking another again
                if (noBlockTimer++ >= delay.get()) firstBlock = true;
                return;
            }
            else {
                noBlockTimer = 0;
            }

            // Update timer
            if (!firstBlock && !lastBlockPos.equals(blocks.get(0))) {
                timer = delay.get();

                firstBlock = false;
                lastBlockPos.set(blocks.get(0));

                if (timer > 0) return;
            }

            // Break
            int count = 0;

            for (BlockPos block : blocks) {
                if (count >= maxBlocksPerTick.get()) break;

                boolean canInstaMine = BlockUtils.canInstaBreak(block);

                BlockUtils.breakBlock(block, swingHand.get());
                renderBlocks.add(renderBlockPool.get().set(block));

                lastBlockPos.set(block);

                count++;
                if (!canInstaMine) break;
            }

            firstBlock = false;

            // Clear current block positions
            for (BlockPos.Mutable blockPos : blocks) blockPosPool.free(blockPos);
            blocks.clear();
        });
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Modules.get().isActive(InfinityMiner.class)) return;

        if (switchBack.get() && !mc.options.attackKey.isPressed() && wasPressed && InvUtils.previousSlot != -1) {
            InvUtils.swapBack();
            wasPressed = false;
            return;
        }

        if (ticks <= 0 && shouldSwitch && bestSlot != -1) {
            InvUtils.swap(bestSlot, switchBack.get());
            shouldSwitch = false;
        } else {
            ticks--;
        }

        wasPressed = mc.options.attackKey.isPressed();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (Modules.get().isActive(InfinityMiner.class)) return;

        // Get blockState
        BlockState blockState = mc.world.getBlockState(event.blockPos);
        if (!BlockUtils.canBreak(event.blockPos, blockState)) return;

        // Check if we should switch to a better tool
        ItemStack currentStack = mc.player.getMainHandStack();

        double bestScore = -1;
        bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            double score = getScore(mc.player.getInventory().getStack(i), blockState, silkTouchForEnderChest.get(), prefer.get(), itemStack -> !shouldStopUsing(itemStack));
            if (score < 0) continue;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if ((bestSlot != -1 && (bestScore > getScore(currentStack, blockState, silkTouchForEnderChest.get(), prefer.get(), itemStack -> !shouldStopUsing(itemStack))) || shouldStopUsing(currentStack) || !isTool(currentStack))) {
            ticks = switchDelay.get();

            if (ticks == 0) InvUtils.swap(bestSlot, true);
            else shouldSwitch = true;
        }

        // Anti break
        currentStack = mc.player.getMainHandStack();

        if (shouldStopUsing(currentStack) && isTool(currentStack)) {
            mc.options.attackKey.setPressed(false);
            event.setCancelled(true);
        }
    }

    private boolean shouldStopUsing(ItemStack itemStack) {
        return antiBreak.get() && (itemStack.getMaxDamage() - itemStack.getDamage()) < (itemStack.getMaxDamage() * breakDurability.get() / 100);
    }

    public static double getScore(ItemStack itemStack, BlockState state, boolean silkTouchEnderChest, RedstoneNuker.EnchantPreference enchantPreference, Predicate<ItemStack> good) {
        if (!good.test(itemStack) || !isTool(itemStack)) return -1;

        if (silkTouchEnderChest
                && state.getBlock() == Blocks.ENDER_CHEST
                && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) == 0) {
            return -1;
        }

        double score = 0;

        score += itemStack.getMiningSpeedMultiplier(state) * 1000;
        score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);

        if (enchantPreference == EnchantPreference.Fortune) score += EnchantmentHelper.getLevel(Enchantments.FORTUNE, itemStack);
        if (enchantPreference == EnchantPreference.SilkTouch) score += EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack);

        if (itemStack.getItem() instanceof SwordItem item && (state.getBlock() instanceof BambooBlock || state.getBlock() instanceof BambooSaplingBlock))
            score += 9000 + (item.getMaterial().getMiningLevel() * 1000);


        return score;
    }

    public static boolean isTool(ItemStack itemStack) {
        return itemStack.getItem() instanceof ToolItem || itemStack.getItem() instanceof ShearsItem;
    }
    private boolean filterBlocks(Block block) {
        return isRedstoneBlock(block);
    }

    private boolean isRedstoneBlock(Block block) {
        return block instanceof RedstoneBlock ||
                block instanceof RedstoneTorchBlock ||
                block instanceof AbstractPressurePlateBlock ||
                block instanceof DaylightDetectorBlock ||
                block instanceof RedstoneWireBlock ||
                block instanceof ComparatorBlock ||
                block instanceof RepeaterBlock ||
                block instanceof ButtonBlock ||
                block instanceof DetectorRailBlock ||
                block instanceof LeverBlock ||
                block instanceof SculkSensorBlock ||
                block instanceof TargetBlock ||
                block instanceof TrappedChestBlock ||
                block instanceof FireBlock ||
                block instanceof LightningRodBlock ||
                block instanceof LecternBlock ||
                block instanceof TripwireHookBlock ||
                block instanceof TripwireBlock ||
                block instanceof ObserverBlock;
    }
    public enum Mode {
        All,
        Flatten,
        Smash
    }

    public enum SortMode {
        None,
        Closest,
        Furthest,
        TopDown

    }
    public enum Shape {
        Cube,
        UniformCube,
        Sphere
    }
    public enum EnchantPreference {
        None,
        Fortune,
        SilkTouch
    }


    public static double maxDist(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Gets the largest X, Y or Z difference, manhattan style
        double dX = Math.ceil(Math.abs(x2 - x1));
        double dY = Math.ceil(Math.abs(y2 - y1));
        double dZ = Math.ceil(Math.abs(z2 - z1));
        return Math.max(Math.max(dX, dY), dZ);
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = 8;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;

            event.renderer.box(pos, sides, lines, shapeMode, 0);

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }
}
