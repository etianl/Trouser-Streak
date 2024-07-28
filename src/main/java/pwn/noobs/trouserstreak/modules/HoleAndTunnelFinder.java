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
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import pwn.noobs.trouserstreak.Trouser;

import java.util.HashSet;
import java.util.Set;

import java.util.Queue;
import java.util.LinkedList;

public class HoleAndTunnelFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgParams = settings.createGroup("Parameters");
    private final SettingGroup sgRender = settings.createGroup("Rendering");
    private final Setting<DetectionMode> detectionMode = sgGeneral.add(new EnumSetting.Builder<DetectionMode>()
            .name("Detection Mode")
            .description("Choose what to detect: holes, tunnels, or both.")
            .defaultValue(DetectionMode.HOLES_AND_TUNNELS)
            .build()
    );
    private final Setting<Integer> maxChunks = sgGeneral.add(new IntSetting.Builder()
            .name("Chunks to process/tick")
            .description("Amount of Chunks to process per tick")
            .defaultValue(10)
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
    private final Setting<Integer> minYLevel = sgParams.add(new IntSetting.Builder()
            .name("Min Y Level")
            .description("Minimum Y level to scan")
            .defaultValue(-64)
            .min(-64)
            .sliderRange(-64, 319)
            .build()
    );

    private final Setting<Integer> maxYLevel = sgParams.add(new IntSetting.Builder()
            .name("Max Y Level")
            .description("Maximum Y level to scan")
            .defaultValue(256)
            .min(-64)
            .sliderRange(-64, 319)
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

    private final Setting<Integer> tunnelHeight = sgParams.add(new IntSetting.Builder()
            .name("Tunnel Height")
            .description("Height of the tunnels to be detected")
            .defaultValue(2)
            .min(1)
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
            .defaultValue(new SettingColor(255, 0, 0, 155)) // Opaque red
            .build()
    );

    private final Setting<SettingColor> holeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("hole-side-color")
            .description("The color of the sides for the holes being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 50)) // Semi-transparent red
            .build()
    );

    private final Setting<SettingColor> tunnelLineColor = sgRender.add(new ColorSetting.Builder()
            .name("tunnel-line-color")
            .description("The color of the lines for the tunnels being rendered.")
            .defaultValue(new SettingColor(0, 0, 255, 155)) // Opaque blue
            .build()
    );

    private final Setting<SettingColor> tunnelSideColor = sgRender.add(new ColorSetting.Builder()
            .name("tunnel-side-color")
            .description("The color of the sides for the tunnels being rendered.")
            .defaultValue(new SettingColor(0, 0, 255, 50)) // Semi-transparent blue
            .build()
    );
    private static final Direction[] DIRECTIONS = { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH };
    private final Long2ObjectMap<TChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final Queue<Chunk> chunkQueue = new LinkedList<>();

    public HoleAndTunnelFinder() {
        super(Trouser.Main, "Hole/TunnelFinder", "Finds and highlights holes and/or tunnels.");
    }

    @Override
    public void onDeactivate() {
        chunks.clear();
        chunkQueue.clear();
    }

    private void searchChunk(Chunk chunk, TChunk tChunk) {
        Set<Box> holes = new HashSet<>();
        Set<Box> tunnels = new HashSet<>();

        int minSectionY = Math.max(chunk.getSectionIndex(minYLevel.get()), 0);
        int maxSectionY = Math.min(chunk.getSectionIndex(maxYLevel.get()), chunk.getSectionArray().length - 1);

        for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
            ChunkSection section = chunk.getSectionArray()[sectionY];
            if (section == null || section.isEmpty()) continue;

            int startY = Math.max(sectionY * 16, minYLevel.get());
            int endY = Math.min((sectionY + 1) * 16 - 1, maxYLevel.get());

            for (int y = startY; y <= endY; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = chunk.getPos().getBlockPos(x, y, z);
                        if (isPassableBlock(pos)) {
                            if (detectionMode.get() == DetectionMode.HOLES || detectionMode.get() == DetectionMode.HOLES_AND_TUNNELS) {
                                checkHole(chunk, pos, holes);
                            }
                            if (detectionMode.get() == DetectionMode.TUNNELS || detectionMode.get() == DetectionMode.HOLES_AND_TUNNELS) {
                                checkTunnel(chunk, pos, tunnels);
                            }
                        }
                    }
                }
            }
        }

        tChunk.holes = holes;
        tChunk.tunnels = tunnels;
    }

    private boolean isTunnelSection(BlockPos pos, Direction dir) {
        int height = tunnelHeight.get();
        for (int i = 0; i < height; i++) {
            if (!isPassableBlock(pos.up(i))) return false;
        }

        // Check for solid blocks above and below
        if (isPassableBlock(pos.down()) || isPassableBlock(pos.up(height))) return false;

        // Check for solid blocks on the sides perpendicular to the tunnel direction
        Direction[] perpDirs = dir.getAxis() == Direction.Axis.X ?
                new Direction[]{Direction.NORTH, Direction.SOUTH} :
                new Direction[]{Direction.EAST, Direction.WEST};

        for (Direction perpDir : perpDirs) {
            for (int i = 0; i < height; i++) {
                if (isPassableBlock(pos.up(i).offset(perpDir))) {
                    return false;
                }
            }
        }

        return true;
    }

    private void checkTunnel(Chunk chunk, BlockPos pos, Set<Box> tunnels) {
        for (Direction dir : DIRECTIONS) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            int length = 0;

            while (isTunnelSection(currentPos, dir)) {
                length++;
                currentPos.move(dir);

                // Check if the next position is out of the current chunk boundaries
                if (!chunk.getPos().equals(mc.world.getChunk(currentPos).getPos())) {
                    break;
                }
            }

            if (length >= minTunnelLength.get()) {
                BlockPos endPos = pos.offset(dir, length - 1);
                Box tunnelBox = new Box(
                        Math.min(pos.getX(), endPos.getX()),
                        Math.max(pos.getY(), minYLevel.get()),
                        Math.min(pos.getZ(), endPos.getZ()),
                        Math.max(pos.getX(), endPos.getX()) + 1,
                        Math.min(pos.getY() + tunnelHeight.get(), maxYLevel.get()),
                        Math.max(pos.getZ(), endPos.getZ()) + 1
                );

                // Check if this tunnel overlaps with any existing tunnel
                if (tunnels.stream().noneMatch(existingTunnel -> existingTunnel.intersects(tunnelBox))) {
                    tunnels.add(tunnelBox);
                }
                return;  // Only add one tunnel per starting position
            }
        }
    }

    private void checkHole(Chunk chunk, BlockPos pos, Set<Box> holes) {
        if (isValidHoleSection(pos) && !isValidHoleSection(pos.up())) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            int depth = 0;
            while (isValidHoleSection(currentPos) && currentPos.getY() >= minYLevel.get()) {
                depth++;
                currentPos.move(Direction.DOWN);

                // Check if the next position is out of the current chunk boundaries
                if (!chunk.getPos().equals(mc.world.getChunk(currentPos).getPos())) {
                    break;
                }
            }
            if (depth >= minHoleDepth.get()) {
                Box holeBox = new Box(
                        pos.getX(), Math.max(pos.getY() - depth + 1, minYLevel.get()), pos.getZ(),
                        pos.getX() + 1, Math.min(pos.getY() + 1, maxYLevel.get()), pos.getZ() + 1
                );
                if (holes.stream().noneMatch(existingHole -> existingHole.intersects(holeBox))) {
                    holes.add(holeBox);
                }
            }
        }
    }

    private boolean isValidHoleSection(BlockPos pos) {
        return isPassableBlock(pos) &&
                !isPassableBlock(pos.north()) &&
                !isPassableBlock(pos.south()) &&
                !isPassableBlock(pos.east()) &&
                !isPassableBlock(pos.west());
    }

    private boolean isPassableBlock(BlockPos pos) {
        Chunk chunk = mc.world.getChunk(pos);
        BlockState state = chunk.getBlockState(pos);
        if (airBlocks.get()) return state.isAir();
        else return state.getCollisionShape(mc.world, pos).isEmpty() || state.getFluidState().isStill();
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

    private void processChunkQueue() {
        int maxChunksPerTick = maxChunks.get();  // Adjust this value based on performance needs
        int processed = 0;

        while (!chunkQueue.isEmpty() && processed < maxChunksPerTick) {
            Chunk chunk = chunkQueue.poll();
            if (chunk != null) {
                long key = ChunkPos.toLong(chunk.getPos().x, chunk.getPos().z);
                TChunk tChunk = new TChunk(chunk.getPos().x, chunk.getPos().z);
                chunks.put(tChunk.getKey(), tChunk);

                MeteorExecutor.execute(() -> searchChunk(chunk, tChunk));
                processed++;
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        synchronized (chunks) {
            for (TChunk chunk : chunks.values()) chunk.render(event.renderer);
        }
    }

    private class TChunk {
        private final int x, z;
        public Set<Box> holes;
        public Set<Box> tunnels;
        public boolean marked;

        public TChunk(int x, int z) {
            this.x = x;
            this.z = z;
            this.marked = true;
        }

        public void render(Renderer3D renderer) {
            if ((detectionMode.get() == DetectionMode.HOLES || detectionMode.get() == DetectionMode.HOLES_AND_TUNNELS) && holes != null) {
                for (Box box : holes) {
                    renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, holeSideColor.get(), holeLineColor.get(), shapeMode.get(), 0);
                }
            }
            if ((detectionMode.get() == DetectionMode.TUNNELS || detectionMode.get() == DetectionMode.HOLES_AND_TUNNELS) && tunnels != null) {
                for (Box box : tunnels) {
                    renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, tunnelSideColor.get(), tunnelLineColor.get(), shapeMode.get(), 0);
                }
            }
        }

        public long getKey() {
            return ChunkPos.toLong(x, z);
        }
    }

    public enum DetectionMode {
        HOLES_AND_TUNNELS,
        HOLES,
        TUNNELS
    }
}