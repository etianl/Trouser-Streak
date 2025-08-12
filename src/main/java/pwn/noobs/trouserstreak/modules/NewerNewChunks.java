package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.packet.c2s.play.AcknowledgeChunksC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.*;
import pwn.noobs.trouserstreak.Trouser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;

// Baritone
// Baritone accessed reflectively to avoid hard dependency

/*
    Ported from: https://github.com/BleachDrinker420/BleachHack/blob/master/BleachHack-Fabric-1.16/src/main/java/bleach/hack/module/mods/NewChunks.java
    updated by etianl :D
*/
public class NewerNewChunks extends Module {
	public enum DetectMode {
		Normal,
		IgnoreBlockExploit,
		BlockExploitMode
	}

	// Auto-follow configuration
	public enum FollowType {
		New,
		Old,
		BeingUpdated,
		OldGeneration,
		BlockExploit
	}

	public enum PathingMode {
		Regular,
		Elytra
	}
	private final SettingGroup specialGroup = settings.createGroup("Disable PaletteExploit if server version <1.18");
	private final SettingGroup specialGroup2 = settings.createGroup("Detection for chunks that were generated in old versions.");
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgCdata = settings.createGroup("Saved Chunk Data");
	private final SettingGroup sgcacheCdata = settings.createGroup("Cached Chunk Data");
	private final SettingGroup sgRender = settings.createGroup("Render");
	private final SettingGroup sgFollow = settings.createGroup("Auto Follow");

