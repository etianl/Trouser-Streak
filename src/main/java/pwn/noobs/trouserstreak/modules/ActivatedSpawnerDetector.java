//made by etianl :D
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.block.enums.TrialSpawnerState;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;

public class ActivatedSpawnerDetector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

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
            .visible(() -> extramessage.get())
            .build()
    );
    private final Setting<Boolean> displaycoords = sgGeneral.add(new BoolSetting.Builder()
            .name("DisplayCoords")
            .description("Displays coords of activated spawners in chat.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> deactivatedSpawner = sgGeneral.add(new BoolSetting.Builder()
            .name("De-Activated Spawner Detector")
            .description("Detects spawners with torches on them.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> trialSpawner = sgGeneral.add(new BoolSetting.Builder()
            .name("Trial Spawner Detector")
            .description("Detects activated Trial Spawners.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> deactivatedSpawnerdistance = sgGeneral.add(new IntSetting.Builder()
            .name("Torch Scan distance")
            .description("How many blocks from the spawner to look for blocks that make light")
            .defaultValue(1)
            .min(1)
            .sliderRange(1,10)
            .visible(() -> deactivatedSpawner.get())
            .build()
    );
    private final Setting<Boolean> lessRenderSpam = sgRender.add(new BoolSetting.Builder()
            .name("Less Render Spam")
            .description("Do not render if NO chests within 16 blocks of spawner.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
            .name("Render-Distance(Chunks)")
            .description("How many chunks from the character to render the detected chunks.")
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
            .visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
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
            .visible(() -> trialSpawner.get() && (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
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

    private final Set<BlockPos> spawnerPositions = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> trialspawnerPositions = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> deactivatedSpawnerPositions = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> noRenderPositions = Collections.synchronizedSet(new HashSet<>());

    public ActivatedSpawnerDetector() {
        super(Trouser.Main,"ActivatedSpawnerDetector", "Detects if a player has been near a mob spawner in the past. May be useful for finding player made stashes in dungeons, mineshafts, and other places.");
    }
    @Override
    public void onActivate() {
        spawnerPositions.clear();
        deactivatedSpawnerPositions.clear();
        noRenderPositions.clear();
        trialspawnerPositions.clear();
    }
    @Override
    public void onDeactivate() {
        spawnerPositions.clear();
        deactivatedSpawnerPositions.clear();
        noRenderPositions.clear();
        trialspawnerPositions.clear();
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.world == null) return;

        int renderDistance = mc.options.getViewDistance().getValue();
        ChunkPos playerChunkPos = new ChunkPos(mc.player.getBlockPos());
        for (int chunkX = playerChunkPos.x - renderDistance; chunkX <= playerChunkPos.x + renderDistance; chunkX++) {
            for (int chunkZ = playerChunkPos.z - renderDistance; chunkZ <= playerChunkPos.z + renderDistance; chunkZ++) {
                WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
                List<BlockEntity> blockEntities = new ArrayList<>(chunk.getBlockEntities().values());

                for (BlockEntity blockEntity : blockEntities) {
                    if (blockEntity instanceof MobSpawnerBlockEntity){
                        MobSpawnerBlockEntity spawner = (MobSpawnerBlockEntity) blockEntity;
                        BlockPos pos = spawner.getPos();
                        if (!trialspawnerPositions.contains(pos) && !noRenderPositions.contains(pos) && !deactivatedSpawnerPositions.contains(pos) && !spawnerPositions.contains(pos) && spawner.getLogic().spawnDelay != 20) {
                            if (mc.world.getRegistryKey() == World.NETHER && spawner.getLogic().spawnDelay == 0) return;
                            if (spawner.getLogic().spawnEntry.getNbt().get("id") != null){
                                String monster = spawner.getLogic().spawnEntry.getNbt().get("id").toString();
                                if (monster != null){
                                    if (monster.contains("zombie") || monster.contains("skeleton")) {
                                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cDUNGEON§r Spawner! Block Position: " + pos));
                                        else ChatUtils.sendMsg(Text.of("Detected Activated §cDUNGEON§r Spawner!"));
                                    } else if (monster.contains(":spider")) {
                                        if (mc.world.getBlockState(spawner.getPos().down()).getBlock() == Blocks.BIRCH_PLANKS){
                                            if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cWOODLAND MANSION§r Spawner! Block Position: " + pos));
                                            else ChatUtils.sendMsg(Text.of("Detected Activated §cWOODLAND MANSION§r Spawner!"));
                                        } else {
                                            if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cDUNGEON§r Spawner! Block Position: " + pos));
                                            else ChatUtils.sendMsg(Text.of("Detected Activated §cDUNGEON§r Spawner!"));
                                        }
                                    } else if (monster.contains("cave_spider")) {
                                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cMINESHAFT§r Spawner! Block Position: " + pos));
                                        else ChatUtils.sendMsg(Text.of("Detected Activated §cMINESHAFT§r Spawner!"));
                                    } else if (monster.contains("silverfish")) {
                                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cSTRONGHOLD§r Spawner! Block Position: " + pos));
                                        else ChatUtils.sendMsg(Text.of("Detected Activated §cSTRONGHOLD§r Spawner!"));
                                    } else if (monster.contains("blaze")) {
                                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cFORTRESS§r Spawner! Block Position: " + pos));
                                        else ChatUtils.sendMsg(Text.of("Detected Activated §cFORTRESS§r Spawner!"));
                                    } else if (monster.contains("magma")) {
                                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cBASTION§r Spawner! Block Position: " + pos));
                                        else ChatUtils.sendMsg(Text.of("Detected Activated §cBASTION§r Spawner!"));
                                    } else {
                                        if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated Spawner! Block Position: " + pos));
                                        else ChatUtils.sendMsg(Text.of("Detected Activated Spawner!"));
                                    }
                                } else {
                                    if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated Spawner! Block Position: " + pos));
                                    else ChatUtils.sendMsg(Text.of("Detected Activated Spawner!"));
                                }
                            } else {
                                if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated Spawner! Block Position: " + pos));
                                else ChatUtils.sendMsg(Text.of("Detected Activated Spawner!"));
                            }

                            spawnerPositions.add(pos);
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
                                if (lightsFound == true) ChatUtils.sendMsg(Text.of("The Spawner has torches or other light blocks!"));
                            }
                            boolean chestfound = false;
                            for (int x = -16; x < 17; x++) {
                                for (int y = -16; y < 17; y++) {
                                    for (int z = -16; z < 17; z++) {
                                        BlockPos bpos = new BlockPos(pos.getX()+x, pos.getY()+y, pos.getZ()+z);
                                        if (mc.world.getBlockState(bpos).getBlock() == Blocks.CHEST) {
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
                                spawnerPositions.remove(pos);
                                noRenderPositions.add(pos);
                            }
                            if (lessSpam.get() && chestfound && extramessage.get()) error("There may be stashed items in the storage near the spawners!");
                            else if (!lessSpam.get() && extramessage.get()) error("There may be stashed items in the storage near the spawners!");
                        }
                    }
                    if (blockEntity instanceof TrialSpawnerBlockEntity){
                        TrialSpawnerBlockEntity trialSpawner = (TrialSpawnerBlockEntity) blockEntity;
                        BlockPos tPos = trialSpawner.getPos();
                        if (!trialspawnerPositions.contains(tPos) && !noRenderPositions.contains(tPos) && !deactivatedSpawnerPositions.contains(tPos) && !spawnerPositions.contains(tPos) && trialSpawner.getSpawnerState() != TrialSpawnerState.WAITING_FOR_PLAYERS) {
                            if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated §cTRIAL§r Spawner! Block Position: " + tPos));
                            else ChatUtils.sendMsg(Text.of("Detected Activated §cTRIAL§r Spawner!"));
                            trialspawnerPositions.add(tPos);
                            boolean chestfound = false;
                            for (int x = -14; x < 15; x++) {
                                for (int y = -14; y < 15; y++) {
                                    for (int z = -14; z < 15; z++) {
                                        BlockPos bpos = new BlockPos(tPos.getX()+x, tPos.getY()+y, tPos.getZ()+z);
                                        if (mc.world.getBlockState(bpos).getBlock() == Blocks.CHEST) {
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
                                trialspawnerPositions.remove(tPos);
                                noRenderPositions.add(tPos);
                            }
                            if (lessSpam.get() && chestfound && extramessage.get()) error("There may be stashed items in the storage near the spawners!");
                            else if (!lessSpam.get() && extramessage.get()) error("There may be stashed items in the storage near the spawners!");
                        }
                    }
                }
            }
        }
        if (removerenderdist.get())removeChunksOutsideRenderDistance();
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
                        if (rangerendering.get())render(new Box(new Vec3d(startX+17, startY+17, startZ+17), new Vec3d(endX-16, endY-16, endZ-16)), rangeSideColor.get(), rangeLineColor.get(), shapeMode.get(), event);
                        if (deactivatedSpawnerPositions.contains(pos)) render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), despawnerSideColor.get(), despawnerLineColor.get(), shapeMode.get(), event);
                        else render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), spawnerSideColor.get(), spawnerLineColor.get(), shapeMode.get(), event);
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
                        if (trialSpawner.get() && rangerendering.get())render(new Box(new Vec3d(startX+15, startY+15, startZ+15), new Vec3d(endX-14, endY-14, endZ-14)), trangeSideColor.get(), trangeLineColor.get(), shapeMode.get(), event);
                        if (deactivatedSpawnerPositions.contains(pos)) render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), despawnerSideColor.get(), despawnerLineColor.get(), shapeMode.get(), event);
                        else render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), trialSideColor.get(), trialLineColor.get(), shapeMode.get(), event);
                    }
                }
            }
        }
    }

    private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
    }
    private void removeChunksOutsideRenderDistance() {
        double renderDistanceBlocks = renderDistance.get() * 16;

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
}