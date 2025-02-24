//made by etianl :D
package pwn.noobs.trouserstreak.modules;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
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
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.block.enums.TrialSpawnerState;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class ActivatedSpawnerDetector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgLocations = settings.createGroup("Location Toggles");
    private final SettingGroup locationLogs = settings.createGroup("Location Logs");
    private final Setting<Boolean> trialSpawner = sgLocations.add(new BoolSetting.Builder()
            .name("Trial Spawner Detector")
            .description("Detects activated Trial Spawners.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> showmoremenu = sgLocations.add(new BoolSetting.Builder()
            .name("Show More Location Toggles")
            .description("Make the options menu bigger or smaller.")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> enableDungeon = sgLocations.add(new BoolSetting.Builder()
            .name("Enable Dungeon")
            .description("Enable detection for dungeons.")
            .defaultValue(true)
            .visible(showmoremenu::get)
            .build());

    private final Setting<Boolean> enableMineshaft = sgLocations.add(new BoolSetting.Builder()
            .name("Enable Mineshaft")
            .description("Enable detection for mineshafts.")
            .defaultValue(true)
            .visible(showmoremenu::get)
            .build());

    private final Setting<Boolean> enableBastion = sgLocations.add(new BoolSetting.Builder()
            .name("Enable Bastion")
            .description("Enable detection for bastions.")
            .defaultValue(true)
            .visible(showmoremenu::get)
            .build());

    private final Setting<Boolean> enableWoodlandMansion = sgLocations.add(new BoolSetting.Builder()
            .name("Enable Woodland Mansion")
            .description("Enable detection for woodland mansions.")
            .defaultValue(true)
            .visible(showmoremenu::get)
            .build());
    private final Setting<Boolean> enableFortress = sgLocations.add(new BoolSetting.Builder()
            .name("Enable Fortress")
            .description("Enable detection for fortresses.")
            .defaultValue(true)
            .visible(showmoremenu::get)
            .build());
    private final Setting<Boolean> enableStronghold = sgLocations.add(new BoolSetting.Builder()
            .name("Enable Stronghold")
            .description("Enable detection for strongholds.")
            .defaultValue(true)
            .visible(showmoremenu::get)
            .build());
    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("Chat feedback")
            .description("Display info about spawners in chat.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> displaycoords = sgGeneral.add(new BoolSetting.Builder()
            .name("DisplayCoords")
            .description("Displays coords of activated spawners in chat.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> extramessage = sgGeneral.add(new BoolSetting.Builder()
            .name("Stash Message")
            .description("Toggle the message reminding you about stashes.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> lessSpam = sgGeneral.add(new BoolSetting.Builder()
            .name("Less Stash Spam")
            .description("Do not display the message reminding you about stashes if NO chests within 16 blocks of spawner.")
            .defaultValue(true)
            .visible(extramessage::get)
            .build()
    );
    private final Setting<Boolean> airChecker = sgGeneral.add(new BoolSetting.Builder()
            .name("Check Air Disturbances")
            .description("Displays spawners as activated if there are disturbances in the air around them. For example if a torch was placed and removed it will detect that. THERE CAN BE SOME FALSE POSITIVES WITH THIS!")
            .defaultValue(false)
            .build()
    );
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("Storage Blocks")
            .description("Storage Blocks the module checks for when considering displaying messages and renders.")
            .defaultValue(Blocks.CHEST, Blocks.BARREL, Blocks.HOPPER, Blocks.DISPENSER)
            .build()
    );
    private final Setting<Boolean> deactivatedSpawner = sgGeneral.add(new BoolSetting.Builder()
            .name("De-Activated Spawner Detector")
            .description("Detects spawners with torches on them.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> deactivatedSpawnerdistance = sgGeneral.add(new IntSetting.Builder()
            .name("Torch Scan distance")
            .description("How many blocks from the spawner to look for blocks that make light")
            .defaultValue(1)
            .min(1)
            .sliderRange(1,10)
            .visible(deactivatedSpawner::get)
            .build()
    );
    private final Setting<Boolean> lessRenderSpam = sgRender.add(new BoolSetting.Builder()
            .name("Less Render Spam")
            .description("Do not render big box if NO chests within range of spawner.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
            .name("Render-Distance(Chunks)")
            .description("How many chunks from the character to render the detected spawners.")
            .defaultValue(32)
            .min(6)
            .sliderRange(6,1024)
            .build()
    );
    private final Setting<Boolean> removerenderdist = sgRender.add(new BoolSetting.Builder()
            .name("RemoveOutsideRenderDistance")
            .description("Removes the cached block positions when they leave the defined render distance.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> trcr = sgRender.add(new BoolSetting.Builder()
            .name("Tracers")
            .description("Show tracers to the Spawner.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> nearesttrcr = sgRender.add(new BoolSetting.Builder()
            .name("Tracer to nearest Spawner Only")
            .description("Show only one tracer to the nearest Spawner.")
            .defaultValue(false)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> spawnerSideColor = sgRender.add(new ColorSetting.Builder()
            .name("spawner-side-color")
            .description("Color of the activated spawner.")
            .defaultValue(new SettingColor(251, 5, 5, 70))
            .visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> spawnerLineColor = sgRender.add(new ColorSetting.Builder()
            .name("spawner-line-color")
            .description("Color of the activated spawner.")
            .defaultValue(new SettingColor(251, 5, 5, 235))
            .visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both || trcr.get()))
            .build()
    );
    private final Setting<SettingColor> trialSideColor = sgRender.add(new ColorSetting.Builder()
            .name("trial-side-color")
            .description("Color of the activated trial spawner.")
            .defaultValue(new SettingColor(255, 100, 0, 70))
            .visible(() -> trialSpawner.get() && (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> trialLineColor = sgRender.add(new ColorSetting.Builder()
            .name("trial-line-color")
            .description("Color of the activated trial spawner.")
            .defaultValue(new SettingColor(255, 100, 0, 235))
            .visible(() -> trialSpawner.get() && (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both || trcr.get()))
            .build()
    );
    private final Setting<SettingColor> despawnerSideColor = sgRender.add(new ColorSetting.Builder()
            .name("deactivated-spawner-side-color")
            .description("Color of the spawner with torches.")
            .defaultValue(new SettingColor(251, 5, 251, 70))
            .visible(() -> deactivatedSpawner.get() && (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> despawnerLineColor = sgRender.add(new ColorSetting.Builder()
            .name("deactivated-spawner-line-color")
            .description("Color of the spawner with torches.")
            .defaultValue(new SettingColor(251, 5, 251, 235))
            .visible(() -> deactivatedSpawner.get() && (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<Boolean> rangerendering = sgRender.add(new BoolSetting.Builder()
            .name("spawner-range-rendering")
            .description("Renders the rough active range of a mob spawner block.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> rangeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("spawner-range-side-color")
            .description("Color of the active spawner range.")
            .defaultValue(new SettingColor(5, 178, 251, 30))
            .visible(() -> rangerendering.get() && (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> rangeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("spawner-range-line-color")
            .description("Color of the active spawner range.")
            .defaultValue(new SettingColor(5, 178, 251, 155))
            .visible(() -> rangerendering.get() && (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> trangeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("trial-range-side-color")
            .description("Color of the active trial spawner range.")
            .defaultValue(new SettingColor(150, 178, 251, 30))
            .visible(() -> trialSpawner.get() && rangerendering.get() && (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> trangeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("trial-range-line-color")
            .description("Color of the active trial spawner range.")
            .defaultValue(new SettingColor(150, 178, 251, 155))
            .visible(() -> trialSpawner.get() && rangerendering.get() && (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<Boolean> locLogging = locationLogs.add(new BoolSetting.Builder()
            .name("Enable Location Logging")
            .description("Logs the locations of detected spawners to a csv file as well as a table in this options menu.")
            .defaultValue(false)
            .build()
    );
    private final List<LoggedSpawner> loggedSpawners = new ArrayList<>();
    private final Set<BlockPos> loggedSpawnerPositions = new HashSet<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<BlockPos> scannedPositions = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> spawnerPositions = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> trialspawnerPositions = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> deactivatedSpawnerPositions = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> noRenderPositions = Collections.synchronizedSet(new HashSet<>());
    private int closestSpawnerX = 2000000000;
    private int closestSpawnerY = 2000000000;
    private int closestSpawnerZ = 2000000000;
    private double SpawnerDistance = 2000000000;
    private boolean activatedSpawnerFound = false;

    public ActivatedSpawnerDetector() {
        super(Trouser.Main, "ActivatedSpawnerDetector", "Detects if a player has been near a mob spawner in the past. May be useful for finding player made stashes in dungeons, mineshafts, and other places.");
    }
    @Override
    public void onActivate() {
        clearChunkData();
        loadLogs();
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
        scannedPositions.clear();
        spawnerPositions.clear();
        deactivatedSpawnerPositions.clear();
        noRenderPositions.clear();
        trialspawnerPositions.clear();
        closestSpawnerX = 2000000000;
        closestSpawnerY = 2000000000;
        closestSpawnerZ = 2000000000;
        SpawnerDistance = 2000000000;
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        int renderdistance = mc.options.getViewDistance().getValue();
        ChunkPos playerChunkPos = new ChunkPos(mc.player.getBlockPos());
        for (int chunkX = playerChunkPos.x - renderdistance; chunkX <= playerChunkPos.x + renderdistance; chunkX++) {
            for (int chunkZ = playerChunkPos.z - renderdistance; chunkZ <= playerChunkPos.z + renderdistance; chunkZ++) {
                WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
                List<BlockEntity> blockEntities = new ArrayList<>(chunk.getBlockEntities().values());

                for (BlockEntity blockEntity : blockEntities) {
                    if (blockEntity instanceof MobSpawnerBlockEntity){
                        activatedSpawnerFound = false;
                        MobSpawnerBlockEntity spawner = (MobSpawnerBlockEntity) blockEntity;
                        BlockPos pos = spawner.getPos();
                        BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
                        String monster = null;
                        if (spawner.getLogic().spawnEntry != null && spawner.getLogic().spawnEntry.getNbt().get("id") != null) monster = spawner.getLogic().spawnEntry.getNbt().get("id").toString();
                        if (playerPos.isWithinDistance(pos, renderDistance.get() * 16) && !trialspawnerPositions.contains(pos) && !noRenderPositions.contains(pos) && !deactivatedSpawnerPositions.contains(pos) && !spawnerPositions.contains(pos)) {
                            if (airChecker.get() && (spawner.getLogic().spawnDelay == 20 || spawner.getLogic().spawnDelay == 0)) {
                                boolean airFound = false;
                                boolean caveAirFound = false;
                                if (monster != null && !scannedPositions.contains(pos)) {
                                    if (monster.contains("zombie") || monster.contains("skeleton") || monster.contains(":spider")) {
                                        for (int x = -2; x < 2; x++) {
                                            for (int y = -1; y < 3; y++) {
                                                for (int z = -2; z < 2; z++) {
                                                    BlockPos bpos = new BlockPos(pos.getX()+x,pos.getY()+y,pos.getZ()+z);
                                                    if (mc.world.getBlockState(bpos).getBlock() == Blocks.AIR) airFound = true;
                                                    if (mc.world.getBlockState(bpos).getBlock() == Blocks.CAVE_AIR) caveAirFound = true;
                                                    if (caveAirFound && airFound) break;
                                                }
                                            }
                                        }
                                        if (caveAirFound && airFound) {
                                            if (monster == ":spider") displayMessage("dungeon", pos, ":spider");
                                            else displayMessage("dungeon", pos, "null");
                                        }
                                    } else if (monster.contains("cave_spider")) {
                                        for (int x = -1; x < 2; x++) {
                                            for (int y = 0; y < 2; y++) {
                                                for (int z = -1; z < 2; z++) {
                                                    BlockPos bpos = new BlockPos(pos.getX()+x,pos.getY()+y,pos.getZ()+z);
                                                    if (mc.world.getBlockState(bpos).getBlock() == Blocks.AIR) airFound = true;
                                                    if (mc.world.getBlockState(bpos).getBlock() == Blocks.CAVE_AIR) caveAirFound = true;
                                                    if (caveAirFound && airFound) break;
                                                }
                                            }
                                        }
                                        if (caveAirFound && airFound) {
                                            displayMessage("cave_spider", pos, "null");
                                        }
                                    } else if (monster.contains("silverfish")) {
                                        for (int x = -3; x < 3+1; x++) {
                                            for (int y = -2; y < 3+1; y++) {
                                                for (int z = -3; z < 3+1; z++) {
                                                    BlockPos bpos = new BlockPos(pos.getX()+x,pos.getY()+y,pos.getZ()+z);
                                                    if (mc.world.getBlockState(bpos).getBlock() == Blocks.AIR) airFound = true;
                                                    if (mc.world.getBlockState(bpos).getBlock() == Blocks.CAVE_AIR) caveAirFound = true;
                                                    if (caveAirFound && airFound) break;
                                                }
                                            }
                                        }
                                        if (caveAirFound && airFound) {
                                            displayMessage("silverfish", pos, "null");
                                        }
                                    }
                                }
                                scannedPositions.add(pos);
                            } else if (spawner.getLogic().spawnDelay != 20) {
                                if (mc.world.getRegistryKey() == World.NETHER && spawner.getLogic().spawnDelay == 0) return;
                                if (chatFeedback.get()) {
                                    if (monster != null){
                                        if (monster.contains("zombie") || monster.contains("skeleton") || monster.contains(":spider")) {
                                            if (monster == ":spider") displayMessage("dungeon", pos, ":spider");
                                            else displayMessage("dungeon", pos, "null");
                                        } else if (monster.contains("cave_spider")) {
                                            displayMessage("cave_spider", pos, "null");
                                        } else if (monster.contains("silverfish")) {
                                            displayMessage("silverfish", pos, "null");
                                        } else if (monster.contains("blaze")) {
                                            displayMessage("blaze", pos, "null");
                                        } else if (monster.contains("magma")) {
                                            displayMessage("magma", pos, "null");
                                        } else {
                                            if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated Spawner! Block Position: " + pos));
                                            else ChatUtils.sendMsg(Text.of("Detected Activated Spawner!"));
                                            spawnerPositions.add(pos);
                                            activatedSpawnerFound = true;
                                            if (locLogging.get())logSpawner(pos);
                                        }
                                    } else {
                                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated Spawner! Block Position: " + pos));
                                        else ChatUtils.sendMsg(Text.of("Detected Activated Spawner!"));
                                        spawnerPositions.add(pos);
                                        activatedSpawnerFound = true;
                                        if (locLogging.get())logSpawner(pos);
                                    }
                                }
                            }
                            if (activatedSpawnerFound == true) {
                                if (deactivatedSpawner.get()){
                                    boolean lightsFound = false;
                                    for (int x = -deactivatedSpawnerdistance.get(); x < deactivatedSpawnerdistance.get()+1; x++) {
                                        for (int y = -deactivatedSpawnerdistance.get(); y < deactivatedSpawnerdistance.get()+1; y++) {
                                            for (int z = -deactivatedSpawnerdistance.get(); z < deactivatedSpawnerdistance.get()+1; z++) {
                                                BlockPos bpos = new BlockPos(pos.getX()+x,pos.getY()+y,pos.getZ()+z);
                                                if (mc.world.getBlockState(bpos).getBlock() == Blocks.TORCH || mc.world.getBlockState(bpos).getBlock() == Blocks.SOUL_TORCH || mc.world.getBlockState(bpos).getBlock() == Blocks.REDSTONE_TORCH || mc.world.getBlockState(bpos).getBlock() == Blocks.JACK_O_LANTERN || mc.world.getBlockState(bpos).getBlock() == Blocks.GLOWSTONE || mc.world.getBlockState(bpos).getBlock() == Blocks.SHROOMLIGHT || mc.world.getBlockState(bpos).getBlock() == Blocks.OCHRE_FROGLIGHT || mc.world.getBlockState(bpos).getBlock() == Blocks.PEARLESCENT_FROGLIGHT || mc.world.getBlockState(bpos).getBlock() == Blocks.PEARLESCENT_FROGLIGHT || mc.world.getBlockState(bpos).getBlock() == Blocks.SEA_LANTERN || mc.world.getBlockState(bpos).getBlock() == Blocks.LANTERN || mc.world.getBlockState(bpos).getBlock() == Blocks.SOUL_LANTERN || mc.world.getBlockState(bpos).getBlock() == Blocks.CAMPFIRE || mc.world.getBlockState(bpos).getBlock() == Blocks.SOUL_CAMPFIRE){
                                                    lightsFound = true;
                                                    deactivatedSpawnerPositions.add(pos);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (chatFeedback.get() && lightsFound == true) ChatUtils.sendMsg(Text.of("The Spawner has torches or other light blocks!"));
                                }

                                boolean chestfound = false;
                                for (int x = -16; x < 17; x++) {
                                    for (int y = -16; y < 17; y++) {
                                        for (int z = -16; z < 17; z++) {
                                            BlockPos bpos = new BlockPos(pos.getX()+x, pos.getY()+y, pos.getZ()+z);
                                            if (blocks.get().contains(mc.world.getBlockState(bpos).getBlock())) {
                                                chestfound = true;
                                                break;
                                            }
                                            Box box = new Box(bpos);
                                            List<ChestMinecartEntity> minecarts = mc.world.getEntitiesByClass(ChestMinecartEntity.class, box, entity -> true);
                                            if (!minecarts.isEmpty()) {
                                                chestfound = true;
                                                break;
                                            }
                                        }
                                        if (chestfound) break;
                                    }
                                    if (chestfound) break;
                                }
                                if (!chestfound && lessRenderSpam.get()){
                                    noRenderPositions.add(pos);
                                }
                                if (chatFeedback.get()) {
                                    if (lessSpam.get() && chestfound && extramessage.get()) error("There may be stashed items in the storage near the spawners!");
                                    else if (!lessSpam.get() && extramessage.get()) error("There may be stashed items in the storage near the spawners!");
                                }
                            }
                        }
                    }
                    if (blockEntity instanceof TrialSpawnerBlockEntity) {
                        TrialSpawnerBlockEntity trialspawner = (TrialSpawnerBlockEntity) blockEntity;
                        BlockPos tPos = trialspawner.getPos();
                        BlockPos playerPos = new BlockPos(mc.player.getBlockX(), tPos.getY(), mc.player.getBlockZ());
                        if (playerPos.isWithinDistance(tPos, renderDistance.get() * 16) && trialSpawner.get() && !trialspawnerPositions.contains(tPos) && !noRenderPositions.contains(tPos) && !deactivatedSpawnerPositions.contains(tPos) && !spawnerPositions.contains(tPos) && trialspawner.getSpawnerState() != TrialSpawnerState.WAITING_FOR_PLAYERS) {
                            if (chatFeedback.get()) {
                                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cTRIAL§r Spawner! Block Position: " + tPos));
                                else ChatUtils.sendMsg(Text.of("Detected Activated §cTRIAL§r Spawner!"));
                            }
                            trialspawnerPositions.add(tPos);
                            boolean chestfound = false;
                            for (int x = -14; x < 15; x++) {
                                for (int y = -14; y < 15; y++) {
                                    for (int z = -14; z < 15; z++) {
                                        BlockPos bpos = new BlockPos(tPos.getX() + x, tPos.getY() + y, tPos.getZ() + z);
                                        if (blocks.get().contains(mc.world.getBlockState(bpos).getBlock())) {
                                            chestfound = true;
                                            break;
                                        }
                                        Box box = new Box(bpos);
                                        List<ChestMinecartEntity> minecarts = mc.world.getEntitiesByClass(ChestMinecartEntity.class, box, entity -> true);
                                        if (!minecarts.isEmpty()) {
                                            chestfound = true;
                                            break;
                                        }
                                    }
                                    if (chestfound) break;
                                }
                                if (chestfound) break;
                            }
                            if (!chestfound && lessRenderSpam.get()) {
                                noRenderPositions.add(tPos);
                            }
                            if (chatFeedback.get()) {
                                if (lessSpam.get() && chestfound && extramessage.get()) error("There may be stashed items in the storage near the spawners!");
                                else if (!lessSpam.get() && extramessage.get()) error("There may be stashed items in the storage near the spawners!");
                            }
                            if (locLogging.get())logSpawner(tPos);
                        }
                    }
                }
            }
        }
        if (nearesttrcr.get()){
            try {
                Set<BlockPos> CombinedPositions = Collections.synchronizedSet(new HashSet<>());
                CombinedPositions.addAll(spawnerPositions);
                CombinedPositions.addAll(deactivatedSpawnerPositions);
                CombinedPositions.addAll(trialspawnerPositions);

                if (CombinedPositions.stream().toList().size() > 0) {
                    for (int b = 0; b < CombinedPositions.stream().toList().size(); b++) {
                        if (SpawnerDistance > Math.sqrt(Math.pow(CombinedPositions.stream().toList().get(b).getX() - mc.player.getBlockX(), 2) + Math.pow(CombinedPositions.stream().toList().get(b).getZ() - mc.player.getBlockZ(), 2))) {
                            closestSpawnerX = Math.round((float) CombinedPositions.stream().toList().get(b).getX());
                            closestSpawnerY = Math.round((float) CombinedPositions.stream().toList().get(b).getY());
                            closestSpawnerZ = Math.round((float) CombinedPositions.stream().toList().get(b).getZ());
                            SpawnerDistance = Math.sqrt(Math.pow(CombinedPositions.stream().toList().get(b).getX() - mc.player.getBlockX(), 2) + Math.pow(CombinedPositions.stream().toList().get(b).getZ() - mc.player.getBlockZ(), 2));
                        }
                    }
                    SpawnerDistance = 2000000000;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (removerenderdist.get()) removeChunksOutsideRenderDistance();
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (spawnerSideColor.get().a > 5 || spawnerLineColor.get().a > 5 || rangeSideColor.get().a > 5 || rangeLineColor.get().a > 5) {
            synchronized (spawnerPositions) {
                for (BlockPos pos : spawnerPositions) {
                    BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
                    if (pos != null && playerPos.isWithinDistance(pos, renderDistance.get() * 16)) {
                        int startX = pos.getX();
                        int startY = pos.getY();
                        int startZ = pos.getZ();
                        int endX = pos.getX();
                        int endY = pos.getY();
                        int endZ = pos.getZ();
                        if (!nearesttrcr.get()) {
                            if (rangerendering.get() && !lessRenderSpam.get())renderRange(new Box(new Vec3d(startX+17, startY+17, startZ+17), new Vec3d(endX-16, endY-16, endZ-16)), rangeSideColor.get(), rangeLineColor.get(), shapeMode.get(), event);
                            else if (rangerendering.get() && lessRenderSpam.get() && !noRenderPositions.contains(pos))renderRange(new Box(new Vec3d(startX+17, startY+17, startZ+17), new Vec3d(endX-16, endY-16, endZ-16)), rangeSideColor.get(), rangeLineColor.get(), shapeMode.get(), event);
                            if (deactivatedSpawnerPositions.contains(pos)) render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), despawnerSideColor.get(), despawnerLineColor.get(), shapeMode.get(), event);
                            else render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), spawnerSideColor.get(), spawnerLineColor.get(), shapeMode.get(), event);
                        } else if (nearesttrcr.get()) {
                            if (rangerendering.get() && !lessRenderSpam.get())renderRange(new Box(new Vec3d(startX+17, startY+17, startZ+17), new Vec3d(endX-16, endY-16, endZ-16)), rangeSideColor.get(), rangeLineColor.get(), shapeMode.get(), event);
                            else if (rangerendering.get() && lessRenderSpam.get() && !noRenderPositions.contains(pos))renderRange(new Box(new Vec3d(startX+17, startY+17, startZ+17), new Vec3d(endX-16, endY-16, endZ-16)), rangeSideColor.get(), rangeLineColor.get(), shapeMode.get(), event);
                            if (deactivatedSpawnerPositions.contains(pos)) render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), despawnerSideColor.get(), despawnerLineColor.get(), shapeMode.get(), event);
                            else render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), spawnerSideColor.get(), spawnerLineColor.get(), shapeMode.get(), event);
                            render2(new Box(new Vec3d(closestSpawnerX, closestSpawnerY, closestSpawnerZ), new Vec3d (closestSpawnerX, closestSpawnerY, closestSpawnerZ)), spawnerSideColor.get(), spawnerLineColor.get(),ShapeMode.Sides, event);
                        }
                    }
                }
            }
            synchronized (trialspawnerPositions) {
                for (BlockPos pos : trialspawnerPositions) {
                    BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
                    if (pos != null && playerPos.isWithinDistance(pos, renderDistance.get() * 16)) {
                        int startX = pos.getX();
                        int startY = pos.getY();
                        int startZ = pos.getZ();
                        int endX = pos.getX();
                        int endY = pos.getY();
                        int endZ = pos.getZ();
                        if (!nearesttrcr.get()) {
                            if (trialSpawner.get() && rangerendering.get() && !lessRenderSpam.get())renderRange(new Box(new Vec3d(startX+15, startY+15, startZ+15), new Vec3d(endX-14, endY-14, endZ-14)), trangeSideColor.get(), trangeLineColor.get(), shapeMode.get(), event);
                            else if (trialSpawner.get() && rangerendering.get() && lessRenderSpam.get() && !noRenderPositions.contains(pos))renderRange(new Box(new Vec3d(startX+15, startY+15, startZ+15), new Vec3d(endX-14, endY-14, endZ-14)), trangeSideColor.get(), trangeLineColor.get(), shapeMode.get(), event);
                            if (deactivatedSpawnerPositions.contains(pos)) render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), despawnerSideColor.get(), despawnerLineColor.get(), shapeMode.get(), event);
                            else render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), trialSideColor.get(), trialLineColor.get(), shapeMode.get(), event);
                        } else if (nearesttrcr.get()) {
                            if (trialSpawner.get() && rangerendering.get() && !lessRenderSpam.get())renderRange(new Box(new Vec3d(startX+15, startY+15, startZ+15), new Vec3d(endX-14, endY-14, endZ-14)), trangeSideColor.get(), trangeLineColor.get(), shapeMode.get(), event);
                            else if (trialSpawner.get() && rangerendering.get() && lessRenderSpam.get() && !noRenderPositions.contains(pos))renderRange(new Box(new Vec3d(startX+15, startY+15, startZ+15), new Vec3d(endX-14, endY-14, endZ-14)), trangeSideColor.get(), trangeLineColor.get(), shapeMode.get(), event);
                            if (deactivatedSpawnerPositions.contains(pos)) render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), despawnerSideColor.get(), despawnerLineColor.get(), shapeMode.get(), event);
                            else render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), trialSideColor.get(), trialLineColor.get(), shapeMode.get(), event);
                            render2(new Box(new Vec3d(closestSpawnerX, closestSpawnerY, closestSpawnerZ), new Vec3d (closestSpawnerX, closestSpawnerY, closestSpawnerZ)), trialSideColor.get(), trialLineColor.get(),ShapeMode.Sides, event);
                        }
                    }
                }
            }
        }
    }
    private void displayMessage(String key, BlockPos pos, String key2) {
        if (chatFeedback.get()) {
            if (key=="dungeon") {
                if (key2==":spider") {
                    if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.BIRCH_PLANKS && enableWoodlandMansion.get()) {
                        activatedSpawnerFound = true;
                        spawnerPositions.add(pos);
                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cWOODLAND MANSION§r Spawner! Block Position: " + pos));
                        else ChatUtils.sendMsg(Text.of("Detected Activated §cWOODLAND MANSION§r Spawner!"));
                    } else {
                        if (enableDungeon.get()) {
                            activatedSpawnerFound = true;
                            spawnerPositions.add(pos);
                            if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cDUNGEON§r Spawner! Block Position: " + pos));
                            else ChatUtils.sendMsg(Text.of("Detected Activated §cDUNGEON§r Spawner!"));
                        }
                    }
                } else {
                    if (enableDungeon.get()) {
                        activatedSpawnerFound = true;
                        spawnerPositions.add(pos);
                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cDUNGEON§r Spawner! Block Position: " + pos));
                        else ChatUtils.sendMsg(Text.of("Detected Activated §cDUNGEON§r Spawner!"));
                    }
                }
            } else if (key=="cave_spider" && enableMineshaft.get()) {
                activatedSpawnerFound = true;
                spawnerPositions.add(pos);
                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cMINESHAFT§r Spawner! Block Position: " + pos));
                else ChatUtils.sendMsg(Text.of("Detected Activated §cMINESHAFT§r Spawner!"));
            } else if (key=="silverfish" && enableStronghold.get()) {
                activatedSpawnerFound = true;
                spawnerPositions.add(pos);
                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cSTRONGHOLD§r Spawner! Block Position: " + pos));
                else ChatUtils.sendMsg(Text.of("Detected Activated §cSTRONGHOLD§r Spawner!"));
            } else if (key=="blaze" && enableFortress.get()) {
                activatedSpawnerFound = true;
                spawnerPositions.add(pos);
                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cFORTRESS§r Spawner! Block Position: " + pos));
                else ChatUtils.sendMsg(Text.of("Detected Activated §cFORTRESS§r Spawner!"));
            } else if (key=="magma" && enableBastion.get()) {
                activatedSpawnerFound = true;
                spawnerPositions.add(pos);
                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cBASTION§r Spawner! Block Position: " + pos));
                else ChatUtils.sendMsg(Text.of("Detected Activated §cBASTION§r Spawner!"));
            } else {
                activatedSpawnerFound = true;
                spawnerPositions.add(pos);
                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated Spawner! Block Position: " + pos));
                else ChatUtils.sendMsg(Text.of("Detected Activated Spawner!"));
            }
            if (locLogging.get())logSpawner(pos);
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
    private void renderRange(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
    }
    private void removeChunksOutsideRenderDistance() {
        double renderDistanceBlocks = renderDistance.get() * 16;

        removeChunksOutsideRenderDistance(scannedPositions, renderDistanceBlocks);
        removeChunksOutsideRenderDistance(spawnerPositions, renderDistanceBlocks);
        removeChunksOutsideRenderDistance(deactivatedSpawnerPositions, renderDistanceBlocks);
        removeChunksOutsideRenderDistance(trialspawnerPositions, renderDistanceBlocks);
        removeChunksOutsideRenderDistance(noRenderPositions, renderDistanceBlocks);
    }
    private void removeChunksOutsideRenderDistance(Set<BlockPos> chunkSet, double renderDistanceBlocks) {
        chunkSet.removeIf(blockPos -> {
            BlockPos playerPos = new BlockPos(mc.player.getBlockX(), blockPos.getY(), mc.player.getBlockZ());
            return !playerPos.isWithinDistance(blockPos, renderDistanceBlocks);
        });
    }

    private void logSpawner(BlockPos pos) {
        if (!loggedSpawnerPositions.contains(pos)) {
            loggedSpawnerPositions.add(pos);
            loggedSpawners.add(new LoggedSpawner(pos.getX(), pos.getY(), pos.getZ()));
            saveJson();
            saveCsv();
        }
    }

    private void saveCsv() {
        try {
            File file = getCsvFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            writer.write("X,Y,Z\n");
            for (LoggedSpawner ls : loggedSpawners) {
                ls.write(writer);
            }
            writer.close();
        } catch (IOException ignored) {}
    }

    private void saveJson() {
        try {
            File file = getJsonFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            GSON.toJson(loggedSpawners, writer);
            writer.close();
        } catch (IOException ignored) {}
    }

    private File getJsonFile() {
        return new File(new File(new File("TrouserStreak", "ActivatedSpawners"), Utils.getFileWorldName()), "spawners.json");
    }

    private File getCsvFile() {
        return new File(new File(new File("TrouserStreak", "ActivatedSpawners"), Utils.getFileWorldName()), "spawners.csv");
    }

    private void loadLogs() {
        File file = getJsonFile();
        boolean loaded = false;
        if (file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                List<LoggedSpawner> data = GSON.fromJson(reader, new TypeToken<List<LoggedSpawner>>() {}.getType());
                reader.close();
                if (data != null) {
                    loggedSpawners.addAll(data);
                    for (LoggedSpawner ls : data) {
                        loggedSpawnerPositions.add(new BlockPos(ls.x, ls.y, ls.z));
                    }
                    loaded = true;
                }
            } catch (Exception ignored) {}
        }
        if (!loaded) {
            file = getCsvFile();
            if (file.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] values = line.split(",");
                        LoggedSpawner ls = new LoggedSpawner(
                                Integer.parseInt(values[0]),
                                Integer.parseInt(values[1]),
                                Integer.parseInt(values[2])
                        );
                        loggedSpawners.add(ls);
                        loggedSpawnerPositions.add(new BlockPos(ls.x, ls.y, ls.z));
                    }
                    reader.close();
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        // Sort by Y coordinate for display purposes.
        loggedSpawners.sort(Comparator.comparingInt(s -> s.y));
        WVerticalList list = theme.verticalList();
        WButton clear = list.add(theme.button("Clear Logged Positions")).widget();
        WTable table = new WTable();
        if (!loggedSpawners.isEmpty()) list.add(table);
        clear.action = () -> {
            loggedSpawners.clear();
            loggedSpawnerPositions.clear();
            table.clear();
            saveJson();
            saveCsv();
        };
        fillTable(theme, table);
        return list;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        List<LoggedSpawner> spawnerCoords = new ArrayList<>();
        for (LoggedSpawner ls : loggedSpawners) {
            if (!spawnerCoords.contains(ls)) {
                spawnerCoords.add(ls);
                table.add(theme.label("Pos: " + ls.x + ", " + ls.y + ", " + ls.z));
                WButton gotoBtn = table.add(theme.button("Goto")).widget();
                gotoBtn.action = () -> PathManagers.get().moveTo(new BlockPos(ls.x, ls.y, ls.z), true);
                WMinus delete = table.add(theme.minus()).widget();
                delete.action = () -> {
                    loggedSpawners.remove(ls);
                    loggedSpawnerPositions.remove(new BlockPos(ls.x, ls.y, ls.z));
                    table.clear();
                    fillTable(theme, table);
                    saveJson();
                    saveCsv();
                };
                table.row();
            }
        }
    }

    // ─── INNER CLASS: LoggedSpawner ─────────────────────────────────────────────
    private static class LoggedSpawner {
        public int x, y, z;

        public LoggedSpawner(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void write(Writer writer) throws IOException {
            writer.write(x + "," + y + "," + z + "\n");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LoggedSpawner)) return false;
            LoggedSpawner that = (LoggedSpawner) o;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
}