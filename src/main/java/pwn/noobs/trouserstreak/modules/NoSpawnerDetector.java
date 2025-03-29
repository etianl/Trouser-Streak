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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.io.*;
import java.util.*;

public class NoSpawnerDetector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLocations = settings.createGroup("Location Toggles");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup locationLogs = settings.createGroup("Location Logs");
    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("Chat feedback")
            .description("Display info about Structure in chat.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> displaycoords = sgGeneral.add(new BoolSetting.Builder()
            .name("DisplayCoords")
            .description("Displays coords of Structure in chat.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> enableDungeon = sgLocations.add(new BoolSetting.Builder()
            .name("Enable Dungeon")
            .description("Enable detection for dungeons.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> spawnerDetect = sgLocations.add(new BoolSetting.Builder()
            .name("No Spawner Detection (Dungeon)")
            .description("Only display dungeons if NO spawner.")
            .defaultValue(true)
            .visible(() -> enableDungeon.get())
            .build()
    );
    private final Setting<Boolean> chestDetect = sgLocations.add(new BoolSetting.Builder()
            .name("Chest Detection")
            .description("Only detect dungeons with chests.")
            .defaultValue(true)
            .visible(() -> enableDungeon.get())
            .build()
    );
    private final Setting<Boolean> enableMineshaft = sgLocations.add(new BoolSetting.Builder()
            .name("Enable Mineshaft")
            .description("Enable detection for Mineshaft spawners.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> spawnerDetectmineshaft = sgLocations.add(new BoolSetting.Builder()
            .name("No Spawner Detection (Mineshaft)")
            .description("Only display Mineshaft spawner locations if NO spawner.")
            .defaultValue(true)
            .visible(() -> enableMineshaft.get())
            .build()
    );
    public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
            .name("Render-Distance(Chunks)")
            .description("How many chunks from the character to render the detected Structure.")
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
            .description("Show tracers to the Structure.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> nearesttrcr = sgRender.add(new BoolSetting.Builder()
            .name("Tracer to nearest Structure Only")
            .description("Show only one tracer to the nearest Structure.")
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
            .name("structure-side-color")
            .description("Color of the structure indicator.")
            .defaultValue(new SettingColor(251, 5, 5, 70))
            .visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> spawnerLineColor = sgRender.add(new ColorSetting.Builder()
            .name("structure-line-color")
            .description("Color of the structure indicator.")
            .defaultValue(new SettingColor(251, 5, 5, 235))
            .visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both || trcr.get()))
            .build()
    );
    private final Setting<Boolean> locLogging = locationLogs.add(new BoolSetting.Builder()
            .name("Enable Location Logging")
            .description("Logs the locations of detected structures to a csv file as well as a table in this options menu.")
            .defaultValue(false)
            .build()
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<LoggedStructure> loggedStructures = new ArrayList<>();
    private final Set<ChunkPos> scannedChunks = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> scannedBlocks = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> checkedBlocks = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> StructurePositions = Collections.synchronizedSet(new HashSet<>());
    private int closestStructureX = 2000000000;
    private int closestStructureY = 2000000000;
    private int closestStructureZ = 2000000000;
    private double StructureDistance = 2000000000;

    public NoSpawnerDetector() {
        super(Trouser.Main, "NoSpawnerDetector", "Detects Structures and also their lack of mob spawner.");
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
        loggedStructures.clear();
        scannedBlocks.clear();
        scannedChunks.clear();
        checkedBlocks.clear();
        StructurePositions.clear();
        closestStructureX = 2000000000;
        closestStructureY = 2000000000;
        closestStructureZ = 2000000000;
        StructureDistance = 2000000000;
    }
    private boolean chunkContainsBlock(WorldChunk chunk, Block target, int sectionsToCheck) {
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sectionsToCheck; i++) {
            ChunkSection section = sections[i];
            if (!section.isEmpty()) {
                var blockStatesContainer = section.getBlockStateContainer();
                Palette<BlockState> blockStatePalette = blockStatesContainer.data.palette();
                int blockPaletteLength = blockStatePalette.getSize();
                for (int i2 = 0; i2 < blockPaletteLength; i2++) {
                    BlockState blockPaletteEntry = blockStatePalette.get(i2);
                    if (blockPaletteEntry.getBlock() == target) return true;
                }
            }
        }
        return false;
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        int renderdistance = mc.options.getViewDistance().getValue();
        ChunkPos playerChunkPos = new ChunkPos(mc.player.getBlockPos());
        for (int chunkX = playerChunkPos.x - renderdistance; chunkX <= playerChunkPos.x + renderdistance; chunkX++) {
            for (int chunkZ = playerChunkPos.z - renderdistance; chunkZ <= playerChunkPos.z + renderdistance; chunkZ++) {
                WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
                if (chunk.isEmpty() || scannedChunks.contains(chunk.getPos())) continue;
                if ((enableDungeon.get() && mc.world.getRegistryKey() == World.OVERWORLD && chunkContainsBlock(chunk, Blocks.MOSSY_COBBLESTONE, Math.min(chunk.getSectionArray().length, 20))) || (enableMineshaft.get() && mc.world.getRegistryKey() == World.OVERWORLD && chunkContainsBlock(chunk, Blocks.COBWEB, Math.min(chunk.getSectionArray().length, 20)))) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockPos blockPos = new BlockPos(x + chunkX * 16, y, z + chunkZ * 16);
                                if (enableDungeon.get() && mc.world.getBlockState(blockPos).getBlock() == Blocks.MOSSY_COBBLESTONE) {
                                    if (!scannedBlocks.contains(blockPos) && !checkedBlocks.contains(blockPos))scanForDungeonFloor(blockPos);
                                    checkedBlocks.add(blockPos);
                                }
                                if (enableMineshaft.get() && mc.world.getBlockState(blockPos).getBlock() == Blocks.COBWEB) {
                                    if (!scannedBlocks.contains(blockPos) && !checkedBlocks.contains(blockPos))scanForMineshaftSpawner(blockPos);
                                    checkedBlocks.add(blockPos);
                                }
                            }
                        }
                    }
                }
                scannedChunks.add(chunk.getPos());
            }
        }
        if (nearesttrcr.get()){
            try {
                if (StructurePositions.stream().toList().size() > 0) {
                    for (int b = 0; b < StructurePositions.stream().toList().size(); b++) {
                        if (StructureDistance > Math.sqrt(Math.pow(StructurePositions.stream().toList().get(b).getX() - mc.player.getBlockX(), 2) + Math.pow(StructurePositions.stream().toList().get(b).getZ() - mc.player.getBlockZ(), 2))) {
                            closestStructureX = Math.round((float) StructurePositions.stream().toList().get(b).getX());
                            closestStructureY = Math.round((float) StructurePositions.stream().toList().get(b).getY());
                            closestStructureZ = Math.round((float) StructurePositions.stream().toList().get(b).getZ());
                            StructureDistance = Math.sqrt(Math.pow(StructurePositions.stream().toList().get(b).getX() - mc.player.getBlockX(), 2) + Math.pow(StructurePositions.stream().toList().get(b).getZ() - mc.player.getBlockZ(), 2));
                        }
                    }
                    StructureDistance = 2000000000;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (removerenderdist.get()) removeChunksOutsideRenderDistance();
    }
    private void scanForDungeonFloor(BlockPos blockPos) {
        int radius = 4;
        int y = blockPos.getY();
        int minX = blockPos.getX() - radius;
        int maxX = blockPos.getX() + radius;
        int minZ = blockPos.getZ() - radius;
        int maxZ = blockPos.getZ() + radius;

        int mossyCobbleCount = 0;
        int cobbleCount = 0;
        int totalBlocks = (maxX - minX + 1) * (maxZ - minZ + 1);

        Set<BlockPos> potentialBlocks = Collections.synchronizedSet(new HashSet<>());
        boolean foundChests = false;
        boolean foundSpawner = false;
        boolean foundCaveAir = false;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos scanPos = new BlockPos(x, y, z);
                if (mc.world.getBlockState(scanPos).getBlock() == Blocks.MOSSY_COBBLESTONE) mossyCobbleCount++;
                else if (mc.world.getBlockState(scanPos).getBlock() == Blocks.COBBLESTONE) cobbleCount++;
                BlockPos extraScanPos = new BlockPos(x, y+1, z);
                if (!foundCaveAir) if (mc.world.getBlockState(extraScanPos).getBlock() == Blocks.CAVE_AIR)foundCaveAir=true;
                if (chestDetect.get() && !foundChests) if (mc.world.getBlockState(extraScanPos).getBlock() == Blocks.CHEST)foundChests=true;
                if (spawnerDetect.get() && !foundSpawner) if (mc.world.getBlockState(extraScanPos).getBlock() == Blocks.SPAWNER)foundSpawner=true;
                potentialBlocks.add(scanPos);
            }
        }

        int requiredBlocks = (int) (totalBlocks * 0.55);

        if (mossyCobbleCount + cobbleCount > requiredBlocks) {
            if ((chestDetect.get() && foundChests) || !chestDetect.get()) {
                if ((spawnerDetect.get() && !foundSpawner) || !spawnerDetect.get()) {
                    if (foundCaveAir) {
                        scannedBlocks.addAll(potentialBlocks);
                        displayMessage("dungeon", blockPos);
                    }
                }
            }
        }
    }
    private void scanForMineshaftSpawner(BlockPos blockPos) {
        int radius = 1;
        int minY = blockPos.getY();
        int maxY = blockPos.getY() + radius;
        int minX = blockPos.getX() - radius;
        int maxX = blockPos.getX() + radius;
        int minZ = blockPos.getZ() - radius;
        int maxZ = blockPos.getZ() + radius;

        int cobwebCount = 0;
        int totalBlocks = (maxX - minX + 1) * (maxZ - minZ + 1) * (maxY - minY + 1);

        Set<BlockPos> potentialBlocks = Collections.synchronizedSet(new HashSet<>());
        boolean foundAir = false;
        boolean foundCaveAir = false;
        boolean foundSpawner = false;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos scanPos = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(scanPos).getBlock() == Blocks.COBWEB) cobwebCount++;
                    if (!foundCaveAir)
                        if (mc.world.getBlockState(scanPos).getBlock() == Blocks.CAVE_AIR) foundCaveAir = true;
                    if (!foundAir)
                        if (mc.world.getBlockState(scanPos).getBlock() == Blocks.AIR) foundAir = true;
                    if (!foundSpawner)
                        if (mc.world.getBlockState(scanPos).getBlock() == Blocks.SPAWNER) foundSpawner = true;
                    potentialBlocks.add(scanPos);
                }
            }
        }
        int requiredBlocks = (int) (totalBlocks * 0.45);
        boolean doublechecknospawner = false;
        if (spawnerDetectmineshaft.get() && !foundSpawner && foundAir && cobwebCount > requiredBlocks){
            int radius2 = 18;
            int y = blockPos.getY();
            int minX2 = blockPos.getX() - radius2;
            int maxX2 = blockPos.getX() + radius2;
            int minZ2 = blockPos.getZ() - radius2;
            int maxZ2 = blockPos.getZ() + radius2;
            for (int x = minX2; x <= maxX2; x++) {
                for (int z = minZ2; z <= maxZ2; z++) {
                    BlockPos scanPos = new BlockPos(x, y, z);
                    if (!doublechecknospawner) if (mc.world.getBlockState(scanPos).getBlock() == Blocks.SPAWNER)doublechecknospawner=true;
                    if (!doublechecknospawner) if (mc.world.getBlockState(scanPos).getBlock() == Blocks.VOID_AIR)doublechecknospawner=true;
                }
            }
        }
        if (cobwebCount > requiredBlocks) {
            if ((spawnerDetectmineshaft.get() && !doublechecknospawner && !foundSpawner && foundAir) || !spawnerDetectmineshaft.get()) {
                if (foundCaveAir) {
                    scannedBlocks.addAll(potentialBlocks);
                    displayMessage("mineshaft", blockPos);
                }
            }
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (spawnerSideColor.get().a > 5 || spawnerLineColor.get().a > 5) {
            synchronized (StructurePositions) {
                for (BlockPos pos : StructurePositions) {
                    BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
                    if (pos != null && playerPos.isWithinDistance(pos, renderDistance.get() * 16)) {
                        int startX = pos.getX();
                        int startY = pos.getY();
                        int startZ = pos.getZ();
                        int endX = pos.getX();
                        int endY = pos.getY();
                        int endZ = pos.getZ();
                        if (!nearesttrcr.get()) {
                            render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), spawnerSideColor.get(), spawnerLineColor.get(), shapeMode.get(), event);
                        } else if (nearesttrcr.get()) {
                            render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), spawnerSideColor.get(), spawnerLineColor.get(), shapeMode.get(), event);
                            render2(new Box(new Vec3d(closestStructureX, closestStructureY, closestStructureZ), new Vec3d (closestStructureX, closestStructureY, closestStructureZ)), spawnerSideColor.get(), spawnerLineColor.get(),ShapeMode.Sides, event);
                        }
                    }
                }
            }
        }
    }
    private void displayMessage(String key, BlockPos pos) {
        if (chatFeedback.get()) {
            if (key=="dungeon") {
                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected §cDUNGEON§r! Block Position: " + pos));
                else ChatUtils.sendMsg(Text.of("Detected §cDUNGEON§r!"));
                logStructure(pos);
            }
            if (key=="mineshaft") {
                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected §cMINESHAFT§r! Block Position: " + pos));
                else ChatUtils.sendMsg(Text.of("Detected §cMINESHAFT§r!"));
                logStructure(pos);
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
        int renderdist = renderDistance.get();
        if (mc.options.getViewDistance().getValue() > renderDistance.get())renderdist = mc.options.getViewDistance().getValue()+3;
        double renderDistanceBlocks = renderdist * 16;

        removechunksOutsideRenderDistance(scannedChunks,mc.player.getBlockPos(),renderDistanceBlocks);
        removeBlockPosOutsideRenderDistance(StructurePositions, renderDistanceBlocks);
        removeBlockPosOutsideRenderDistance(scannedBlocks, renderDistanceBlocks);
        removeBlockPosOutsideRenderDistance(checkedBlocks, renderDistanceBlocks);
    }
    private void removeBlockPosOutsideRenderDistance(Set<BlockPos> chunkSet, double renderDistanceBlocks) {
        chunkSet.removeIf(blockPos -> {
            BlockPos playerPos = new BlockPos(mc.player.getBlockX(), blockPos.getY(), mc.player.getBlockZ());
            return !playerPos.isWithinDistance(blockPos, renderDistanceBlocks);
        });
    }
    private void removechunksOutsideRenderDistance(Set<ChunkPos> chunkSet, BlockPos playerPos, double renderDistanceBlocks) {
        chunkSet.removeIf(c -> !playerPos.isWithinDistance(new BlockPos(c.getCenterX(), mc.player.getBlockY(), c.getCenterZ()), renderDistanceBlocks));
    }
    private void logStructure(BlockPos pos) {
        if (! StructurePositions.contains(pos)) {
            StructurePositions.add(pos);
            if (locLogging.get()){
                loggedStructures.add(new LoggedStructure(pos.getX(), pos.getY(), pos.getZ()));
                saveJson();
                saveCsv();
            }
        }
    }

    private void saveCsv() {
        try {
            File file = getCsvFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            writer.write("X,Y,Z\n");
            for (LoggedStructure ls : loggedStructures) {
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
            GSON.toJson(loggedStructures, writer);
            writer.close();
        } catch (IOException ignored) {}
    }

    private File getJsonFile() {
        return new File(new File(new File("TrouserStreak", "NoSpawnerDetector"), Utils.getFileWorldName()), "NoSpawnerDetector.json");
    }

    private File getCsvFile() {
        return new File(new File(new File("TrouserStreak", "NoSpawnerDetector"), Utils.getFileWorldName()), "NoSpawnerDetector.csv");
    }

    private void loadLogs() {
        File file = getJsonFile();
        boolean loaded = false;
        if (file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                List<LoggedStructure> data = GSON.fromJson(reader, new TypeToken<List<LoggedStructure>>() {}.getType());
                reader.close();
                if (data != null) {
                    loggedStructures.addAll(data);
                    for (LoggedStructure ls : data) {
                        StructurePositions.add(new BlockPos(ls.x, ls.y, ls.z));
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
                        LoggedStructure ls = new LoggedStructure(
                                Integer.parseInt(values[0]),
                                Integer.parseInt(values[1]),
                                Integer.parseInt(values[2])
                        );
                        loggedStructures.add(ls);
                        StructurePositions.add(new BlockPos(ls.x, ls.y, ls.z));
                    }
                    reader.close();
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        // Sort by Y coordinate for display purposes.
        loggedStructures.sort(Comparator.comparingInt(s -> s.y));
        WVerticalList list = theme.verticalList();
        WButton clear = list.add(theme.button("Clear Logged Positions")).widget();
        WTable table = new WTable();
        if (!loggedStructures.isEmpty()) list.add(table);
        clear.action = () -> {
            loggedStructures.clear();
            StructurePositions.clear();
            table.clear();
            saveJson();
            saveCsv();
        };
        fillTable(theme, table);
        return list;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        List<LoggedStructure> spawnerCoords = new ArrayList<>();
        for (LoggedStructure ls : loggedStructures) {
            if (!spawnerCoords.contains(ls)) {
                spawnerCoords.add(ls);
                table.add(theme.label("Pos: " + ls.x + ", " + ls.y + ", " + ls.z));
                WButton gotoBtn = table.add(theme.button("Goto")).widget();
                gotoBtn.action = () -> PathManagers.get().moveTo(new BlockPos(ls.x, ls.y, ls.z), true);
                WMinus delete = table.add(theme.minus()).widget();
                delete.action = () -> {
                    loggedStructures.remove(ls);
                    StructurePositions.remove(new BlockPos(ls.x, ls.y, ls.z));
                    table.clear();
                    fillTable(theme, table);
                    saveJson();
                    saveCsv();
                };
                table.row();
            }
        }
    }

    // ─── INNER CLASS: LoggedStructure ─────────────────────────────────────────────
    private static class LoggedStructure {
        public int x, y, z;

        public LoggedStructure(int x, int y, int z) {
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
            if (!(o instanceof LoggedStructure)) return false;
            LoggedStructure that = (LoggedStructure) o;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
}