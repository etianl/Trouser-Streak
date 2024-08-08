package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.util.math.*;

import net.minecraft.world.chunk.*;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;

public class TrouserBlockESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<List<Block>> Blawcks = sgGeneral.add(new BlockListSetting.Builder()
            .name("Blocks To Look For")
            .description("Find these blocks.")
            .build()
    );
    private final Setting<Integer> tickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("Chunk Scan Delay")
            .description("Delay between chunk scans")
            .min(0)
            .sliderRange(0,200)
            .defaultValue(5)
            .build()
    );
    // render
    public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
            .name("Render-Distance(Chunks)")
            .description("How many chunks from the character to render the detected blocks.")
            .defaultValue(24)
            .min(1)
            .sliderRange(1,256)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> ESPBLOCKSLineColor = sgRender.add(new ColorSetting.Builder()
            .name("ESPBLOCKSLineColor")
            .description("ESPBLOCKSLineColor")
            .defaultValue(new SettingColor(255, 0, 0, 95))
            .visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );

    private final Setting<SettingColor> ESPBLOCKSSideColor = sgRender.add(new ColorSetting.Builder()
            .name("ESPBLOCKSSideColor")
            .description("ESPBLOCKSSideColor")
            .defaultValue(new SettingColor(255, 0, 0, 35))
            .visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
            .build()
    );

    private Set<ChunkPos> scannedChunks = Collections.synchronizedSet(new HashSet<>());
    private Set<BlockPos> ESPBLOCKS = Collections.synchronizedSet(new HashSet<>());
    private int ticks = 0;

    public TrouserBlockESP() {
        super(Trouser.Main,"TrouserBlockESP", "Looks for blocks and renders an overlay where they are.");
    }

    @Override
    public void onActivate() {
        ESPBLOCKS.clear();
        scannedChunks.clear();
    }
    @Override
    public void onDeactivate() {
        ESPBLOCKS.clear();
        scannedChunks.clear();
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            ESPBLOCKS.clear();
            scannedChunks.clear();
        }
        if (event.screen instanceof DownloadingTerrainScreen) {
            ESPBLOCKS.clear();
            scannedChunks.clear();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        ESPBLOCKS.clear();
        scannedChunks.clear();
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        removeStuffOutsideRenderDistance();
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (ESPBLOCKSLineColor.get().a > 5 || ESPBLOCKSSideColor.get().a > 5) {
            synchronized (ESPBLOCKS) {
                for (BlockPos blockPos : ESPBLOCKS) {
                    if (blockPos != null && mc.getCameraEntity().getBlockPos().isWithinDistance(blockPos, renderDistance.get() * 16)) {
                        Vec3d boxStart = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                        Vec3d boxEnd = new Vec3d(blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);
                        Box box = new Box(boxStart, boxEnd);

                        render(box, ESPBLOCKSSideColor.get(), ESPBLOCKSLineColor.get(), shapeMode.get(), event);
                    }
                }
            }
        }
    }
    private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null) return;
        ticks++;
        if (ticks >= tickdelay.get()) {
            for (Chunk chunk : Utils.chunks()) {
                ChunkPos chunkPos = chunk.getPos();
                if (scannedChunks.contains(chunkPos)) continue;
                try {
                    ChunkSection[] sections = chunk.getSectionArray();
                    int baseY = mc.world.getBottomY();
                    int chunkX = chunkPos.x * 16;
                    int chunkZ = chunkPos.z * 16;

                    for (ChunkSection section : sections) {
                        if (section == null || section.isEmpty()) {
                            baseY += 16;
                            continue;
                        }

                        for (int x = 0; x < 16; x++) {
                            for (int y = 0; y < 16; y++) {
                                for (int z = 0; z < 16; z++) {
                                    int currentY = baseY + y;
                                    BlockPos blockPos = new BlockPos(chunkX + x, currentY, chunkZ + z);
                                    BlockState blockState = section.getBlockState(x, y, z);
                                    if (Blawcks.get().contains(blockState.getBlock())) {
                                        ESPBLOCKS.add(blockPos);
                                    }
                                }
                            }
                        }
                        baseY += 16;
                    }
                    scannedChunks.add(chunkPos);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ticks = 0;
        }
    }
    private void removeStuffOutsideRenderDistance() {
        BlockPos cameraPos = mc.getCameraEntity().getBlockPos();
        double renderDistanceBlocks = renderDistance.get() * 16;

        ESPBLOCKS.removeIf(blockpos -> !cameraPos.isWithinDistance(blockpos, renderDistanceBlocks));
        scannedChunks.removeIf(chunkPos -> !cameraPos.isWithinDistance(chunkPos.getStartPos(), renderDistanceBlocks));
    }
}