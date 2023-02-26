package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/*
    Ported from: https://github.com/BleachDrinker420/BleachHack/blob/master/BleachHack-Fabric-1.16/src/main/java/bleach/hack/module/mods/NewChunks.java
    updated by etianll :D
*/
public class NewerNewChunks extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgRender = settings.createGroup("Render");

	// general
	private final Setting<Boolean> ignore = sgGeneral.add(new BoolSetting.Builder()
			.name("IgnoreFlowBelow0")
			.description("For Tracing servers updated to the new Build Limits from an old version. Ignores flow if no flow above zero and there is flow below zero.")
			.defaultValue(true)
			.build()
	);
	private final Setting<Boolean> advanced = sgGeneral.add(new BoolSetting.Builder()
			.name("AdvancedMode")
			.description("Shows another colour if liquids are flowing below Y=0 but not above. READ THE README BEFORE TRYING.")
			.defaultValue(false)
			.build()
	);
	private final Setting<Boolean> remove = sgGeneral.add(new BoolSetting.Builder()
        .name("remove")
        .description("Removes the cached chunks when disabling the module.")
        .defaultValue(true)
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
			.defaultValue(new SettingColor(255, 0, 0, 75))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
			.build()
	);
	private final Setting<SettingColor> olderoldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("FlowIsBelowY0-side-color")
			.description("MAY STILL BE NEW. Color of the chunks that have liquids flowing below Y=0")
			.defaultValue(new SettingColor(255, 255, 0, 75))
			.visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) && advanced.get())
			.build()
	);

	private final Setting<SettingColor> oldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-side-color")
			.description("Color of the chunks that have (most likely) been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 75))
			.visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> newChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("new-chunks-line-color")
			.description("Color of the chunks that are (most likely) completely new.")
			.defaultValue(new SettingColor(255, 0, 0, 255))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
			.build()
	);
	private final Setting<SettingColor> olderoldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("FlowIsBelowY0-line-color")
			.description("MAY STILL BE NEW. Color of the chunks that have liquids flowing below Y=0")
			.defaultValue(new SettingColor(255, 255, 0, 255))
			.visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both) && advanced.get())
			.build()
	);

	private final Setting<SettingColor> oldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-line-color")
			.description("Color of the chunks that have (most likely) been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 255))
			.visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
			.build()
	);

    private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
    private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> olderoldChunks = Collections.synchronizedSet(new HashSet<>());
    private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };
	private int ticks=0;
    public NewerNewChunks() {
        super(Trouser.Main,"NewerNewChunks", "Estimates new chunks by checking liquid flow.");
    }
	@Override
	public void onActivate() {
		ticks=0;
	}

	@Override
	public void onDeactivate() {
		ticks=0;
		if (remove.get()) {
			newChunks.clear();
			oldChunks.clear();
			olderoldChunks.clear();
		}
		super.onDeactivate();
	}
	@EventHandler
	private void onPreTick(TickEvent.Pre event) {
		if (advanced.get() && ignore.get()){
			ticks++;
			if (ticks==2){
		error("Use IgnoreFlow or Advanced mode, not both.");
			} else if (ticks==100){
				error("Use IgnoreFlow or Advanced mode, not both.");
				ticks=3;
			}
	} else if (!advanced.get() || !ignore.get()){
			ticks=0;
		}
	}
	@EventHandler
	private void onRender(Render3DEvent event) {
		if (newChunksLineColor.get().a > 5 || newChunksSideColor.get().a > 5) {
			synchronized (newChunks) {
				for (ChunkPos c : newChunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						render(new Box(c.getStartPos(), c.getStartPos().add(16, renderHeight.get(), 16)), newChunksSideColor.get(), newChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (olderoldChunksLineColor.get().a > 5 || olderoldChunksSideColor.get().a > 5) {
			synchronized (olderoldChunks) {
				for (ChunkPos c : olderoldChunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						render(new Box(c.getStartPos(), c.getStartPos().add(16, renderHeight.get(), 16)), olderoldChunksSideColor.get(), olderoldChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}

		if (oldChunksLineColor.get().a > 5 || oldChunksSideColor.get().a > 5){
			synchronized (oldChunks) {
				for (ChunkPos c : oldChunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						render(new Box(c.getStartPos(), c.getStartPos().add(16, renderHeight.get(), 16)), oldChunksSideColor.get(), oldChunksLineColor.get(), shapeMode.get(), event);
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
					ChunkPos chunkPos = new ChunkPos(pos);

					for (Direction dir: searchDirs) {
						if (advanced.get() && !ignore.get()){
							if (pos.offset(dir).getY()>0 && !mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos) && !olderoldChunks.contains(chunkPos)) {
								newChunks.add(chunkPos);
								return;
							}else if ((pos.offset(dir).getY()<0 && !mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill()) && (pos.offset(dir).getY()>0 && !mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill()) && !oldChunks.contains(chunkPos) && !olderoldChunks.contains(chunkPos)) {
								newChunks.add(chunkPos);
								return;
							}else if (pos.offset(dir).getY()<0 && !mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos) &&  !newChunks.contains(chunkPos)) {
								olderoldChunks.add(chunkPos);
								return;
							}
						}
						if (ignore.get() && !advanced.get()){
							if (pos.offset(dir).getY()>0 && mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos)) {
								newChunks.add(chunkPos);
								return;
							}
							if ((pos.offset(dir).getY()<0 && !mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill()) && (pos.offset(dir).getY()>0 && !mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill()) && !oldChunks.contains(chunkPos)) {
								newChunks.add(chunkPos);
								return;
							}
							}
						if (!advanced.get() && !ignore.get()){
							if (mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos)) {
								newChunks.add(chunkPos);
								return;
							}
						}
					}
				}
			});
		}

		else if (event.packet instanceof BlockUpdateS2CPacket) {
			BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;

			if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill()) {
				ChunkPos chunkPos = new ChunkPos(packet.getPos());

				for (Direction dir: searchDirs) {
					if (advanced.get() && !ignore.get()){
						if (packet.getPos().offset(dir).getY()>0 && !mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos) && !olderoldChunks.contains(chunkPos)) {
							newChunks.add(chunkPos);
							return;
						}else if ((packet.getPos().offset(dir).getY()<0 && !mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill()) && (packet.getPos().offset(dir).getY()>0 && !mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill()) && !oldChunks.contains(chunkPos) && !olderoldChunks.contains(chunkPos)) {
							newChunks.add(chunkPos);
							return;
						}else if (packet.getPos().offset(dir).getY()<0 && !mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() &&  !oldChunks.contains(chunkPos) &&  !newChunks.contains(chunkPos)) {
							olderoldChunks.add(chunkPos);
							return;
						}
					}
					if (ignore.get() && !advanced.get()){
						if (packet.getPos().offset(dir).getY()>0 && mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos)) {
							newChunks.add(chunkPos);
							return;
						}
						if ((packet.getPos().offset(dir).getY()<0 && !mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill()) && (packet.getPos().offset(dir).getY()>0 && !mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill()) && !oldChunks.contains(chunkPos)) {
							newChunks.add(chunkPos);
							return;
						}
					}
					if (!advanced.get() && !ignore.get()){
						if (mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos)) {
							newChunks.add(chunkPos);
							return;
						}
					}
				}
			}
		}

		else if (event.packet instanceof ChunkDataS2CPacket && mc.world != null) {
			ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;

			ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());

			if (!olderoldChunks.contains(pos) && !newChunks.contains(pos) && mc.world.getChunkManager().getChunk(packet.getX(), packet.getZ()) == null) {
				WorldChunk chunk = new WorldChunk(mc.world, pos);
				try {
					chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()));
				} catch (ArrayIndexOutOfBoundsException e) {
					return;
				}


				for (int x = 0; x < 16; x++) {
					for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
						for (int z = 0; z < 16; z++) {
							FluidState fluid = chunk.getFluidState(x, y, z);

							if (!fluid.isEmpty() && !fluid.isStill()) {
								oldChunks.add(pos);
								return;
							}
						}
					}
				}
			}
		}
	}
}
