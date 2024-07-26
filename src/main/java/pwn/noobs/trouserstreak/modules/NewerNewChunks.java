package pwn.noobs.trouserstreak.modules;

import io.netty.buffer.Unpooled;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.AcknowledgeChunksC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private final SettingGroup specialGroup = settings.createGroup("Disable PaletteExploit if server version <1.18");
	private final SettingGroup specialGroup2 = settings.createGroup("Detection for chunks that were generated in old versions.");
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgCdata = settings.createGroup("Saved Chunk Data");
	private final SettingGroup sgcacheCdata = settings.createGroup("Cached Chunk Data");
	private final SettingGroup sgRender = settings.createGroup("Render");

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
			.visible(() -> load.get())
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

	// render
	public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
			.name("Render-Distance(Chunks)")
			.description("How many chunks from the character to render the detected chunks.")
			.defaultValue(128)
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
	private final ExecutorService taskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
	private int reloadworld=0;
	public int chunkcounterticks=0;
	public static boolean chunkcounter;
	public static int newchunksfound=0;
	public static int oldchunksfound=0;
	public static int beingUpdatedOldChunksfound=0;
	public static int OldGenerationOldChunksfound=0;
	public static int tickexploitchunksfound=0;
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
	private static final Set<Block> NEW_OVERWORLD_BLOCKS = new HashSet<>();
	static {
		NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.AMETHYST_BLOCK);
		NEW_OVERWORLD_BLOCKS.add(Blocks.AZALEA);
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
		NEW_OVERWORLD_BLOCKS.add(Blocks.POINTED_DRIPSTONE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.SMOOTH_BASALT);
		NEW_OVERWORLD_BLOCKS.add(Blocks.TUFF);
		NEW_OVERWORLD_BLOCKS.add(Blocks.CALCITE);
		NEW_OVERWORLD_BLOCKS.add(Blocks.HANGING_ROOTS);
		NEW_OVERWORLD_BLOCKS.add(Blocks.ROOTED_DIRT);
		NEW_OVERWORLD_BLOCKS.add(Blocks.AZALEA_LEAVES);
		NEW_OVERWORLD_BLOCKS.add(Blocks.FLOWERING_AZALEA_LEAVES);
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
		NEW_NETHER_BLOCKS.add(Blocks.WARPED_NYLIUM);
		NEW_NETHER_BLOCKS.add(Blocks.CRIMSON_NYLIUM);
		NEW_NETHER_BLOCKS.add(Blocks.TWISTING_VINES);
		NEW_NETHER_BLOCKS.add(Blocks.WEEPING_VINES);
		NEW_NETHER_BLOCKS.add(Blocks.BONE_BLOCK);
		NEW_NETHER_BLOCKS.add(Blocks.CHAIN);
	}
	Set<String> FILE_NAMES = new HashSet<>(Set.of(
			"OldChunkData.txt",
			"BeingUpdatedChunkData.txt",
			"OldGenerationChunkData.txt",
			"NewChunkData.txt",
			"BlockExploitChunkData.txt"
	));
	public NewerNewChunks() {
		super(Trouser.Main,"NewerNewChunks", "Detects new chunks by scanning the order of chunk section palettes. Can also check liquid flow, and block ticking packets.");
	}
	private void resetCounterValues() {
		chunkcounterticks=0;
		newchunksfound=0;
		oldchunksfound=0;
		beingUpdatedOldChunksfound=0;
		OldGenerationOldChunksfound=0;
		tickexploitchunksfound=0;
	}
	private void clearChunkData() {
		newChunks.clear();
		oldChunks.clear();
		beingUpdatedOldChunks.clear();
		OldGenerationOldChunks.clear();
		tickexploitChunks.clear();
	}
	@Override
	public void onActivate() {
		if (autoreload.get()) {
			clearChunkData();
		}
		if (save.get() || load.get()) {
			world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
			if (mc.isInSingleplayer()){
				String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
				serverip=array[array.length-2];
			} else {
				serverip = mc.getCurrentServerEntry().address.replace(':', '_');
			}
		}
		if (save.get()){
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world).mkdirs();
		}
		if (save.get() || load.get()) {
			for (String fileName : FILE_NAMES) {
				String fullPath = "TrouserStreak/NewChunks/" + serverip + "/" + world + "/" + fileName;
				if (!Files.exists(Paths.get(fullPath))) {
					File file = new File(fullPath);
					try {
						file.createNewFile();
					} catch (IOException e) {
						//e.printStackTrace();
					}
				}
			}
		}
		if (load.get()){
			loadData();
		}
		autoreloadticks=0;
		loadingticks=0;
		reloadworld=0;
	}

	@Override
	public void onDeactivate() {
		chunkcounterticks=0;
		autoreloadticks=0;
		loadingticks=0;
		reloadworld=0;
		if (remove.get()|autoreload.get()) {
			clearChunkData();
		}
		super.onDeactivate();
	}
	@EventHandler
	private void onScreenOpen(OpenScreenEvent event) {
		if (event.screen instanceof DisconnectedScreen) {
			resetCounterValues();
			if (worldleaveremove.get()) {
				clearChunkData();
			}
		}
		if (event.screen instanceof DownloadingTerrainScreen) {
			resetCounterValues();
			reloadworld=0;
		}
	}
	@EventHandler
	private void onGameLeft(GameLeftEvent event) {
		resetCounterValues();
		if (worldleaveremove.get()) {
			clearChunkData();
		}
	}
	@EventHandler
	private void onPreTick(TickEvent.Pre event) {
		world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');

		if (mc.player.getHealth()==0) {
			resetCounterValues();
			reloadworld=0;
		}

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
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/NewChunkData.txt").delete();
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldChunkData.txt").delete();
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BeingUpdatedChunkData.txt").delete();
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldGenerationChunkData.txt").delete();
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BlockExploitChunkData.txt").delete();
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
			loadingticks++;
			if (loadingticks<2){
				loadData();
			}
		} else if (!load.get()){
			loadingticks=0;
		}

		if (chunkcounter=true){ //this only runs when the NewChunkCounter command is used
			chunkcounterticks++;
			if (chunkcounterticks>=1){
				resetCounterValues();
				chunkcounter=false;
			}
			if (chunkcounter=true && chunkcounterticks<1){
				try {
					List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldChunkData.txt"));

					for (String line : allLines) {
						oldchunksfound++;
					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
				try {
					List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BeingUpdatedChunkData.txt"));

					for (String line : allLines) {
						beingUpdatedOldChunksfound++;
					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
				try {
					List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldGenerationChunkData.txt"));

					for (String line : allLines) {
						OldGenerationOldChunksfound++;
					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
				try {
					List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/NewChunkData.txt"));

					for (String line : allLines) {
						newchunksfound++;
					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
				try {
					List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BlockExploitChunkData.txt"));

					for (String line : allLines) {
						tickexploitchunksfound++;
					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
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
		//autoreload when entering different dimensions
		if (reloadworld<6){
			reloadworld++;
		}
		if (reloadworld==5){
			for (String fileName : FILE_NAMES) {
				String fullPath = "TrouserStreak/NewChunks/" + serverip + "/" + world + "/" + fileName;
				if (!Files.exists(Paths.get(fullPath))) {
					File file = new File(fullPath);
					try {
						file.createNewFile();
					} catch (IOException e) {
						//e.printStackTrace();
					}
				}
			}
			if (worldleaveremove.get()){
				clearChunkData();
			}
			loadData();
		}
	}
	@EventHandler
	private void onRender(Render3DEvent event) {
		if (newChunksLineColor.get().a > 5 || newChunksSideColor.get().a > 5) {
			synchronized (newChunks) {
				for (ChunkPos c : newChunks) {
					if (c != null && mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), renderDistance.get()*16)) {
						render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), newChunksSideColor.get(), newChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (tickexploitChunksLineColor.get().a > 5 || tickexploitChunksSideColor.get().a > 5) {
			synchronized (tickexploitChunks) {
				for (ChunkPos c : tickexploitChunks) {
					if (c != null && mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), renderDistance.get()*16)) {
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
					if (c != null && mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), renderDistance.get()*16)) {
						render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), oldChunksSideColor.get(), oldChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (beingUpdatedOldChunksLineColor.get().a > 5 || beingUpdatedOldChunksSideColor.get().a > 5){
			synchronized (beingUpdatedOldChunks) {
				for (ChunkPos c : beingUpdatedOldChunks) {
					if (c != null && mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), renderDistance.get()*16)) {
						render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), beingUpdatedOldChunksSideColor.get(), beingUpdatedOldChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (OldGenerationOldChunksLineColor.get().a > 5 || OldGenerationOldChunksSideColor.get().a > 5){
			synchronized (OldGenerationOldChunks) {
				for (ChunkPos c : OldGenerationOldChunks) {
					if (c != null && mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), renderDistance.get()*16)) {
						render(new Box(new Vec3d(c.getStartPos().getX(), c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()), new Vec3d(c.getStartPos().getX()+16, c.getStartPos().getY()+renderHeight.get(), c.getStartPos().getZ()+16)), OldGenerationOldChunksSideColor.get(), OldGenerationOldChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
	}

	private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
		event.renderer.box(
				box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
	}

	@EventHandler
	private void onReadPacket(PacketEvent.Receive event) {
		if (event.packet instanceof AcknowledgeChunksC2SPacket)return;
		if (event.packet instanceof ChunkDeltaUpdateS2CPacket && liquidexploit.get()) {
			ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet;

			packet.visitUpdates((pos, state) -> {
				ChunkPos chunkPos = new ChunkPos(pos);
				if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
					for (Direction dir: searchDirs) {
						try {
							if (mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
								if (tickexploitChunks.contains(chunkPos)) tickexploitChunks.remove(chunkPos);
								newChunks.add(chunkPos);
								if (save.get()){
									saveData("/NewChunkData.txt", chunkPos);
								}
								return;
							}
						} catch (Exception e) {}
					}
				}
			});
		}
		else if (!(event.packet instanceof AcknowledgeChunksC2SPacket) && event.packet instanceof BlockUpdateS2CPacket) {
			BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;
			ChunkPos chunkPos = new ChunkPos(packet.getPos());
			if (blockupdateexploit.get()){
				try {
					if (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !tickexploitChunks.contains(chunkPos) && !oldChunks.contains(chunkPos) && !newChunks.contains(chunkPos)){
						tickexploitChunks.add(chunkPos);
						if (save.get()){
							saveData("/BlockExploitChunkData.txt", chunkPos);
						}
						return;
					}
				}
				catch (Exception e){}
			}
			if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill() && liquidexploit.get()) {
				for (Direction dir: searchDirs) {
					try {
						if (mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
							if (tickexploitChunks.contains(chunkPos)) tickexploitChunks.remove(chunkPos);
							newChunks.add(chunkPos);
							if (save.get()){
								saveData("/NewChunkData.txt", chunkPos);
							}
							return;
						}
					} catch (Exception e) {}
				}
			}
		}
		else if (!(event.packet instanceof PlayerMoveC2SPacket) && event.packet instanceof ChunkDataS2CPacket && mc.world != null) {
			ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;
			ChunkPos oldpos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());

			if (mc.world.getChunkManager().getChunk(packet.getChunkX(), packet.getChunkZ()) == null) {
				WorldChunk chunk = new WorldChunk(mc.world, oldpos);
				try {
					taskExecutor.execute(() -> chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getChunkX(), packet.getChunkZ())));
				} catch (ArrayIndexOutOfBoundsException e) {
					return;
				}
				boolean isNewChunk = false;
				boolean isOldGeneration = false;
				boolean chunkIsBeingUpdated = false;
				boolean foundAnyOre = false;
				boolean isNewOverworldGeneration = false;
				boolean isNewNetherGeneration = false;

				if (overworldOldChunksDetector.get() && mc.world.getRegistryKey() == World.OVERWORLD) {
					for (int x = 0; x < 16; x++) {
						for (int y = mc.world.getBottomY(); y < 260; y++) {
							for (int z = 0; z < 16; z++) {
								if (!foundAnyOre && ORE_BLOCKS.contains(chunk.getBlockState(new BlockPos(x, y, z)).getBlock())) foundAnyOre = true; //prevent false flags in flat world
								if (y > 5 && !isNewOverworldGeneration && NEW_OVERWORLD_BLOCKS.contains(chunk.getBlockState(new BlockPos(x, y, z)).getBlock())) {
									isNewOverworldGeneration = true;
									break;
								}
							}
						}
					}
					if (foundAnyOre && !isOldGeneration && !isNewOverworldGeneration) isOldGeneration = true;
				}
				if (netherOldChunksDetector.get() && mc.world.getRegistryKey() == World.NETHER) {
					for (int x = 0; x < 16; x++) {
						for (int y = mc.world.getBottomY(); y < 128; y++) {
							for (int z = 0; z < 16; z++) {
								if (!isNewNetherGeneration && NEW_NETHER_BLOCKS.contains(chunk.getBlockState(new BlockPos(x, y, z)).getBlock()) && mc.world.getRegistryKey() == World.NETHER) {
									isNewNetherGeneration = true;
									break;
								}
							}
						}
					}
					if (!isOldGeneration && !isNewNetherGeneration) isOldGeneration = true;
				}
				PacketByteBuf buf = null;
				if (PaletteExploit.get() || (endOldChunksDetector.get() && mc.world.getRegistryKey() == World.END))	{
					buf = packet.getChunkData().getSectionsDataBuf();
				}
				if (endOldChunksDetector.get() && mc.world.getRegistryKey() == World.END) {
					PacketByteBuf bufferCopy = new PacketByteBuf(Unpooled.copiedBuffer(buf.nioBuffer())); //copy the packetByteBuf for later use
					if (bufferCopy.readableBytes() < 2) return;

					try {
							short blockCount = bufferCopy.readShort();
							if (bufferCopy.readableBytes() < 1) return;
							int blockBitsPerEntry2 = bufferCopy.readUnsignedByte();
							if (blockBitsPerEntry2 == 0) {
								int singleBlockValue = bufferCopy.readVarInt();
								bufferCopy.readVarInt(); // Data Array Length (should be 0)
							} else if (blockBitsPerEntry2 >= 4 && blockBitsPerEntry2 <= 8) {
								int blockPaletteLength = bufferCopy.readVarInt();
								for (int i = 0; i < blockPaletteLength; i++) {
									int blockPaletteEntry = bufferCopy.readVarInt();
								}
								int blockDataArrayLength = bufferCopy.readVarInt();
								if (bufferCopy.readableBytes() >= blockDataArrayLength * 8) {
									bufferCopy.skipBytes(blockDataArrayLength * 8);
								} else {
									bufferCopy.skipBytes(bufferCopy.readableBytes());
									return;
								}
							} else if (blockBitsPerEntry2 == 15) {
								int blockDataArrayLength = bufferCopy.readVarInt();
								if (bufferCopy.readableBytes() >= blockDataArrayLength * 8) {
									bufferCopy.skipBytes(blockDataArrayLength * 8);
								} else {
									bufferCopy.skipBytes(bufferCopy.readableBytes());
									return;
								}
							} else {
								return;
							}

							if (bufferCopy.readableBytes() < 1) {
								return;
							}

							int biomeBitsPerEntry = bufferCopy.readUnsignedByte();

							if (biomeBitsPerEntry == 0) {
								int singleBiomeValue = bufferCopy.readVarInt();
								//Registry<Biome> biomeRegistry = mc.world.getRegistryManager().get(RegistryKeys.BIOME);
								//Biome biome = biomeRegistry.get(singleBiomeValue);
								//Identifier biomeId = biomeRegistry.getId(biome);
								//System.out.println("Single Biome Value: " + singleBiomeValue + " | Biome: " + biomeId.toString());
								if (singleBiomeValue == 55) {
									isOldGeneration = true;
								}
								bufferCopy.readVarInt(); // Data Array Length (should be 0)
							} else if (biomeBitsPerEntry >= 1 && biomeBitsPerEntry <= 3) {
								int biomePaletteLength = bufferCopy.readVarInt();
								//System.out.println("Biome palette length: " + biomePaletteLength);
								for (int i = 0; i < biomePaletteLength; i++) {
									if (bufferCopy.readableBytes() < 1) {
										//System.out.println("Incomplete biome palette data");
										break;
									}
									int biomePaletteEntry = bufferCopy.readVarInt();
									//Registry<Biome> biomeRegistry = mc.world.getRegistryManager().get(RegistryKeys.BIOME);
									//Biome biome = biomeRegistry.get(biomePaletteEntry);
									//Identifier biomeId = biomeRegistry.getId(biome);
									//System.out.println("Biome palette entry " + i + ": " + biomePaletteEntry + " | Biome: " + biomeId.toString());
									if (biomePaletteEntry == 55) {
										isOldGeneration = true;
										break;
									}
								}
							} else {
								//no need to read if other biomeBitsPerEntry values, we can just return here
								return;
							}
					} catch (Exception e) {
						//e.printStackTrace();
					}
				}

				if (PaletteExploit.get()) {
					if (buf.readableBytes() < 3) return; // Ensure we have at least 3 bytes (short + byte)

					boolean firstchunkappearsnew = false;
					int loops = 0;
					int newChunkQuantifier = 0;
					int oldChunkQuantifier = 0;

					try {
						while (buf.readableBytes() > 0 && loops<8) {
							// Chunk Section structure
							short blockCount = buf.readShort();
							//System.out.println("Section: " + loops + " | Block count: " + blockCount);

							// Block states Paletted Container
							if (buf.readableBytes() < 1) break;
							int blockBitsPerEntry2 = buf.readUnsignedByte();
							//System.out.println("Section: " + loops + " | Block Bits Per Entry: " + blockBitsPerEntry2);

							if (blockBitsPerEntry2 == 0) {
								// Single valued palette
								int singleBlockValue = buf.readVarInt();
								//BlockState blockState = Block.STATE_IDS.get(singleBlockValue);
								//System.out.println("Section: " + loops + " | Single Block Value: " + singleBlockValue + " | Blockstate: " + blockState);
								buf.readVarInt(); // Data Array Length (should be 0)
							} else if (blockBitsPerEntry2 >= 4 && blockBitsPerEntry2 <= 8) {
								ChunkSection section = chunk.getSectionArray()[loops];
								PalettedContainer<BlockState> palettedContainer = section.getBlockStateContainer();
								Set<BlockState> bstates = new HashSet<>();
								for (int x = 0; x < 16; x++){
									for (int y = 0; y < 16; y++){
										for (int z = 0; z < 16; z++){
											bstates.add(palettedContainer.get(x, y, z));
										}
									}
								}
								// Indirect palette
								int blockPaletteLength = buf.readVarInt();
								//System.out.println("Section: " + loops + " | Block palette length: " + blockPaletteLength);
								//System.out.println("Section: " + loops + " | bstates.size() "+bstates.size());
								//System.out.println("Section: " + loops + " | blockPaletteLength"+blockPaletteLength);
								int isNewSection = 0;
								int isBeingUpdatedSection = 0;
								int bstatesSize = bstates.size();
								if (bstatesSize<=1) bstatesSize = blockPaletteLength;
								if (bstatesSize<blockPaletteLength) {
									isNewSection = 2;
									//System.out.println("Section: " + loops + " | smaller bstates size!!!!!!!");
									newChunkQuantifier++; //double the weight of this
								}
								for (int i = 0; i < blockPaletteLength; i++) {
									int blockPaletteEntry = buf.readVarInt();
									//BlockState blockState = Block.STATE_IDS.get(blockPaletteEntry);
									//System.out.println("Section: " + loops + " | Block palette entry " + i + ": " + blockPaletteEntry + " | Blockstate: " + blockState);
									if (i == 0 && loops == 0 && blockPaletteEntry == 0 && mc.world.getRegistryKey() != World.END) firstchunkappearsnew = true;
									if (i == 0 && blockPaletteEntry == 0 && mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END) isNewSection++;
									if (i == 1 && (blockPaletteEntry == 80 || blockPaletteEntry == 1 || blockPaletteEntry == 9 || blockPaletteEntry == 5781) && mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END) isNewSection++;
									if (i == 2 && (blockPaletteEntry == 5781 || blockPaletteEntry == 10 || blockPaletteEntry == 22318) && mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END) isNewSection++;
									if (loops == 4 && blockPaletteEntry == 79 && mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END) {
										//System.out.println("CHUNK IS BEING UPDATED!!!!!!");
										if (!chunkIsBeingUpdated && beingUpdatedDetector.get()) chunkIsBeingUpdated = true;
									}
									if (blockPaletteEntry == 0 && (mc.world.getRegistryKey() == World.NETHER || mc.world.getRegistryKey() == World.END)) isBeingUpdatedSection++;
								}
								if (isBeingUpdatedSection>=2) oldChunkQuantifier++;
								if (isNewSection >= 2) newChunkQuantifier++;

								// Data Array
								int blockDataArrayLength = buf.readVarInt();
								//System.out.println("Section: " + loops + " | Block Data Array Length: " + blockDataArrayLength);
								if (buf.readableBytes() >= blockDataArrayLength * 8) {
									buf.skipBytes(blockDataArrayLength * 8);
								} else {
									//System.out.println("Section: " + loops + " | Not enough data for block array, skipping remaining: " + buf.readableBytes());
									buf.skipBytes(buf.readableBytes());
									break;
								}
							} else if (blockBitsPerEntry2 == 15) {
								// Direct palette (no palette sent)
								int blockDataArrayLength = buf.readVarInt();
								//System.out.println("Section: " + loops + " | Block Data Array Length (Direct): " + blockDataArrayLength);
								if (buf.readableBytes() >= blockDataArrayLength * 8) {
									buf.skipBytes(blockDataArrayLength * 8);
								} else {
									//System.out.println("Section: " + loops + " | Not enough data for block array, skipping remaining: " + buf.readableBytes());
									buf.skipBytes(buf.readableBytes());
									break;
								}
							} else {
								//System.out.println("Section: " + loops + " | Invalid block bits per entry: " + blockBitsPerEntry2);
								break;
							}

							// Biomes Paletted Container
							if (buf.readableBytes() < 1) {
								//System.out.println("Section: " + loops + " | No biome data available");
								break;
							}

							int biomeBitsPerEntry = buf.readUnsignedByte();
							//System.out.println("Section: " + loops + " | Biome Bits Per Entry: " + biomeBitsPerEntry);

							if (biomeBitsPerEntry == 0) {
								// Single valued palette
								int singleBiomeValue = buf.readVarInt();
								//Registry<Biome> biomeRegistry = mc.world.getRegistryManager().get(RegistryKeys.BIOME);
								//Biome biome = biomeRegistry.get(singleBiomeValue);
								//Identifier biomeId = biomeRegistry.getId(biome);
								//System.out.println("Section: " + loops + " | Single Biome Value: " + singleBiomeValue + " | Biome: " + biomeId.toString());
								if (singleBiomeValue == 39 && mc.world.getRegistryKey() == World.END) isNewChunk = true;
								buf.readVarInt(); // Data Array Length (should be 0)
							} else if (biomeBitsPerEntry >= 1 && biomeBitsPerEntry <= 3) {
								// Indirect palette
								int biomePaletteLength = buf.readVarInt();
								//System.out.println("Section: " + loops + " | Biome palette length: " + biomePaletteLength);
								for (int i = 0; i < biomePaletteLength; i++) {
									if (buf.readableBytes() < 1) {
										//System.out.println("Section: " + loops + " | Incomplete biome palette data");
										break;
									}
									int biomePaletteEntry = buf.readVarInt();
									//Registry<Biome> biomeRegistry = mc.world.getRegistryManager().get(RegistryKeys.BIOME);
									//Biome biome = biomeRegistry.get(biomePaletteEntry);
									//Identifier biomeId = biomeRegistry.getId(biome);
									//System.out.println("Section: " + loops + " | Biome palette entry " + i + ": " + biomePaletteEntry + " | Biome: " + biomeId.toString());
									if (i == 0 && biomePaletteEntry == 39 && mc.world.getRegistryKey() == World.END) isNewChunk = true;
									if (!isNewChunk && i == 0 && biomePaletteEntry != 55 && mc.world.getRegistryKey() == World.END) isNewChunk = false;
								}

								// Data Array
								if (buf.readableBytes() >= 1) {
									int biomeDataArrayLength = buf.readVarInt();
									//System.out.println("Section: " + loops + " | Biome Data Array Length: " + biomeDataArrayLength);
									if (buf.readableBytes() >= biomeDataArrayLength * 8) {
										buf.skipBytes(biomeDataArrayLength * 8);
									} else {
										//System.out.println("Section: " + loops + " | Not enough data for biome array, skipping remaining: " + buf.readableBytes());
										buf.skipBytes(buf.readableBytes());
										break;
									}
								} else {
									//System.out.println("Section: " + loops + " | Not enough data for biome array length");
									break;
								}
							} else if (biomeBitsPerEntry == 6) {
								// Direct palette (no palette sent)
								int biomeDataArrayLength = buf.readVarInt();
								//System.out.println("Section: " + loops + " | Biome Data Array Length (Direct): " + biomeDataArrayLength);
								if (buf.readableBytes() >= biomeDataArrayLength * 8) {
									buf.skipBytes(biomeDataArrayLength * 8);
								} else {
									//System.out.println("Section: " + loops + " | Not enough data for biome array, skipping remaining: " + buf.readableBytes());
									buf.skipBytes(buf.readableBytes());
									break;
								}
							} else {
								//System.out.println("Section: " + loops + " | Invalid biome bits per entry: " + biomeBitsPerEntry);
								break;
							}

							loops++;
						}

						//System.out.println("newChunkQuantifier: " + newChunkQuantifier + ", loops: " + loops);
						if (loops > 0) {
							if (beingUpdatedDetector.get() && (mc.world.getRegistryKey() == World.NETHER || mc.world.getRegistryKey() == World.END)){
								double oldpercentage = ((double) oldChunkQuantifier / loops) * 100;
								//System.out.println("Percentage: " + oldpercentage);
								if (oldpercentage >= 25) chunkIsBeingUpdated = true;
							}
							else if (mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END){
								double percentage = ((double) newChunkQuantifier / loops) * 100;
								//System.out.println("Percentage: " + percentage);
								if (percentage >= 65) isNewChunk = true;
							}
						}
					} catch (Exception e) {
						//e.printStackTrace();
						if (beingUpdatedDetector.get() && (mc.world.getRegistryKey() == World.NETHER || mc.world.getRegistryKey() == World.END)){
							double oldpercentage = ((double) oldChunkQuantifier / loops) * 100;
							//System.out.println("Percentage: " + oldpercentage);
							if (oldpercentage >= 25) chunkIsBeingUpdated = true;
						}
						else if (mc.world.getRegistryKey() != World.NETHER && mc.world.getRegistryKey() != World.END){
							double percentage = ((double) newChunkQuantifier / loops) * 100;
							//System.out.println("Percentage: " + percentage);
							if (percentage >= 65) isNewChunk = true;
						}
					}

					if (firstchunkappearsnew) isNewChunk = true;
					boolean bewlian = (mc.world.getRegistryKey() == World.END) ? isNewChunk : !isOldGeneration;
					if (isNewChunk && !chunkIsBeingUpdated && bewlian) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								newChunks.add(oldpos);
								if (save.get()) {
									saveData("/NewChunkData.txt", oldpos);
								}
								return;
							}
						} catch (Exception e) {
							//e.printStackTrace();
						}
					}
					else if (!isNewChunk && !chunkIsBeingUpdated && isOldGeneration) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								OldGenerationOldChunks.add(oldpos);
								if (save.get()){
									saveData("/OldGenerationChunkData.txt", oldpos);
								}
							}
						} catch (Exception e) {
							//e.printStackTrace();
						}
					}
					else if (chunkIsBeingUpdated) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								beingUpdatedOldChunks.add(oldpos);
								if (save.get()){
									saveData("/BeingUpdatedChunkData.txt", oldpos);
								}
							}
						} catch (Exception e) {
							//e.printStackTrace();
						}
					}
					else if (!isNewChunk) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								oldChunks.add(oldpos);
								if (save.get()) {
									saveData("/OldChunkData.txt", oldpos);
								}
								return;
							}
						} catch (Exception e) {
							//e.printStackTrace();
						}
					}
				}
				if (liquidexploit.get()) {
					for (int x = 0; x < 16; x++) {
						for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
							for (int z = 0; z < 16; z++) {
								FluidState fluid = chunk.getFluidState(x, y, z);
								try {
									if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos) && !fluid.isEmpty() && !fluid.isStill()) {
										oldChunks.add(oldpos);
										if (save.get()){
											saveData("/OldChunkData.txt", oldpos);
										}
										return;
									}
								} catch (Exception e) {
									//e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
	}
	private void loadData() {
		loadChunkData("/BlockExploitChunkData.txt", tickexploitChunks);
		loadChunkData("/OldChunkData.txt", oldChunks);
		loadChunkData("/NewChunkData.txt", newChunks);
		loadChunkData("/BeingUpdatedChunkData.txt", beingUpdatedOldChunks);
		loadChunkData("/OldGenerationChunkData.txt", OldGenerationOldChunks);
	}
	private void loadChunkData(String savedDataLocation, Set<ChunkPos> chunkSet) {
		try {
			List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+savedDataLocation));

			for (String line : allLines) {
				String s = line;
				if (s !=null){
					String[] array = s.split(", ");
					if (array.length==2) {
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
			//e.printStackTrace();
		}
	}
	private void saveData(String savedDataLocation, ChunkPos chunkpos) {
		try {
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world).mkdirs();
			FileWriter writer = new FileWriter("TrouserStreak/NewChunks/"+serverip+"/"+world+savedDataLocation, true);
			writer.write(String.valueOf(chunkpos));
			writer.write("\r\n");
			writer.close();
		} catch (IOException e) {
		}
	}
}