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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;

public class PotESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> potMessage = sgGeneral.add(new BoolSetting.Builder()
            .name("Extra Message")
            .description("Toggle the message reminding you about pots.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> displaycoords = sgGeneral.add(new BoolSetting.Builder()
            .name("Display Coords")
            .description("Displays coords of activated spawners in chat.")
            .defaultValue(true)
            .visible(()->potMessage.get())
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
            .description("Removes the cached chunks when they leave the defined render distance.")
            .defaultValue(true)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> potSideColor = sgRender.add(new ColorSetting.Builder()
            .name("pot-side-color")
            .description("Color of the dank pot.")
            .defaultValue(new SettingColor(255, 135, 125, 70))
            .visible(() -> (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both))
            .build()
    );
    private final Setting<SettingColor> potLineColor = sgRender.add(new ColorSetting.Builder()
            .name("pot-line-color")
            .description("Color of the dank pot.")
            .defaultValue(new SettingColor(255, 135, 125, 235))
            .visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both))
            .build()
    );

    private final Set<BlockPos> potLocations = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Item> naturalPot = new HashSet<>();
    static {
        naturalPot.add(Items.AIR);
        naturalPot.add(Items.STRING);
        naturalPot.add(Items.EMERALD);
        naturalPot.add(Items.EMERALD_BLOCK);
        naturalPot.add(Items.IRON_INGOT);
        naturalPot.add(Items.TRIAL_KEY);
        naturalPot.add(Items.DIAMOND);
        naturalPot.add(Items.DIAMOND_BLOCK);
    }
    public PotESP() {
        super(Trouser.Main,"PotESP", "Finds the dank pots... In Minecraft (Locates decorated pots with un-natural items in them)");
    }
    @Override
    public void onActivate() {
        potLocations.clear();
    }
    @Override
    public void onDeactivate() {
        potLocations.clear();
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
                    if (blockEntity instanceof DecoratedPotBlockEntity){
                        DecoratedPotBlockEntity pot = (DecoratedPotBlockEntity) blockEntity;
                        Item potItem = pot.stack.getItem();

                        BlockPos potLocation = pot.getPos();
                        if (!potLocations.contains(potLocation) && !naturalPot.contains(potItem)) {
                            if (potMessage.get()) {
                                if (displaycoords.get())
                                    ChatUtils.sendMsg(Text.of("Found a dank pot! It contains: " + potItem + " Location: " + potLocation));
                                else ChatUtils.sendMsg(Text.of("Found a dank pot! It contains: " + potItem));
                            }
                            potLocations.add(potLocation);
                        }
                    }
                }
            }
        }
        if (removerenderdist.get())removeChunksOutsideRenderDistance();
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (potSideColor.get().a > 5 || potLineColor.get().a > 5) {
            synchronized (potLocations) {
                for (BlockPos pos : potLocations) {
                    if (pos != null && mc.getCameraEntity().getBlockPos().isWithinDistance(pos, renderDistance.get() * 16)) {
                        int startX = pos.getX();
                        int startY = pos.getY();
                        int startZ = pos.getZ();
                        int endX = pos.getX();
                        int endY = pos.getY();
                        int endZ = pos.getZ();
                        render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), potSideColor.get(), potLineColor.get(), shapeMode.get(), event);
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

        removeChunksOutsideRenderDistance(potLocations, cameraPos, renderDistanceBlocks);
    }
    private void removeChunksOutsideRenderDistance(Set<BlockPos> chunkSet, BlockPos cameraPos, double renderDistanceBlocks) {
        chunkSet.removeIf(chunkPos -> !cameraPos.isWithinDistance(chunkPos, renderDistanceBlocks));
    }
}