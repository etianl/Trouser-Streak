package pwn.noobs.trouserstreak.modules;

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
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.AcknowledgeChunksC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executor;
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
	private final SettingGroup specialGroup = settings.createGroup("Disable Pre 1.17 Chunk Detector if server version <1.17");
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgCdata = settings.createGroup("Saved Chunk Data");
	private final SettingGroup sgcacheCdata = settings.createGroup("Cached Chunk Data");
	private final SettingGroup sgRender = settings.createGroup("Render");

	private final Setting<Boolean> oldchunksdetector = specialGroup.add(new BoolSetting.Builder()
			.name("Pre 1.17 OldChunk Detector")
			.description("Marks chunks as old if they have no copper above Y 0 and are in the overworld. For detecting oldchunks in servers updated from old version.")
			.defaultValue(false)
			.build()
	);
	public final Setting<DetectMode> detectmode = sgGeneral.add(new EnumSetting.Builder<DetectMode>()
			.name("Chunk Detection Mode")
			.description("Anything other than normal is for old servers where build limits are being increased due to updates.")
			.defaultValue(DetectMode.Normal)
			.build()
	);
	private final Setting<Boolean> byteexploit = sgGeneral.add(new BoolSetting.Builder()
			.name("ByteExploit")
			.description("Detects new chunks by scanning the order of chunk section palettes, and also by checking the capacity of the writer index of chunks.")
			.defaultValue(true)
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
			.description("Color of the chunks that are (most likely) completely new.")
			.defaultValue(new SettingColor(255, 0, 0, 95))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
			.build()
	);
	private final Setting<SettingColor> tickexploitChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("BlockExploitChunks-side-color")
			.description("MAY POSSIBLY BE OLD. Color of the chunks that have been triggered via block ticking packets")
			.defaultValue(new SettingColor(0, 0, 255, 75))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) && detectmode.get()== DetectMode.BlockExploitMode && blockupdateexploit.get())
			.build()
	);

	private final Setting<SettingColor> oldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-side-color")
			.description("Color of the chunks that have (most likely) been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 40))
			.visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> newChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("new-chunks-line-color")
			.description("Color of the chunks that are (most likely) completely new.")
			.defaultValue(new SettingColor(255, 0, 0, 205))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
			.build()
	);
	private final Setting<SettingColor> tickexploitChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("BlockExploitChunks-line-color")
			.description("MAY POSSIBLY BE OLD. Color of the chunks that have been triggered via block ticking packets")
			.defaultValue(new SettingColor(0, 0, 255, 170))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both) && detectmode.get()== DetectMode.BlockExploitMode && blockupdateexploit.get())
			.build()
	);

	private final Setting<SettingColor> oldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-line-color")
			.description("Color of the chunks that have (most likely) been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 100))
			.visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
			.build()
	);
	private final Executor taskExecutor = Executors.newSingleThreadExecutor();
	private int deletewarningTicks=666;
	private int deletewarning=0;
	private String serverip;
	private String world;
	private ChunkPos chunkPos;
	private ChunkPos oldpos;
	private boolean isNewGeneration;
	private boolean foundAnyOre;

	private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());
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
	public static int tickexploitchunksfound=0;
	private final Set<Integer> newChunkWidxValues = new HashSet<>(Arrays.asList(
			288, 6444, 4392, 2340, 8496, 11280, 11276, 11290, 10562,
			11282, 11270, 11278, 11274, 10548, 10556, 10576, 11272, 11286,
			11292, 13340, 8504, 11284, 10580, 10578,
			11298, 8512, 11266, 11288, 11294, 13332, 9240, 10584, 8516,
			10564, 11980, 13334, 8510, 10574, 10590, 8524, 9232, 9246,
			10554, 10568, 13338, 11262, 11268, 13336, 8502, 10570, 11302,
			8520, 10586, 10588, 11258, 8508, 9236,
			10566, 10560, 10582, 11296, 11976, 10592, 11264, 13330, 8514,
			10558, 10572, 10598, 13326, 8526, 8528, 10600, 11300, 11982,
			8538, 9222, 11992, 11984, 8518, 10594, 13328, 8506, 13342, 9226, 9926,
			13348, 13350, 8530, 8536, 9224, 9230, 9234, 12630, 13318,
			13322, 13324, 8532, 8540, 9244, 9928, 9938, 10552, 11304,
			11978, 12632, 13346, 8534, 9214, 9218, 9228, 9238, 9940,
			8522, 11986, 11260, 13344, 9242, 11988, 10596, 13320, 6458,
			9256, 11994, 12636, 13314, 8546, 9934, 10612, 11306, 11308,
			11974, 11990, 11996, 13352, 13354, 9220, 9930, 12620, 12628, 8498, 9216, 12626, 13316, 13358,
			13360, 6446, 8542, 8548, 9210, 9248, 9250, 9946, 10604,
			11998, 12624, 12640, 12648, 14702, 15384, 15414,
			8544, 11970, 6450, 11256, 12612, 6454, 9212, 9932, 11320,
			4400, 6452, 6460, 6462, 9252, 12000, 12002, 15422, 6448,
			6470, 9936, 10550, 10602, 11271, 12614, 12618, 14082, 15388,
			6456, 6476, 7174, 9920, 9924, 10549, 10620, 11299, 12004,
			12608, 12622, 12634, 12642, 14036, 14040, 15402, 15410, 15412,
			15418, 15434, 15990, 16142, 16746, 16750, 16774, 16786, 16846,
			17440, 17442, 17476, 17488, 17500, 18178, 18182, 19542, 19560,
			19626, 20264, 20970, 20972, 21700, 22188, 22328, 23068, 2342,
			2346, 4398, 4420, 4436, 5132, 5138, 6466, 6472, 6490, 6492,
			7180, 7184, 7192, 7876, 7890, 8500, 8550, 9208, 9258, 9916,
			9942, 9944, 13356, 9922, 15420, 6474, 9948, 9964, 11972, 12604, 16100,
			18230, 14050, 15396, 14722, 6468, 9950, 14056, 16110, 6464,
			7158, 9260, 9918, 12006, 12638, 14038, 14052, 14692, 15428,
			15438, 17492, 19606, 2344, 6478, 6480, 6484, 6488, 7170,
			7190, 9254, 9912, 9956, 10547, 10565, 11277, 11283, 11293,
			11295, 11303, 11310, 11312, 11326, 11966, 11968, 11989, 12010,
			12016, 12020, 12610, 12644, 12646, 12660, 12694, 13312, 13335,
			14044, 14688, 14708, 14730, 15390, 15430, 16118, 16146, 16748,
			17450, 17512, 18196, 18222, 18228, 18832, 18908, 18916, 18938,
			20276, 20980, 21664, 23016, 2350, 25114, 4402, 4408, 5120,
			5122, 5152, 6459, 6504, 7154, 7156, 7160, 7162, 7196, 8495,
			8521, 8552, 9200, 9206, 9262, 9276, 9960, 9962, 9970,
			12616, 15406, 7176, 12008, 13310, 17470, 18924, 9952, 11316,
			11962, 12652, 13366, 14032, 14064, 14706, 16134, 16842, 17472,
			17480, 18172, 18904, 20236, 20254, 2358, 6482, 7182, 7870,
			8497, 8572, 10603, 10622, 11254, 11275, 11279, 11281, 12012,
			12606, 12650, 12658, 12674, 13337, 13378, 14034, 14042, 14062,
			14092, 14690, 14700, 14716, 14720, 14776, 14800, 14806, 15392,
			15404, 15408, 15416, 15432, 15972, 16104, 16106, 16108, 16126,
			16128, 16144, 16722, 16738, 16766, 16818, 16828, 16848, 17452,
			17456, 17464, 17468, 17486, 17490, 17504, 17514, 18200, 18202,
			18204, 18206, 18866, 18878, 18900, 18918, 18922, 19480, 19512,
			19524, 19540, 19548, 19570, 19600, 19616, 20244, 20266, 20278,
			20952, 20962, 20966, 21546, 21582, 21724, 21726, 22244, 22950,
			23054, 24498, 25122, 4394, 4410, 5130, 5140, 6445, 6486, 6494,
			6496, 7164, 7166, 7168, 7172, 7178, 7186, 7194, 7200, 7894,
			8556, 8558, 8566, 9264, 9910, 16124, 11252, 11301, 15398, 2352, 7188, 11273, 11291, 11870,
			12024, 12670, 12712, 13308, 13364, 13370, 13944, 14024, 14076,
			14078, 14086, 14106, 14676, 14714, 14732, 14782, 15370, 15378,
			15400, 15436, 16162, 16752, 16756, 16758, 16768, 16854, 17422,
			17438, 17482, 17508, 17524, 17526, 18174, 18812, 18882, 18910,
			18928, 19528, 19536, 19556, 19580, 20116, 20262, 20272, 21604,
			2354, 2360, 23748, 24374, 25080, 4406, 4422, 4432, 4434, 5126,
			5134, 7204, 9204, 9914
	));

	public NewerNewChunks() {
		super(Trouser.Main,"NewerNewChunks", "Detects new chunks by scanning the order of chunk section palettes, and also by checking the capacity of the writer index of chunks. Can also check liquid flow, and block ticking packets.");
	}
	@Override
	public void onActivate() {
		if (autoreload.get()) {
			newChunks.clear();
			oldChunks.clear();
			tickexploitChunks.clear();
		}
		if (mc.isInSingleplayer()==true){
			String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
			serverip=array[array.length-2];
			world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
		} else {
			serverip = mc.getCurrentServerEntry().address.replace(':', '_');}
		world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
		if (save.get()){
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world).mkdirs();
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
			newChunks.clear();
			oldChunks.clear();
			tickexploitChunks.clear();
		}
		super.onDeactivate();
	}
	@EventHandler
	private void onScreenOpen(OpenScreenEvent event) {
		if (event.screen instanceof DisconnectedScreen) {
			chunkcounterticks=0;
			newchunksfound=0;
			oldchunksfound=0;
			tickexploitchunksfound=0;
			if (worldleaveremove.get()) {
				newChunks.clear();
				oldChunks.clear();
				tickexploitChunks.clear();
			}
		}
		if (event.screen instanceof DownloadingTerrainScreen) {
			chunkcounterticks=0;
			newchunksfound=0;
			oldchunksfound=0;
			tickexploitchunksfound=0;
			reloadworld=0;
		}
	}
	@EventHandler
	private void onGameLeft(GameLeftEvent event) {
		chunkcounterticks=0;
		newchunksfound=0;
		oldchunksfound=0;
		tickexploitchunksfound=0;
		if (worldleaveremove.get()) {
			newChunks.clear();
			oldChunks.clear();
			tickexploitChunks.clear();
		}
	}

	@EventHandler
	private void onPreTick(TickEvent.Pre event) {
		if (mc.player.getHealth()==0) {
			chunkcounterticks=0;
			newchunksfound=0;
			oldchunksfound=0;
			tickexploitchunksfound=0;
			reloadworld=0;
		}
		if (deletewarningTicks<=100) deletewarningTicks++;
		else deletewarning=0;
		if (deletewarning>=2){
			newChunks.clear();
			oldChunks.clear();
			tickexploitChunks.clear();
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/NewChunkData.txt").delete();
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldChunkData.txt").delete();
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
		if (!Files.exists(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldChunkData.txt"))){
			File file = new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldChunkData.txt");
			try {
				file.createNewFile();
			} catch (IOException e) {}
		}
		if (!Files.exists(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/NewChunkData.txt"))){
			File file = new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/NewChunkData.txt");
			try {
				file.createNewFile();
			} catch (IOException e) {}
		}
		if (!Files.exists(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BlockExploitChunkData.txt"))){
			File file = new File("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BlockExploitChunkData.txt");
			try {
				file.createNewFile();
			} catch (IOException e) {}
		}
		if (chunkcounter=true){
			chunkcounterticks++;
			if (chunkcounterticks>=1){
				chunkcounterticks=0;
				newchunksfound=0;
				oldchunksfound=0;
				tickexploitchunksfound=0;
				chunkcounter=false;}
			if (chunkcounter=true && chunkcounterticks<1){
				try {
					List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldChunkData.txt"));

					for (String line : allLines) {
						oldchunksfound++;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/NewChunkData.txt"));

					for (String line : allLines) {
						newchunksfound++;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BlockExploitChunkData.txt"));

					for (String line : allLines) {
						tickexploitchunksfound++;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if (mc.isInSingleplayer()==true){
			String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
			serverip=array[array.length-2];
			world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
		} else {
			serverip = mc.getCurrentServerEntry().address.replace(':', '_');}
		world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');

		if (autoreload.get()) {
			autoreloadticks++;
			if (autoreloadticks==removedelay.get()*20){
				newChunks.clear();
				oldChunks.clear();
				tickexploitChunks.clear();
				if (load.get()){
					loadData();
				}
			} else if (autoreloadticks>=removedelay.get()*20){
				autoreloadticks=0;
			}
		}
		//autoreload when entering different dimensions
		if (reloadworld<10){
			reloadworld++;
		}
		if (reloadworld==5){
			if (worldleaveremove.get()){
				newChunks.clear();
				oldChunks.clear();
				tickexploitChunks.clear();
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
				chunkPos = new ChunkPos(pos);
				if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
					for (Direction dir: searchDirs) {
						if (mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && (!newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
							if (tickexploitChunks.contains(chunkPos)) tickexploitChunks.remove(chunkPos);
							newChunks.add(chunkPos);
							if (save.get()){
								saveNewChunkData(chunkPos);
							}
							return;
						}
					}
				}
			});
		}
		else if (!(event.packet instanceof AcknowledgeChunksC2SPacket) && event.packet instanceof BlockUpdateS2CPacket) {
			BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;
			chunkPos = new ChunkPos(packet.getPos());
			if (blockupdateexploit.get()){
				try {
					if (!tickexploitChunks.contains(chunkPos) && !oldChunks.contains(chunkPos) && !newChunks.contains(chunkPos)){
						tickexploitChunks.add(chunkPos);
						if (save.get()){
							saveBlockExploitChunkData(chunkPos);
						}
					}
				}
				catch (Exception e){
					e.printStackTrace();
				}
			}
			if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill() && liquidexploit.get()) {
				for (Direction dir: searchDirs) {
					if (mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && (!newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
						if (tickexploitChunks.contains(chunkPos)) tickexploitChunks.remove(chunkPos);
						newChunks.add(chunkPos);
						if (save.get()){
							saveNewChunkData(chunkPos);
						}
						return;
					}
				}
			}
		}
		else if (event.packet instanceof ChunkDataS2CPacket && mc.world != null) {
			ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;
			oldpos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());

			if (mc.world.getChunkManager().getChunk(packet.getChunkX(), packet.getChunkZ()) == null) {
				WorldChunk chunk = new WorldChunk(mc.world, oldpos);
				try {
					taskExecutor.execute(() -> chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getChunkX(), packet.getChunkZ())));
				} catch (ArrayIndexOutOfBoundsException e) {
					return;
				}
				PacketByteBuf buf = packet.getChunkData().getSectionsDataBuf();
				int widx = buf.writerIndex();
				if ((mc.world.getRegistryKey() == World.OVERWORLD || mc.world.getRegistryKey() == World.NETHER || mc.world.getRegistryKey() == World.END) && byteexploit.get()) {
					boolean isNewChunk = false;

					if (mc.world.getRegistryKey() == World.END){
						//if (!newChunkWidxValues.contains(widx)) System.out.println("widx: " + widx);

						if (newChunkWidxValues.contains(widx)) isNewChunk = true;

						if (buf.readableBytes() < 1) return; // Ensure we have at least 3 bytes (short + byte)

						buf.readShort(); // Skip block count

						// Block palette
						int blockBitsPerEntry = buf.readUnsignedByte();
						if (blockBitsPerEntry >= 4 && blockBitsPerEntry <= 8) {
							// Indirect palette
							int blockPaletteLength = buf.readVarInt();
							//System.out.println("Block palette length: " + blockPaletteLength);
							for (int i = 0; i < blockPaletteLength; i++) {
								int blockPaletteEntry = buf.readVarInt();
								//System.out.println("Block palette entry " + i + ": " + blockPaletteEntry);
							}

							// Skip block data array
							int blockDataArrayLength = buf.readVarInt();
							int bytesToSkip = blockDataArrayLength * 8; // Each entry is a long (8 bytes)
							if (buf.readableBytes() >= bytesToSkip) {
								buf.skipBytes(bytesToSkip);
							} else {
								//System.out.println("Not enough data for block array, skipping remaining: " + buf.readableBytes());
								buf.skipBytes(buf.readableBytes());
								return; // Exit early as we don't have biome data
							}
						}

						// Check if we have enough data for biome information
						if (buf.readableBytes() < 1) {
							//System.out.println("No biome data available");
							return;
						}

						// Biome palette
						int biomeBitsPerEntry = buf.readUnsignedByte();
						if (biomeBitsPerEntry >= 0 && biomeBitsPerEntry <= 3) {
							// Indirect palette
							int biomePaletteLength = buf.readVarInt();
							//System.out.println("Biome palette length: " + biomePaletteLength);

							int biomePaletteEntry = buf.readVarInt();
							if (biomePaletteEntry != 0) isNewChunk = true;
							//System.out.println("Biome palette entry " + i + ": " + biomePaletteEntry);
						} else {
							//System.out.println("Invalid biome bits per entry: " + biomeBitsPerEntry);
							return;
						}

					} else {
						if (buf.readableBytes() < 1) return; // Ensure we have at least 3 bytes (short + byte)


						buf.readShort(); // Skip block count

						// Block palette
						int blockBitsPerEntry = buf.readUnsignedByte();
						if (blockBitsPerEntry >= 4 && blockBitsPerEntry <= 8) {
							// Indirect palette
							int blockPaletteLength = buf.readVarInt();
							//System.out.println("Block palette length: " + blockPaletteLength);
							int blockPaletteEntry = buf.readVarInt();
							if (blockPaletteEntry == 0) isNewChunk = true;
							//System.out.println("Block palette entry " + i + ": " + blockPaletteEntry);
						}
					}

					if (isNewChunk == false) {
						try {
							if (!tickexploitChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								oldChunks.add(oldpos);
								if (save.get()) {
									saveOldChunkData(oldpos);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}//>0 works for flat overworld
					} else if (isNewChunk == true) {
						try {
							if (!tickexploitChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								newChunks.add(oldpos);
								if (save.get()) {
									saveNewChunkData(oldpos);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				isNewGeneration = false;
				foundAnyOre = false;
				if (liquidexploit.get() || oldchunksdetector.get()) {
					for (int x = 0; x < 16; x++) {
						for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
							for (int z = 0; z < 16; z++) {
								if (liquidexploit.get()) {
									FluidState fluid = chunk.getFluidState(x, y, z);
									if (!oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos) && !fluid.isEmpty() && !fluid.isStill()) {
										oldChunks.add(oldpos);
										if (save.get()){
											saveOldChunkData(oldpos);
										}
										return;
									}
								}
								if (!foundAnyOre && isOreBlock(chunk.getBlockState(new BlockPos(x, y, z)).getBlock()) && mc.world.getRegistryKey().getValue().toString().toLowerCase().contains("overworld")) foundAnyOre = true;
								if (!isNewGeneration && y<256 && y>=0 && (chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.COPPER_ORE || chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.DEEPSLATE_COPPER_ORE) && mc.world.getRegistryKey().getValue().toString().toLowerCase().contains("overworld")) isNewGeneration = true;
							}
						}
					}
				}
				if (oldchunksdetector.get() && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos) && foundAnyOre == true && isNewGeneration == false && mc.world.getRegistryKey().getValue().toString().toLowerCase().contains("overworld")) {
					oldChunks.add(oldpos);
					if (save.get()){
						saveOldChunkData(oldpos);
					}
				}
			}
		}
	}

	private boolean isOreBlock(Block block) {
		return block == Blocks.COAL_ORE
				|| block == Blocks.COPPER_ORE
				|| block == Blocks.DEEPSLATE_COPPER_ORE
				|| block == Blocks.IRON_ORE
				|| block == Blocks.DEEPSLATE_IRON_ORE
				|| block == Blocks.GOLD_ORE
				|| block == Blocks.DEEPSLATE_GOLD_ORE
				|| block == Blocks.LAPIS_ORE
				|| block == Blocks.DEEPSLATE_LAPIS_ORE
				|| block == Blocks.DIAMOND_ORE
				|| block == Blocks.DEEPSLATE_DIAMOND_ORE
				|| block == Blocks.REDSTONE_ORE
				|| block == Blocks.DEEPSLATE_REDSTONE_ORE
				|| block == Blocks.EMERALD_ORE
				|| block == Blocks.DEEPSLATE_EMERALD_ORE;
	}
	private void loadData() {
		try {
			List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldChunkData.txt"));

			for (String line : allLines) {
				String s = line;
				if (s !=null){
					String[] array = s.split(", ");
					if (array.length==2) {
						int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]", ""));
						int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]", ""));
						oldpos = new ChunkPos(X, Z);
						if (!oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
							oldChunks.add(oldpos);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/NewChunkData.txt"));

			for (String line : allLines) {
				String s = line;
				if (s !=null){
					String[] array = s.split(", ");
					if (array.length==2) {
						int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]", ""));
						int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]", ""));
						chunkPos = new ChunkPos(X, Z);
						if (!tickexploitChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos)) {
							newChunks.add(chunkPos);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BlockExploitChunkData.txt"));

			for (String line : allLines) {
				String s = line;
				if (s !=null){
					String[] array = s.split(", ");
					if (array.length==2) {
						int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]", ""));
						int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]", ""));
						chunkPos = new ChunkPos(X, Z);
						if (!tickexploitChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos)) {
							tickexploitChunks.add(chunkPos);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void saveNewChunkData(ChunkPos chunkpos) {
		try {
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world).mkdirs();
			FileWriter writer = new FileWriter("TrouserStreak/NewChunks/"+serverip+"/"+world+"/NewChunkData.txt", true);
			writer.write(String.valueOf(chunkpos));
			writer.write("\r\n");   // write new line
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void saveOldChunkData(ChunkPos chunkpos) {
		try {
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world).mkdirs();
			FileWriter writer = new FileWriter("TrouserStreak/NewChunks/"+serverip+"/"+world+"/OldChunkData.txt", true);
			writer.write(String.valueOf(chunkpos));
			writer.write("\r\n");   // write new line
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void saveBlockExploitChunkData(ChunkPos chunkpos) {
		try {
			new File("TrouserStreak/NewChunks/"+serverip+"/"+world).mkdirs();
			FileWriter writer = new FileWriter("TrouserStreak/NewChunks/"+serverip+"/"+world+"/BlockExploitChunkData.txt", true);
			writer.write(String.valueOf(chunkpos));
			writer.write("\r\n");   // write new line
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}