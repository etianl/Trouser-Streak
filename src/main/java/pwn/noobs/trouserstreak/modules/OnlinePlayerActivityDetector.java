//made by etianl :D
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.AcknowledgeChunksC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.*;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;
import java.util.concurrent.*;

public class OnlinePlayerActivityDetector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpecial = settings.createGroup("There may be false positive detection near NewChunks.");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> removerenderdist = sgGeneral.add(new BoolSetting.Builder()
            .name("RemoveOutsideRenderDistance")
            .description("Removes the cached chunks when they leave the defined render distance.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> detectOres = sgSpecial.add(new BoolSetting.Builder()
            .name("Detect Removed Ore Blocks")
            .description("Detects ores that were mined at the cost of some false positives near New Chunks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<Block>> Blawcks = sgSpecial.add(new BlockListSetting.Builder()
            .name("OVERWORLD False Positive blocks")
            .description("Exclude these blocks from the detection.")
            .defaultValue(
                    Blocks.BEDROCK, Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SNOW_BLOCK, Blocks.BLUE_ICE, Blocks.SAND, Blocks.GRAVEL, Blocks.SUSPICIOUS_GRAVEL,
                    Blocks.DIORITE, Blocks.GRANITE, Blocks.ANDESITE, Blocks.TUFF, Blocks.DEEPSLATE, Blocks.STONE,
                    Blocks.RAW_IRON_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD,
                    Blocks.WAXED_WEATHERED_COPPER_BULB, Blocks.WAXED_OXIDIZED_COPPER_BULB, Blocks.WAXED_COPPER_BLOCK,
                    Blocks.COBWEB, Blocks.OAK_FENCE, Blocks.DARK_OAK_FENCE, Blocks.RAIL, Blocks.SCULK_VEIN, Blocks.SCULK_SENSOR,
                    Blocks.ACACIA_LEAVES, Blocks.BIRCH_LEAVES, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.CHERRY_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.CACTUS,
                    Blocks.CAVE_VINES, Blocks.SUGAR_CANE, Blocks.TALL_GRASS, Blocks.SHORT_GRASS, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS, Blocks.VINE, Blocks.FERN, Blocks.LARGE_FERN, Blocks.KELP,
                    Blocks.MOSS_BLOCK, Blocks.BIG_DRIPLEAF, Blocks.BIG_DRIPLEAF_STEM, Blocks.GLOW_LICHEN,
                    Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.FIRE, Blocks.CAVE_AIR, Blocks.BARRIER, Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.BUBBLE_COLUMN
            )
            .build()
    );
    private final Setting<List<Block>> Blawcks2 = sgSpecial.add(new BlockListSetting.Builder()
            .name("NETHER False Positive blocks")
            .description("Exclude these blocks from the detection.")
            .defaultValue(
                    Blocks.BARRIER, Blocks.AIR, Blocks.CAVE_AIR, Blocks.LAVA, Blocks.FIRE, Blocks.NETHERRACK, Blocks.MAGMA_BLOCK, Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks. NETHER_BRICK_FENCE,
                    Blocks.CRIMSON_NYLIUM, Blocks.CRIMSON_ROOTS, Blocks.WEEPING_VINES_PLANT, Blocks.WEEPING_VINES, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS
            )
            .build()
    );
    private final Setting<List<Block>> Blawcks3 = sgSpecial.add(new BlockListSetting.Builder()
            .name("END False Positive blocks")
            .description("Exclude these blocks from the detection.")
            .defaultValue(
                    Blocks.AIR, Blocks.BARRIER, Blocks.CHORUS_PLANT, Blocks.PURPUR_STAIRS
            )
            .build()
    );
    public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
            .name("Render-Distance(Chunks)")
            .description("How many chunks from the character to render the detected chunks.")
            .defaultValue(32)
            .min(6)
            .sliderRange(6,1024)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> playerChunksSideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("Color of the chunks.")
            .defaultValue(new SettingColor(255, 0, 255, 30))
            .visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> playerChunksLineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Color of the chunks.")
            .defaultValue(new SettingColor(255, 0, 255, 235))
            .visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private static final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private final Set<BlockPos> playerActivityPositions = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Block> FalsePositivesOVERWORLD = new HashSet<>();
    private static final Set<Block> FalsePositivesNETHER = new HashSet<>();
    private static final Set<Block> FalsePositivesEND = new HashSet<>();
    private static final Set<Block> ORE_BLOCKS = new HashSet<>();
    static {
        ORE_BLOCKS.add(Blocks.COAL_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_COAL_ORE);
        ORE_BLOCKS.add(Blocks.COPPER_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_COPPER_ORE);
        ORE_BLOCKS.add(Blocks.IRON_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_IRON_ORE);
        ORE_BLOCKS.add(Blocks.GOLD_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_GOLD_ORE);
        ORE_BLOCKS.add(Blocks.LAPIS_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_LAPIS_ORE);
        ORE_BLOCKS.add(Blocks.DIAMOND_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        ORE_BLOCKS.add(Blocks.REDSTONE_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        ORE_BLOCKS.add(Blocks.EMERALD_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_EMERALD_ORE);
    }
    private static final Set<Block> NETHER_ORE_BLOCKS = new HashSet<>();
    static {
        ORE_BLOCKS.add(Blocks.NETHER_GOLD_ORE);
        ORE_BLOCKS.add(Blocks.NETHER_QUARTZ_ORE);
        ORE_BLOCKS.add(Blocks.GILDED_BLACKSTONE);
    }
    public OnlinePlayerActivityDetector() {
        super(Trouser.baseHunting,"OnlinePlayerActivityDetector", "Detects if an online player is still nearby if there are blocks missing from a BlockState palette and your render distances are overlapping.");
    }
    @Override
    public void onActivate() {
        playerActivityPositions.clear();
        FalsePositivesOVERWORLD.clear();
        FalsePositivesNETHER.clear();
        FalsePositivesEND.clear();
        if (Blawcks.get() != null) {
            FalsePositivesOVERWORLD.addAll(Blawcks.get());
        }
        if (Blawcks2.get() != null) {
            FalsePositivesNETHER.addAll(Blawcks2.get());
        }
        if (Blawcks3.get() != null) {
            FalsePositivesEND.addAll(Blawcks3.get());
        }
    }
    @Override
    public void onDeactivate() {
        playerActivityPositions.clear();
        FalsePositivesOVERWORLD.clear();
        FalsePositivesNETHER.clear();
        FalsePositivesEND.clear();
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        FalsePositivesOVERWORLD.clear();
        FalsePositivesNETHER.clear();
        FalsePositivesEND.clear();
        if (Blawcks.get() != null) {
            FalsePositivesOVERWORLD.addAll(Blawcks.get());
        }
        if (Blawcks2.get() != null) {
            FalsePositivesNETHER.addAll(Blawcks2.get());
        }
        if (Blawcks3.get() != null) {
            FalsePositivesEND.addAll(Blawcks3.get());
        }
        if (removerenderdist.get())removeChunksOutsideRenderDistance();
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if ((playerChunksLineColor.get().a > 5 || playerChunksSideColor.get().a > 5) && mc.player != null) {
            synchronized (playerActivityPositions) {
                for (BlockPos pos : playerActivityPositions) {
                    BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
                    if (pos != null && playerPos.isWithinDistance(pos, renderDistance.get() * 16)) {
                        int startX = pos.getX() - 8;
                        int startY = pos.getY() - 8;
                        int startZ = pos.getZ() - 8;
                        int endX = pos.getX() + 8;
                        int endY = pos.getY() + 8;
                        int endZ = pos.getZ() + 8;

                        render(new Box(new Vec3d(startX, startY, startZ), new Vec3d(endX, endY, endZ)), playerChunksSideColor.get(), playerChunksLineColor.get(), shapeMode.get(), event);
                    }
                }
            }
        }
    }

    private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof AcknowledgeChunksC2SPacket)return; //for some reason this packet keeps getting cast to other packets
        if (!(event.packet instanceof AcknowledgeChunksC2SPacket) && !(event.packet instanceof PlayerMoveC2SPacket) && event.packet instanceof ChunkDataS2CPacket packet && mc.world != null) {
            ChunkPos playerActivityPos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());

            if (mc.world.getChunkManager().getChunk(packet.getChunkX(), packet.getChunkZ()) == null) {
                WorldChunk chunk = new WorldChunk(mc.world, playerActivityPos);
                try {
                    Map<Heightmap.Type, long[]> heightmaps = new EnumMap<>(Heightmap.Type.class);

                    Heightmap.Type type = Heightmap.Type.MOTION_BLOCKING;
                    long[] emptyHeightmapData = new long[37];
                    heightmaps.put(type, emptyHeightmapData);

                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), heightmaps,
                                packet.getChunkData().getBlockEntities(packet.getChunkX(), packet.getChunkZ()));
                    }, taskExecutor);
                    future.join();
                } catch (CompletionException e) {}

                ChunkSection[] sections = chunk.getSectionArray();

                try {
                    int Y=mc.world.getBottomY();
                    int i=0;
                    boolean firstsectionappearsnew = false;
                    for (ChunkSection section : sections) {
                        var blockStatesContainer = section.getBlockStateContainer();

                        Palette<BlockState> blockStatePalette = blockStatesContainer.data.palette();
                        if (!(blockStatePalette instanceof ArrayPalette<BlockState>))return;

                        int blockPaletteLength = blockStatePalette.getSize();
                        for (int i2 = 0; i2 < blockPaletteLength; i2++) {
                            BlockState blockPaletteEntry = blockStatePalette.get(i2);
                            if (i2 == 0 && i == 0 && blockPaletteEntry.getBlock() == Blocks.AIR && mc.world.getRegistryKey() != World.END){
                                firstsectionappearsnew = true;
                                break;
                            }
                        }
                        if (firstsectionappearsnew) break;
                        Set<BlockState> bstates = new HashSet<>();
                        for (int x = 0; x < 16; x++) {
                            for (int y = 0; y < 16; y++) {
                                for (int z = 0; z < 16; z++) {
                                    bstates.add(blockStatesContainer.get(x, y, z));
                                }
                            }
                        }

                        int bstatesSize = bstates.size();
                        if (bstatesSize <= 1) bstatesSize = blockPaletteLength;
                        if (bstatesSize < blockPaletteLength && !firstsectionappearsnew) {
                            Set<BlockState> missingBlocks = new HashSet<>();
                            for (int i2 = 0; i2 < blockPaletteLength; i2++) {
                                BlockState blockPaletteEntry = blockStatePalette.get(i2);
                                if (!bstates.contains(blockPaletteEntry)) {
                                    missingBlocks.add(blockPaletteEntry);
                                }
                            }
                            boolean falsepositive = false;
                            boolean missingAblock = false;
                            Set<BlockState> detectedBlocks = new HashSet<>();

                            if (!missingBlocks.isEmpty()) {
                                for (BlockState missingBlock : missingBlocks) {
                                    if (mc.world.getRegistryKey() == World.OVERWORLD) {
                                        if (FalsePositivesOVERWORLD.contains(missingBlock.getBlock())) falsepositive = true;
                                        if (!detectOres.get() && ORE_BLOCKS.contains(missingBlock.getBlock())) falsepositive = true;
                                        if (!falsepositive && !detectOres.get() && !FalsePositivesOVERWORLD.contains(missingBlock.getBlock())) {
                                            detectedBlocks.add(missingBlock);
                                            missingAblock = true;
                                        }
                                        else if (!falsepositive && detectOres.get() && !ORE_BLOCKS.contains(missingBlock.getBlock()) && !FalsePositivesOVERWORLD.contains(missingBlock.getBlock())) {
                                            detectedBlocks.add(missingBlock);
                                            missingAblock = true;
                                        }
                                    } else if (mc.world.getRegistryKey() == World.NETHER) {
                                        if (FalsePositivesNETHER.contains(missingBlock.getBlock())) falsepositive = true;
                                        if (!detectOres.get() && NETHER_ORE_BLOCKS.contains(missingBlock.getBlock())) falsepositive = true;
                                        if (!falsepositive && !detectOres.get() && !FalsePositivesNETHER.contains(missingBlock.getBlock())) {
                                            detectedBlocks.add(missingBlock);
                                            missingAblock = true;
                                        }
                                        else if (!falsepositive && detectOres.get() && !NETHER_ORE_BLOCKS.contains(missingBlock.getBlock()) && !FalsePositivesNETHER.contains(missingBlock.getBlock())) {
                                            detectedBlocks.add(missingBlock);
                                            missingAblock = true;
                                        }
                                    } else if (mc.world.getRegistryKey() == World.END) {
                                        if (FalsePositivesEND.contains(missingBlock.getBlock())) falsepositive = true;
                                        if (!falsepositive && !FalsePositivesEND.contains(missingBlock.getBlock())) {
                                            detectedBlocks.add(missingBlock);
                                            missingAblock = true;
                                        }
                                    }
                                }
                            }

                            if (!playerActivityPositions.contains(playerActivityPos) && !falsepositive && missingAblock) {
                                for (BlockState state : detectedBlocks) {
                                    ChatUtils.sendMsg(Text.of("Missing block in Section " + i + ": " + state.getBlock()));
                                }
                                ChatUtils.sendMsg(Text.of("Detected Player Activity. X: " + playerActivityPos.getCenterX() + " Y: " + (Y+8) + " Z: " + playerActivityPos.getCenterZ()));
                                playerActivityPositions.add(new BlockPos(playerActivityPos.getCenterX(), Y+8, playerActivityPos.getCenterZ()));
                            }
                        }
                        i++;
                        Y+=16;
                    }
                } catch (Exception e) {}
            }
        }
    }
    private void removeChunksOutsideRenderDistance() {
        double renderDistanceBlocks = renderDistance.get() * 16;

        removeChunksOutsideRenderDistance(playerActivityPositions, renderDistanceBlocks);
    }
    private void removeChunksOutsideRenderDistance(Set<BlockPos> chunkSet, double renderDistanceBlocks) {
        chunkSet.removeIf(blockPos -> {
            BlockPos playerPos = new BlockPos(mc.player.getBlockX(), blockPos.getY(), mc.player.getBlockZ());
            return !playerPos.isWithinDistance(blockPos, renderDistanceBlocks);
        });
    }
}