//made by etianl :D
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CaveDisturbanceDetector extends Module {
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgRender = settings.createGroup("Render");
	private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
			.name("Chat feedback")
			.description("Displays info for you.")
			.defaultValue(false)
			.build()
	);
	private final Setting<Boolean> displaycoords = sgGeneral.add(new BoolSetting.Builder()
			.name("DisplayCoords")
			.description("Displays coords of air disturbances in chat.")
			.defaultValue(true)
			.build()
	);
	public final Setting<Integer> FPdistance = sgGeneral.add(new IntSetting.Builder()
			.name("False Positive Distance")
			.description("If extra normal air within this range of the cave air disturbance then ignore the disturbance")
			.defaultValue(1)
			.min(1)
			.sliderRange(1,10)
			.build()
	);
	private final Setting<Boolean> removerenderdist = sgRender.add(new BoolSetting.Builder()
			.name("RemoveOutsideRenderDistance")
			.description("Removes the cached disturbances when they leave the defined render distance.")
			.defaultValue(true)
			.build()
	);
	public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
			.name("Render-Distance(Chunks)")
			.description("How many chunks from the character to render the detected disturbances.")
			.defaultValue(32)
			.min(6)
			.sliderRange(6,1024)
			.build()
	);
	private final Setting<Boolean> trcr = sgRender.add(new BoolSetting.Builder()
			.name("Tracers")
			.description("Show tracers to the air disturbances.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> nearesttrcr = sgRender.add(new BoolSetting.Builder()
			.name("Tracer to nearest Disturbance Only")
			.description("Show only one tracer to the nearest air disturbance.")
			.defaultValue(false)
			.build()
	);
	private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
			.name("shape-mode")
			.description("How the shapes are rendered.")
			.defaultValue(ShapeMode.Both)
			.build()
	);
	private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
			.name("air-disturbance-side-color")
			.description("Color of possible air disturbances.")
			.defaultValue(new SettingColor(255, 0, 130, 55))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
			.build()
	);
	private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
			.name("air-disturbance-line-color")
			.description("Color of possible air disturbances.")
			.defaultValue(new SettingColor(255, 0, 130, 200))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both || trcr.get()))
			.build()
	);
	private final Set<ChunkPos> scannedChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<BlockPos> scannedAir = Collections.synchronizedSet(new HashSet<>());
	private final Set<BlockPos> disturbanceLocations = Collections.synchronizedSet(new HashSet<>());
	private int closestX=2000000000;
	private int closestY=2000000000;
	private int closestZ=2000000000;
	private double distance=2000000000;

	public CaveDisturbanceDetector() {
		super(Trouser.Main,"CaveDisturbanceDetector", "Scans for single air blocks within the cave air blocks found in caves and underground structures in 1.13+ chunks. There are several false positives.");
	}

	@Override
	public void onActivate() {
		clearChunkData();
	}
	private void scanTheAir() {
		if (mc.world == null) return;
		List<ChunkPos> chunksToProcess = new ArrayList<>();
		AtomicReferenceArray<WorldChunk> chunks = mc.world.getChunkManager().chunks.chunks;
		for (int i = 0; i < chunks.length(); i++) {
			WorldChunk chunk = chunks.get(i);
			if (chunk != null) {
				chunksToProcess.add(chunk.getPos());
			}
		}
		chunksToProcess.parallelStream().forEach(chunkPos -> {
			WorldChunk chunk = mc.world.getChunk(chunkPos.x, chunkPos.z);
			if (chunk != null && !scannedChunks.contains(chunk.getPos())) {
				processChunk(chunk);
				scannedChunks.add(chunk.getPos());
			}
		});
	}
	@Override
	public void onDeactivate() {
		clearChunkData();
	}
	@EventHandler
	private void onScreenOpen(OpenScreenEvent event) {
		if (event.screen instanceof DisconnectedScreen || event.screen instanceof DownloadingTerrainScreen) clearChunkData();
	}
	@EventHandler
	private void onGameLeft(GameLeftEvent event) {
		clearChunkData();
	}
	private void clearChunkData(){
		scannedChunks.clear();
		scannedAir.clear();
		disturbanceLocations.clear();
		closestX=2000000000;
		closestY=2000000000;
		closestZ=2000000000;
		distance=2000000000;
	}
	@EventHandler
	private void onPreTick(TickEvent.Pre event) {
		scanTheAir();
		if (nearesttrcr.get()){
			try {
				if (disturbanceLocations.stream().toList().size() > 0) {
					for (int b = 0; b < disturbanceLocations.stream().toList().size(); b++) {
						if (distance > Math.sqrt(Math.pow(disturbanceLocations.stream().toList().get(b).getX() - mc.player.getBlockX(), 2) + Math.pow(disturbanceLocations.stream().toList().get(b).getZ() - mc.player.getBlockZ(), 2))) {
							closestX = disturbanceLocations.stream().toList().get(b).getX();
							closestY = disturbanceLocations.stream().toList().get(b).getY();
							closestZ = disturbanceLocations.stream().toList().get(b).getZ();
							distance = Math.sqrt(Math.pow(disturbanceLocations.stream().toList().get(b).getX() - mc.player.getBlockX(), 2) + Math.pow(disturbanceLocations.stream().toList().get(b).getZ() - mc.player.getBlockZ(), 2));
						}
					}
					distance = 2000000000;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (removerenderdist.get())removeChunksOutsideRenderDistance();
	}

	private void processChunk(WorldChunk chunk) {
		int minY = mc.world.getBottomY();
		int maxY = 180;
		if (mc.world.getRegistryKey() == World.NETHER) maxY = 126;

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					BlockPos blockPos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
					BlockState blockState = chunk.getBlockState(blockPos);

					if (blockState.getBlock() == Blocks.CAVE_AIR) {
						isSurroundingBlockRegAir(blockPos);
					}
				}
			}
		}
	}

	private void isSurroundingBlockRegAir(BlockPos bPos) {
		for (int dir = 1; dir < 6; dir++) {
			switch (dir){
				case 1 -> {
					BlockPos Air = bPos.north();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.add(0,0,-1);
						if (mc.world.getBlockState(Air).getBlock() == Blocks.AIR && mc.world.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.world.getBlockState(Air.add(1,0,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(-1,0,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,1,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,-1,0)).getBlock() != Blocks.AIR)
							{
								if (!FPcheck(Air))disturbanceFound(Air);
							}
						}
					}
					scannedAir.add(Air);
				}
				case 2 -> {
					BlockPos Air = bPos.south();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.add(0,0,1);
						if (mc.world.getBlockState(Air).getBlock() == Blocks.AIR && mc.world.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.world.getBlockState(Air.add(1,0,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(-1,0,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,1,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,-1,0)).getBlock() != Blocks.AIR)
							{
								if (!FPcheck(Air))disturbanceFound(Air);
							}
						}
					}
					scannedAir.add(Air);
				}
				case 3 -> {
					BlockPos Air = bPos.west();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.add(-1,0,0);
						if (mc.world.getBlockState(Air).getBlock() == Blocks.AIR && mc.world.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.world.getBlockState(Air.add(0,1,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,-1,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,0,1)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,0,-1)).getBlock() != Blocks.AIR)
							{
								if (!FPcheck(Air))disturbanceFound(Air);
							}
						}
					}
					scannedAir.add(Air);
				}
				case 4 -> {
					BlockPos Air = bPos.east();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.add(1,0,0);
						if (mc.world.getBlockState(Air).getBlock() == Blocks.AIR && mc.world.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.world.getBlockState(Air.add(0,1,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,-1,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,0,1)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,0,-1)).getBlock() != Blocks.AIR)
							{
								if (!FPcheck(Air))disturbanceFound(Air);
							}
						}
					}
					scannedAir.add(Air);
				}
				case 5 -> {
					BlockPos Air = bPos.up();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.add(0,1,0);
						if (mc.world.getBlockState(Air).getBlock() == Blocks.AIR && mc.world.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.world.getBlockState(Air.add(1,0,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(-1,0,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,0,1)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,0,-1)).getBlock() != Blocks.AIR)
							{
								if (!FPcheck(Air))disturbanceFound(Air);
							}
						}
					}
					scannedAir.add(Air);
				}
				case 6 -> {
					BlockPos Air = bPos.down();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.add(0,-1,0);
						if (mc.world.getBlockState(Air).getBlock() == Blocks.AIR && mc.world.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.world.getBlockState(Air.add(1,0,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(-1,0,0)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,0,1)).getBlock() != Blocks.AIR &&
									mc.world.getBlockState(Air.add(0,0,-1)).getBlock() != Blocks.AIR)
							{
								if (!FPcheck(Air))disturbanceFound(Air);
							}
						}
					}
					scannedAir.add(Air);
				}
			}
		}
	}
	private boolean FPcheck(BlockPos disturbance){
		boolean extraAirFound = false;
		for (int x = -FPdistance.get(); x < FPdistance.get()+1; x++) {
			for (int y = -FPdistance.get(); y < FPdistance.get()+1; y++) {
				for (int z = -FPdistance.get(); z < FPdistance.get()+1; z++) {
					BlockPos bpos = new BlockPos(disturbance.add(x,y,z));
					if (bpos.equals(disturbance))continue;
					if (mc.world.getBlockState(bpos).getBlock() == Blocks.AIR){
						extraAirFound = true;
						break;
					}
				}
			}
		}
		return extraAirFound;
	}
	private void disturbanceFound(BlockPos disturbance){
		if (!disturbanceLocations.contains(disturbance)){
			disturbanceLocations.add(disturbance);
			if (chatFeedback.get()){
				if (displaycoords.get())
					ChatUtils.sendMsg(Text.of("Disturbance in the Cave Air found: " + disturbance));
				else if (!displaycoords.get()) ChatUtils.sendMsg(Text.of("Disturbance in the Cave Air found!"));
			}
		}
	}

	@EventHandler
	private void onRender(Render3DEvent event) {
		if ((sideColor.get().a > 5 || lineColor.get().a > 5) && mc.player != null) {
			synchronized (disturbanceLocations) {
				if (!nearesttrcr.get()) {
					for (BlockPos pos : disturbanceLocations) {
						BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
						if (pos != null && playerPos.isWithinDistance(pos, renderDistance.get() * 16)) {
							int startX = pos.getX();
							int startY = pos.getY();
							int startZ = pos.getZ();
							int endX = pos.getX();
							int endY = pos.getY();
							int endZ = pos.getZ();
							render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), sideColor.get(), lineColor.get(), shapeMode.get(), event);
						}
					}
				} else if (nearesttrcr.get()){
					for (BlockPos pos : disturbanceLocations) {
						BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
						if (pos != null && playerPos.isWithinDistance(pos, renderDistance.get() * 16)) {
							int startX = pos.getX();
							int startY = pos.getY();
							int startZ = pos.getZ();
							int endX = pos.getX();
							int endY = pos.getY();
							int endZ = pos.getZ();
							render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), sideColor.get(), lineColor.get(), shapeMode.get(), event);
						}
					}
					render2(new Box(new Vec3d(closestX+1, closestY+1, closestZ+1), new Vec3d (closestX, closestY, closestZ)), sideColor.get(), lineColor.get(),ShapeMode.Sides, event);
				}
			}
		}
	}
	private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
		if (trcr.get() && Math.abs(box.minX- RenderUtils.center.x)<=renderDistance.get()*16 && Math.abs(box.minZ-RenderUtils.center.z)<=renderDistance.get()*16)
			if (!nearesttrcr.get())
				event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX+0.5, box.minY+((box.maxY-box.minY)/2), box.minZ+0.5, lines);
		event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, new Color(0,0,0,0), shapeMode, 0);
	}
	private void render2(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
		if (trcr.get() && Math.abs(box.minX-RenderUtils.center.x)<=renderDistance.get()*16 && Math.abs(box.minZ-RenderUtils.center.z)<=renderDistance.get()*16)
			event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX+0.5, box.minY+((box.maxY-box.minY)/2), box.minZ+0.5, lines);
		event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, new Color(0,0,0,0), shapeMode, 0);
	}
	private void removeChunksOutsideRenderDistance() {
		double renderDistanceBlocks = renderDistance.get() * 16;

		removechunksOutsideRenderDistance(scannedChunks, mc.player.getBlockPos(), renderDistanceBlocks);
		removeChunksOutsideRenderDistance(disturbanceLocations, renderDistanceBlocks);
		removeChunksOutsideRenderDistance(scannedAir, renderDistanceBlocks);
	}
	private void removeChunksOutsideRenderDistance(Set<BlockPos> chunkSet, double renderDistanceBlocks) {
		chunkSet.removeIf(blockPos -> {
			BlockPos playerPos = new BlockPos(mc.player.getBlockX(), blockPos.getY(), mc.player.getBlockZ());
			return !playerPos.isWithinDistance(blockPos, renderDistanceBlocks);
		});
	}
	private void removechunksOutsideRenderDistance(Set<ChunkPos> chunkSet, BlockPos playerPos, double renderDistanceBlocks) {
		chunkSet.removeIf(c -> !playerPos.isWithinDistance(new BlockPos(c.getCenterX(), mc.player.getBlockY(), c.getCenterZ()), renderDistanceBlocks));
	}
}