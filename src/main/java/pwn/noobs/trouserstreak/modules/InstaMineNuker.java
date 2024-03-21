package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InstaMineNuker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<Boolean> onlyInstamineable = sgGeneral.add(new BoolSetting.Builder()
            .name("Only break Instamineable blocks")
            .description("Only breaks the Instamineable blocks")
            .defaultValue(false)
            .build()
    );
    private final Setting<List<Block>> skippableBlox = sgGeneral.add(new BlockListSetting.Builder()
            .name("Blocks to Skip")
            .description("Skips instamining this block.")
            .build()
    );
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("The break range.")
            .defaultValue(4)
            .min(0)
            .build()
    );
    private final Setting<Boolean> nobelowfeet = sgGeneral.add(new BoolSetting.Builder()
            .name("Don't break below feet")
            .description("Don't target blocks below feet")
            .defaultValue(false)
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
            .description("Maximum blocks to try to break per tick.")
            .defaultValue(4)
            .min(1)
            .sliderRange(1, 100)
            .build()
    );
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("Swing Hand")
            .description("Do or Do Not swing hand when instamining.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Faces the blocks being mined server side.")
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
            .defaultValue(new SettingColor(255, 0, 0, 10))
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the target block rendering.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
    );
    private Direction direction;
    private int ticks;
    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();


    public InstaMineNuker() {
        super(Trouser.Main, "InstaMineNuker", "Sends packets to break blocks around you.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
            // Broken block
            renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeModeBreak.get()));

    }
    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        direction = event.direction;
    }
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        ticks++;
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        int count = 0;
        int bottomlimit = mc.player.getBlockY() - range.get();
        if (nobelowfeet.get()) bottomlimit = mc.player.getBlockY();
        if (ticks>=delay.get()){
            // Create a list of all the blocks within the specified range
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = mc.player.getBlockX() - range.get(); x <= mc.player.getBlockX() + range.get(); x++) {
                for (int y = bottomlimit; y <= mc.player.getBlockY() + range.get(); y++) {
                    for (int z = mc.player.getBlockZ() - range.get(); z <= mc.player.getBlockZ() + range.get(); z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        double distance = mc.player.getPos().distanceTo(blockPos.toCenterPos());
                        if (distance <= range.get()) blocks.add(blockPos);
                    }
                }
            }

            // Sort the blocks by distance from the player
            blocks.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(mc.player.getPos())));

            for (BlockPos blockPos : blocks) {
                        if (count >= maxBlocksPerTick.get()) break;
                        // Get the block at the current coordinates
                        if (onlyInstamineable.get()){
                            if (!skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock()) && rotate.get() && BlockUtils.canBreak(blockPos) && BlockUtils.canInstaBreak(blockPos)){
                                renderBlocks.add(renderBlockPool.get().set(blockPos));
                                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                                if (swing.get()){
                                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                }
                            }
                            else if (!skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock()) && !rotate.get() && BlockUtils.canBreak(blockPos) && BlockUtils.canInstaBreak(blockPos)){
                                renderBlocks.add(renderBlockPool.get().set(blockPos));
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                                if (swing.get()){
                                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                }
                            }
                            if (!skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock()) && rotate.get() && BlockUtils.canBreak(blockPos) && BlockUtils.canInstaBreak(blockPos)){
                                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                                count++;
                            }
                            else if (!skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock()) && !rotate.get() && BlockUtils.canBreak(blockPos) && BlockUtils.canInstaBreak(blockPos)){
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                                count++;
                            }
                        } else if (!onlyInstamineable.get()) {
                            if (!skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock()) && rotate.get() && BlockUtils.canBreak(blockPos)){
                                renderBlocks.add(renderBlockPool.get().set(blockPos));
                                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                                if (swing.get()){
                                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                }
                            }
                            else if (!skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock()) && !rotate.get() && BlockUtils.canBreak(blockPos)){
                                renderBlocks.add(renderBlockPool.get().set(blockPos));
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                                if (swing.get()){
                                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                }
                            }
                            if (!skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock()) && rotate.get() && BlockUtils.canBreak(blockPos)){
                                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                                count++;
                            }
                            else if (!skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock()) && !rotate.get() && BlockUtils.canBreak(blockPos)){
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                                count++;
                            }


                }
            }
        ticks=0;
        }
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