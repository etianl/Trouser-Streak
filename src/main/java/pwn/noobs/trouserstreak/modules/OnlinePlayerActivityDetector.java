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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LinearPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bedrock")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("grass_block")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dirt")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("snow_block")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blue_ice")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sand")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gravel")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("suspicious_gravel")),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diorite")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("granite")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("andesite")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tuff")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stone")),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("raw_iron_block")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("budding_amethyst")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("small_amethyst_bud")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("medium_amethyst_bud")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("large_amethyst_bud")),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_weathered_copper_bulb")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_oxidized_copper_bulb")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_copper_block")),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cobweb")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("oak_fence")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dark_oak_fence")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("rail")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sculk_vein")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sculk_sensor")),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("acacia_leaves")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("birch_leaves")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("oak_leaves")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("spruce_leaves")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dark_oak_leaves")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cherry_leaves")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("jungle_leaves")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cactus")),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cave_vines")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sugar_cane")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tall_grass")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("short_grass")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("seagrass")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tall_seagrass")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("vine")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("fern")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("large_fern")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("kelp")),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("moss_block")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("big_dripleaf")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("big_dripleaf_stem")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("glow_lichen")),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brown_mushroom")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_mushroom")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("fire")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cave_air")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("barrier")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("air")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("water")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lava")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bubble_column"))
            )
            .build()
    );
    private final Setting<List<Block>> Blawcks2 = sgSpecial.add(new BlockListSetting.Builder()
            .name("NETHER False Positive blocks")
            .description("Exclude these blocks from the detection.")
            .defaultValue(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("barrier")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("air")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cave_air")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lava")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("fire")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("netherrack")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("magma_block")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("soul_sand")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("soul_soil")), Blocks. NETHER_BRICK_FENCE,
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crimson_nylium")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crimson_roots")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("weeping_vines_plant")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("weeping_vines")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_mushroom")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brown_mushroom")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crimson_fungus")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("warped_fungus"))
            )
            .build()
    );
    private final Setting<List<Block>> Blawcks3 = sgSpecial.add(new BlockListSetting.Builder()
            .name("END False Positive blocks")
            .description("Exclude these blocks from the detection.")
            .defaultValue(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("air")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("barrier")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chorus_plant")), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("purpur_stairs"))
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
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("coal_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_coal_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_copper_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_iron_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gold_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_gold_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lapis_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_lapis_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_diamond_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("redstone_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_redstone_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("emerald_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_emerald_ore")));
    }
    private static final Set<Block> NETHER_ORE_BLOCKS = new HashSet<>();
    static {
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("nether_gold_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("nether_quartz_ore")));
        ORE_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gilded_blackstone")));
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
                    if (pos != null && playerPos.closerThan(pos, renderDistance.get() * 16)) {
                        int startX = pos.getX() - 8;
                        int startY = pos.getY() - 8;
                        int startZ = pos.getZ() - 8;
                        int endX = pos.getX() + 8;
                        int endY = pos.getY() + 8;
                        int endZ = pos.getZ() + 8;

                        render(new AABB(new Vec3(startX, startY, startZ), new Vec3(endX, endY, endZ)), playerChunksSideColor.get(), playerChunksLineColor.get(), shapeMode.get(), event);
                    }
                }
            }
        }
    }

    private void render(AABB box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof ServerboundChunkBatchReceivedPacket)return; //for some reason this packet keeps getting cast to other packets
        if (!(event.packet instanceof ServerboundChunkBatchReceivedPacket) && !(event.packet instanceof ServerboundMovePlayerPacket) && event.packet instanceof ClientboundLevelChunkWithLightPacket packet && mc.level != null) {
            ChunkPos playerActivityPos = new ChunkPos(packet.getX(), packet.getZ());

            if (mc.level.getChunkSource().getChunkForLighting(packet.getX(), packet.getZ()) == null) {
                LevelChunk chunk = new LevelChunk(mc.level, playerActivityPos);
                try {
                    Map<Heightmap.Types, long[]> heightmaps = new EnumMap<>(Heightmap.Types.class);

                    Heightmap.Types type = Heightmap.Types.MOTION_BLOCKING;
                    long[] emptyHeightmapData = new long[37];
                    heightmaps.put(type, emptyHeightmapData);

                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        chunk.replaceWithPacketData(packet.getChunkData().getReadBuffer(), heightmaps,
                                packet.getChunkData().getBlockEntitiesTagsConsumer(packet.getX(), packet.getZ()));
                    }, taskExecutor);
                    future.join();
                } catch (CompletionException e) {}

                LevelChunkSection[] sections = chunk.getSections();

                try {
                    int Y=mc.level.getMinY();
                    int i=0;
                    boolean firstsectionappearsnew = false;
                    for (LevelChunkSection section : sections) {
                        var blockStatesContainer = section.getStates();

                        Palette<BlockState> blockStatePalette = blockStatesContainer.data.palette();
                        if (!(blockStatePalette instanceof LinearPalette<BlockState>))return;

                        int blockPaletteLength = blockStatePalette.getSize();
                        for (int i2 = 0; i2 < blockPaletteLength; i2++) {
                            BlockState blockPaletteEntry = blockStatePalette.valueFor(i2);
                            if (i2 == 0 && i == 0 && blockPaletteEntry.getBlock() == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("air")) && mc.level.dimension() != Level.END){
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
                                BlockState blockPaletteEntry = blockStatePalette.valueFor(i2);
                                if (!bstates.contains(blockPaletteEntry)) {
                                    missingBlocks.add(blockPaletteEntry);
                                }
                            }
                            boolean falsepositive = false;
                            boolean missingAblock = false;
                            Set<BlockState> detectedBlocks = new HashSet<>();

                            if (!missingBlocks.isEmpty()) {
                                for (BlockState missingBlock : missingBlocks) {
                                    if (mc.level.dimension() == Level.OVERWORLD) {
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
                                    } else if (mc.level.dimension() == Level.NETHER) {
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
                                    } else if (mc.level.dimension() == Level.END) {
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
                                    ChatUtils.sendMsg(Component.nullToEmpty("Missing block in Section " + i + ": " + state.getBlock()));
                                }
                                ChatUtils.sendMsg(Component.nullToEmpty("Detected Player Activity. X: " + playerActivityPos.getMiddleBlockX() + " Y: " + (Y+8) + " Z: " + playerActivityPos.getMiddleBlockZ()));
                                playerActivityPositions.add(new BlockPos(playerActivityPos.getMiddleBlockX(), Y+8, playerActivityPos.getMiddleBlockZ()));
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
            return !playerPos.closerThan(blockPos, renderDistanceBlocks);
        });
    }
}