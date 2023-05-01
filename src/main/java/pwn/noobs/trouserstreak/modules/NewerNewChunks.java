package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    Ported from: https://github.com/BleachDrinker420/BleachHack/blob/master/BleachHack-Fabric-1.16/src/main/java/bleach/hack/module/mods/NewChunks.java
    updated by etianll :D
*/
public class NewerNewChunks extends Module {
	public enum DetectMode {
		Normal,
		IgnoreFlowBelow0,
		Advanced
	}

	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgCdata = settings.createGroup("Saved Chunk Data");
	private final SettingGroup sgcacheCdata = settings.createGroup("Cached Chunk Data");
	private final SettingGroup sgRender = settings.createGroup("Render");

	// general
	public final Setting<DetectMode> detectmode = sgGeneral.add(new EnumSetting.Builder<DetectMode>()
			.name("Chunk Detection Mode")
			.description("Anything other than normal is for old servers where build limits are being increased due to updates.")
			.defaultValue(DetectMode.Normal)
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
	private final Setting<Boolean> autoremove = sgcacheCdata.add(new BoolSetting.Builder()
			.name("RemoveAutomatically")
			.description("Removes the cached chunks on a delay to help prevent RAM from getting overloaded over time.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Integer> removedelay = sgcacheCdata.add(new IntSetting.Builder()
			.name("AutoRemoveDelayInSeconds")
			.description("Removes the cached chunks on a delay to help prevent RAM from getting overloaded over time.")
			.sliderRange(1,300)
			.defaultValue(15)
			.visible(() -> autoremove.get())
			.build());
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
	private final Setting<Boolean> reload = sgcacheCdata.add(new BoolSetting.Builder()
			.name("LoadSavedChunksAfterAutoRemove")
			.description("Reloads saved chunks from data files after cached data is auto removed. May cause lag if loading too often.)")
			.defaultValue(true)
			.visible(() -> autoremove.get() && load.get())
			.build()
	);
	private final Setting<Boolean> delete = sgCdata.add(new BoolSetting.Builder()
			.name("DeleteChunkData")
			.description("Deletes the saved chunks.")
			.defaultValue(false)
			.build()
	);

	// render
	public final Setting<Integer> renderHeight = sgRender.add(new IntSetting.Builder()
			.name("render-height")
			.description("The height at which new chunks will be rendered")
			.defaultValue(0)
			.min(-64)
			.sliderRange(-64,319)
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
	private final Setting<SettingColor> olderoldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("FlowIsBelowY0-side-color")
			.description("MAY STILL BE NEW. Color of the chunks that have liquids flowing below Y=0")
			.defaultValue(new SettingColor(255, 255, 0, 75))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) && detectmode.get()== DetectMode.Advanced)
			.build()
	);

