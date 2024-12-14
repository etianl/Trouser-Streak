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
            .name("Extra Warning Message")
            .description("Toggle the message reminding you about stashes.")
            .defaultValue(true)
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
    public final Setting<Integer> deactivatedSpawnerdistance = sgGeneral.add(new IntSetting.Builder()
            .name("Torch Scan distance")
            .description("How many blocks from the spawner to look for blocks that make light")
            .defaultValue(1)
            .min(1)
            .sliderRange(1,10)
            .visible(() -> deactivatedSpawner.get())
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
            .description("Color of the chunks.")
            .defaultValue(new SettingColor(5, 178, 251, 30))
            .visible(() -> rangerendering.get() && (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> rangeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("spawner-range-line-color")
            .description("Color of the chunks.")
            .defaultValue(new SettingColor(5, 178, 251, 155))
            .visible(() -> rangerendering.get() && (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
            .build()
    );

    private final Set<BlockPos> spawnerPositions = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> deactivatedSpawnerPositions = Collections.synchronizedSet(new HashSet<>());

    public ActivatedSpawnerDetector() {
        super(Trouser.Main,"ActivatedSpawnerDetector", "Detects if a player has been near a mob spawner in the past. May be useful for finding player made stashes in dungeons, mineshafts, and other places.");
    }
    @Override
    public void onActivate() {
        spawnerPositions.clear();
        deactivatedSpawnerPositions.clear();
    }
    @Override
    public void onDeactivate() {
        spawnerPositions.clear();
        deactivatedSpawnerPositions.clear();
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
                        if (!deactivatedSpawnerPositions.contains(pos) && !spawnerPositions.contains(pos) && spawner.getLogic().spawnDelay != 20) {
                            if (mc.world.getRegistryKey() == World.NETHER && spawner.getLogic().spawnDelay == 0) return;
                            if (displaycoords.get()) ChatUtils.sendMsg(Text.of("Detected Activated Spawner! Block Position: " + pos));
                            else ChatUtils.sendMsg(Text.of("Detected Activated Spawner!"));
                            if (extramessage.get()) error("There may be stashed items in the storage near the spawners!");
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
                    if (pos != null && mc.getCameraEntity().getBlockPos().isWithinDistance(pos, renderDistance.get() * 16)) {
                        int startX = pos.getX();
                        int startY = pos.getY();
                        int startZ = pos.getZ();
                        int endX = pos.getX();
                        int endY = pos.getY();
                        int endZ = pos.getZ();
                        if (rangerendering.get())render(new Box(new Vec3d(startX+15, startY+15, startZ+15), new Vec3d(endX-14, endY-14, endZ-14)), rangeSideColor.get(), rangeLineColor.get(), shapeMode.get(), event);
                        if (deactivatedSpawnerPositions.contains(pos)) render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), despawnerSideColor.get(), despawnerLineColor.get(), shapeMode.get(), event);
                        else render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), spawnerSideColor.get(), spawnerLineColor.get(), shapeMode.get(), event);
                    }
                }
            }
        }
    }

    private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
    }
    private void removeChunksOutsideRenderDistance() {
        BlockPos cameraPos = mc.getCameraEntity().getBlockPos();
        double renderDistanceBlocks = renderDistance.get() * 16;

        removeChunksOutsideRenderDistance(spawnerPositions, cameraPos, renderDistanceBlocks);
        removeChunksOutsideRenderDistance(deactivatedSpawnerPositions, cameraPos, renderDistanceBlocks);
    }
    private void removeChunksOutsideRenderDistance(Set<BlockPos> chunkSet, BlockPos cameraPos, double renderDistanceBlocks) {
        chunkSet.removeIf(chunkPos -> !cameraPos.isWithinDistance(chunkPos, renderDistanceBlocks));
    }
}