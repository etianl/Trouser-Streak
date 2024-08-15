//made by etianl with "inspiration" from Meteor Client.
//Thank you to them for a bit of this code https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/modules/render/TunnelESP.java

package pwn.noobs.trouserstreak.modules;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;

public class HoleAndTunnelAndStairsESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgParams = settings.createGroup("Parameters");
    private final SettingGroup sgRender = settings.createGroup("Rendering");
    private final Setting<DetectionMode> detectionMode = sgGeneral.add(new EnumSetting.Builder<DetectionMode>()
            .name("Detection Mode")
            .description("Choose what to detect: holes, tunnels, stairs, or all.")
            .defaultValue(DetectionMode.ALL)
            .build()
    );
    private final Setting<Integer> maxChunks = sgGeneral.add(new IntSetting.Builder()
            .name("Chunks to process/tick")
            .description("Amount of Chunks to process per tick")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 100)
            .build()
    );
    private final Setting<Boolean> airBlocks = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect only Air blocks as passable.")
            .description("Only marks tunnels or holes if their blocks are air as oppose to if the blocks are passable.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> minY = sgParams.add(new IntSetting.Builder()
            .name("Detection Y Minimum OffSet")
            .description("Scans blocks above or at this this many blocks from minimum build limit.")
            .min(0)
            .sliderRange(0,319)
            .defaultValue(0)
            .build()
    );
    private final Setting<Integer> maxY = sgParams.add(new IntSetting.Builder()
            .name("Detection Y Maximum OffSet")
            .description("Scans blocks below or at this this many blocks from maximum build limit.")
            .min(0)
            .sliderRange(0,319)
            .defaultValue(0)
            .build()
    );
    private final Setting<Integer> minHoleDepth = sgParams.add(new IntSetting.Builder()
            .name("Min Hole Depth")
            .description("Minimum depth for a hole to be detected")
            .defaultValue(4)
            .min(1)
            .sliderMax(20)
            .build()
    );
    private final Setting<Integer> minTunnelLength = sgParams.add(new IntSetting.Builder()
            .name("Min Tunnel Length")
            .description("Minimum length for a tunnel to be detected")
            .defaultValue(3)
            .min(1)
            .sliderMax(20)
            .build()
    );
    private final Setting<Integer> minTunnelHeight = sgParams.add(new IntSetting.Builder()
            .name("Min Tunnel Height")
            .description("Minimum height of the tunnels to be detected")
            .defaultValue(2)
            .min(1)
            .sliderMax(5)
            .build()
    );
    private final Setting<Integer> maxTunnelHeight = sgParams.add(new IntSetting.Builder()
            .name("Max Tunnel Height")
            .description("Maximum height of the tunnels to be detected")
            .defaultValue(3)
            .min(2)
            .sliderMax(5)
            .build()
    );
    private final Setting<Integer> minStaircaseLength = sgParams.add(new IntSetting.Builder()
            .name("Min Staircase Length")
            .description("Minimum length for a staircase to be detected")
            .defaultValue(3)
            .min(1)
            .sliderMax(20)
            .build()
    );
    private final Setting<Integer> minStaircaseHeight = sgParams.add(new IntSetting.Builder()
            .name("Min Staircase Height")
            .description("Minimum height of the staircase to be detected")
            .defaultValue(2)
            .min(2)
            .sliderMax(5)
            .build()
    );
    private final Setting<Integer> maxStaircaseHeight = sgParams.add(new IntSetting.Builder()
            .name("Max Staircase Height")
            .description("Maximum height of the staircase to be detected")
            .defaultValue(3)
            .min(2)
            .sliderMax(5)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> holeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("hole-line-color")
            .description("The color of the lines for the holes being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 155))
            .build()
    );
    private final Setting<SettingColor> holeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("hole-side-color")
            .description("The color of the sides for the holes being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 50))
            .build()
    );
    private final Setting<SettingColor> tunnelLineColor = sgRender.add(new ColorSetting.Builder()
            .name("tunnel-line-color")
            .description("The color of the lines for the tunnels being rendered.")
            .defaultValue(new SettingColor(0, 0, 255, 155))
            .build()
    );
    private final Setting<SettingColor> tunnelSideColor = sgRender.add(new ColorSetting.Builder()
            .name("tunnel-side-color")
            .description("The color of the sides for the tunnels being rendered.")
            .defaultValue(new SettingColor(0, 0, 255, 50))
            .build()
    );
    private final Setting<SettingColor> staircaseLineColor = sgRender.add(new ColorSetting.Builder()
            .name("staircase-line-color")
            .description("The color of the lines for the staircases being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 155))
            .build()
    );
    private final Setting<SettingColor> staircaseSideColor = sgRender.add(new ColorSetting.Builder()
            .name("staircase-side-color")
            .description("The color of the sides for the staircases being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 50))
            .build()
    );

    private static final Direction[] DIRECTIONS = { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH };
    private final Long2ObjectMap<TChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final Queue<Chunk> chunkQueue = new LinkedList<>();

    public HoleAndTunnelAndStairsESP() {
        super(Trouser.Main, "Hole/Tunnel/StairsESP", "Finds and highlights holes and tunnels and stairs.");
    }

    @Override
    public void onDeactivate() {
        chunks.clear();
        chunkQueue.clear();
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        synchronized (chunks) {
            for (TChunk tChunk : chunks.values()) tChunk.marked = false;

            for (Chunk chunk : Utils.chunks(true)) {
                long key = ChunkPos.toLong(chunk.getPos().x, chunk.getPos().z);

                if (chunks.containsKey(key)) chunks.get(key).marked = true;
                else if (!chunkQueue.contains(chunk)) {
                    chunkQueue.add(chunk);
                }
            }

            processChunkQueue();
            chunks.values().removeIf(tChunk -> !tChunk.marked);
        }
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        synchronized (chunks) {
            for (TChunk chunk : chunks.values()) chunk.render(event.renderer);
        }
    }
    private void processChunkQueue() {
        int maxChunksPerTick = maxChunks.get();  // Adjust this value based on performance needs
        int processed = 0;

        while (!chunkQueue.isEmpty() && processed < maxChunksPerTick) {
            Chunk chunk = chunkQueue.poll();
            if (chunk != null) {
                TChunk tChunk = new TChunk(chunk.getPos().x, chunk.getPos().z);
                chunks.put(tChunk.getKey(), tChunk);

                MeteorExecutor.execute(() -> searchChunk(chunk, tChunk));
                processed++;
            }
        }
    }
    private void searchChunk(Chunk chunk, TChunk tChunk) {
        Set<Box> holes = new HashSet<>();
        Set<Box> tunnels = new HashSet<>();
        Set<Box> staircases = new HashSet<>();
        var sections = chunk.getSectionArray();
        int Ymin = mc.world.getBottomY() + minY.get();
        int Ymax = mc.world.getTopY() - maxY.get();
        int Y = mc.world.getBottomY();
        for (ChunkSection section : sections) {
            if (section != null && !section.isEmpty()) {
                for (int z = 0; z <= 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            int currentY = Y + y;
                            if (currentY <= Ymin || currentY >= Ymax) continue;
                            BlockPos pos = chunk.getPos().getBlockPos(x, currentY, z);
                            if (isPassableBlock(pos)) {
                                switch (detectionMode.get()) {
                                    case ALL:
                                        checkHole(pos, holes);
                                        checkTunnel(chunk, pos, tunnels);
                                        checkStaircase(chunk, pos, staircases);
                                        break;
                                    case HOLES_AND_TUNNELS:
                                        checkHole(pos, holes);
                                        checkTunnel(chunk, pos, tunnels);
                                        break;
                                    case HOLES_AND_STAIRCASES:
                                        checkHole(pos, holes);
                                        checkStaircase(chunk, pos, staircases);
                                        break;
                                    case TUNNELS_AND_STAIRCASES:
                                        checkTunnel(chunk, pos, tunnels);
                                        checkStaircase(chunk, pos, staircases);
                                        break;
                                    case HOLES:
                                        checkHole(pos, holes);
                                        break;
                                    case TUNNELS:
                                        checkTunnel(chunk, pos, tunnels);
                                        break;
                                    case STAIRCASES:
                                        checkStaircase(chunk, pos, staircases);
                                        break;
                                }
                            }
                        }
                    }
                }
            }
            Y += 16;
        }

        tChunk.holes = holes;
        tChunk.tunnels = tunnels;
        tChunk.staircases = staircases;
    }
    private void checkTunnel(Chunk chunk, BlockPos pos, Set<Box> tunnels) {
        for (Direction dir : DIRECTIONS) {
            BlockPos.Mutable currentPos = pos.mutableCopy();

            int maxHeight = 0;
            while (isTunnelSection(currentPos, dir)) {
                currentPos.move(dir);
                maxHeight = Math.max(maxHeight, getTunnelHeight(currentPos));

                if (!chunk.getPos().equals(mc.world.getChunk(currentPos).getPos())) {
                    break;
                }
            }

            int length = switch (dir.getAxis()) {
                case X -> Math.abs(currentPos.getX() - pos.getX());
                case Z -> Math.abs(currentPos.getZ() - pos.getZ());
                default -> 0;
            };

            if (length >= minTunnelLength.get() && maxHeight >= minTunnelHeight.get() && maxHeight <= maxTunnelHeight.get()) {
                currentPos.move(dir.getOpposite());

                Box tunnelBox = new Box(
                        Math.min(pos.getX(), currentPos.getX()),
                        pos.getY(),
                        Math.min(pos.getZ(), currentPos.getZ()),
                        Math.max(pos.getX(), currentPos.getX()) + 1,
                        pos.getY() + maxHeight,
                        Math.max(pos.getZ(), currentPos.getZ()) + 1
                );
                if (tunnels.stream().noneMatch(existingTunnel -> existingTunnel.intersects(tunnelBox))) {
                    tunnels.add(tunnelBox);
                }
            }
        }
    }

    private int getTunnelHeight(BlockPos pos) {
        int height = 0;
        while (isPassableBlock(pos.up(height)) && height < maxTunnelHeight.get()) {
            height++;
        }
        return height;
    }

    private boolean isTunnelSection(BlockPos pos, Direction dir) {
        int height = getTunnelHeight(pos);
        if (height < minTunnelHeight.get() || height > maxTunnelHeight.get()) return false;
        if (isPassableBlock(pos.down()) || isPassableBlock(pos.up(height))) return false;
        Direction[] perpDirs = dir.getAxis() == Direction.Axis.X ? new Direction[]{Direction.NORTH, Direction.SOUTH} : new Direction[]{Direction.EAST, Direction.WEST};
        for (Direction perpDir : perpDirs) {
            for (int i = 0; i < height; i++) {
                if (isPassableBlock(pos.up(i).offset(perpDir))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkHole(BlockPos pos, Set<Box> holes) {
        if (isValidHoleSection(pos)) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            while (isValidHoleSection(currentPos)) {
                currentPos.move(Direction.UP);
            }
            if (currentPos.getY()-pos.getY() >= minHoleDepth.get()) {
                Box holeBox = new Box(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, currentPos.getY(), pos.getZ() + 1
                );
                if (holes.stream().noneMatch(existingHole -> existingHole.intersects(holeBox))) {
                    holes.add(holeBox);
                }
            }
        }
    }
    private boolean isValidHoleSection(BlockPos pos) {
        return isPassableBlock(pos) && !isPassableBlock(pos.north()) && !isPassableBlock(pos.south()) && !isPassableBlock(pos.east()) && !isPassableBlock(pos.west());
    }
    private void checkStaircase(Chunk chunk, BlockPos pos, Set<Box> staircases) {
        for (Direction dir : DIRECTIONS) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            int stepCount = 0;
            List<Box> potentialStaircaseBoxes = new ArrayList<>();

            while (isStaircaseSection(currentPos, dir)) {
                int height = getStaircaseHeight(currentPos);
                Box stairsBox = new Box(
                        currentPos.getX(),
                        currentPos.getY(),
                        currentPos.getZ(),
                        currentPos.getX() + 1,
                        currentPos.getY() + height,
                        currentPos.getZ() + 1
                );
                potentialStaircaseBoxes.add(stairsBox);

                currentPos.move(dir);
                currentPos.move(Direction.UP);
                stepCount++;

                if (!chunk.getPos().equals(mc.world.getChunk(currentPos).getPos())) {
                    break;
                }
            }

            if (stepCount >= minStaircaseLength.get()) {
                staircases.addAll(potentialStaircaseBoxes);
            }
        }
    }

    private int getStaircaseHeight(BlockPos pos) {
        int height = 0;
        while (isPassableBlock(pos.up(height)) && height < maxStaircaseHeight.get()) {
            height++;
        }
        return height;
    }

    private boolean isStaircaseSection(BlockPos pos, Direction dir) {
        int height = getStaircaseHeight(pos);
        if (height < minStaircaseHeight.get() || height > maxStaircaseHeight.get()) return false;
        if (isPassableBlock(pos.down()) || isPassableBlock(pos.up(height))) return false;
        Direction[] perpDirs = dir.getAxis() == Direction.Axis.X ? new Direction[]{Direction.NORTH, Direction.SOUTH} : new Direction[]{Direction.EAST, Direction.WEST};
        for (Direction perpDir : perpDirs) {
            for (int i = 0; i < height; i++) {
                if (isPassableBlock(pos.up(i).offset(perpDir))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPassableBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (airBlocks.get()) {
            return state.isAir();
        } else {
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            return shape.isEmpty() || !VoxelShapes.fullCube().equals(shape);
        }
    }

    public enum DetectionMode {
        ALL,
        HOLES_AND_TUNNELS,
        HOLES_AND_STAIRCASES,
        TUNNELS_AND_STAIRCASES,
        HOLES,
        TUNNELS,
        STAIRCASES
    }

    private class TChunk {
        private final int x, z;
        public Set<Box> holes;
        public Set<Box> tunnels;
        public Set<Box> staircases;
        public boolean marked;

        public TChunk(int x, int z) {
            this.x = x;
            this.z = z;
            this.marked = true;
        }
        public void render(Renderer3D renderer) {
            switch (detectionMode.get()) {
                case ALL:
                    renderHoles(renderer);
                    renderTunnels(renderer);
                    renderStaircases(renderer);
                    break;
                case HOLES_AND_TUNNELS:
                    renderHoles(renderer);
                    renderTunnels(renderer);
                    break;
                case HOLES_AND_STAIRCASES:
                    renderHoles(renderer);
                    renderStaircases(renderer);
                    break;
                case TUNNELS_AND_STAIRCASES:
                    renderTunnels(renderer);
                    renderStaircases(renderer);
                    break;
                case HOLES:
                    renderHoles(renderer);
                    break;
                case TUNNELS:
                    renderTunnels(renderer);
                    break;
                case STAIRCASES:
                    renderStaircases(renderer);
                    break;
            }
        }
        private void renderHoles(Renderer3D renderer) {
            if (holes != null) {
                for (Box box : holes) {
                    renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, holeSideColor.get(), holeLineColor.get(), shapeMode.get(), 0);
                }
            }
        }
        private void renderTunnels(Renderer3D renderer) {
            if (tunnels != null) {
                for (Box box : tunnels) {
                    renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, tunnelSideColor.get(), tunnelLineColor.get(), shapeMode.get(), 0);
                }
            }
        }
        private void renderStaircases(Renderer3D renderer) {
            if (staircases != null) {
                for (Box box : staircases) {
                    renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, staircaseSideColor.get(), staircaseLineColor.get(), shapeMode.get(), 0);
                }
            }
        }
        public long getKey() {
            return ChunkPos.toLong(x, z);
        }
    }
}