	private final Setting<SettingColor> oldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-side-color")
			.description("Color of the chunks that have (most likely) been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 55))
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
	private final Setting<SettingColor> olderoldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("FlowIsBelowY0-line-color")
			.description("MAY STILL BE NEW. Color of the chunks that have liquids flowing below Y=0")
			.defaultValue(new SettingColor(255, 255, 0, 170))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both) && detectmode.get()== DetectMode.Advanced)
			.build()
	);

	private final Setting<SettingColor> oldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-line-color")
			.description("Color of the chunks that have (most likely) been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 130))
			.visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
			.build()
	);
	private String serverip;
	private String world;
	private ChunkPos chunkPos;
	private ChunkPos oldpos;
	private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> olderoldChunks = Collections.synchronizedSet(new HashSet<>());
	private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };
	private int autoremoveticks=0;
	private int loadingticks=0;
	private int reloadworld=0;
	public int chunkcounterticks=0;
	public static boolean chunkcounter;
	public static int newchunksfound=0;
	public static int oldchunksfound=0;
	public static int olderoldchunksfound=0;
	public NewerNewChunks() {
		super(Trouser.Main,"NewerNewChunks", "Estimates new chunks by checking liquid flow.");
	}
	@Override
	public void onActivate() {
		if (autoremove.get()) {
			newChunks.clear();
			oldChunks.clear();
			olderoldChunks.clear();
		}
		if (mc.isInSingleplayer()==true){
			String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
			serverip=array[array.length-2];
			world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
		} else {
			serverip = mc.getCurrentServerEntry().address.replace(':', '_');}
		world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
		if (save.get()){
			new File("NewChunks/"+serverip+"/"+world).mkdirs();
		}
		if (load.get()){
			loadData();
		}
		autoremoveticks=0;
		loadingticks=0;
		reloadworld=0;
	}

	@Override
	public void onDeactivate() {
		chunkcounterticks=0;
		autoremoveticks=0;
		loadingticks=0;
		reloadworld=0;
		if (remove.get()|autoremove.get()) {
			newChunks.clear();
			oldChunks.clear();
			olderoldChunks.clear();
		}
		super.onDeactivate();
	}
	@EventHandler
	private void onScreenOpen(OpenScreenEvent event) {
		if (event.screen instanceof DisconnectedScreen) {
			chunkcounterticks=0;
			newchunksfound=0;
			oldchunksfound=0;
			olderoldchunksfound=0;
			if (worldleaveremove.get()) {
				newChunks.clear();
				oldChunks.clear();
				olderoldChunks.clear();
			}
		}
		if (event.screen instanceof DownloadingTerrainScreen) {
			chunkcounterticks=0;
			newchunksfound=0;
			oldchunksfound=0;
			olderoldchunksfound=0;
			reloadworld=0;
		}
	}
	@EventHandler
	private void onGameLeft(GameLeftEvent event) {
		chunkcounterticks=0;
		newchunksfound=0;
		oldchunksfound=0;
		olderoldchunksfound=0;
		if (worldleaveremove.get()) {
			newChunks.clear();
			oldChunks.clear();
			olderoldChunks.clear();
		}
	}

	@EventHandler
	private void onPreTick(TickEvent.Pre event) {
		if (load.get()){
			loadingticks++;
			if (loadingticks<2){
				loadData();
			}
		} else if (!load.get()){
			loadingticks=0;
		}
		if (chunkcounter=true){
			chunkcounterticks++;
			if (chunkcounterticks>=1){
				chunkcounterticks=0;
				newchunksfound=0;
				oldchunksfound=0;
				olderoldchunksfound=0;
				chunkcounter=false;}
			if (chunkcounter=true && chunkcounterticks<1){
				try {
					List<String> allLines = Files.readAllLines(Paths.get("NewChunks/"+serverip+"/"+world+"/OldChunkData.txt"));

					for (String line : allLines) {
						oldchunksfound++;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					List<String> allLines = Files.readAllLines(Paths.get("NewChunks/"+serverip+"/"+world+"/NewChunkData.txt"));

					for (String line : allLines) {
						newchunksfound++;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					List<String> allLines = Files.readAllLines(Paths.get("NewChunks/"+serverip+"/"+world+"/FlowIsBelowY0ChunkData.txt"));

					for (String line : allLines) {
						olderoldchunksfound++;
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

		if (delete.get()){
			newChunks.clear();
			oldChunks.clear();
			olderoldChunks.clear();
			new File("NewChunks/"+serverip+"/"+world+"/NewChunkData.txt").delete();
			new File("NewChunks/"+serverip+"/"+world+"/OldChunkData.txt").delete();
			new File("NewChunks/"+serverip+"/"+world+"/FlowIsBelowY0ChunkData.txt").delete();
		}

		if (autoremove.get()) {
			autoremoveticks++;
			if (autoremoveticks==removedelay.get()*20){
				newChunks.clear();
				oldChunks.clear();
				olderoldChunks.clear();
				if (load.get() && reload.get()){
					loadData();
				}
			} else if (autoremoveticks>=removedelay.get()*20){
				autoremoveticks=0;
			}
		}
		//autoreload when entering different dimensions
		if (reloadworld<10){
			reloadworld++;
		}
		if (reloadworld==3){
			if (worldleaveremove.get()){
				newChunks.clear();
				oldChunks.clear();
				olderoldChunks.clear();
			}
			if (load.get()){
				loadData();
			}
		}
	}
	@EventHandler
	private void onRender(Render3DEvent event) {
		if (newChunksLineColor.get().a > 5 || newChunksSideColor.get().a > 5) {
			synchronized (newChunks) {
				for (ChunkPos c : newChunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						render(new Box(c.getStartPos().add(0, renderHeight.get(), 0), c.getStartPos().add(16, renderHeight.get(), 16)), newChunksSideColor.get(), newChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (olderoldChunksLineColor.get().a > 5 || olderoldChunksSideColor.get().a > 5) {
			synchronized (olderoldChunks) {
				for (ChunkPos c : olderoldChunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						if (detectmode.get()== DetectMode.Advanced) {
							render(new Box(c.getStartPos().add(0, renderHeight.get(), 0), c.getStartPos().add(16, renderHeight.get(), 16)), olderoldChunksSideColor.get(), olderoldChunksLineColor.get(), shapeMode.get(), event);
						} else if (detectmode.get()== DetectMode.Normal) {
							render(new Box(c.getStartPos().add(0, renderHeight.get(), 0), c.getStartPos().add(16, renderHeight.get(), 16)), newChunksSideColor.get(), newChunksLineColor.get(), shapeMode.get(), event);
						} else if (detectmode.get()== DetectMode.IgnoreFlowBelow0) {
							render(new Box(c.getStartPos().add(0, renderHeight.get(), 0), c.getStartPos().add(16, renderHeight.get(), 16)), oldChunksSideColor.get(), oldChunksLineColor.get(), shapeMode.get(), event);
						}
					}
				}
			}
		}

		if (oldChunksLineColor.get().a > 5 || oldChunksSideColor.get().a > 5){
			synchronized (oldChunks) {
				for (ChunkPos c : oldChunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						render(new Box(c.getStartPos().add(0, renderHeight.get(), 0), c.getStartPos().add(16, renderHeight.get(), 16)), oldChunksSideColor.get(), oldChunksLineColor.get(), shapeMode.get(), event);
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
		if (event.packet instanceof ChunkDeltaUpdateS2CPacket) {
			ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet;

			packet.visitUpdates((pos, state) -> {
				if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
					chunkPos = new ChunkPos(pos);

					for (Direction dir: searchDirs) {
						if (pos.offset(dir).getY()>0 && mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && (!newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
							if (olderoldChunks.contains(chunkPos)) olderoldChunks.remove(chunkPos);
							newChunks.add(chunkPos);
							if (save.get()){
								saveNewChunkData();
							}
							return;
						}else if (pos.offset(dir).getY()<=0 && mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && (!newChunks.contains(chunkPos) && !olderoldChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
							olderoldChunks.add(chunkPos);
							if (save.get()){
								saveOlderOldChunkData();
							}
							return;
						}
					}

				}
			});
		}
		else if (event.packet instanceof BlockUpdateS2CPacket) {
			BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;

			if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill()) {
				chunkPos = new ChunkPos(packet.getPos());

				for (Direction dir: searchDirs) {
					if (packet.getPos().offset(dir).getY()>0 && mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && (!newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
						if (olderoldChunks.contains(chunkPos)) olderoldChunks.remove(chunkPos);
						newChunks.add(chunkPos);
						if (save.get()){
							saveNewChunkData();
						}
						return;
					}else if (packet.getPos().offset(dir).getY()<=0 && mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() &&  (!newChunks.contains(chunkPos) && !olderoldChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
						if (oldChunks.contains(chunkPos)) oldChunks.remove(chunkPos);
						olderoldChunks.add(chunkPos);
						if (save.get()){
							saveOlderOldChunkData();
						}
						return;
					}
				}
			}
		}
		else if (event.packet instanceof ChunkDataS2CPacket && mc.world != null) {
			ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;

			oldpos = new ChunkPos(packet.getX(), packet.getZ());

			if (mc.world.getChunkManager().getChunk(packet.getX(), packet.getZ()) == null) {
				WorldChunk chunk = new WorldChunk(mc.world, oldpos);
				try {
					chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()));
				} catch (ArrayIndexOutOfBoundsException e) {
					return;
				}


				for (int x = 0; x < 16; x++) {
					for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
						for (int z = 0; z < 16; z++) {
							FluidState fluid = chunk.getFluidState(x, y, z);

							if (!olderoldChunks.contains(oldpos) && !newChunks.contains(oldpos) && !fluid.isEmpty() && !fluid.isStill()) {
								oldChunks.add(oldpos);
								if (save.get()){
									saveOldChunkData();
								}
								return;
							}
						}
					}
				}
			}
		}
	}
	private void loadData() {
		try {
			List<String> allLines = Files.readAllLines(Paths.get("NewChunks/"+serverip+"/"+world+"/OldChunkData.txt"));

			for (String line : allLines) {
				String s = line;
				String[] array = s.split(", ");
				int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]",""));
				int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]",""));
				oldpos = new ChunkPos(X,Z);
				oldChunks.add(oldpos);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			List<String> allLines = Files.readAllLines(Paths.get("NewChunks/"+serverip+"/"+world+"/NewChunkData.txt"));

			for (String line : allLines) {
				String s = line;
				String[] array = s.split(", ");
				int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]",""));
				int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]",""));
				chunkPos = new ChunkPos(X,Z);
				if (!newChunks.contains(chunkPos) && !olderoldChunks.contains(chunkPos) && !oldChunks.contains(chunkPos)){
					newChunks.add(chunkPos);}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			List<String> allLines = Files.readAllLines(Paths.get("NewChunks/"+serverip+"/"+world+"/FlowIsBelowY0ChunkData.txt"));

			for (String line : allLines) {
				String s = line;
				String[] array = s.split(", ");
				int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]",""));
				int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]",""));
				chunkPos = new ChunkPos(X,Z);
				if (!newChunks.contains(chunkPos) && !olderoldChunks.contains(chunkPos) && !oldChunks.contains(chunkPos)){
					olderoldChunks.add(chunkPos);}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void saveNewChunkData() {
		try {
			new File("NewChunks/"+serverip+"/"+world).mkdirs();
			FileWriter writer = new FileWriter("NewChunks/"+serverip+"/"+world+"/NewChunkData.txt", true);
			writer.write(String.valueOf(chunkPos));
			writer.write("\r\n");   // write new line
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void saveOldChunkData() {
		try {
			new File("NewChunks/"+serverip+"/"+world).mkdirs();
			FileWriter writer = new FileWriter("NewChunks/"+serverip+"/"+world+"/OldChunkData.txt", true);
			writer.write(String.valueOf(oldpos));
			writer.write("\r\n");   // write new line
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void saveOlderOldChunkData() {
		try {
			new File("NewChunks/"+serverip+"/"+world).mkdirs();
			FileWriter writer = new FileWriter("NewChunks/"+serverip+"/"+world+"/FlowIsBelowY0ChunkData.txt", true);
			writer.write(String.valueOf(chunkPos));
			writer.write("\r\n");   // write new line
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}