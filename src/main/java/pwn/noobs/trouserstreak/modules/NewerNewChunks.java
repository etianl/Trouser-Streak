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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.AcknowledgeChunksC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
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
	private final SettingGroup sgAlarm = settings.createGroup("Chunk Alarms");

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
	private final Setting<Boolean> alarms = sgAlarm.add(new BoolSetting.Builder()
			.name("Enable Alarms for NewChunks")
			.description("rings alarms when newchunks detected")
			.defaultValue(false)
			.build()
	);
	public final Setting<Integer> amountofrings = sgAlarm.add(new IntSetting.Builder()
			.name("Amount Of Rings")
			.description("How many times the alarm will ring when newchunk detected.")
			.defaultValue(1)
			.sliderRange(1, 10)
			.min(1)
			.visible(() -> alarms.get())
			.build()
	);

	public final Setting<Integer> ringdelay = sgAlarm.add(new IntSetting.Builder()
			.name("Delay Between Rings (ticks)")
			.description("The delay between rings (in ticks).")
			.defaultValue(20)
			.sliderRange(1, 100)
			.min(1)
			.visible(() -> alarms.get())
			.build()
	);
	public final Setting<Double> volume = sgAlarm.add(new DoubleSetting.Builder()
			.name("Volume")
			.description("The volume of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.0, 1.0)
			.visible(() -> alarms.get())
			.build()
	);

	public final Setting<Double> pitch = sgAlarm.add(new DoubleSetting.Builder()
			.name("Pitch")
			.description("The pitch of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.5, 2.0)
			.visible(() -> alarms.get())
			.build()
	);

	public final Setting<List<SoundEvent>> soundtouse = sgAlarm.add(new SoundEventListSetting.Builder()
			.name("Sound to play (pick one)")
			.description("The sound to play. Just pick one.")
			.defaultValue(SoundEvents.BLOCK_BELL_USE)
			.visible(() -> alarms.get())
			.build()
	);
	private final Setting<Boolean> oldalarms = sgAlarm.add(new BoolSetting.Builder()
			.name("Enable Alarms for OldChunks")
			.description("rings alarms when oldchunks detected")
			.defaultValue(false)
			.build()
	);
	public final Setting<Integer> oldamountofrings = sgAlarm.add(new IntSetting.Builder()
			.name("Amount Of Rings")
			.description("How many times the alarm will ring when oldchunk detected.")
			.defaultValue(1)
			.sliderRange(1, 10)
			.min(1)
			.visible(() -> oldalarms.get())
			.build()
	);

	public final Setting<Integer> oldringdelay = sgAlarm.add(new IntSetting.Builder()
			.name("Delay Between Rings (ticks)")
			.description("The delay between rings (in ticks).")
			.defaultValue(20)
			.sliderRange(1, 100)
			.min(1)
			.visible(() -> oldalarms.get())
			.build()
	);
	public final Setting<Double> oldvolume = sgAlarm.add(new DoubleSetting.Builder()
			.name("Volume")
			.description("The volume of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.0, 1.0)
			.visible(() -> oldalarms.get())
			.build()
	);

	public final Setting<Double> oldpitch = sgAlarm.add(new DoubleSetting.Builder()
			.name("Pitch")
			.description("The pitch of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.5, 2.0)
			.visible(() -> oldalarms.get())
			.build()
	);

	public final Setting<List<SoundEvent>> oldsoundtouse = sgAlarm.add(new SoundEventListSetting.Builder()
			.name("Sound to play (pick one)")
			.description("The sound to play. Just pick one.")
			.defaultValue(SoundEvents.BLOCK_BELL_USE)
			.visible(() -> oldalarms.get())
			.build()
	);
	private final Setting<Boolean> beingupdatedchunksalarms = sgAlarm.add(new BoolSetting.Builder()
			.name("Enable Alarms for BeingUpdatedChunks")
			.description("rings alarms when BeingUpdatedChunks detected")
			.defaultValue(false)
			.build()
	);
	public final Setting<Integer> beingupdatedchunksamountofrings = sgAlarm.add(new IntSetting.Builder()
			.name("Amount Of Rings")
			.description("How many times the alarm will ring when BeingUpdatedChunks detected.")
			.defaultValue(1)
			.sliderRange(1, 10)
			.min(1)
			.visible(() -> beingupdatedchunksalarms.get())
			.build()
	);

	public final Setting<Integer> beingupdatedchunksringdelay = sgAlarm.add(new IntSetting.Builder()
			.name("Delay Between Rings (ticks)")
			.description("The delay between rings (in ticks).")
			.defaultValue(20)
			.sliderRange(1, 100)
			.min(1)
			.visible(() -> beingupdatedchunksalarms.get())
			.build()
	);
	public final Setting<Double> beingupdatedchunksvolume = sgAlarm.add(new DoubleSetting.Builder()
			.name("Volume")
			.description("The volume of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.0, 1.0)
			.visible(() -> beingupdatedchunksalarms.get())
			.build()
	);

	public final Setting<Double> beingupdatedchunkspitch = sgAlarm.add(new DoubleSetting.Builder()
			.name("Pitch")
			.description("The pitch of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.5, 2.0)
			.visible(() -> beingupdatedchunksalarms.get())
			.build()
	);

	public final Setting<List<SoundEvent>> beingupdatedchunkssoundtouse = sgAlarm.add(new SoundEventListSetting.Builder()
			.name("Sound to play (pick one)")
			.description("The sound to play. Just pick one.")
			.defaultValue(SoundEvents.BLOCK_BELL_USE)
			.visible(() -> beingupdatedchunksalarms.get())
			.build()
	);
	private final Setting<Boolean> oldversionchunksalarms = sgAlarm.add(new BoolSetting.Builder()
			.name("Enable Alarms for OldVersionChunks")
			.description("rings alarms when OldVersionChunks detected")
			.defaultValue(false)
			.build()
	);
	public final Setting<Integer> oldversionchunksamountofrings = sgAlarm.add(new IntSetting.Builder()
			.name("Amount Of Rings")
			.description("How many times the alarm will ring when OldVersionChunks detected.")
			.defaultValue(1)
			.sliderRange(1, 10)
			.min(1)
			.visible(() -> oldversionchunksalarms.get())
			.build()
	);

	public final Setting<Integer> oldversionchunksringdelay = sgAlarm.add(new IntSetting.Builder()
			.name("Delay Between Rings (ticks)")
			.description("The delay between rings (in ticks).")
			.defaultValue(20)
			.sliderRange(1, 100)
			.min(1)
			.visible(() -> oldversionchunksalarms.get())
			.build()
	);
	public final Setting<Double> oldversionchunksvolume = sgAlarm.add(new DoubleSetting.Builder()
			.name("Volume")
			.description("The volume of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.0, 1.0)
			.visible(() -> oldversionchunksalarms.get())
			.build()
	);

	public final Setting<Double> oldversionchunkspitch = sgAlarm.add(new DoubleSetting.Builder()
			.name("Pitch")
			.description("The pitch of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.5, 2.0)
			.visible(() -> oldversionchunksalarms.get())
			.build()
	);

	public final Setting<List<SoundEvent>> oldversionchunkssoundtouse = sgAlarm.add(new SoundEventListSetting.Builder()
			.name("Sound to play (pick one)")
			.description("The sound to play. Just pick one.")
			.defaultValue(SoundEvents.BLOCK_BELL_USE)
			.visible(() -> oldversionchunksalarms.get())
			.build()
	);
	private final Setting<Boolean> blockexploitchunksalarms = sgAlarm.add(new BoolSetting.Builder()
			.name("Enable Alarms for BlockExploitChunks")
			.description("rings alarms when BlockExploitChunks detected")
			.defaultValue(false)
			.build()
	);
	public final Setting<Integer> blockexploitchunksamountofrings = sgAlarm.add(new IntSetting.Builder()
			.name("Amount Of Rings")
			.description("How many times the alarm will ring when BlockExploitChunks detected.")
			.defaultValue(1)
			.sliderRange(1, 10)
			.min(1)
			.visible(() -> blockexploitchunksalarms.get())
			.build()
	);

	public final Setting<Integer> blockexploitchunksringdelay = sgAlarm.add(new IntSetting.Builder()
			.name("Delay Between Rings (ticks)")
			.description("The delay between rings (in ticks).")
			.defaultValue(20)
			.sliderRange(1, 100)
			.min(1)
			.visible(() -> blockexploitchunksalarms.get())
			.build()
	);
	public final Setting<Double> blockexploitchunksvolume = sgAlarm.add(new DoubleSetting.Builder()
			.name("Volume")
			.description("The volume of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.0, 1.0)
			.visible(() -> blockexploitchunksalarms.get())
			.build()
	);

	public final Setting<Double> blockexploitchunkspitch = sgAlarm.add(new DoubleSetting.Builder()
			.name("Pitch")
			.description("The pitch of the sound.")
			.defaultValue(1.0)
			.sliderRange(0.5, 2.0)
			.visible(() -> blockexploitchunksalarms.get())
			.build()
	);

	public final Setting<List<SoundEvent>> blockexploitchunkssoundtouse = sgAlarm.add(new SoundEventListSetting.Builder()
			.name("Sound to play (pick one)")
			.description("The sound to play. Just pick one.")
			.defaultValue(SoundEvents.BLOCK_BELL_USE)
			.visible(() -> blockexploitchunksalarms.get())
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
			.defaultValue(128)
			.min(6)
			.sliderRange(6,128)
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
	private ExecutorService taskExecutor;
	private int deletewarningTicks=666;
	private int ticks = 0;
	private int ringsLeft = 0;
	private boolean ringring = false;
	private int oldticks = 0;
	private int oldringsLeft = 0;
	private boolean oldringring = false;
	private int beingupdatedticks = 0;
	private int beingupdatedringsLeft = 0;
	private boolean beingupdatedringring = false;
	private int oldversionticks = 0;
	private int oldversionringsLeft = 0;
	private boolean oldversionringring = false;
	private int blockexploitticks = 0;
	private int blockexploitringsLeft = 0;
	private boolean blockexploitringring = false;
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
	private void clearChunkData() {
		newChunks.clear();
		oldChunks.clear();
		beingUpdatedOldChunks.clear();
		OldGenerationOldChunks.clear();
		tickexploitChunks.clear();
	}
	@Override
	public void onActivate() {
		taskExecutor = Executors.newCachedThreadPool();
		if (save.get())saveDataWasOn = true;
		else if (!save.get())saveDataWasOn = false;
		if (autoreload.get()) {
			clearChunkData();
		}
		if ((save.get() || load.get()) && mc.world != null) {
			world= mc.world.getRegistryKey().getValue().toString().replaceAll("[^a-zA-Z0-9._\\-]", "_");
			if (mc.isInSingleplayer()){
				Path worldPath = mc.getServer().getSavePath(WorldSavePath.ROOT);
				Path savesDir = worldPath.getParent();
				if (savesDir != null) {
					Path worldDir = savesDir.getFileName();
					serverip = (worldDir != null ? worldDir.toString() : "singleplayer")
							.replaceAll("[^a-zA-Z0-9._-]", "_");
				} else {
					serverip = "singleplayer";
				}
			} else {
				serverip = mc.getCurrentServerEntry().address.replaceAll("[^a-zA-Z0-9._\\-]", "_");
			}
		}
		if (save.get()){
			try {
				Files.createDirectories(FabricLoader.getInstance().getGameDir().resolve("TrouserStreak").resolve("NewChunks").resolve(serverip).resolve(world));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (save.get() || load.get()) {
			Path baseDir = FabricLoader.getInstance().getGameDir()
					.resolve("TrouserStreak")
					.resolve("NewChunks")
					.resolve(serverip)
					.resolve(world);

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
		ringring = false;
		ticks = 0;
		ringsLeft = 0;
		oldringring = false;
		oldticks = 0;
		oldringsLeft = 0;
		beingupdatedringring = false;
		beingupdatedticks = 0;
		beingupdatedringsLeft = 0;
		oldversionringring = false;
		oldversionticks = 0;
		oldversionringsLeft = 0;
		blockexploitringring = false;
		blockexploitticks = 0;
		blockexploitringsLeft = 0;
		autoreloadticks=0;
		loadingticks=0;
		worldchange=false;
		justenabledsavedata=0;
	}
	@Override
	public void onDeactivate() {
		taskExecutor.shutdownNow();
		ringring = false;
		ticks = 0;
		ringsLeft = 0;
		oldringring = false;
		oldticks = 0;
		oldringsLeft = 0;
		beingupdatedringring = false;
		beingupdatedticks = 0;
		beingupdatedringsLeft = 0;
		oldversionringring = false;
		oldversionticks = 0;
		oldversionringsLeft = 0;
		blockexploitringring = false;
		blockexploitticks = 0;
		blockexploitringsLeft = 0;
		autoreloadticks=0;
		loadingticks=0;
		worldchange=false;
		justenabledsavedata=0;
		if (remove.get() || autoreload.get()) {
			clearChunkData();
		}
		super.onDeactivate();
	}
	@EventHandler
	private void onScreenOpen(OpenScreenEvent event) {
		if (event.screen instanceof DisconnectedScreen) {
			if (worldleaveremove.get()) {
				clearChunkData();
			}
		}
		if (event.screen instanceof LevelLoadingScreen) {
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
		world= mc.world.getRegistryKey().getValue().toString().replaceAll("[^a-zA-Z0-9._\\-]", "_");

		if (deletewarningTicks<=100) deletewarningTicks++;
		else deletewarning=0;
		if (deletewarning>=2){
			if (mc.isInSingleplayer()){
				Path worldPath = mc.getServer().getSavePath(WorldSavePath.ROOT);
				Path savesDir = worldPath.getParent();
				if (savesDir != null) {
					Path worldDir = savesDir.getFileName();
					serverip = (worldDir != null ? worldDir.toString() : "singleplayer")
							.replaceAll("[^a-zA-Z0-9._-]", "_");
				} else {
					serverip = "singleplayer";
				}
			} else {
				serverip = mc.getCurrentServerEntry().address.replaceAll("[^a-zA-Z0-9._\\-]", "_");
			}
			clearChunkData();
			try {
				Path baseDir = FabricLoader.getInstance().getGameDir().resolve("TrouserStreak").resolve("NewChunks").resolve(serverip).resolve(world);
				Files.deleteIfExists(baseDir.resolve("NewChunkData.txt"));
				Files.deleteIfExists(baseDir.resolve("OldChunkData.txt"));
				Files.deleteIfExists(baseDir.resolve("BeingUpdatedChunkData.txt"));
				Files.deleteIfExists(baseDir.resolve("OldGenerationChunkData.txt"));
				Files.deleteIfExists(baseDir.resolve("BlockExploitChunkData.txt"));
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
		if (alarms.get()){
			if (ringring && ringsLeft > 0) {
				if (ticks <= 0) {
					playSound(1);
					ticks = ringdelay.get();
					ringsLeft--;
					if (ringsLeft <= 0) {
						ringring = false;
					}
				} else {
					ticks--;
				}
			}
		}
		if (oldalarms.get()){
			if (oldringring && oldringsLeft > 0) {
				if (oldticks <= 0) {
					playSound(2);
					oldticks = oldringdelay.get();
					oldringsLeft--;
					if (oldringsLeft <= 0) {
						oldringring = false;
					}
				} else {
					oldticks--;
				}
			}
		}
		if (beingupdatedchunksalarms.get()){
			if (beingupdatedringring && beingupdatedringsLeft > 0) {
				if (beingupdatedticks <= 0) {
					playSound(3);
					beingupdatedticks = beingupdatedchunksringdelay.get();
					beingupdatedringsLeft--;
					if (beingupdatedringsLeft <= 0) {
						beingupdatedringring = false;
					}
				} else {
					beingupdatedticks--;
				}
			}
		}
		if (oldversionchunksalarms.get()){
			if (oldversionringring && oldversionringsLeft > 0) {
				if (oldversionticks <= 0) {
					playSound(4);
					oldversionticks = oldversionchunksringdelay.get();
					oldversionringsLeft--;
					if (oldversionringsLeft <= 0) {
						oldversionringring = false;
					}
				} else {
					oldversionticks--;
				}
			}
		}
		if (blockexploitchunksalarms.get()){
			if (blockexploitringring && blockexploitringsLeft > 0) {
				if (blockexploitticks <= 0) {
					playSound(5);
					blockexploitticks = blockexploitchunksringdelay.get();
					blockexploitringsLeft--;
					if (blockexploitringsLeft <= 0) {
						blockexploitringring = false;
					}
				} else {
					blockexploitticks--;
				}
			}
		}
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
						} else if ((detectmode.get()== DetectMode.BlockExploitMode || detectmode.get()== DetectMode.Normal || detectmode.get()== DetectMode.IgnoreBlockExploit) && !blockupdateexploit.get()) {
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
		} catch (Exception e) {e.printStackTrace();}
	}

	@EventHandler
	private void onReadPacket(PacketEvent.Receive event) {
		if (event.packet instanceof AcknowledgeChunksC2SPacket) return; //for some reason this packet keeps getting cast to other packets
		if (event.packet instanceof ChunkDeltaUpdateS2CPacket packet && liquidexploit.get()) {

			packet.visitUpdates((pos, state) -> {
				ChunkPos chunkPos = new ChunkPos(pos);
				if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
					for (Direction dir: searchDirs) {
						try {
							if (mc.world != null && mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
								tickexploitChunks.remove(chunkPos);
								newChunks.add(chunkPos);
								if (alarms.get()) {
									ringring = true;
									ringsLeft = amountofrings.get();
								}
								if (save.get()){
									saveData(Paths.get("NewChunkData.txt"), chunkPos);
								}
								return;
							}
						} catch (Exception e) {e.printStackTrace();}
					}
				}
			});
		}
		else if (event.packet instanceof BlockUpdateS2CPacket packet) {
			ChunkPos chunkPos = new ChunkPos(packet.getPos());
			if (blockupdateexploit.get()){
				try {
					if (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !tickexploitChunks.contains(chunkPos) && !oldChunks.contains(chunkPos) && !newChunks.contains(chunkPos)){
						tickexploitChunks.add(chunkPos);
						if (blockexploitchunksalarms.get()) {
							blockexploitringring = true;
							blockexploitringsLeft = blockexploitchunksamountofrings.get();
						}
						if (save.get()){
							saveData(Paths.get("BlockExploitChunkData.txt"), chunkPos);
						}
					}
				}
				catch (Exception e){e.printStackTrace();}
			}
			if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill() && liquidexploit.get()) {
				for (Direction dir: searchDirs) {
					try {
						if (mc.world != null && mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos))) {
							tickexploitChunks.remove(chunkPos);
							newChunks.add(chunkPos);
							if (alarms.get()) {
								ringring = true;
								ringsLeft = amountofrings.get();
							}
							if (save.get()){
								saveData(Paths.get("NewChunkData.txt"), chunkPos);
							}
							return;
						}
					} catch (Exception e) {e.printStackTrace();}
				}
			}
		}
		else if (!(event.packet instanceof PlayerMoveC2SPacket) && event.packet instanceof ChunkDataS2CPacket packet && mc.world != null) {
			ChunkPos oldpos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());

			if (mc.world.getChunkManager().getChunk(packet.getChunkX(), packet.getChunkZ()) == null) {
				WorldChunk chunk = new WorldChunk(mc.world, oldpos);
				try {
					NbtCompound heightmapsNbt = new NbtCompound();
					NbtCompound motionBlocking = new NbtCompound();
					Heightmap.Type type = Heightmap.Type.MOTION_BLOCKING;

					long[] emptyHeightmapData = new long[37];
					motionBlocking.putLongArray("data", emptyHeightmapData);
					heightmapsNbt.put(type.getName(), motionBlocking);

					chunk.loadFromPacket(
							packet.getChunkData().getSectionsDataBuf(),
							heightmapsNbt,
							packet.getChunkData().getBlockEntities(packet.getChunkX(), packet.getChunkZ())
					);
				} catch (CompletionException e) {e.printStackTrace();}

				boolean isNewChunk = false;
				boolean isOldGeneration = false;
				boolean chunkIsBeingUpdated = false;
				boolean foundAnyOre = false;
				boolean isNewOverworldGeneration = false;
				boolean isNewNetherGeneration = false;
				ChunkSection[] sections = chunk.getSectionArray();
				int safeamountofsectionstoscan = 17;
				if (sections.length < 17) safeamountofsectionstoscan = sections.length;
				if (overworldOldChunksDetector.get() && mc.world.getRegistryKey() == World.OVERWORLD && chunk.getStatus().isAtLeast(ChunkStatus.FULL) && !chunk.isEmpty()) {
					for (int i = 0; i < safeamountofsectionstoscan; i++) {
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
								if (alarms.get()) {
									ringring = true;
									ringsLeft = amountofrings.get();
								}
								if (save.get()) {
									saveData(Paths.get("NewChunkData.txt"), oldpos);
								}
								return;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					else if (!isNewChunk && !chunkIsBeingUpdated && isOldGeneration) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								OldGenerationOldChunks.add(oldpos);
								if (oldversionchunksalarms.get()) {
									oldversionringring = true;
									oldversionringsLeft = oldversionchunksamountofrings.get();
								}
								if (save.get()){
									saveData(Paths.get("OldGenerationChunkData.txt"), oldpos);
								}
								return;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					else if (chunkIsBeingUpdated) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								beingUpdatedOldChunks.add(oldpos);
								if (beingupdatedchunksalarms.get()) {
									beingupdatedringring = true;
									beingupdatedringsLeft = beingupdatedchunksamountofrings.get();
								}
								if (save.get()){
									saveData(Paths.get("BeingUpdatedChunkData.txt"), oldpos);
								}
								return;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					else if (!isNewChunk) {
						try {
							if (!OldGenerationOldChunks.contains(oldpos) && !beingUpdatedOldChunks.contains(oldpos) && !tickexploitChunks.contains(oldpos) && !oldChunks.contains(oldpos) && !newChunks.contains(oldpos)) {
								oldChunks.add(oldpos);
								if (oldalarms.get()) {
									oldringring = true;
									oldringsLeft = oldamountofrings.get();
								}
								if (save.get()) {
									saveData(Paths.get("OldChunkData.txt"), oldpos);
								}
								return;
							}
						} catch (Exception e) {
							e.printStackTrace();
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
										if (oldalarms.get()) {
											oldringring = true;
											oldringsLeft = oldamountofrings.get();
										}
										if (save.get()){
											saveData(Paths.get("OldChunkData.txt"), oldpos);
										}
										return;
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
	}
	private void loadData() {
		Path baseDir = FabricLoader.getInstance().getGameDir()
				.resolve("TrouserStreak").resolve("NewChunks").resolve(serverip).resolve(world);

		loadChunkData(baseDir.resolve("BlockExploitChunkData.txt"), tickexploitChunks);
		loadChunkData(baseDir.resolve("OldChunkData.txt"), oldChunks);
		loadChunkData(baseDir.resolve("NewChunkData.txt"), newChunks);
		loadChunkData(baseDir.resolve("BeingUpdatedChunkData.txt"), beingUpdatedOldChunks);
		loadChunkData(baseDir.resolve("OldGenerationChunkData.txt"), OldGenerationOldChunks);
	}

	private void loadChunkData(Path savedDataLocation, Set<ChunkPos> chunkSet) {
		try {
			if (!Files.exists(savedDataLocation)) return;

			List<String> allLines = Files.readAllLines(savedDataLocation);

			for (String line : allLines) {
				if (line != null && !line.isEmpty()) {
					String[] array = line.split(",");
					if (array.length == 2) {
						int X = Integer.parseInt(array[0].trim());
						int Z = Integer.parseInt(array[1].trim());
						ChunkPos chunkPos = new ChunkPos(X, Z);
						if (!OldGenerationOldChunks.contains(chunkPos) && !beingUpdatedOldChunks.contains(chunkPos) && !tickexploitChunks.contains(chunkPos) && !newChunks.contains(chunkPos) && !oldChunks.contains(chunkPos)) {
							chunkSet.add(chunkPos);
						}
					}
				}
			}
		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
		}
	}

	private void saveData(Path savedDataLocation, ChunkPos chunkpos) {
		taskExecutor.submit(() -> {
			try {
				Path baseDir = FabricLoader.getInstance().getGameDir()
						.resolve("TrouserStreak").resolve("NewChunks").resolve(serverip).resolve(world);
				Files.createDirectories(baseDir);
				Path filePath = baseDir.resolve(savedDataLocation);
				String data = chunkpos.x + "," + chunkpos.z + System.lineSeparator();
				Files.write(filePath, data.getBytes(StandardCharsets.UTF_8),
						StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
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
	private void playSound(int soundtype) {
		if (mc.player != null) {
			Vec3d pos = mc.player.getPos();
			SoundEvent sound = soundtouse.get().get(0);
			if (soundtype == 2) sound = oldsoundtouse.get().get(0);
			else if (soundtype == 3) sound = beingupdatedchunkssoundtouse.get().get(0);
			else if (soundtype == 4) sound = oldversionchunkssoundtouse.get().get(0);
			else if (soundtype == 5) sound = blockexploitchunkssoundtouse.get().get(0);
			float volumeSetting = volume.get().floatValue();
			float pitchSetting = pitch.get().floatValue();

			mc.world.playSound(pos.x, pos.y, pos.z, sound, mc.player.getSoundCategory(), volumeSetting, pitchSetting, false);
		}
	}
}