	private final Setting<Boolean> PaletteExploit = specialGroup.add(new BoolSetting.Builder()
			.name("PaletteExploit")
			.description("Detects new chunks by scanning the order of chunk section palettes. Highlights chunks being updated from an old version.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> beingUpdatedDetector = specialGroup.add(new BoolSetting.Builder()
			.name("Detection for chunks that haven't been explored since <=1.17")
			.description("Marks chunks as their own color if they are currently being updated from old version.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> overworldOldChunksDetector = specialGroup2.add(new BoolSetting.Builder()
			.name("Pre 1.17 Overworld OldChunk Detector")
			.description("Marks chunks as generated in an old version if they have specific blocks above Y 0 and are in the overworld.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> netherOldChunksDetector = specialGroup2.add(new BoolSetting.Builder()
			.name("Pre 1.16 Nether OldChunk Detector")
			.description("Marks chunks as generated in an old version if they are missing blocks found in the new Nether.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> endOldChunksDetector = specialGroup2.add(new BoolSetting.Builder()
			.name("Pre 1.13 End OldChunk Detector")
			.description("Marks chunks as generated in an old version if they have the biome of minecraft:the_end.")
			.defaultValue(true)
			.build()
	);
	public final Setting<DetectMode> detectmode = sgGeneral.add(new EnumSetting.Builder<DetectMode>()
			.name("Chunk Detection Mode")
			.description("Anything other than normal is for old servers where build limits are being increased due to updates.")
			.defaultValue(DetectMode.Normal)
			.build()
	);
	private final Setting<Boolean> liquidexploit = sgGeneral.add(new BoolSetting.Builder()
			.name("LiquidExploit")
			.description("Estimates newchunks based on flowing liquids.")
			.defaultValue(false)
			.build()
	);
	private final Setting<Boolean> blockupdateexploit = sgGeneral.add(new BoolSetting.Builder()
			.name("BlockUpdateExploit")
			.description("Estimates newchunks based on block updates. THESE MAY POSSIBLY BE OLD. BlockExploitMode needed to help determine false positives.")
			.defaultValue(false)
			.build()
	);
	private final Setting<Boolean> remove = sgcacheCdata.add(new BoolSetting.Builder()
			.name("RemoveOnModuleDisabled")
			.description("Removes the cached chunks when disabling the module.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> worldleaveremove = sgcacheCdata.add(new BoolSetting.Builder()
			.name("RemoveOnLeaveWorldOrChangeDimensions")
			.description("Removes the cached chunks when leaving the world or changing dimensions.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> removerenderdist = sgcacheCdata.add(new BoolSetting.Builder()
			.name("RemoveOutsideRenderDistance")
			.description("Removes the cached chunks when they leave the defined render distance.")
			.defaultValue(false)
			.build()
	);

	private final Setting<Boolean> save = sgCdata.add(new BoolSetting.Builder()
			.name("SaveChunkData")
			.description("Saves the cached chunks to a file.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> load = sgCdata.add(new BoolSetting.Builder()
			.name("LoadChunkData")
			.description("Loads the saved chunks from the file.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> autoreload = sgCdata.add(new BoolSetting.Builder()
			.name("AutoReloadChunks")
			.description("Reloads the chunks automatically from your savefiles on a delay.")
			.defaultValue(false)
			.visible(load::get)
			.build()
	);
	private final Setting<Integer> removedelay = sgCdata.add(new IntSetting.Builder()
			.name("AutoReloadDelayInSeconds")
			.description("Reloads the chunks automatically from your savefiles on a delay.")
			.sliderRange(1,300)
			.defaultValue(60)
			.visible(() -> autoreload.get() && load.get())
			.build()
	);

	@Override
	public WWidget getWidget(GuiTheme theme) {
		WTable table = theme.table();
		WButton deletedata = table.add(theme.button("**DELETE CHUNK DATA**")).expandX().minWidth(100).widget();
		deletedata.action = () -> {
			if (deletewarning==0) error("PRESS AGAIN WITHIN 5s TO DELETE ALL CHUNK DATA FOR THIS DIMENSION.");
			deletewarningTicks=0;
			deletewarning++;
		};
		table.row();
		return table;
	}
	public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
			.name("Render-Distance(Chunks)")
			.description("How many chunks from the character to render the detected chunks.")
			.defaultValue(64)
			.min(6)
			.sliderRange(6,1024)
			.build()
	);
	public final Setting<Integer> renderHeight = sgRender.add(new IntSetting.Builder()
			.name("render-height")
			.description("The height at which new chunks will be rendered")
			.defaultValue(0)
			.sliderRange(-112,319)
			.build()
	);

	private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
			.name("shape-mode")
			.description("How the shapes are rendered.")
			.defaultValue(ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> newChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("new-chunks-side-color")
			.description("Color of the chunks that are completely new.")
			.defaultValue(new SettingColor(255, 0, 0, 95))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
			.build()
	);
	private final Setting<SettingColor> tickexploitChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("BlockExploitChunks-side-color")
			.description("MAY POSSIBLY BE OLD. Color of the chunks that have been triggered via block ticking packets")
			.defaultValue(new SettingColor(0, 0, 255, 75))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) && detectmode.get()== DetectMode.BlockExploitMode)
			.build()
	);

	private final Setting<SettingColor> oldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-side-color")
			.description("Color of the chunks that have been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 40))
			.visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
			.build()
	);
	private final Setting<SettingColor> beingUpdatedOldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("being-updated-chunks-side-color")
			.description("Color of the chunks that haven't been explored since versions <=1.17.")
			.defaultValue(new SettingColor(255, 210, 0, 60))
			.visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
			.build()
	);
	private final Setting<SettingColor> OldGenerationOldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("old-version-chunks-side-color")
			.description("Color of the chunks that have been loaded before in old versions.")
			.defaultValue(new SettingColor(190, 255, 0, 40))
			.visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> newChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("new-chunks-line-color")
			.description("Color of the chunks that are completely new.")
			.defaultValue(new SettingColor(255, 0, 0, 205))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
			.build()
	);
	private final Setting<SettingColor> tickexploitChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("BlockExploitChunks-line-color")
			.description("MAY POSSIBLY BE OLD. Color of the chunks that have been triggered via block ticking packets")
			.defaultValue(new SettingColor(0, 0, 255, 170))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both) && detectmode.get()== DetectMode.BlockExploitMode)
			.build()
	);

	private final Setting<SettingColor> oldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-line-color")
			.description("Color of the chunks that have been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 80))
			.visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
			.build()
	);
	private final Setting<SettingColor> beingUpdatedOldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("being-updated-chunks-line-color")
			.description("Color of the chunks that haven't been explored since versions <=1.17.")
			.defaultValue(new SettingColor(255, 220, 0, 100))
			.visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
			.build()
	);
	private final Setting<SettingColor> OldGenerationOldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("old-version-chunks-line-color")
			.description("Color of the chunks that have been loaded before in old versions.")
			.defaultValue(new SettingColor(190, 255, 0, 80))
			.visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
			.build()
	);
	private static final ExecutorService taskExecutor = Executors.newCachedThreadPool();
	private int deletewarningTicks=666;
	private int deletewarning=0;
	private String serverip;
	private String world;
	private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> beingUpdatedOldChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> OldGenerationOldChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> tickexploitChunks = Collections.synchronizedSet(new HashSet<>());
	private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };
	private int errticks=0;
	private int autoreloadticks=0;
	private int loadingticks=0;
	private boolean worldchange=false;
	private int justenabledsavedata=0;
	private boolean saveDataWasOn = false;

    // Auto-follow state
    private ChunkPos currentTarget = null;
    private long lastSetGoalTime = 0L;
    private boolean baritoneWarned = false;
    // Anti-oscillation: remember last completed target for a short cooldown window (still used lightly)
    private ChunkPos lastCompletedTarget = null;
    private long lastCompletedAt = 0L;
    private static final long BACKTRACK_COOLDOWN_MS = 4000L;
    // Dynamic heading that updates as we progress; used to forbid backwards moves
    private Direction lastHeading = null;
    // Recent chunk history for trend + oscillation detection
    private static final int CHUNK_HISTORY_SIZE = 8;
    private static final long OSCILLATION_WINDOW_MS = 15_000L;
    private final Deque<ChunkVisit> chunkHistory = new ArrayDeque<>();
    private ChunkPos lastPlayerChunk = null;
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
	private static final Set<Block> DEEPSLATE_BLOCKS = new HashSet<>();
	static {
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE);
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE_COPPER_ORE);
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE_IRON_ORE);
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE_COAL_ORE);
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE_REDSTONE_ORE);
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE_EMERALD_ORE);
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE_GOLD_ORE);
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE_LAPIS_ORE);
		DEEPSLATE_BLOCKS.add(Blocks.DEEPSLATE_DIAMOND_ORE);
	}
	private static final Set<Block> NEW_OVERWORLD_BLOCKS = new HashSet<>();
	static {
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.AMETHYST_BLOCK);
		NEW_OVERWORLD_BLOCKS.add(Blocks.BUDDING_AMETHYST);
		NEW_OVERWORLD_BLOCKS.add(Blocks.AZALEA);
		NEW_OVERWORLD_BLOCKS.add(Blocks.FLOWERING_AZALEA);
		NEW_OVERWORLD_BLOCKS.add(Blocks.BIG_DRIPLEAF);
		NEW_OVERWORLD_BLOCKS.add(Blocks.BIG_DRIPLEAF_STEM);
		NEW_OVERWORLD_BLOCKS.add(Blocks.SMALL_DRIPLEAF);
		NEW_OVERWORLD_BLOCKS.add(Blocks.CAVE_VINES);
		NEW_OVERWORLD_BLOCKS.add(Blocks.CAVE_VINES_PLANT);
		NEW_OVERWORLD_BLOCKS.add(Blocks.SPORE_BLOSSOM);
		NEW_OVERWORLD_BLOCKS.add(Blocks.COPPER_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_COPPER_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_IRON_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_COAL_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_REDSTONE_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_EMERALD_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_GOLD_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_LAPIS_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_DIAMOND_ORE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.GLOW_LICHEN);
		NEW_OVERWORLD_BLOCKS.add(Blocks.RAW_COPPER_BLOCK);
		NEW_OVERWORLD_BLOCKS.add(Blocks.RAW_IRON_BLOCK);
		NEW_OVERWORLD_BLOCKS.add(Blocks.DRIPSTONE_BLOCK);
		NEW_OVERWORLD_BLOCKS.add(Blocks.MOSS_BLOCK);
		NEW_OVERWORLD_BLOCKS.add(Blocks.MOSS_CARPET);
		NEW_OVERWORLD_BLOCKS.add(Blocks.POINTED_DRIPSTONE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.SMOOTH_BASALT);
		NEW_OVERWORLD_BLOCKS.add(Blocks.TUFF);
		NEW_OVERWORLD_BLOCKS.add(Blocks.CALCITE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.HANGING_ROOTS);
		NEW_OVERWORLD_BLOCKS.add(Blocks.ROOTED_DIRT);
		NEW_OVERWORLD_BLOCKS.add(Blocks.AZALEA_LEAVES);
		NEW_OVERWORLD_BLOCKS.add(Blocks.FLOWERING_AZALEA_LEAVES);
		NEW_OVERWORLD_BLOCKS.add(Blocks.POWDER_SNOW);
	}
	private static final Set<Block> NEW_NETHER_BLOCKS = new HashSet<>();
	static {
		NEW_NETHER_BLOCKS.add(Blocks.ANCIENT_DEBRIS);
		NEW_NETHER_BLOCKS.add(Blocks.BASALT);
		NEW_NETHER_BLOCKS.add(Blocks.BLACKSTONE);
		NEW_NETHER_BLOCKS.add(Blocks.GILDED_BLACKSTONE);
		NEW_NETHER_BLOCKS.add(Blocks.POLISHED_BLACKSTONE_BRICKS);
		NEW_NETHER_BLOCKS.add(Blocks.CRIMSON_STEM);
		NEW_NETHER_BLOCKS.add(Blocks.CRIMSON_NYLIUM);
		NEW_NETHER_BLOCKS.add(Blocks.NETHER_GOLD_ORE);
		NEW_NETHER_BLOCKS.add(Blocks.WARPED_NYLIUM);
		NEW_NETHER_BLOCKS.add(Blocks.WARPED_STEM);
		NEW_NETHER_BLOCKS.add(Blocks.TWISTING_VINES);
		NEW_NETHER_BLOCKS.add(Blocks.WEEPING_VINES);
		NEW_NETHER_BLOCKS.add(Blocks.BONE_BLOCK);
		NEW_NETHER_BLOCKS.add(Blocks.CHAIN);
		NEW_NETHER_BLOCKS.add(Blocks.OBSIDIAN);
		NEW_NETHER_BLOCKS.add(Blocks.CRYING_OBSIDIAN);
		NEW_NETHER_BLOCKS.add(Blocks.SOUL_SOIL);
		NEW_NETHER_BLOCKS.add(Blocks.SOUL_FIRE);
	}
	Set<Path> FILE_PATHS = new HashSet<>(Set.of(
			Paths.get("OldChunkData.txt"),
			Paths.get("BeingUpdatedChunkData.txt"),
			Paths.get("OldGenerationChunkData.txt"),
			Paths.get("NewChunkData.txt"),
			Paths.get("BlockExploitChunkData.txt")
	));
	public NewerNewChunks() {
		super(Trouser.baseHunting,"NewerNewChunks", "Detects new chunks by scanning the order of chunk section palettes. Can also check liquid flow, and block ticking packets.");
	}

	// Auto-follow settings
	private final Setting<Boolean> autoFollow = sgFollow.add(new BoolSetting.Builder()
		.name("auto-follow")
		.description("Automatically path to adjacent chunks of a chosen type using Baritone.")
		.defaultValue(false)
		.build()
	);

	private final Setting<FollowType> followType = sgFollow.add(new EnumSetting.Builder<FollowType>()
		.name("follow-type")
		.description("Which detected chunk type to follow.")
		.defaultValue(FollowType.New)
		.build()
	);

    private final Setting<PathingMode> pathingMode = sgFollow.add(new EnumSetting.Builder<PathingMode>()
        .name("pathing-mode")
        .description("Choose Regular walking pathing or Elytra-oriented pathing.")
        .defaultValue(PathingMode.Regular)
        .build()
    );

	private final Setting<Integer> lookAhead = sgFollow.add(new IntSetting.Builder()
		.name("look-ahead-chunks")
		.description("How many chunks ahead to consider for a continuous path.")
		.defaultValue(2)
		.min(1)
		.sliderRange(1, 8)
		.build()
	);

    private final Setting<Integer> searchRadius = sgFollow.add(new IntSetting.Builder()
        .name("search-radius-chunks")
        .description("Max chunk distance to search for next target when no adjacent candidate exists.")
        .defaultValue(8)
        .min(1)
        .sliderRange(1, 64)
        .build()
    );
    private final Setting<Boolean> followLogging = sgFollow.add(new BoolSetting.Builder()
        .name("chat-logging")
        .description("Log auto-follow decisions and Baritone calls in chat.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> maxGap = sgFollow.add(new IntSetting.Builder()
        .name("gap-allowance-chunks")
        .description("Allow skipping this many non-target chunks straight ahead to keep direction.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 8)
        .build()
    );
    private final Setting<Boolean> pauseOnInput = sgFollow.add(new BoolSetting.Builder()
        .name("pause-on-input")
        .description("Pause auto-follow while you press movement/interaction keys.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> logoutOnNoTargets = sgFollow.add(new BoolSetting.Builder()
        .name("logout-when-empty")
        .description("Logout when no chunks of the chosen type are detected.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> logoutOnTrailEnd = sgFollow.add(new BoolSetting.Builder()
        .name("logout-on-trail-end")
        .description("Logout when forward progress ends due to a trail ending (prevents ping-pong backtracking).")
        .defaultValue(true)
        .build()
    );
	private void clearChunkData() {
		newChunks.clear();
		oldChunks.clear();
		beingUpdatedOldChunks.clear();
		OldGenerationOldChunks.clear();
		tickexploitChunks.clear();
	}
	@Override
	public void onActivate() {
		if (save.get())saveDataWasOn = true;
		else if (!save.get())saveDataWasOn = false;
		if (autoreload.get()) {
			clearChunkData();
		}
		if (save.get() || load.get() && mc.world != null) {
			world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
			if (mc.isInSingleplayer()){
				String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
				serverip=array[array.length-2];
			} else {
				serverip = mc.getCurrentServerEntry().address.replace(':', '_');
			}
		}
		if (save.get()){
			try {
				Files.createDirectories(Paths.get("TrouserStreak", "NewChunks", serverip, world));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (save.get() || load.get()) {
			Path baseDir = Paths.get("TrouserStreak", "NewChunks", serverip, world);

			for (Path fileName : FILE_PATHS) {
				Path fullPath = baseDir.resolve(fileName);
				try {
					Files.createDirectories(fullPath.getParent());
					if (Files.notExists(fullPath)) {
						Files.createFile(fullPath);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (load.get()){
			loadData();
		}
		autoreloadticks=0;
		loadingticks=0;
		worldchange=false;
		justenabledsavedata=0;
	}
	@Override
    public void onDeactivate() {
		autoreloadticks=0;
		loadingticks=0;
		worldchange=false;
		justenabledsavedata=0;
		if (remove.get()|autoreload.get()) {
			clearChunkData();
		}
		// Stop Baritone pathing if active (reflection)
        try { baritoneCancel(); } catch (Throwable ignored) {}
        currentTarget = null;
        baritoneWarned = false;
        lastHeading = null;
        super.onDeactivate();
    }
	@EventHandler
	private void onScreenOpen(OpenScreenEvent event) {
		if (event.screen instanceof DisconnectedScreen) {
			if (worldleaveremove.get()) {
				clearChunkData();
			}
		}
		if (event.screen instanceof DownloadingTerrainScreen) {
			worldchange=true;
		}
	}
	@EventHandler
	private void onGameLeft(GameLeftEvent event) {
		if (worldleaveremove.get()) {
			clearChunkData();
		}
	}
	@EventHandler
    private void onPreTick(TickEvent.Pre event) {
        world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');

		if (deletewarningTicks<=100) deletewarningTicks++;
		else deletewarning=0;
		if (deletewarning>=2){
			if (mc.isInSingleplayer()){
				String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
				serverip=array[array.length-2];
			} else {
				serverip = mc.getCurrentServerEntry().address.replace(':', '_');
			}
			clearChunkData();
			try {
				Files.deleteIfExists(Paths.get("TrouserStreak", "NewChunks", serverip, world, "NewChunkData.txt"));
				Files.deleteIfExists(Paths.get("TrouserStreak", "NewChunks", serverip, world, "OldChunkData.txt"));
				Files.deleteIfExists(Paths.get("TrouserStreak", "NewChunks", serverip, world, "BeingUpdatedChunkData.txt"));
				Files.deleteIfExists(Paths.get("TrouserStreak", "NewChunks", serverip, world, "OldGenerationChunkData.txt"));
				Files.deleteIfExists(Paths.get("TrouserStreak", "NewChunks", serverip, world, "BlockExploitChunkData.txt"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			error("Chunk Data deleted for this Dimension.");
			deletewarning=0;
		}

		if (detectmode.get()== DetectMode.Normal && blockupdateexploit.get()){
			if (errticks<6){
				errticks++;}
			if (errticks==5){
				error("BlockExploitMode RECOMMENDED. Required to determine false positives from the Block Exploit from the OldChunks.");
			}
		} else errticks=0;

		if (load.get()){
			if (loadingticks<1){
				loadData();
				loadingticks++;
			}
		} else if (!load.get()){
			loadingticks=0;
		}

		if (autoreload.get()) {
			autoreloadticks++;
			if (autoreloadticks==removedelay.get()*20){
				clearChunkData();
				if (load.get()){
					loadData();
				}
			} else if (autoreloadticks>=removedelay.get()*20){
				autoreloadticks=0;
			}
		}

		if (load.get() && worldchange){		//autoreload when entering different dimensions
			if (worldleaveremove.get()){
				clearChunkData();
			}
			loadData();
			worldchange=false;
		}

		if (!save.get())saveDataWasOn = false;
		if (save.get() && justenabledsavedata<=2 && !saveDataWasOn){
			justenabledsavedata++;
			if (justenabledsavedata == 1){
				synchronized (newChunks) {
					for (ChunkPos chunk : newChunks){
						saveData(Paths.get("NewChunkData.txt"), chunk);
					}
				}
				synchronized (OldGenerationOldChunks) {
					for (ChunkPos chunk : OldGenerationOldChunks){
						saveData(Paths.get("OldGenerationChunkData.txt"), chunk);
					}
				}
				synchronized (beingUpdatedOldChunks) {
					for (ChunkPos chunk : beingUpdatedOldChunks){
						saveData(Paths.get("BeingUpdatedChunkData.txt"), chunk);
					}
				}
				synchronized (oldChunks) {
					for (ChunkPos chunk : oldChunks){
						saveData(Paths.get("OldChunkData.txt"), chunk);
					}
				}
				synchronized (tickexploitChunks) {
					for (ChunkPos chunk : tickexploitChunks){
						saveData(Paths.get("BlockExploitChunkData.txt"), chunk);
					}
				}
			}
		}

        if (removerenderdist.get())removeChunksOutsideRenderDistance();

        // Track player's chunk history for trend + oscillation detection
        if (mc.player != null) {
            ChunkPos now = new ChunkPos(mc.player.getBlockX() >> 4, mc.player.getBlockZ() >> 4);
            if (!now.equals(lastPlayerChunk)) {
                lastPlayerChunk = now;
                pushChunkVisit(now);
                // Check for ABAB oscillation within a short window
                if (detectOscillation()) {
                    logFollow("Detected oscillation between chunks. Cancelling and logging out.");
                    try { baritoneCancel(); } catch (Throwable ignored) {}
                    if (logoutOnTrailEnd.get()) try { logoutClient("Oscillation detected at trail end"); } catch (Throwable ignored) {}
                }
            }
        }

		// Auto-follow tick
        if (autoFollow.get()) {
            if (!baritoneAvailable()) {
                if (!baritoneWarned) { info("Baritone not found. Auto-follow will not path."); baritoneWarned = true; }
                return;
            }
            baritoneWarned = false;
            try { updateAutoFollow(); } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    private void onKeyEvent(KeyEvent event) {
        if (!pauseOnInput.get()) return;
        if (mc == null || mc.player == null) return;
        // If any movement/interaction key is currently pressed, pause auto-follow
        if (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() ||
            mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() ||
            mc.options.sneakKey.isPressed() || mc.options.jumpKey.isPressed() ||
            mc.options.sprintKey.isPressed() || mc.options.attackKey.isPressed() ||
            mc.options.useKey.isPressed()) {
            disableAutoFollowDueToInput();
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (!pauseOnInput.get()) return;
        if (mc == null || mc.player == null) return;
        // If attack/use are currently pressed, pause auto-follow
        if (mc.options.attackKey.isPressed() || mc.options.useKey.isPressed()) {
            disableAutoFollowDueToInput();
        }
    }
	@EventHandler
	private void onRender(Render3DEvent event) {
		if (mc.world == null || mc.player == null) return;
		if (event.renderer == null) return;
		BlockPos playerPos = new BlockPos(mc.player.getBlockX(), renderHeight.get(), mc.player.getBlockZ());
		if (newChunksLineColor.get().a > 5 || newChunksSideColor.get().a > 5) {
			synchronized (newChunks) {
				for (ChunkPos c : newChunks) {
					if (c != null && playerPos.isWithinDistance(new BlockPos(c.getCenterX(), renderHeight.get(), c.getCenterZ()), renderDistance.get()*16)) {
						render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), newChunksSideColor.get(), newChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (tickexploitChunksLineColor.get().a > 5 || tickexploitChunksSideColor.get().a > 5) {
			synchronized (tickexploitChunks) {
				for (ChunkPos c : tickexploitChunks) {
					if (c != null && playerPos.isWithinDistance(new BlockPos(c.getCenterX(), renderHeight.get(), c.getCenterZ()), renderDistance.get()*16)) {
						if (detectmode.get()== DetectMode.BlockExploitMode && blockupdateexploit.get()) {
							render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), tickexploitChunksSideColor.get(), tickexploitChunksLineColor.get(), shapeMode.get(), event);
						} else if ((detectmode.get()== DetectMode.Normal) && blockupdateexploit.get()) {
							render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), newChunksSideColor.get(), newChunksLineColor.get(), shapeMode.get(), event);
						} else if ((detectmode.get()== DetectMode.IgnoreBlockExploit) && blockupdateexploit.get()) {
							render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), oldChunksSideColor.get(), oldChunksLineColor.get(), shapeMode.get(), event);
						} else if ((detectmode.get()== DetectMode.BlockExploitMode | detectmode.get()== DetectMode.Normal | detectmode.get()== DetectMode.IgnoreBlockExploit) && !blockupdateexploit.get()) {
							render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), oldChunksSideColor.get(), oldChunksLineColor.get(), shapeMode.get(), event);
						}
					}
				}
			}
		}
		if (oldChunksLineColor.get().a > 5 || oldChunksSideColor.get().a > 5){
			synchronized (oldChunks) {
				for (ChunkPos c : oldChunks) {
					if (c != null && playerPos.isWithinDistance(new BlockPos(c.getCenterX(), renderHeight.get(), c.getCenterZ()), renderDistance.get()*16)) {
						render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), oldChunksSideColor.get(), oldChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (beingUpdatedOldChunksLineColor.get().a > 5 || beingUpdatedOldChunksSideColor.get().a > 5){
			synchronized (beingUpdatedOldChunks) {
				for (ChunkPos c : beingUpdatedOldChunks) {
					if (c != null && playerPos.isWithinDistance(new BlockPos(c.getCenterX(), renderHeight.get(), c.getCenterZ()), renderDistance.get()*16)) {
						render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), beingUpdatedOldChunksSideColor.get(), beingUpdatedOldChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (OldGenerationOldChunksLineColor.get().a > 5 || OldGenerationOldChunksSideColor.get().a > 5){
			synchronized (OldGenerationOldChunks) {
				for (ChunkPos c : OldGenerationOldChunks) {
					if (c != null && playerPos.isWithinDistance(new BlockPos(c.getCenterX(), renderHeight.get(), c.getCenterZ()), renderDistance.get()*16)) {
						render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), OldGenerationOldChunksSideColor.get(), OldGenerationOldChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
	}

	private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
		try {
			event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
		} catch (Exception e) {}
	}

	@EventHandler
	private void onReadPacket(PacketEvent.Receive event) {
		if (event.packet instanceof AcknowledgeChunksC2SPacket )return; //for some reason this packet keeps getting cast to other packets
		if (!(event.packet instanceof AcknowledgeChunksC2SPacket) && event.packet instanceof ChunkDeltaUpdateS2CPacket packet && liquidexploit.get()) {

			packet.visitUpdates((pos, state) -> {
				ChunkPos chunkPos = new ChunkPos(pos);
				if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
					for (Direction dir: searchDirs) {
						try {
							if (mc.world != null && mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
								tickexploitChunks.remove(chunkPos);
								newChunks.add(chunkPos);
								if (save.get()){
									saveData(Paths.get("NewChunkData.txt"), chunkPos);
								}
								return;
							}
						} catch (Exception e) {}
					}
				}
			});
		}
		else if (!(event.packet instanceof AcknowledgeChunksC2SPacket) && event.packet instanceof BlockUpdateS2CPacket packet) {
			ChunkPos chunkPos = new ChunkPos(packet.getPos());
			if (blockupdateexploit.get()){
				try {
					if (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !tickexploitChunks.contains(chunkPos) && !oldChunks.contains(chunkPos) && !newChunks.contains(chunkPos)){
						tickexploitChunks.add(chunkPos);
						if (save.get()){
							saveData(Paths.get("BlockExploitChunkData.txt"), chunkPos);
						}
					}
				}
				catch (Exception e){}
			}
			if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill() && liquidexploit.get()) {
				for (Direction dir: searchDirs) {
					try {
						if (mc.world != null && mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
							tickexploitChunks.remove(chunkPos);
							newChunks.add(chunkPos);
							if (save.get()){
								saveData(Paths.get("NewChunkData.txt"), chunkPos);
							}
							return;
						}
					} catch (Exception e) {}
				}
			}
		}
		else if (!(event.packet instanceof AcknowledgeChunksC2SPacket) && !(event.packet instanceof PlayerMoveC2SPacket) && event.packet instanceof ChunkDataS2CPacket packet && mc.world != null) {
			ChunkPos oldpos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());

			if (mc.world.getChunkManager().getChunk(packet.getChunkX(), packet.getChunkZ()) == null) {
				WorldChunk chunk = new WorldChunk(mc.world, oldpos);
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

				boolean isNewChunk = false;
				boolean isOldGeneration = false;
				boolean chunkIsBeingUpdated = false;
				boolean foundAnyOre = false;
				boolean isNewOverworldGeneration = false;
				boolean isNewNetherGeneration = false;
				ChunkSection[] sections = chunk.getSectionArray();

				if (overworldOldChunksDetector.get() && mc.world.getRegistryKey() == World.OVERWORLD && chunk.getStatus().isAtLeast(ChunkStatus.FULL) && !chunk.isEmpty()) {
					for (int i = 0; i < 17; i++) {
						ChunkSection section = sections[i];
						if (section != null && !section.isEmpty()) {
							for (int x = 0; x < 16; x++) {
								for (int y = 0; y < 16; y++) {
									for (int z = 0; z < 16; z++) {
										if (!foundAnyOre && ORE_BLOCKS.contains(section.getBlockState(x, y, z).getBlock())) foundAnyOre = true; //prevent false flags in flat world
										if (((y >= 5 && i == 4) || i > 4) && !isNewOverworldGeneration && (NEW_OVERWORLD_BLOCKS.contains(section.getBlockState(x, y, z).getBlock()) || DEEPSLATE_BLOCKS.contains(section.getBlockState(x, y, z).getBlock()))) {
											isNewOverworldGeneration = true;
											break;
										}
									}
								}
							}
						}
					}
					if (foundAnyOre && !isOldGeneration && !isNewOverworldGeneration) isOldGeneration = true;
				}

				if (netherOldChunksDetector.get() && mc.world.getRegistryKey() == World.NETHER && chunk.getStatus().isAtLeast(ChunkStatus.FULL) && !chunk.isEmpty()) {
					for (int i = 0; i < 8; i++) {
						ChunkSection section = sections[i];
						if (section != null && !section.isEmpty()) {
							for (int x = 0; x < 16; x++) {
								for (int y = 0; y < 16; y++) {
									for (int z = 0; z < 16; z++) {
										if (!isNewNetherGeneration && NEW_NETHER_BLOCKS.contains(section.getBlockState(x, y, z).getBlock())) {
											isNewNetherGeneration = true;
											break;
										}
									}
								}
							}
						}
					}
					if (!isOldGeneration && !isNewNetherGeneration) isOldGeneration = true;
				}

				if (endOldChunksDetector.get() && mc.world.getRegistryKey() == World.END && chunk.getStatus().isAtLeast(ChunkStatus.FULL) && !chunk.isEmpty()) {
					ChunkSection section = chunk.getSection(0);
					var biomesContainer = section.getBiomeContainer();
					if (biomesContainer instanceof PalettedContainer<RegistryEntry<Biome>> biomesPaletteContainer) {
						Palette<RegistryEntry<Biome>> biomePalette = biomesPaletteContainer.data.palette();
						for (int i = 0; i < biomePalette.getSize(); i++) {
							if (biomePalette.get(i).getKey().get() == BiomeKeys.THE_END) {
								isOldGeneration = true;
								break;
							}
						}
					}
				}

				if (PaletteExploit.get()) {
					boolean firstchunkappearsnew = false;
					int loops = 0;
					int newChunkQuantifier = 0;
					int oldChunkQuantifier = 0;
					try {
						for (ChunkSection section : sections) {
							if (section != null) {
								int isNewSection = 0;
								int isBeingUpdatedSection = 0;

								if (!section.isEmpty()) {
									var blockStatesContainer = section.getBlockStateContainer();
									Palette<BlockState> blockStatePalette = blockStatesContainer.data.palette();
									int blockPaletteLength = blockStatePalette.getSize();

									if (blockStatePalette instanceof BiMapPalette<BlockState>){
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
										if (bstatesSize < blockPaletteLength) {
											isNewSection = 2;
										}
									}

									for (int i2 = 0; i2 < blockPaletteLength; i2++) {
										BlockState blockPaletteEntry = blockStatePalette.get(i2);
										if (i2 == 0 && loops == 0 && blockPaletteEntry.getBlock() == Blocks.AIR && mc.world.getRegistryKey() != World.END)
											firstchunkappearsnew = true;
										if (i2 == 0 && blockPaletteEntry.getBlock() == Blocks.AIR && mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END)
											isNewSection++;
										if (i2 == 1 && (blockPaletteEntry.getBlock() == Blocks.WATER || blockPaletteEntry.getBlock() == Blocks.STONE || blockPaletteEntry.getBlock() == Blocks.GRASS_BLOCK || blockPaletteEntry.getBlock() == Blocks.SNOW_BLOCK) && mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END)
											isNewSection++;
										if (i2 == 2 && (blockPaletteEntry.getBlock() == Blocks.SNOW_BLOCK || blockPaletteEntry.getBlock() == Blocks.DIRT || blockPaletteEntry.getBlock() == Blocks.POWDER_SNOW) && mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END)
											isNewSection++;
										if (loops == 4 && blockPaletteEntry.getBlock() == Blocks.BEDROCK && mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END) {
											if (!chunkIsBeingUpdated && beingUpdatedDetector.get())
												chunkIsBeingUpdated = true;
										}
										if (blockPaletteEntry.getBlock() == Blocks.AIR && (mc.world.getRegistryKey() == World.NETHER || mc.world.getRegistryKey() == World.END))
											isBeingUpdatedSection++;
									}
									if (isBeingUpdatedSection >= 2) oldChunkQuantifier++;
									if (isNewSection >= 2) newChunkQuantifier++;
								}
								if (mc.world.getRegistryKey() == World.END) {
									var biomesContainer = section.getBiomeContainer();
									if (biomesContainer instanceof PalettedContainer<RegistryEntry<Biome>> biomesPaletteContainer) {
										Palette<RegistryEntry<Biome>> biomePalette = biomesPaletteContainer.data.palette();
										for (int i3 = 0; i3 < biomePalette.getSize(); i3++) {
											if (i3 == 0 && biomePalette.get(i3).getKey().get() == BiomeKeys.PLAINS) isNewChunk = true;
											if (!isNewChunk && i3 == 0 && biomePalette.get(i3).getKey().get() != BiomeKeys.THE_END) isNewChunk = false;
										}
									}
								}
								if (!section.isEmpty())loops++;
							}
						}

						if (loops > 0) {
							if (beingUpdatedDetector.get() && (mc.world.getRegistryKey() == World.NETHER || mc.world.getRegistryKey() == World.END)){
								double oldpercentage = ((double) oldChunkQuantifier / loops) * 100;
								if (oldpercentage >= 25) chunkIsBeingUpdated = true;
							}
							else if (mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END){
								double percentage = ((double) newChunkQuantifier / loops) * 100;
								if (percentage >= 51) isNewChunk = true;
							}
						}
					} catch (Exception e) {
						if (beingUpdatedDetector.get() && (mc.world.getRegistryKey() == World.NETHER || mc.world.getRegistryKey() == World.END)){
							double oldpercentage = ((double) oldChunkQuantifier / loops) * 100;
							if (oldpercentage >= 25) chunkIsBeingUpdated = true;
						}
						else if (mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END){
							double percentage = ((double) newChunkQuantifier / loops) * 100;
							if (percentage >= 51) isNewChunk = true;
						}
					}

					if (firstchunkappearsnew) isNewChunk = true;
					boolean bewlian = (mc.world.getRegistryKey() == World.END) ? isNewChunk : !isOldGeneration;
					if (isNewChunk && !chunkIsBeingUpdated && bewlian) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								newChunks.add(oldpos);
								if (save.get()) {
									saveData(Paths.get("NewChunkData.txt"), oldpos);
								}
								return;
							}
						} catch (Exception e) {
						}
					}
					else if (!isNewChunk && !chunkIsBeingUpdated && isOldGeneration) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								OldGenerationOldChunks.add(oldpos);
								if (save.get()){
									saveData(Paths.get("OldGenerationChunkData.txt"), oldpos);
								}
								return;
							}
						} catch (Exception e) {
						}
					}
					else if (chunkIsBeingUpdated) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								beingUpdatedOldChunks.add(oldpos);
								if (save.get()){
									saveData(Paths.get("BeingUpdatedChunkData.txt"), oldpos);
								}
								return;
							}
						} catch (Exception e) {
						}
					}
					else if (!isNewChunk) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								oldChunks.add(oldpos);
								if (save.get()) {
									saveData(Paths.get("OldChunkData.txt"), oldpos);
								}
								return;
							}
						} catch (Exception e) {
						}
					}
				}
				if (liquidexploit.get()) {
					for (int x = 0; x < 16; x++) {
						for (int y = mc.world.getBottomY(); y < mc.world.getTopYInclusive(); y++) {
							for (int z = 0; z < 16; z++) {
								FluidState fluid = chunk.getFluidState(x, y, z);
								try {
									if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos) && !fluid.isEmpty() && !fluid.isStill()) {
										oldChunks.add(oldpos);
										if (save.get()){
											saveData(Paths.get("OldChunkData.txt"), oldpos);
										}
										return;
									}
								} catch (Exception e) {
								}
							}
						}
					}
				}
			}
		}
	}
	private void loadData() {
		loadChunkData(Paths.get("BlockExploitChunkData.txt"), tickexploitChunks);
		loadChunkData(Paths.get("OldChunkData.txt"), oldChunks);
		loadChunkData(Paths.get("NewChunkData.txt"), newChunks);
		loadChunkData(Paths.get("BeingUpdatedChunkData.txt"), beingUpdatedOldChunks);
		loadChunkData(Paths.get("OldGenerationChunkData.txt"), OldGenerationOldChunks);
	}
    private void loadChunkData(Path savedDataLocation, Set<ChunkPos> chunkSet) {
		try {
			Path filePath = Paths.get("TrouserStreak/NewChunks", serverip, world).resolve(savedDataLocation);
			List<String> allLines = Files.readAllLines(filePath);

			for (String line : allLines) {
				if (line != null && !line.isEmpty()) {
					String[] array = line.split(", ");
					if (array.length == 2) {
						int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]", ""));
						int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]", ""));
						ChunkPos chunkPos = new ChunkPos(X, Z);
						if (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !tickexploitChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos)) {
							chunkSet.add(chunkPos);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void saveData(Path savedDataLocation, ChunkPos chunkpos) {
		try {
			Path dirPath = Paths.get("TrouserStreak", "NewChunks", serverip, world);
			Files.createDirectories(dirPath);

			Path filePath = dirPath.resolve(savedDataLocation);
			String data = chunkpos.toString() + System.lineSeparator();

			Files.write(filePath, data.getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void removeChunksOutsideRenderDistance() {
		if (mc.player == null) return;
		BlockPos playerPos = new BlockPos(mc.player.getBlockX(), renderHeight.get(), mc.player.getBlockZ());
		double renderDistanceBlocks = renderDistance.get() * 16;

		removeChunksOutsideRenderDistance(newChunks, playerPos, renderDistanceBlocks);
		removeChunksOutsideRenderDistance(oldChunks, playerPos, renderDistanceBlocks);
		removeChunksOutsideRenderDistance(beingUpdatedOldChunks, playerPos, renderDistanceBlocks);
		removeChunksOutsideRenderDistance(OldGenerationOldChunks, playerPos, renderDistanceBlocks);
		removeChunksOutsideRenderDistance(tickexploitChunks, playerPos, renderDistanceBlocks);
	}
    private void removeChunksOutsideRenderDistance(Set<ChunkPos> chunkSet, BlockPos playerPos, double renderDistanceBlocks) {
        chunkSet.removeIf(c -> !playerPos.isWithinDistance(new BlockPos(c.getCenterX(), renderHeight.get(), c.getCenterZ()), renderDistanceBlocks));
    }

    // --- Auto-follow implementation ---
    private void updateAutoFollow() {
        if (mc == null || mc.player == null || mc.world == null) return;
        // Ensure Baritone is available before doing any goal work
        if (!baritoneAvailable()) {
            if (!baritoneWarned) {
                logFollow("Baritone not detected. Enable Baritone to use auto-follow.");
                baritoneWarned = true;
            }
            return;
        } else if (baritoneWarned) {
            // Reset once Baritone is detected again
            baritoneWarned = false;
        }
        // Resolve candidate set
        Set<ChunkPos> poolRef = getPoolForFollowType();
        if (poolRef == null || poolRef.isEmpty()) {
            logFollowOnce("No chunks available to follow for " + followType.get() + ".");
            // Stop navigating and logout if configured
            try { baritoneCancel(); } catch (Throwable ignored) {}
            if (logoutOnNoTargets.get()) try { logoutClient("No target chunks detected for " + followType.get()); } catch (Throwable ignored) {}
            return;
        }
        Set<ChunkPos> pool;
        // Create a snapshot to avoid concurrent modification
        synchronized (poolRef) {
            pool = new HashSet<>(poolRef);
        }

        ChunkPos playerChunk = new ChunkPos(mc.player.getBlockX() >> 4, mc.player.getBlockZ() >> 4);

        // If in current target, clear it to pick next
        if (currentTarget != null && isWithinChunk(currentTarget, mc.player.getBlockPos())) {
            logFollow("Reached target chunk " + currentTarget.x + "," + currentTarget.z + ", selecting next.");
            // Mark last completed to avoid immediate backtracking/ping-pong
            lastCompletedTarget = currentTarget;
            lastCompletedAt = System.currentTimeMillis();
            currentTarget = null;
        }

        // Pick a new target only when none is active; guided by recent trend and never backtrack
        if (currentTarget == null) {
            Direction trend = trendHeadingFromHistory();
            NextChoice choice = chooseNextAlongTrail(playerChunk, pool, lookAhead.get(), trend != null ? trend : lastHeading);
            if (choice == null) {
                logFollow("Trail ended in allowed direction(s). Cancelling and logging out.");
                try { baritoneCancel(); } catch (Throwable ignored) {}
                if (logoutOnTrailEnd.get()) try { logoutClient("Trail ended for " + followType.get()); } catch (Throwable ignored) {}
                return;
            }
            // If the choice would immediately backtrack to the previous chunk, treat as oscillation/trail end
            ChunkPos prev = previousVisitedChunk();
            if (prev != null && lastPlayerChunk != null && choice.chunk.equals(prev)) {
                logFollow("Backtracking detected towards previous chunk. Cancelling and logging out.");
                try { baritoneCancel(); } catch (Throwable ignored) {}
                if (logoutOnTrailEnd.get()) try { logoutClient("Backtracking detected at trail end"); } catch (Throwable ignored) {}
                return;
            }
            currentTarget = choice.chunk;
            lastHeading = choice.heading; // update our heading to the chosen first step
            logFollow("New target chunk " + currentTarget.x + "," + currentTarget.z + " heading=" + lastHeading + ".");
            setGoalForChunk(currentTarget);
            lastSetGoalTime = System.currentTimeMillis();
        }
    }

	private Set<ChunkPos> getPoolForFollowType() {
		switch (followType.get()) {
			case New: return newChunks;
			case Old: return oldChunks;
			case BeingUpdated: return beingUpdatedOldChunks;
			case OldGeneration: return OldGenerationOldChunks;
			case BlockExploit: return tickexploitChunks;
		}
		return null;
	}

    // Choose next along trail using relative heading, never going backwards; allows left/right turns
    // Enforces that every intermediate step stays forward-or-lateral relative to the initial heading from 'start'.
    private NextChoice chooseNextAlongTrail(ChunkPos start, Set<ChunkPos> pool, int ahead, Direction heading) {
        int need = Math.max(1, ahead);
        // Build ordered directions to try: current heading, then left, then right.
        // If unknown, derive from player's look and EXCLUDE the opposite/backwards direction.
        Direction[] tryDirs;
        if (heading != null) {
            tryDirs = new Direction[]{heading, leftOf(heading), rightOf(heading)};
        } else {
            Direction[] lookOrder = getLookOrderedDirs();
            Direction seed = lookOrder.length > 0 ? lookOrder[0] : Direction.NORTH;
            tryDirs = new Direction[]{seed, leftOf(seed), rightOf(seed)};
        }
        for (Direction dir : tryDirs) {
            if (dir == null) continue;
            ChunkPos first = new ChunkPos(start.x + dir.getOffsetX(), start.z + dir.getOffsetZ());
            // Require candidate to be in pool and not behind relative to the initial heading from 'start'
            if (!pool.contains(first)) continue;
            if (!isForwardOrLateral(start, first, dir)) continue;
            if (need <= 1) return new NextChoice(first, dir);
            ChunkPos end = walkTrailRelative(start, first, pool, need - 1, dir);
            if (end != null) return new NextChoice(end, dir);
        }
        return null;
    }

    // Walk forward up to 'steps' steps, allowing left/right turns, but never moving behind relative to the initial heading from 'originStart'.
    private ChunkPos walkTrailRelative(ChunkPos originStart, ChunkPos start, Set<ChunkPos> pool, int steps, Direction heading) {
        ChunkPos current = start;
        Direction dir = heading;
        int advanced = 0;
        while (advanced < steps) {
            // straight
            ChunkPos straight = new ChunkPos(current.x + dir.getOffsetX(), current.z + dir.getOffsetZ());
            if (pool.contains(straight) && isForwardOrLateral(originStart, straight, heading)) { current = straight; advanced++; continue; }
            // try lateral (left then right)
            boolean moved = false;
            for (Direction turn : new Direction[]{leftOf(dir), rightOf(dir)}) {
                if (turn == null) continue;
                ChunkPos next = new ChunkPos(current.x + turn.getOffsetX(), current.z + turn.getOffsetZ());
                // Keep projection non-negative along the original heading from originStart
                if (pool.contains(next) && isForwardOrLateral(originStart, next, heading)) { current = next; dir = turn; advanced++; moved = true; break; }
            }
            if (!moved) break;
        }
        return advanced > 0 ? current : null;
    }

    private Direction leftOf(Direction d) {
        return switch (d) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST  -> Direction.NORTH;
            case WEST  -> Direction.SOUTH;
            default -> null;
        };
    }

    private Direction rightOf(Direction d) {
        return switch (d) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST  -> Direction.SOUTH;
            case WEST  -> Direction.NORTH;
            default -> null;
        };
    }

    // removed radius-based nearest; we only follow the contiguous trail now

    private boolean hasChain(ChunkPos from, Set<ChunkPos> pool, int remaining, ChunkPos avoid, Direction forward, ChunkPos originStart) {
        if (remaining <= 0) return true;
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            ChunkPos n = new ChunkPos(from.x + dir.getOffsetX(), from.z + dir.getOffsetZ());
            if (avoid != null && n.equals(avoid)) continue;
            if (forward != null && !isForwardOrLateral(originStart, n, forward)) continue;
            if (pool.contains(n)) {
                if (hasChain(n, pool, remaining - 1, from, forward, originStart)) return true;
            }
        }
        return false;
    }

    private boolean hasChainLinear(ChunkPos from, Set<ChunkPos> pool, int remaining, Direction dir, Direction forward, ChunkPos originStart) {
        if (remaining <= 0) return true;
        ChunkPos next = new ChunkPos(from.x + dir.getOffsetX(), from.z + dir.getOffsetZ());
        if (!pool.contains(next)) return false;
        if (forward != null && !isForwardOrLateral(originStart, next, forward)) return false;
        return hasChainLinear(next, pool, remaining - 1, dir, forward, originStart);
    }

	private boolean isWithinChunk(ChunkPos chunk, BlockPos pos) {
		return (pos.getX() >> 4) == chunk.x && (pos.getZ() >> 4) == chunk.z;
	}

    private void setGoalForChunk(ChunkPos cp) {
        if (mc.world == null) return;
        int cx = cp.getCenterX();
        int cz = cp.getCenterZ();
        // Use GoalXZ only to ignore Y axis completely
        logFollow("Setting Baritone GoalXZ to (" + cx + "," + cz + ")");
        try { baritoneSetGoalXZ(cx, cz); }
        catch (Throwable t) { logFollow("Failed to set GoalXZ: " + t.getClass().getSimpleName() + (t.getMessage() != null ? (" - " + t.getMessage()) : "")); }
    }

    private int getTopYAt(int x, int z) {
        try {
            return mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        } catch (Throwable t) {
            return Math.max(mc.world.getBottomY(), mc.player != null ? mc.player.getBlockY() : 64);
        }
    }

    private Direction[] getLookOrderedDirs() {
        // Order the 4 horizontal directions by alignment with player's look vector
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        if (flat.lengthSquared() < 1e-6) flat = new Vec3d(0, 0, 1);
        final Vec3d dirVec = flat.normalize();
        Direction[] dirs = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        Arrays.sort(dirs, (a, b) -> {
            double da = dot(dirVec, a);
            double db = dot(dirVec, b);
            return Double.compare(db, da); // descending
        });
        return dirs;
    }

    private double dot(Vec3d v, Direction d) {
        int dx = d.getOffsetX();
        int dz = d.getOffsetZ();
        return v.x * dx + v.z * dz;
    }

    // True if candidate is forward or lateral relative to start when projected onto the locked forward dir
    private boolean isForwardOrLateral(ChunkPos start, ChunkPos candidate, Direction forward) {
        int hx = forward.getOffsetX();
        int hz = forward.getOffsetZ();
        int dx = candidate.x - start.x;
        int dz = candidate.z - start.z;
        int projection = dx * hx + dz * hz;
        return projection >= 0;
    }

    private void onUserMovementPress() {
        // Any user movement input should pause auto-follow
        disableAutoFollowDueToInput();
    }

    private void disableAutoFollowDueToInput() {
        // Disable the option exactly like toggling it off and cancel navigation
        if (autoFollow.get()) {
            autoFollow.set(false);
            logFollow("Input detected: auto-follow disabled.");
        }
        try { baritoneCancel(); } catch (Throwable ignored) {}
        currentTarget = null;
        lastHeading = null;
        lastCompletedTarget = null;
    }

    // --- Chunk history utilities ---
    private static final class ChunkVisit { final ChunkPos pos; final long time; ChunkVisit(ChunkPos p, long t) { pos = p; time = t; } }

    private void pushChunkVisit(ChunkPos pos) {
        long now = System.currentTimeMillis();
        // Avoid duplicates when player jitters within same chunk
        if (!chunkHistory.isEmpty() && chunkHistory.getLast().pos.equals(pos)) return;
        chunkHistory.addLast(new ChunkVisit(pos, now));
        while (chunkHistory.size() > CHUNK_HISTORY_SIZE) chunkHistory.removeFirst();
        // Prune by time window for oscillation checks
        while (!chunkHistory.isEmpty() && (now - chunkHistory.getFirst().time) > (OSCILLATION_WINDOW_MS * 2)) {
            chunkHistory.removeFirst();
        }
    }

    private ChunkPos previousVisitedChunk() {
        if (chunkHistory.size() < 2) return null;
        Iterator<ChunkVisit> it = chunkHistory.descendingIterator();
        if (!it.hasNext()) return null; // last = current
        ChunkVisit last = it.next();
        if (!it.hasNext()) return null;
        return it.next().pos;
    }

    // Detect ABAB oscillation pattern within a recent window
    private boolean detectOscillation() {
        if (chunkHistory.size() < 4) return false;
        ChunkVisit[] last4 = new ChunkVisit[4];
        int i = 0;
        for (Iterator<ChunkVisit> it = chunkHistory.descendingIterator(); it.hasNext() && i < 4; i++) {
            last4[i] = it.next();
        }
        // most recent first: last4[0], last4[1], last4[2], last4[3]
        if (i < 4) return false;
        long window = last4[0].time - last4[3].time;
        if (window > OSCILLATION_WINDOW_MS) return false;
        ChunkPos A = last4[0].pos; // current
        ChunkPos B = last4[1].pos;
        ChunkPos C = last4[2].pos;
        ChunkPos D = last4[3].pos;
        // Pattern ABAB where A==C and B==D and A!=B
        return A.equals(C) && B.equals(D) && !A.equals(B);
    }

    // Compute a coarse trend heading (cardinal) from recent moves
    private Direction trendHeadingFromHistory() {
        if (chunkHistory.size() < 2) return lastHeading; // fallback to last known heading
        int samples = 0, sx = 0, sz = 0;
        ChunkVisit prev = null;
        for (ChunkVisit cv : chunkHistory) {
            if (prev != null) {
                int dx = cv.pos.x - prev.pos.x;
                int dz = cv.pos.z - prev.pos.z;
                if (dx != 0 || dz != 0) { sx += Integer.signum(dx); sz += Integer.signum(dz); samples++; }
            }
            prev = cv;
        }
        if (samples == 0) return lastHeading;
        // Decide dominant axis & sign
        if (Math.abs(sx) > Math.abs(sz)) return sx >= 0 ? Direction.EAST : Direction.WEST;
        if (Math.abs(sz) > 0) return sz >= 0 ? Direction.SOUTH : Direction.NORTH;
        return lastHeading;
    }

    private boolean isMovementKey(int key) {
        return mc.options.forwardKey.matchesKey(key, 0)
            || mc.options.backKey.matchesKey(key, 0)
            || mc.options.leftKey.matchesKey(key, 0)
            || mc.options.rightKey.matchesKey(key, 0)
            || mc.options.sneakKey.matchesKey(key, 0)
            || mc.options.jumpKey.matchesKey(key, 0)
            || mc.options.sprintKey.matchesKey(key, 0)
            || mc.options.attackKey.matchesKey(key, 0)
            || mc.options.useKey.matchesKey(key, 0);
    }

    private boolean isMovementButton(int button) {
        return mc.options.forwardKey.matchesMouse(button)
            || mc.options.backKey.matchesMouse(button)
            || mc.options.leftKey.matchesMouse(button)
            || mc.options.rightKey.matchesMouse(button)
            || mc.options.sneakKey.matchesMouse(button)
            || mc.options.jumpKey.matchesMouse(button)
            || mc.options.sprintKey.matchesMouse(button)
            || mc.options.attackKey.matchesMouse(button)
            || mc.options.useKey.matchesMouse(button);
    }

    // no longer needed: anyMovementInputPressed removed; input disables setting immediately

    // Helper to carry both next chunk and chosen heading
    private static final class NextChoice {
        final ChunkPos chunk; final Direction heading;
        NextChoice(ChunkPos c, Direction h) { this.chunk = c; this.heading = h; }
    }

    private void logoutClient(String reason) {
        // Multiplayer: disconnect via network connection
        try {
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
                mc.getNetworkHandler().getConnection().disconnect(Text.of("[NewerNewChunks] " + reason));
                return;
            }
        } catch (Throwable ignored) {}
        // Singleplayer or unknown: try world disconnect (handle signature differences across versions)
        try {
            if (mc.world != null) {
                try {
                    // Newer mappings: disconnect(Text) via reflection
                    mc.world.getClass().getMethod("disconnect", Text.class)
                        .invoke(mc.world, Text.of("[NewerNewChunks] " + reason));
                } catch (Throwable t) {
                    // Older mappings: disconnect() via reflection
                    try { mc.world.getClass().getMethod("disconnect").invoke(mc.world); } catch (Throwable ignored2) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    private void baritoneSetGoalXZ(int x, int z) throws Exception {
        Class<?> api = Class.forName("baritone.api.BaritoneAPI");
        Object provider = api.getMethod("getProvider").invoke(null);
        Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
        if (baritone == null) return;

        Object goalProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
        // Support Goal and IGoal
        Class<?> goalIface;
        try { goalIface = Class.forName("baritone.api.pathing.goals.Goal"); }
        catch (ClassNotFoundException e) { goalIface = Class.forName("baritone.api.pathing.goals.IGoal"); }

        // Prefer GoalXZ
        Class<?> goalXZ = Class.forName("baritone.api.pathing.goals.GoalXZ");
        Object goal = goalXZ.getConstructor(int.class, int.class).newInstance(x, z);
        try {
            goalProcess.getClass().getMethod("setGoalAndPath", goalIface).invoke(goalProcess, goal);
        } catch (NoSuchMethodException e) {
            goalProcess.getClass().getMethod("setGoal", goalIface).invoke(goalProcess, goal);
            try { goalProcess.getClass().getMethod("path").invoke(goalProcess); } catch (NoSuchMethodException ignored) {}
        }
    }

    private void baritoneCancel() throws Exception {
        Class<?> api = Class.forName("baritone.api.BaritoneAPI");
        Object provider = api.getMethod("getProvider").invoke(null);
        Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
        if (baritone == null) return;
        Object pathing = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
        try {
            pathing.getClass().getMethod("cancelEverything").invoke(pathing);
            logFollow("Cancelled Baritone pathing (cancelEverything).");
        } catch (NoSuchMethodException e) {
            // Older/newer variants
            try { pathing.getClass().getMethod("forceCancel").invoke(pathing); logFollow("Cancelled Baritone pathing (forceCancel)."); } catch (NoSuchMethodException ignored) {}
        }
    }

    private boolean baritoneAvailable() {
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            Object provider = api.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            return baritone != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // --- Logging helpers ---
    private boolean lastPoolEmptyLogged = false;
    private void logFollowOnce(String msg) {
        if (!followLogging.get()) return;
        if (!lastPoolEmptyLogged) {
            info("[Follow] " + msg);
            lastPoolEmptyLogged = true;
        }
    }
    private void logFollow(String msg) {
        if (!followLogging.get()) return;
        // reset the one-time flag on any positive event
        lastPoolEmptyLogged = false;
        info("[Follow] " + msg);
    }

}
