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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
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
            .visible(potMessage::get)
            .build()
    );
    private final Setting<List<Item>> junkItemList = sgGeneral.add(new ItemListSetting.Builder()
            .name("Junk Items")
            .description("Select the items to no look for. Decorated pots containing these items will not be highlighted.")
            .build()
    );
    public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
            .name("Render-Distance(Chunks)")
            .description("How many chunks from the character to render the detected pots.")
            .defaultValue(32)
            .min(6)
            .sliderRange(6,1024)
            .build()
    );
    private final Setting<Boolean> removerenderdist = sgRender.add(new BoolSetting.Builder()
            .name("RemoveOutsideRenderDistance")
            .description("Removes the cached pots when they leave the defined render distance.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> trcr = sgRender.add(new BoolSetting.Builder()
            .name("Tracers")
            .description("Show tracers to the pots.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> nearesttrcr = sgRender.add(new BoolSetting.Builder()
            .name("Tracer to nearest Pot Only")
            .description("Show only one tracer to the nearest pot.")
            .defaultValue(false)
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
            .visible(() -> (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both || trcr.get()))
            .build()
    );
    private final Set<BlockPos> potLocations = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Item> naturalPot = new HashSet<>();
    static {
        naturalPot.add(Items.AIR);
        naturalPot.add(Items.STRING);
        naturalPot.add(Items.EMERALD);
        naturalPot.add(Items.EMERALD_BLOCK);
        naturalPot.add(Items.RAW_IRON_BLOCK);
        naturalPot.add(Items.IRON_INGOT);
        naturalPot.add(Items.TRIAL_KEY);
        naturalPot.add(Items.DIAMOND);
        naturalPot.add(Items.DIAMOND_BLOCK);
    }
    private int closestPotX=2000000000;
    private int closestPotY=2000000000;
    private int closestPotZ=2000000000;
    private double PotDistance=2000000000;
    public PotESP() {
        super(Trouser.baseHunting,"PotESP", "Finds the dank pots... In Minecraft (Locates decorated pots with un-natural items in them)");
    }
    @Override
    public void onActivate() {
        clearChunkData();
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
        potLocations.clear();
        closestPotX=2000000000;
        closestPotY=2000000000;
        closestPotZ=2000000000;
        PotDistance=2000000000;
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
                        if (!potLocations.contains(potLocation) && !naturalPot.contains(potItem) && !junkItemList.get().contains(potItem)) {
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
        if (nearesttrcr.get()){
            try {
                if (potLocations.stream().toList().size() > 0) {
                    for (int b = 0; b < potLocations.stream().toList().size(); b++) {
                        if (PotDistance > Math.sqrt(Math.pow(potLocations.stream().toList().get(b).getX() - mc.player.getBlockX(), 2) + Math.pow(potLocations.stream().toList().get(b).getZ() - mc.player.getBlockZ(), 2))) {
                            closestPotX = potLocations.stream().toList().get(b).getX();
                            closestPotY = potLocations.stream().toList().get(b).getY();
                            closestPotZ = potLocations.stream().toList().get(b).getZ();
                            PotDistance = Math.sqrt(Math.pow(potLocations.stream().toList().get(b).getX() - mc.player.getBlockX(), 2) + Math.pow(potLocations.stream().toList().get(b).getZ() - mc.player.getBlockZ(), 2));
                        }
                    }
                    PotDistance = 2000000000;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (removerenderdist.get())removeChunksOutsideRenderDistance();
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if ((potSideColor.get().a > 5 || potLineColor.get().a > 5) && mc.player != null) {
            synchronized (potLocations) {
                if (!nearesttrcr.get()) {
                    for (BlockPos pos : potLocations) {
                        BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
                        if (pos != null && playerPos.isWithinDistance(pos, renderDistance.get() * 16)) {
                            int startX = pos.getX();
                            int startY = pos.getY();
                            int startZ = pos.getZ();
                            int endX = pos.getX();
                            int endY = pos.getY();
                            int endZ = pos.getZ();
                            render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), potSideColor.get(), potLineColor.get(), shapeMode.get(), event);
                        }
                    }
                } else if (nearesttrcr.get()){
                    for (BlockPos pos : potLocations) {
                        BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
                        if (pos != null && playerPos.isWithinDistance(pos, renderDistance.get() * 16)) {
                            int startX = pos.getX();
                            int startY = pos.getY();
                            int startZ = pos.getZ();
                            int endX = pos.getX();
                            int endY = pos.getY();
                            int endZ = pos.getZ();
                            render(new Box(new Vec3d(startX+1, startY+1, startZ+1), new Vec3d(endX, endY, endZ)), potSideColor.get(), potLineColor.get(), shapeMode.get(), event);
                        }
                    }
                    render2(new Box(new Vec3d(closestPotX+1, closestPotY+1, closestPotZ+1), new Vec3d (closestPotX, closestPotY, closestPotZ)), potSideColor.get(), potLineColor.get(),ShapeMode.Sides, event);
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
    private void removeChunksOutsideRenderDistance() {
        double renderDistanceBlocks = renderDistance.get() * 16;

        removeChunksOutsideRenderDistance(potLocations, renderDistanceBlocks);
    }
    private void removeChunksOutsideRenderDistance(Set<BlockPos> chunkSet, double renderDistanceBlocks) {
        chunkSet.removeIf(blockPos -> {
            BlockPos playerPos = new BlockPos(mc.player.getBlockX(), blockPos.getY(), mc.player.getBlockZ());
            return !playerPos.isWithinDistance(blockPos, renderDistanceBlocks);
        });
    }
}