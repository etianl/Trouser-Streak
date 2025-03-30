//made by etianl :D
package pwn.noobs.trouserstreak.modules;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
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
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.*;
import pwn.noobs.trouserstreak.Trouser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class PortalPatternFinder extends Module {
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgRender = settings.createGroup("Render");
	private final SettingGroup locationLogs = settings.createGroup("Location Logs");
	private final Setting<Boolean> displaycoords = sgGeneral.add(new BoolSetting.Builder()
			.name("DisplayCoords")
			.description("Displays coords of portal patterns in chat.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> ignorecorners = sgGeneral.add(new BoolSetting.Builder()
			.name("ignore-corner-blocks")
			.description("Also matches portal patterns that are missing the corner blocks.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> falsepositives1 = sgGeneral.add(new BoolSetting.Builder()
			.name("False Positive Removal")
			.description("Removes false positives in relation to the air above and below the portal pattern.")
			.defaultValue(true)
			.build()
	);
	public final Setting<Integer> nonAirPercent = sgGeneral.add(new IntSetting.Builder()
			.name("Non-Air Percent")
			.description("What percentage of the blocks in the portal shape can be non-air.")
			.defaultValue(20)
			.min(0)
			.sliderRange(0, 100)
			.build()
	);
	public final Setting<Integer> percent = sgGeneral.add(new IntSetting.Builder()
			.name("Adjacent Air Percent")
			.description("What percentage of the blocks in the portal shape that is allowed to have air blocks adjacent to it.")
			.defaultValue(15)
			.min(0)
			.sliderRange(0,100)
			.build()
	);
	public final Setting<Integer> pWidth = sgGeneral.add(new IntSetting.Builder()
			.name("Portal Width")
			.description("finds portals that are up to this large")
			.defaultValue(5)
			.min(4)
			.sliderRange(4,8)
			.build()
	);
	public final Setting<Integer> pHeight = sgGeneral.add(new IntSetting.Builder()
			.name("Portal Height")
			.description("finds portals that are up to this large")
			.defaultValue(5)
			.min(5)
			.sliderRange(5,8)
			.build()
	);
	private final Setting<Boolean> removerenderdist = sgRender.add(new BoolSetting.Builder()
			.name("RemoveOutsideRenderDistance")
			.description("Removes the cached portal patterns when they leave render distance.")
			.defaultValue(true)
			.build()
	);
	public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
			.name("Render-Distance(Chunks)")
			.description("How many chunks from the character to render the portal patterns.")
			.defaultValue(32)
			.min(6)
			.sliderRange(6,1024)
			.build()
	);
	private final Setting<Boolean> trcr = sgRender.add(new BoolSetting.Builder()
			.name("Tracers")
			.description("Show tracers to the portal patterns.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> nearesttrcr = sgRender.add(new BoolSetting.Builder()
			.name("Tracer to nearest Portal Only")
			.description("Show only one tracer to the nearest portal pattern.")
			.defaultValue(false)
			.build()
	);
	private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
			.name("shape-mode")
			.description("How the shapes are rendered.")
			.defaultValue(ShapeMode.Both)
			.build()
	);
	private final Setting<SettingColor> portalSideColor = sgRender.add(new ColorSetting.Builder()
			.name("possible-portal-side-color")
			.description("Color of possible portal locations.")
			.defaultValue(new SettingColor(170, 0, 255, 55))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
			.build()
	);
	private final Setting<SettingColor> portalLineColor = sgRender.add(new ColorSetting.Builder()
			.name("possible-portal-line-color")
			.description("Color of possible portal locations.")
			.defaultValue(new SettingColor(170, 0, 255, 200))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both || trcr.get()))
			.build()
	);
	private final Setting<Boolean> locLogging = locationLogs.add(new BoolSetting.Builder()
			.name("Enable Location Logging")
			.description("Logs the locations of detected spawners to a csv file as well as a table in this options menu.")
			.defaultValue(false)
			.build()
	);
	private final Set<ChunkPos> scannedChunks = new CopyOnWriteArraySet<>();
	private final Set<Box> possiblePortalLocations = new CopyOnWriteArraySet<>();
	private final Set<BlockPos> loggedPortalPositions = new CopyOnWriteArraySet<>();
	private int closestPortalX=2000000000;
	private int closestPortalY=2000000000;
	private int closestPortalZ=2000000000;
	private double PortalDistance=2000000000;

	private final List<PortalPattern> portalPatterns = new ArrayList<>();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public PortalPatternFinder() {
		super(Trouser.Main,"PortalPatternFinder", "Scans for the shapes of broken/removed Nether Portals within the cave air blocks found in caves and underground structures in 1.13+ chunks. **May be useful for finding portal skips in the Nether**");
	}

	@Override
	public void onActivate() {
		clearChunkData();
		loadPortalPatterns();
	}
	private void scanTheAir(AtomicReferenceArray<WorldChunk> chunks) {
		List<ChunkPos> chunksToProcess = new ArrayList<>();
		for (int i = 0; i < chunks.length(); i++) {
			WorldChunk chunk = chunks.get(i);
			if (chunk != null && !chunk.isEmpty()) {
				chunksToProcess.add(chunk.getPos());
			}
		}
		chunksToProcess.stream().forEach(chunkPos -> {
			WorldChunk chunk = mc.world.getChunk(chunkPos.x, chunkPos.z);
			if (chunk != null && !chunk.isEmpty() && !scannedChunks.contains(chunk.getPos())) {
				processChunk(chunk);
				scannedChunks.add(chunk.getPos());
			}
		});
	}
	@Override
	public void onDeactivate() {
		clearChunkData();
		portalPatterns.clear();
		loggedPortalPositions.clear();
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
		possiblePortalLocations.clear();
		closestPortalX=2000000000;
		closestPortalY=2000000000;
		closestPortalZ=2000000000;
		PortalDistance=2000000000;
	}
	@EventHandler
	private void onPreTick(TickEvent.Pre event) {
		if (mc.world == null) return;
		AtomicReferenceArray<WorldChunk> chunks = mc.world.getChunkManager().chunks.chunks;
		Set<WorldChunk> chunkSet = new HashSet<>();

		for (int i = 0; i < chunks.length(); i++) {
			WorldChunk chunk = chunks.get(i);
			if (chunk != null) {
				chunkSet.add(chunk);
			}
		}
		scanTheAir(chunks);
		if (nearesttrcr.get()){
			try {
				if (possiblePortalLocations.stream().toList().size() > 0) {
					for (int b = 0; b < possiblePortalLocations.stream().toList().size(); b++) {
						if (PortalDistance > Math.sqrt(Math.pow(possiblePortalLocations.stream().toList().get(b).getCenter().x-1 - mc.player.getBlockX(), 2) + Math.pow(possiblePortalLocations.stream().toList().get(b).getCenter().z-1 - mc.player.getBlockZ(), 2))) {
							closestPortalX = Math.round((float) possiblePortalLocations.stream().toList().get(b).getCenter().x-1);
							closestPortalY = Math.round((float) possiblePortalLocations.stream().toList().get(b).getCenter().y-1);
							closestPortalZ = Math.round((float) possiblePortalLocations.stream().toList().get(b).getCenter().z-1);
							PortalDistance = Math.sqrt(Math.pow(possiblePortalLocations.stream().toList().get(b).getCenter().x-1 - mc.player.getBlockX(), 2) + Math.pow(possiblePortalLocations.stream().toList().get(b).getCenter().z-1 - mc.player.getBlockZ(), 2));
						}
					}
					PortalDistance = 2000000000;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (removerenderdist.get())removeChunksOutsideRenderDistance(chunkSet);
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
		BlockPos airPos=bPos.north();
		BlockPos blockPastTheAir=bPos.north().add(0, 0, -1);
		for (int dir = 1; dir <= 4; dir++) {
			switch (dir) {
				case 1 -> {
					airPos = bPos.north();
					blockPastTheAir = bPos.north().add(0, 0, -1);
				}
				case 2 -> {
					airPos = bPos.south();
					blockPastTheAir = bPos.south().add(0, 0, 1);
				}
				case 3 -> {
					airPos = bPos.west();
					blockPastTheAir = bPos.west().add(-1, 0, 0);
				}
				case 4 -> {
					airPos = bPos.east();
					blockPastTheAir = bPos.east().add(1, 0, 0);
				}
			}
			if (mc.world.getBlockState(airPos).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPastTheAir).getBlock() != Blocks.AIR) findAirShape(airPos);
		}
	}

	private void findAirShape(BlockPos pos) {
		final int squareWidth = pWidth.get();
		final int squareHeight = pHeight.get();
		int areaWidth = (squareWidth / 2) + 1;
		int areaHeight = (squareHeight / 2) + 1;

		List<BlockPos> AirBlockPatternWEast = new ArrayList<>();
		List<BlockPos> AirBlockPatternNouth = new ArrayList<>();
		int AirBlockPatternWEastREJECT = 0;
		int AirBlockPatternNouthREJECT = 0;
		int AirBlockPatternWEastREJECT2 = 0;
		int AirBlockPatternNouthREJECT2 = 0;

		for (int x = -areaWidth; x <= areaWidth; x++) {
			for (int y = -areaHeight; y <= areaHeight; y++) {
				BlockPos bPos = new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ());
				if (mc.world.getBlockState(bPos).getBlock() == Blocks.AIR) {
					int nonairblockonsides = 0;
					BlockPos[] surroundingPositions = new BlockPos[] {
							bPos.north(),
							bPos.south()
					};
					for (BlockPos posi : surroundingPositions) {
						if (mc.world.getBlockState(posi).getBlock() != Blocks.AIR) {
							nonairblockonsides++;
						}
					}
					if (nonairblockonsides>=2)AirBlockPatternWEast.add(bPos);
					else {
						AirBlockPatternWEastREJECT++;
						AirBlockPatternWEast.add(bPos);
					}
				} else if (mc.world.getBlockState(bPos).getBlock() != Blocks.AIR && mc.world.getBlockState(bPos).getBlock() != Blocks.CAVE_AIR) {
					AirBlockPatternWEastREJECT2++;
					AirBlockPatternWEast.add(bPos);
				}
			}
		}
		for (int z = -areaWidth; z <= areaWidth; z++) {
			for (int y = -areaHeight; y <= areaHeight; y++) {
				BlockPos bPos = new BlockPos(pos.getX(), pos.getY() + y, pos.getZ() + z);
				if (mc.world.getBlockState(bPos).getBlock() == Blocks.AIR) {
					int nonairblockonsides = 0;
					BlockPos[] surroundingPositions = new BlockPos[] {
							bPos.west(),
							bPos.east()
					};
					for (BlockPos posi : surroundingPositions) {
						if (mc.world.getBlockState(posi).getBlock() != Blocks.AIR) {
							nonairblockonsides++;
						}
					}
					if (nonairblockonsides>=2)AirBlockPatternNouth.add(bPos);
					else {
						AirBlockPatternNouthREJECT++;
						AirBlockPatternNouth.add(bPos);
					}
				} else if (mc.world.getBlockState(bPos).getBlock() != Blocks.AIR && mc.world.getBlockState(bPos).getBlock() != Blocks.CAVE_AIR) {
					AirBlockPatternNouthREJECT2++;
					AirBlockPatternNouth.add(bPos);
				}
			}
		}

		if (((double) AirBlockPatternWEastREJECT2 / (AirBlockPatternWEast.size()-AirBlockPatternWEastREJECT)) * 100 <= nonAirPercent.get() && ((double) AirBlockPatternWEastREJECT / AirBlockPatternWEast.size()) * 100 <= percent.get()) {
			for (BlockPos block : AirBlockPatternWEast) {
				for (int currentWidth = 4; currentWidth <= squareWidth; currentWidth++) {
					for (int currentHeight = 5; currentHeight <= squareHeight; currentHeight++) {
						if (isValidWEastPortalShape(AirBlockPatternWEast, block, currentWidth, currentHeight)) {
							BlockPos boxStart = block;
							BlockPos boxEnd = new BlockPos(boxStart.getX() + currentWidth - 1, boxStart.getY() + currentHeight - 1, boxStart.getZ());
							boolean airfoundaboveorbelow = false;

							if (falsepositives1.get()) {
								for (int x = 0; x < currentWidth; x++) {
									BlockPos blockPos = boxStart.add(x, -1, 0);
									if (mc.world.getBlockState(blockPos).getBlock() == Blocks.AIR) airfoundaboveorbelow = true;
								}
								for (int x = 0; x < currentWidth; x++) {
									BlockPos blockPos = boxStart.add(x, currentHeight + 1, 0);
									if (mc.world.getBlockState(blockPos).getBlock() == Blocks.AIR) airfoundaboveorbelow = true;
								}
								if (airfoundaboveorbelow) continue;
							}

							Box portalBox = new Box(
									new Vec3d(boxStart.getX(), boxStart.getY(), boxStart.getZ()),
									new Vec3d(boxEnd.getX() + 1, boxEnd.getY() + 1, boxEnd.getZ() + 1)
							);

							boolean intersects = false;
							for (Box existingBox : possiblePortalLocations) {
								if (portalBox.intersects(existingBox)) {
									intersects = true;
									break;
								}
							}

							if (!intersects) {
								portalFound(portalBox);
								break;
							}
						}
					}
				}
			}
		}

		if (((double) AirBlockPatternNouthREJECT2 / (AirBlockPatternNouth.size()-AirBlockPatternNouthREJECT)) * 100 <= nonAirPercent.get() && ((double) AirBlockPatternNouthREJECT / AirBlockPatternNouth.size()) * 100 <= percent.get()) {
			for (BlockPos block : AirBlockPatternNouth) {
				for (int currentWidth = 4; currentWidth <= squareWidth; currentWidth++) {
					for (int currentHeight = 5; currentHeight <= squareHeight; currentHeight++) {
						if (isValidNouthPortalShape(AirBlockPatternNouth, block, currentWidth, currentHeight)) {
							BlockPos boxStart = block;
							BlockPos boxEnd = new BlockPos(boxStart.getX(), boxStart.getY() + currentHeight - 1, boxStart.getZ() + currentWidth - 1);
							boolean airfoundaboveorbelow = false;

							if (falsepositives1.get()) {
								for (int z = 0; z < currentWidth; z++) {
									BlockPos blockPos = boxStart.add(0, -1, z);
									if (mc.world.getBlockState(blockPos).getBlock() == Blocks.AIR)
										airfoundaboveorbelow = true;
								}
								for (int z = 0; z < currentWidth; z++) {
									BlockPos blockPos = boxStart.add(0, currentHeight + 1, z);
									if (mc.world.getBlockState(blockPos).getBlock() == Blocks.AIR)
										airfoundaboveorbelow = true;
								}
								if (airfoundaboveorbelow) continue;
							}

							Box portalBox = new Box(
									new Vec3d(boxStart.getX(), boxStart.getY(), boxStart.getZ()),
									new Vec3d(boxEnd.getX() + 1, boxEnd.getY() + 1, boxEnd.getZ() + 1)
							);

							boolean intersects = false;
							for (Box existingBox : possiblePortalLocations) {
								if (portalBox.intersects(existingBox)) {
									intersects = true;
									break;
								}
							}

							if (!intersects) portalFound(portalBox);
						}
					}
				}
			}
		}
	}
	private void portalFound(Box portalBox){
		if (!possiblePortalLocations.contains(portalBox)){
			possiblePortalLocations.add(portalBox);
			mc.execute(() -> {
				if (displaycoords.get())
					ChatUtils.sendMsg(Text.of("Possible portal found: " + portalBox.getCenter()));
				else if (!displaycoords.get()) ChatUtils.sendMsg(Text.of("Possible portal found!"));
			});
			BlockPos cp = new BlockPos(Math.round((float)portalBox.getCenter().x),Math.round((float)portalBox.getCenter().y),Math.round((float)portalBox.getCenter().z));
			if(!loggedPortalPositions.contains(cp) && locLogging.get()){
				loggedPortalPositions.add(cp);
				portalPatterns.add(new PortalPattern(cp.getX(),cp.getY(),cp.getZ()));
				saveJson();
				saveCsv();
			}
		}
	}

	private boolean isValidWEastPortalShape(List<BlockPos> portalBlocks, BlockPos startBlock, Integer squareWidth, Integer squareHeight) {
		for (int currentWidth = 4; currentWidth <= squareWidth; currentWidth++) {
			for (int currentHeight = 5; currentHeight <= squareHeight; currentHeight++) {
				boolean validShape = true;

				for (int dx = 0; dx < currentWidth; dx++) {
					for (int dy = 0; dy < currentHeight; dy++) {
						BlockPos checkPos = startBlock.add(dx, dy, 0);

						if (ignorecorners.get() && ((dx == 0 && dy == 0) || (dx == currentWidth - 1 && dy == 0) ||
								(dx == 0 && dy == currentHeight - 1) || (dx == currentWidth - 1 && dy == currentHeight - 1))) {
							continue;
						}

						if (!portalBlocks.contains(checkPos)) {
							validShape = false;
							break;
						}
					}
					if (!validShape) break;
				}

				if (validShape) return true;
			}
		}
		return false;
	}

	private boolean isValidNouthPortalShape(List<BlockPos> portalBlocks, BlockPos startBlock, Integer squareWidth, Integer squareHeight) {
		for (int currentWidth = 4; currentWidth <= squareWidth; currentWidth++) {
			for (int currentHeight = 5; currentHeight <= squareHeight; currentHeight++) {
				boolean validShape = true;

				for (int dz = 0; dz < currentWidth; dz++) {
					for (int dy = 0; dy < currentHeight; dy++) {
						BlockPos checkPos = startBlock.add(0, dy, dz);

						if (ignorecorners.get() && ((dz == 0 && dy == 0) || (dz == currentWidth - 1 && dy == 0) ||
								(dz == 0 && dy == currentHeight - 1) || (dz == currentWidth - 1 && dy == currentHeight - 1))) {
							continue;
						}

						if (!portalBlocks.contains(checkPos)) {
							validShape = false;
							break;
						}
					}
					if (!validShape) break;
				}

				if (validShape) return true;
			}
		}
		return false;
	}

	@EventHandler
	private void onRender(Render3DEvent event) {
		if (portalSideColor.get().a > 5 || portalLineColor.get().a > 5) {
			synchronized (possiblePortalLocations) {
				if (!nearesttrcr.get()) {
					for (Box box : possiblePortalLocations) {
						BlockPos playerPos = new BlockPos(mc.player.getBlockX(), Math.round((float)box.getCenter().getY()), mc.player.getBlockZ());
						if (box != null && playerPos.isWithinDistance(box.getCenter(), renderDistance.get() * 16)) {
							render(box, portalSideColor.get(), portalLineColor.get(), shapeMode.get(), event);
						}
					}
				} else if (nearesttrcr.get()) {
					for (Box box : possiblePortalLocations) {
						BlockPos playerPos = new BlockPos(mc.player.getBlockX(), Math.round((float)box.getCenter().getY()), mc.player.getBlockZ());
						if (box != null && playerPos.isWithinDistance(box.getCenter(), renderDistance.get() * 16)) {
							render(box, portalSideColor.get(), portalLineColor.get(), shapeMode.get(), event);
						}
					}
					render2(new Box(new Vec3d(closestPortalX, closestPortalY, closestPortalZ), new Vec3d (closestPortalX, closestPortalY, closestPortalZ)), portalSideColor.get(), portalLineColor.get(),ShapeMode.Sides, event);
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
	private void removeChunksOutsideRenderDistance(Set<WorldChunk> worldChunks) {
		removechunksOutsideRenderDistance(scannedChunks, worldChunks);
		removeChunksOutsideRenderDistance(possiblePortalLocations, worldChunks);
	}
	private void removeChunksOutsideRenderDistance(Set<Box> boxSet, Set<WorldChunk> worldChunks) {
		boxSet.removeIf(box -> {
			BlockPos boxPos = new BlockPos((int)Math.floor(box.getCenter().getX()), (int)Math.floor(box.getCenter().getY()), (int)Math.floor(box.getCenter().getZ()));
			assert mc.world != null;
			return !worldChunks.contains(mc.world.getChunk(boxPos));
		});
	}
	private void removechunksOutsideRenderDistance(Set<ChunkPos> chunkSet, Set<WorldChunk> worldChunks) {
		chunkSet.removeIf(c -> {
			assert mc.world != null;
			return !worldChunks.contains(mc.world.getChunk(c.x, c.z));
		});
	}

	@Override
	public WWidget getWidget(GuiTheme theme) {
		portalPatterns.sort(Comparator.comparingInt(a -> a.y));
		WVerticalList list = theme.verticalList();
		WButton clear = list.add(theme.button("Clear Logged Positions")).widget();
		WTable table = new WTable();
		if (!portalPatterns.isEmpty()) list.add(table);
		clear.action = () -> {
			portalPatterns.clear();
			loggedPortalPositions.clear();
			possiblePortalLocations.clear();
			table.clear();
			saveJson();
			saveCsv();
		};
		fillTable(theme, table);
		return list;
	}
	private void fillTable(GuiTheme theme, WTable table) {
		List<PortalPattern> portalCoords = new ArrayList<>();
		for (PortalPattern p : portalPatterns) {
			if (!portalCoords.contains(p)) {
				portalCoords.add(p);
				table.add(theme.label("Pos: " + p.x + ", " + p.y + ", " + p.z));
				WButton gotoBtn = table.add(theme.button("Goto")).widget();
				gotoBtn.action = () -> PathManagers.get().moveTo(new BlockPos(p.x, p.y, p.z), true);
				WMinus delete = table.add(theme.minus()).widget();
				delete.action = () -> {
					portalPatterns.remove(p);
					loggedPortalPositions.remove(new BlockPos(p.x, p.y, p.z));
					possiblePortalLocations.removeIf(box -> {
						BlockPos cp = new BlockPos(Math.round((float) box.getCenter().x), Math.round((float) box.getCenter().y), Math.round((float) box.getCenter().z));
						return cp.equals(new BlockPos(p.x, p.y, p.z));
					});
					table.clear();
					fillTable(theme, table);
					saveJson();
					saveCsv();
				};
				table.row();
			}
		}
	}
	private void loadPortalPatterns() {
		File file = getJsonFile();
		boolean loaded = false;
		if(file.exists()){
			try{
				FileReader reader = new FileReader(file);
				List<PortalPattern> data = GSON.fromJson(reader, new TypeToken<List<PortalPattern>>() {}.getType());
				reader.close();
				if(data != null){
					portalPatterns.addAll(data);
					for(PortalPattern p : data){
						loggedPortalPositions.add(new BlockPos(p.x, p.y, p.z));
					}
					loaded = true;
				}
			}catch(Exception ignored){}
		}
		if(!loaded){
			file = getCsvFile();
			if(file.exists()){
				try{
					BufferedReader reader = new BufferedReader(new FileReader(file));
					reader.readLine();
					String line;
					while((line = reader.readLine()) != null){
						String[] values = line.split(",");
						PortalPattern p = new PortalPattern(
								Integer.parseInt(values[0]),
								Integer.parseInt(values[1]),
								Integer.parseInt(values[2])
						);
						portalPatterns.add(p);
						loggedPortalPositions.add(new BlockPos(p.x, p.y, p.z));
					}
					reader.close();
				}catch(Exception ignored){}
			}
		}
	}
	private void saveCsv() {
		try{
			File file = getCsvFile();
			file.getParentFile().mkdirs();
			Writer writer = new FileWriter(file);
			writer.write("X,Y,Z\n");
			for(PortalPattern p : portalPatterns){
				p.write(writer);
			}
			writer.close();
		}catch(IOException ignored){}
	}
	private void saveJson() {
		try{
			File file = getJsonFile();
			file.getParentFile().mkdirs();
			Writer writer = new FileWriter(file);
			GSON.toJson(portalPatterns, writer);
			writer.close();
		}catch(IOException ignored){}
	}
	private File getJsonFile() {
		return new File(new File(new File("TrouserStreak", "PortalPatterns"), Utils.getFileWorldName()), "portalpatterns.json");
	}
	private File getCsvFile() {
		return new File(new File(new File("TrouserStreak", "PortalPatterns"), Utils.getFileWorldName()), "portalpatterns.csv");
	}
	private static class PortalPattern {
		private static final StringBuilder sb = new StringBuilder();
		public int x, y, z;
		public PortalPattern(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		public void write(Writer writer) throws IOException {
			sb.setLength(0);
			sb.append(x).append(',').append(y).append(',').append(z).append('\n');
			writer.write(sb.toString());
		}
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			PortalPattern pattern = (PortalPattern) o;
			return x == pattern.x && y == pattern.y && z == pattern.z;
		}
		@Override
		public int hashCode() {
			return Objects.hash(x, y, z);
		}
	}
}