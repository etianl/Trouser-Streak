/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
//plz come back meteor tweaks I miss you

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.modules.addon.TrouserModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BetterScaffold extends TrouserModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Selected blocks.")
            .defaultValue(Blocks.TNT)
            .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
            .name("blocks-filter")
            .description("How to use the block list setting")
            .defaultValue(ListMode.Whitelist)
            .build()
    );
    private final Setting<PlaceMode> placementMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
            .name("Placement Mode")
            .description("Where the blocks go.")
            .defaultValue(PlaceMode.BelowFeet)
            .build()
    );
    private final Setting<Boolean> keepY = sgGeneral.add(new BoolSetting.Builder()
            .name("Keep Y")
            .description("Only places at the Y level you are at when enabling the module.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Double> keepYreach = sgGeneral.add(new DoubleSetting.Builder()
            .name("Keep Y Reach")
            .description("How far to attempt to place blocks from the character.")
            .defaultValue(6)
            .min(0)
            .sliderMax(8)
            .visible(keepY::get)
            .build()
    );
    private final Setting<Boolean> fastTower = sgGeneral.add(new BoolSetting.Builder()
            .name("fast-tower")
            .description("Whether or not to scaffold upwards faster.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> renderSwing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing")
            .description("Renders your client-side swing.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Automatically swaps to a block before placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates towards the blocks being placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Allow air place.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("closest-block-range")
            .description("How far can scaffold place blocks.")
            .defaultValue(4)
            .min(0)
            .sliderMax(8)
            .visible(() -> !airPlace.get())
            .build()
    );

    private final Setting<Boolean> onSurface = sgGeneral.add(new BoolSetting.Builder()
            .name("on-surface")
            .description("Places horizontal and vertical blocks when already standing on a block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("How far scaffold should place blocks horizontally.")
            .defaultValue(1)
            .min(1)
            .sliderMin(1)
            .sliderMax(8)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("How far scaffold should place blocks vertically.")
            .defaultValue(1)
            .min(1)
            .sliderMin(1)
            .sliderMax(8)
            .build()
    );
    private final Setting<Integer> aboveHeadDistance = sgGeneral.add(new IntSetting.Builder()
            .name("distance-above-head")
            .description("How far scaffold should place blocks from the player's head.")
            .defaultValue(2)
            .min(1)
            .sliderMin(1)
            .sliderMax(8)
            .visible(() -> placementMode.get() == PlaceMode.AboveHead)
            .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232))
            .build()
    );

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private final BlockPos.Mutable bp = new BlockPos.Mutable();
    private final BlockPos.Mutable prevBp = new BlockPos.Mutable();
    private int initialY;

    private boolean lastWasSneaking;
    private double lastSneakingY;

    public BetterScaffold() {
        super("betterScaffold", "Automatically places blocks under you. Credits to MeteorTweaks.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        initialY = mc.player.getBlockY() - 1;
        lastWasSneaking = mc.options.sneakKey.isPressed();
        if (lastWasSneaking) {
            assert mc.player != null;
            lastSneakingY = mc.player.getY();
        }

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        // Ticking fade animation
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        if (airPlace.get()) {
            assert mc.player != null;
            Vec3d vec = mc.player.getPos().add(mc.player.getVelocity()).add(0, -0.5f, 0);
            bp.set(vec.getX(), vec.getY(), vec.getZ());

        } else {
            assert mc.player != null;
            if (BlockUtils.getPlaceSide(mc.player.getBlockPos().down()) != null) {
                bp.set(mc.player.getBlockPos().down());

            } else {
                Vec3d pos = mc.player.getPos();
                pos = pos.add(0, -0.98f, 0);
                pos.add(mc.player.getVelocity());

                if (PlayerUtils.distanceTo(prevBp) > placeRange.get()) {
                    List<BlockPos> blockPosArray = new ArrayList<>();

                    for (int x = (int) (mc.player.getX() - placeRange.get()); x < mc.player.getX() + placeRange.get(); x++) {
                        for (int z = (int) (mc.player.getZ() - placeRange.get()); z < mc.player.getZ() + placeRange.get(); z++) {
                            assert mc.world != null;
                            for (int y = (int) Math.max(mc.world.getBottomY(), mc.player.getY() - placeRange.get()); y < Math.min(mc.world.getTopYInclusive(), mc.player.getY() + placeRange.get()); y++) {
                                bp.set(x, y, z);
                                if (!mc.world.getBlockState(bp).isAir()) blockPosArray.add(new BlockPos(bp));
                            }
                        }
                    }
                    if (blockPosArray.isEmpty()) {
                        return;
                    }

                    blockPosArray.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));

                    prevBp.set(blockPosArray.get(0));
                }

                Vec3d vecPrevBP = new Vec3d((double) prevBp.getX() + 0.5f,
                        (double) prevBp.getY() + 0.5f,
                        (double) prevBp.getZ() + 0.5f);

                Vec3d sub = pos.subtract(vecPrevBP);
                Direction facing;
                if (sub.getY() < -0.5f) {
                    facing = Direction.DOWN;
                } else if (sub.getY() > 0.5f) {
                    facing = Direction.UP;
                } else facing = Direction.getFacing(sub.getX(), 0, sub.getZ());

                bp.set(prevBp.offset(facing));
            }
        }
        // Check if keepY is enabled and adjust the block position
        if (keepY.get()) {
            bp.setY(initialY);
            // Skip placing blocks if the distance exceeds keepYreach
            if (bp.getSquaredDistance(mc.player.getPos()) > keepYreach.get()) {
                return;
            }
        }

        FindItemResult item = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        if (!item.found()) return;


        if (item.getHand() == null && !autoSwitch.get()) return;

        // Move down if shifting
        if (mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()) {
            if (lastSneakingY - mc.player.getY() < 0.1) {
                lastWasSneaking = false;
                return;
            }
        } else {
            lastWasSneaking = false;
        }
        if (!lastWasSneaking) lastSneakingY = mc.player.getY();

        if (mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed() && fastTower.get()) {
            mc.player.setVelocity(0, 0.42f, 0);
        }
        int vOffset;
        if (placementMode.get() == PlaceMode.BelowFeet) {
            vOffset = 0;
        } else if (placementMode.get() == PlaceMode.AboveHead) {
            vOffset = aboveHeadDistance.get() + 2;
        } else {
            return; // Skip the rest of the code if the placement mode is not valid
        }

        if (BlockUtils.place(bp.add(0, vOffset, 0), item, rotate.get(), 50, renderSwing.get(), true)) {

            if (horizontalRadius.get() > 1 || verticalRadius.get() > 1) {
                horizontalAndVertical(bp, item);
            }

            // Render block if was placed
            renderBlocks.add(renderBlockPool.get().set(bp.add(0, vOffset, 0)));

            // Move player down so they are on top of the placed block ready to jump again
            if (mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed() && !mc.player.isOnGround()) {
                assert mc.world != null;
                if (!mc.world.getBlockState(bp).isAir() && fastTower.get()) {
                    mc.player.setVelocity(0, -0.28f, 0);
                }
            }
        } else if (onSurface.get()) {
            horizontalAndVertical(new BlockPos.Mutable(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ()), item);
        }

        assert mc.world != null;
        if (!mc.world.getBlockState(bp).isAir()) {
            prevBp.set(bp);
        }
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(block)) return false;

        if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, pos))) return false;
        if (!(block instanceof FallingBlock)) return true;
        assert mc.world != null;
        return !FallingBlock.canFallThrough(mc.world.getBlockState(pos));
    }

    //Place horizontal and vertical blocks
    private void horizontalAndVertical(BlockPos.Mutable bp, FindItemResult item) {
        if (keepY.get()) {
            // Only place blocks at the initial Y level
            bp.setY(initialY);
        }
        if (verticalRadius.get() > 1) {
            for (int i = 1; i < verticalRadius.get(); i++) {
                int vOffset;
                if (placementMode.get() == PlaceMode.BelowFeet) {
                    vOffset = -i;
                } else if (placementMode.get() == PlaceMode.AboveHead) {
                    vOffset = i + aboveHeadDistance.get() + 2;
                } else {
                    continue; // Skip the loop iteration if the placement mode is not valid
                }

                if (BlockUtils.place(bp.add(0, vOffset, 0), item, rotate.get(), 50, renderSwing.get(), true)) {
                    renderBlocks.add(renderBlockPool.get().set(bp.add(0, vOffset, 0)));
                }
            }
        }

        if (horizontalRadius.get() > 1) {

            for (int v = 0; v < verticalRadius.get(); v++) {
                int vOffset;
                if (placementMode.get() == PlaceMode.BelowFeet) {
                    vOffset = -v;
                } else if (placementMode.get() == PlaceMode.AboveHead) {
                    vOffset = v + aboveHeadDistance.get() + 2;
                } else {
                    continue; // Skip the loop iteration if the placement mode is not valid
                }

                for (int h = 1; h < horizontalRadius.get(); h++) {
                    for (int d = -h + 1; d <= h - 1; d++) {
                        //Front
                        if (BlockUtils.place(bp.add(h, vOffset, d), item, rotate.get(), 50, renderSwing.get(), true)) {
                            renderBlocks.add(renderBlockPool.get().set(bp.add(h, vOffset, d)));
                        }
                        //Back
                        if (BlockUtils.place(bp.add(-h, vOffset, d), item, rotate.get(), 50, renderSwing.get(), true)) {
                            renderBlocks.add(renderBlockPool.get().set(bp.add(-h, vOffset, d)));
                        }

                        //Left
                        if (BlockUtils.place(bp.add(d, vOffset, h), item, rotate.get(), 50, renderSwing.get(), true)) {
                            renderBlocks.add(renderBlockPool.get().set(bp.add(d, vOffset, h)));
                        }

                        //Right
                        if (BlockUtils.place(bp.add(d, vOffset, -h), item, rotate.get(), 50, renderSwing.get(), true)) {
                            renderBlocks.add(renderBlockPool.get().set(bp.add(d, vOffset, -h)));
                        }
                    }
                    //Diagonals
                    if (BlockUtils.place(bp.add(h, vOffset, -h), item, rotate.get(), 50, renderSwing.get(), true)) {
                        renderBlocks.add(renderBlockPool.get().set(bp.add(h, vOffset, -h)));
                    }

                    if (BlockUtils.place(bp.add(-h, vOffset, h), item, rotate.get(), 50, renderSwing.get(), true)) {
                        renderBlocks.add(renderBlockPool.get().set(bp.add(-h, vOffset, h)));
                    }

                    if (BlockUtils.place(bp.add(h, vOffset, h), item, rotate.get(), 50, renderSwing.get(), true)) {
                        renderBlocks.add(renderBlockPool.get().set(bp.add(h, vOffset, h)));
                    }

                    if (BlockUtils.place(bp.add(-h, vOffset, -h), item, rotate.get(), 50, renderSwing.get(), true)) {
                        renderBlocks.add(renderBlockPool.get().set(bp.add(-h, vOffset, -h)));
                    }

                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
        renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
    }

    // Rendering

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum PlaceMode {
        BelowFeet,
        AboveHead
    }

    public static class RenderBlock {
        public final BlockPos.Mutable pos = new BlockPos.Mutable();
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