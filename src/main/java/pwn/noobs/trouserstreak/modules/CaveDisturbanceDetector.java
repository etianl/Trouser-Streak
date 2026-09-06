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
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
			.description("Removes the cached disturbances when they leave render distance.")
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
	private Set<BlockPos> scannedAir = Collections.synchronizedSet(new HashSet<>());
	private final Set<BlockPos> disturbanceLocations = Collections.synchronizedSet(new HashSet<>());
	private int closestX=2000000000;
	private int closestY=2000000000;
	private int closestZ=2000000000;
	private double distance=2000000000;

	public CaveDisturbanceDetector() {
		super(Trouser.baseHunting,"CaveDisturbanceDetector", "Scans for single air blocks within the cave air blocks found in caves and underground structures in 1.13+ chunks. There are several false positives.");
	}

	@Override
	public void onActivate() {
		clearChunkData();
	}
	private void scanTheAir() {
		if (mc.level == null) return;
		List<ChunkPos> chunksToProcess = new ArrayList<>();
		AtomicReferenceArray<LevelChunk> chunks = mc.level.getChunkSource().storage.chunks;
		for (int i = 0; i < chunks.length(); i++) {
			LevelChunk chunk = chunks.get(i);
			if (chunk != null && !chunk.isEmpty()) {
				chunksToProcess.add(chunk.getPos());
			}
		}
		chunksToProcess.parallelStream().forEach(chunkPos -> {
			LevelChunk chunk = mc.level.getChunk(chunkPos.x(), chunkPos.z());
			if (chunk != null && !chunk.isEmpty() && !scannedChunks.contains(chunk.getPos())) {
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
		if (event.screen instanceof DisconnectedScreen || event.screen instanceof LevelLoadingScreen) clearChunkData();
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

	private void processChunk(LevelChunk chunk) {
		int minY = mc.level.getMinY();
		int maxY = 180;
		if (mc.level.dimension() == Level.NETHER) maxY = 126;

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					BlockPos blockPos = new BlockPos(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
					BlockState blockState = chunk.getBlockState(blockPos);

					if (blockState.getBlock() == Blocks.CAVE_AIR) {
						isSurroundingBlockRegAir(blockPos);
					}
				}
			}
		}
	}

	private void isSurroundingBlockRegAir(BlockPos bPos) {
		scannedAir = Collections.synchronizedSet(new HashSet<>());
		for (int dir = 1; dir < 6; dir++) {
			switch (dir){
				case 1 -> {
					BlockPos Air = bPos.north();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.offset(0,0,-1);
						if (mc.level.getBlockState(Air).getBlock() == Blocks.AIR && mc.level.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.level.getBlockState(Air.offset(1,0,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(-1,0,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,1,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,-1,0)).getBlock() != Blocks.AIR)
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
						BlockPos BlockPastTheAir = Air.offset(0,0,1);
						if (mc.level.getBlockState(Air).getBlock() == Blocks.AIR && mc.level.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.level.getBlockState(Air.offset(1,0,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(-1,0,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,1,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,-1,0)).getBlock() != Blocks.AIR)
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
						BlockPos BlockPastTheAir = Air.offset(-1,0,0);
						if (mc.level.getBlockState(Air).getBlock() == Blocks.AIR && mc.level.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.level.getBlockState(Air.offset(0,1,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,-1,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,0,1)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,0,-1)).getBlock() != Blocks.AIR)
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
						BlockPos BlockPastTheAir = Air.offset(1,0,0);
						if (mc.level.getBlockState(Air).getBlock() == Blocks.AIR && mc.level.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.level.getBlockState(Air.offset(0,1,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,-1,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,0,1)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,0,-1)).getBlock() != Blocks.AIR)
							{
								if (!FPcheck(Air))disturbanceFound(Air);
							}
						}
					}
					scannedAir.add(Air);
				}
				case 5 -> {
					BlockPos Air = bPos.above();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.offset(0,1,0);
						if (mc.level.getBlockState(Air).getBlock() == Blocks.AIR && mc.level.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.level.getBlockState(Air.offset(1,0,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(-1,0,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,0,1)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,0,-1)).getBlock() != Blocks.AIR)
							{
								if (!FPcheck(Air))disturbanceFound(Air);
							}
						}
					}
					scannedAir.add(Air);
				}
				case 6 -> {
					BlockPos Air = bPos.below();
					if (!scannedAir.contains(Air)) {
						BlockPos BlockPastTheAir = Air.offset(0,-1,0);
						if (mc.level.getBlockState(Air).getBlock() == Blocks.AIR && mc.level.getBlockState(BlockPastTheAir).getBlock() != Blocks.AIR) {
							if (mc.level.getBlockState(Air.offset(1,0,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(-1,0,0)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,0,1)).getBlock() != Blocks.AIR &&
									mc.level.getBlockState(Air.offset(0,0,-1)).getBlock() != Blocks.AIR)
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
					BlockPos bpos = new BlockPos(disturbance.offset(x,y,z));
					if (bpos.equals(disturbance))continue;
					if (mc.level.getBlockState(bpos).getBlock() == Blocks.AIR){
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
					ChatUtils.sendMsg(Component.nullToEmpty("Disturbance in the Cave Air found: " + disturbance));
				else if (!displaycoords.get()) ChatUtils.sendMsg(Component.nullToEmpty("Disturbance in the Cave Air found!"));
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
						if (pos != null && playerPos.closerThan(pos, renderDistance.get() * 16)) {
							int startX = pos.getX();
							int startY = pos.getY();
							int startZ = pos.getZ();
							int endX = pos.getX();
							int endY = pos.getY();
							int endZ = pos.getZ();
							render(new AABB(new Vec3(startX+1, startY+1, startZ+1), new Vec3(endX, endY, endZ)), sideColor.get(), lineColor.get(), shapeMode.get(), event);
						}
					}
				} else if (nearesttrcr.get()){
					for (BlockPos pos : disturbanceLocations) {
						BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
						if (pos != null && playerPos.closerThan(pos, renderDistance.get() * 16)) {
							int startX = pos.getX();
							int startY = pos.getY();
							int startZ = pos.getZ();
							int endX = pos.getX();
							int endY = pos.getY();
							int endZ = pos.getZ();
							render(new AABB(new Vec3(startX+1, startY+1, startZ+1), new Vec3(endX, endY, endZ)), sideColor.get(), lineColor.get(), shapeMode.get(), event);
						}
					}
					render2(new AABB(new Vec3(closestX+1, closestY+1, closestZ+1), new Vec3 (closestX, closestY, closestZ)), sideColor.get(), lineColor.get(),ShapeMode.Sides, event);
				}
			}
		}
	}
	private void render(AABB box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
		if (trcr.get() && Math.abs(box.minX- RenderUtils.center.x)<=renderDistance.get()*16 && Math.abs(box.minZ-RenderUtils.center.z)<=renderDistance.get()*16)
			if (!nearesttrcr.get())
				event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX+0.5, box.minY+((box.maxY-box.minY)/2), box.minZ+0.5, lines);
		event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, new Color(0,0,0,0), shapeMode, 0);
	}
	private void render2(AABB box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
		if (trcr.get() && Math.abs(box.minX-RenderUtils.center.x)<=renderDistance.get()*16 && Math.abs(box.minZ-RenderUtils.center.z)<=renderDistance.get()*16)
			event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX+0.5, box.minY+((box.maxY-box.minY)/2), box.minZ+0.5, lines);
		event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, new Color(0,0,0,0), shapeMode, 0);
	}
	private void removeChunksOutsideRenderDistance() {
		AtomicReferenceArray<LevelChunk> chunks = mc.level.getChunkSource().storage.chunks;
		Set<LevelChunk> chunkSet = new HashSet<>();

		for (int i = 0; i < chunks.length(); i++) {
			LevelChunk chunk = chunks.get(i);
			if (chunk != null) {
				chunkSet.add(chunk);
			}
		}
		removechunksOutsideRenderDistance(scannedChunks, chunkSet);
		removeChunksOutsideRenderDistance(disturbanceLocations, chunkSet);
	}
	private void removeChunksOutsideRenderDistance(Set<BlockPos> boxSet, Set<LevelChunk> worldChunks) {
		boxSet.removeIf(box -> {
			assert mc.level != null;
			return !worldChunks.contains(mc.level.getChunk(box));
		});
	}
	private void removechunksOutsideRenderDistance(Set<ChunkPos> chunkSet, Set<LevelChunk> worldChunks) {
		chunkSet.removeIf(c -> {
			assert mc.level != null;
			return !worldChunks.contains(mc.level.getChunk(c.x(), c.z()));
		});
	}
}