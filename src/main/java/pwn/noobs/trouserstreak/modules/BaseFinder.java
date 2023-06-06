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
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    This BaseFinder was made from the newchunks code,
    Ported from: https://github.com/BleachDrinker420/BleachHack/blob/master/BleachHack-Fabric-1.16/src/main/java/bleach/hack/module/mods/NewChunks.java
    Ported for meteor-rejects
    updated and modified by etianll :D
*/
public class BaseFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sglists = settings.createGroup("Blocks To Check For (Do Not check one block in more than one list.)");
    private final SettingGroup sgCdata = settings.createGroup("Saved Base Data");
    private final SettingGroup sgcacheCdata = settings.createGroup("Cached Base Data");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // general
    private final Setting<Boolean> nearestbasemsg = sgGeneral.add(new BoolSetting.Builder()
            .name("NearestBaseNotifications (Spam)")
            .description("Spams you with messages about where the nearest base is.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> basemsgticks = sgGeneral.add(new IntSetting.Builder()
            .name("NearestBaseMsgDelay")
            .description("# of Blocks to Find")
            .sliderRange(1,100)
            .defaultValue(40)
            .visible(() -> nearestbasemsg.get())
            .build());
    private final Setting<List<Block>> Blawcks1 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #1 (Default)")
            .description("If the total amount of any of these found is greater than the Number, throw a base location. Blocks are checked in listed order from top to bottom.")
            .defaultValue(
                    Blocks.BLACK_BED, Blocks.BROWN_BED, Blocks.GRAY_BED, Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_GRAY_BED, Blocks.MAGENTA_BED, Blocks.PINK_BED,
                    Blocks.BLACK_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.BROWN_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE,
                    Blocks.BLACK_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER, Blocks.CYAN_CONCRETE_POWDER, Blocks.BROWN_CONCRETE_POWDER, Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER, Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER,
                    Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
                    Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS,
                    Blocks.CRIMSON_PRESSURE_PLATE, Blocks.CRIMSON_BUTTON, Blocks.CRIMSON_DOOR, Blocks.CRIMSON_FENCE, Blocks.CRIMSON_FENCE_GATE, Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_SIGN, Blocks.CRIMSON_WALL_SIGN, Blocks.CRIMSON_SLAB, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_TRAPDOOR,
                    Blocks.WARPED_PRESSURE_PLATE, Blocks.WARPED_BUTTON, Blocks.WARPED_DOOR, Blocks.WARPED_FENCE, Blocks.WARPED_FENCE_GATE, Blocks.WARPED_PLANKS, Blocks.WARPED_SIGN, Blocks.WARPED_WALL_SIGN, Blocks.WARPED_SLAB, Blocks.WARPED_STAIRS, Blocks.WARPED_TRAPDOOR,
                    Blocks.SCAFFOLDING, Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.ACACIA_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.DARK_OAK_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.JUNGLE_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.MANGROVE_SIGN, Blocks.MANGROVE_WALL_SIGN, Blocks.SLIME_BLOCK, Blocks.SPONGE, Blocks.TINTED_GLASS,
                    Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_STAIRS, Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.PETRIFIED_OAK_SLAB,
                    Blocks.STRIPPED_ACACIA_WOOD, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_WOOD, Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_STEM, Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_WOOD, Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_WOOD, Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_WARPED_STEM,
                    Blocks.SHULKER_BOX, Blocks.BLACK_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.WHITE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX,
                    Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON, Blocks.ACTIVATOR_RAIL, Blocks.BEACON, Blocks.BEEHIVE, Blocks.REPEATING_COMMAND_BLOCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.COAL_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.IRON_BLOCK, Blocks.NETHERITE_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.CAKE, Blocks.CONDUIT, Blocks.DAYLIGHT_DETECTOR, Blocks.DETECTOR_RAIL, Blocks.DRIED_KELP_BLOCK, Blocks.DROPPER, Blocks.ENCHANTING_TABLE,
                    Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD, Blocks.DRAGON_WALL_HEAD, Blocks.DRAGON_HEAD, Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD, Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD, Blocks.SKELETON_WALL_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL,
                    Blocks.HONEY_BLOCK, Blocks.HONEYCOMB_BLOCK, Blocks.HOPPER, Blocks.JUKEBOX, Blocks.LIGHTNING_ROD, Blocks.LODESTONE, Blocks.OBSERVER, Blocks.PACKED_MUD, Blocks.POWERED_RAIL, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE, Blocks.JUNGLE_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE, Blocks.MANGROVE_PRESSURE_PLATE, Blocks.CRIMSON_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE, Blocks.RESPAWN_ANCHOR
                    )
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks2 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #2 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location. Blocks are checked in numerical listed order from top to bottom.")
            .defaultValue(Blocks.FURNACE, Blocks.SPRUCE_WALL_SIGN)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks3 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #3 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location. Blocks are checked in numerical listed order from top to bottom.")
            .defaultValue(Blocks.CRAFTING_TABLE, Blocks.ENDER_CHEST, Blocks.SMOOTH_QUARTZ, Blocks.NOTE_BLOCK)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks4 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #4 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location. Blocks are checked in numerical listed order from top to bottom.")
            .defaultValue(Blocks.OAK_WALL_SIGN, Blocks.TRAPPED_CHEST, Blocks.IRON_TRAPDOOR, Blocks.LAPIS_BLOCK)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks5 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #5 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location. Blocks are checked in numerical listed order from top to bottom.")
            .defaultValue(Blocks.WALL_TORCH, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.POLISHED_DIORITE, Blocks.QUARTZ_BLOCK, Blocks.RED_BED, Blocks.WHITE_BED, Blocks.YELLOW_BED, Blocks.ORANGE_BED, Blocks.BLUE_BED, Blocks.CYAN_BED, Blocks.GREEN_BED, Blocks.LIME_BED, Blocks.PURPLE_BED)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks6 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #6 (Extra Custom)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location. Blocks are checked in numerical listed order from top to bottom.")
            .defaultValue(Blocks.TORCH)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<Integer> blowkfind1 = sglists.add(new IntSetting.Builder()
            .name("(List #1) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(1)
            .build());
    private final Setting<Integer> blowkfind2 = sglists.add(new IntSetting.Builder()
            .name("(List #2) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(5)
            .build());
    private final Setting<Integer> blowkfind3 = sglists.add(new IntSetting.Builder()
            .name("(List #3) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(3)
            .build());
    private final Setting<Integer> blowkfind4 = sglists.add(new IntSetting.Builder()
            .name("(List #4) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(2)
            .build());
    private final Setting<Integer> blowkfind5 = sglists.add(new IntSetting.Builder()
            .name("(List #5) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(20)
            .build());
    private final Setting<Integer> blowkfind6 = sglists.add(new IntSetting.Builder()
            .name("(List #6) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(24)
            .build());
    private final Setting<Boolean> remove = sgcacheCdata.add(new BoolSetting.Builder()
            .name("RemoveOnModuleDisabled")
            .description("Removes the cached chunks containing bases when disabling the module.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> worldleaveremove = sgcacheCdata.add(new BoolSetting.Builder()
            .name("RemoveOnLeaveWorldOrChangeDimensions")
            .description("Removes the cached chunks containing bases when leaving the world or changing dimensions.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> save = sgCdata.add(new BoolSetting.Builder()
            .name("SaveBaseData")
            .description("Saves the cached bases to a file.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> load = sgCdata.add(new BoolSetting.Builder()
            .name("LoadBaseData")
            .description("Loads the saved bases from the file.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> autoreload = sgCdata.add(new BoolSetting.Builder()
            .name("AutoReloadBases")
            .description("Reloads the bases automatically from your savefiles on a delay.")
            .defaultValue(false)
            .visible(() -> load.get())
            .build()
    );
    private final Setting<Integer> removedelay = sgCdata.add(new IntSetting.Builder()
            .name("AutoReloadDelayInSeconds")
            .description("Reloads the bases automatically from your savefiles on a delay.")
            .sliderRange(1,300)
            .defaultValue(60)
            .visible(() -> autoreload.get() && load.get())
            .build());
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton nearestB = table.add(theme.button("NearestBase")).expandX().minWidth(100).widget();
        nearestB.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else {
                if (closestbaseX<1000000000 && closestbaseZ<1000000000){
                findnearestbase=true;
                ChatUtils.sendMsg(Text.of("#Nearest possible base near X"+closestbaseX+" x Z"+closestbaseZ));
                findnearestbase=false;
                }else error("No Bases Logged Yet.");
            }
        };
        table.row();
        WButton adddata = table.add(theme.button("AddBase")).expandX().minWidth(100).widget();
        adddata.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else {
                AddCoordX= mc.player.getChunkPos().x;
                AddCoordZ= mc.player.getChunkPos().z;
                ChatUtils.sendMsg(Text.of("Base near X"+mc.player.getChunkPos().getCenterX()+", Z"+mc.player.getChunkPos().getCenterZ()+" added to the BaseFinder."));
            }
        };
        table.row();
        WButton deldata = table.add(theme.button("RemoveBase")).expandX().minWidth(100).widget();
        deldata.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else {
                RemoveCoordX= mc.player.getChunkPos().x;
                RemoveCoordZ= mc.player.getChunkPos().z;
                ChatUtils.sendMsg(Text.of("Base near X"+mc.player.getChunkPos().getCenterX()+", Z"+mc.player.getChunkPos().getCenterZ()+" removed from the BaseFinder."));
            }
        };
        table.row();
        WButton deletedata = table.add(theme.button("**DELETE ALL BASE DATA**")).expandX().minWidth(100).widget();
        deletedata.action = () -> {
            if (!(mc.world==null) && mc.world.isChunkLoaded(mc.player.getChunkPos().x,mc.player.getChunkPos().z)){
            if (deletewarning==0) error("THIS WILL DELETE ALL BASE DATA FOR THIS DIMENSION. PRESS AGAIN TO PROCEED.");
            deletewarning++;
            }
        };
        table.row();
        return table;
    }

    // render
    public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
            .name("Render-Distance(Chunks)")
            .description("How many chunks from the character to render the detected chunks with bases.")
            .defaultValue(512)
            .min(6)
            .sliderRange(6,1024)
            .build()
    );
    public final Setting<Integer> renderHeightY = sgRender.add(new IntSetting.Builder()
            .name("render-TopY")
            .description("The render height.")
            .defaultValue(256)
            .sliderRange(-128,512)
            .build()
    );
    public final Setting<Integer> renderHeightYbottom = sgRender.add(new IntSetting.Builder()
            .name("render-BottomY")
            .description("The render height.")
            .defaultValue(150)
            .sliderRange(-128,512)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> baseChunksSideColor = sgRender.add(new ColorSetting.Builder()
            .name("Base-chunks-side-color")
            .description("Color of the chunks that may contain bases or builds.")
            .defaultValue(new SettingColor(255, 127, 0, 40, true))
            .visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> baseChunksLineColor = sgRender.add(new ColorSetting.Builder()
            .name("Base-chunks-line-color")
            .description("Color of the chunks that may contain bases or builds.")
            .defaultValue(new SettingColor(255, 127, 0, 80, true))
            .visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
            .build()
    );
    private int deletewarning=0;
    private boolean checkingchunk1=false;
    private int found1 = 0;
    private boolean checkingchunk2=false;
    private int found2 = 0;
    private boolean checkingchunk3=false;
    private int found3 = 0;
    private boolean checkingchunk4=false;
    private int found4 = 0;
    private boolean checkingchunk5=false;
    private int found5 = 0;
    private boolean checkingchunk6=false;
    private int found6 = 0;
    public static int closestbaseX=2000000000;
    public static int closestbaseZ=2000000000;
    private static int closestX=2000000000;
    private static int closestZ=2000000000;
    private String serverip;
    private String world;
    private ChunkPos basepos;
    private BlockPos blockposi;
    private final Set<ChunkPos> baseChunks = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> blockpositions1 = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> blockpositions2 = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> blockpositions3 = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> blockpositions4 = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> blockpositions5 = Collections.synchronizedSet(new HashSet<>());
    private final Set<BlockPos> blockpositions6 = Collections.synchronizedSet(new HashSet<>());
    public static int isBaseFinderModuleOn=0;
    private int autoreloadticks=0;
    private int loadingticks=0;
    private int reloadworld=0;
    public int basenotifticks=0;
    public static int AddCoordX=2000000000;
    public static int AddCoordZ=2000000000;
    public static int RemoveCoordX=1500000000;
    public static int RemoveCoordZ=1500000000;
    public int findnearestbaseticks=0;
    public static boolean findnearestbase;
    public BaseFinder() {
        super(Trouser.Main,"BaseFinder", "Estimates if a build or base may be in the chunk based on the blocks it contains. May cause lag.");
    }
    @Override
    public void onActivate() {
        isBaseFinderModuleOn=1;
        if (autoreload.get()) {
            baseChunks.clear();
        }
        if (mc.isInSingleplayer()==true){
            String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
            serverip=array[array.length-2];
            world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
        } else {
            serverip = mc.getCurrentServerEntry().address.replace(':', '_');}
        world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
        if (save.get()){
            new File("BaseChunks/"+serverip+"/"+world).mkdirs();
        }
        if (load.get()){
            loadData();
        }
        autoreloadticks=0;
        loadingticks=0;
        reloadworld=0;
    }

    @Override
    public void onDeactivate() {
        isBaseFinderModuleOn=0;
        basenotifticks=0;
        autoreloadticks=0;
        loadingticks=0;
        reloadworld=0;
        if (remove.get()|autoreload.get()) {
            baseChunks.clear();
        }
        super.onDeactivate();
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            basenotifticks=0;
            if (worldleaveremove.get()) {
                baseChunks.clear();
            }
        }
        if (event.screen instanceof DownloadingTerrainScreen) {
            basenotifticks=0;
            reloadworld=0;
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        basenotifticks=0;
        if (worldleaveremove.get()) {
            baseChunks.clear();
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (deletewarning>=2){
                baseChunks.clear();
                new File("BaseChunks/"+serverip+"/"+world+"/BaseChunkData.txt").delete();
                error("Base Data deleted for this Dimension.");
                deletewarning=0;
        }
        if (load.get()){
            loadingticks++;
            if (loadingticks<2){
                loadData();
            }
        } else if (!load.get()){
            loadingticks=0;
        }
        if (!baseChunks.contains(new ChunkPos(AddCoordX,AddCoordZ))){
            baseChunks.add(new ChunkPos(AddCoordX,AddCoordZ));
            try {
                new File("BaseChunks/"+serverip+"/"+world).mkdirs();
                FileWriter writer = new FileWriter("BaseChunks/"+serverip+"/"+world+"/BaseChunkData.txt", true);
                writer.write(String.valueOf(new ChunkPos(AddCoordX,AddCoordZ)));
                writer.write("\r\n");   // write new line
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AddCoordX=2000000000;
            AddCoordZ=2000000000;
        }
        if (baseChunks.contains(new ChunkPos(RemoveCoordX,RemoveCoordZ))){
            baseChunks.remove(new ChunkPos(RemoveCoordX,RemoveCoordZ));
            new File("BaseChunks/"+serverip+"/"+world+"/BaseChunkData.txt").delete();
            for (int rb = 0; rb < baseChunks.stream().toList().size(); rb++){
                try {
                    new File("BaseChunks/"+serverip+"/"+world).mkdirs();
                    FileWriter writer = new FileWriter("BaseChunks/"+serverip+"/"+world+"/BaseChunkData.txt", true);
                    writer.write(String.valueOf(baseChunks.stream().toList().get(rb)));
                    writer.write("\r\n");   // write new line
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            RemoveCoordX=1500000000;
            RemoveCoordZ=1500000000;
        }
        if (findnearestbase=true){
            findnearestbaseticks++;
            if (findnearestbaseticks>=1){
                findnearestbaseticks=0;
                closestX=2000000000;
                closestZ=2000000000;
                findnearestbase=false;}
            if (findnearestbase=true && findnearestbaseticks<1){
                if (baseChunks.stream().toList().size()>0){
                for (int b = 0; b < baseChunks.stream().toList().size(); b++){
                    if(Math.abs(baseChunks.stream().toList().get(b).getCenterX()-mc.player.getChunkPos().getCenterX())<closestX || Math.abs(baseChunks.stream().toList().get(b).getCenterZ()-mc.player.getChunkPos().getCenterZ())<closestZ){
                        closestX=Math.abs(baseChunks.stream().toList().get(b).getCenterX()-mc.player.getChunkPos().getCenterX());
                        closestZ=Math.abs(baseChunks.stream().toList().get(b).getCenterZ()-mc.player.getChunkPos().getCenterZ());
                        closestbaseX=baseChunks.stream().toList().get(b).getCenterX();
                        closestbaseZ=baseChunks.stream().toList().get(b).getCenterZ();
                    }
                }
                }
            }
            if (nearestbasemsg.get()){
                if (closestbaseX<1000000000 && closestbaseZ<1000000000){
                basenotifticks++;
                if (basenotifticks>=basemsgticks.get()){
                    ChatUtils.sendMsg(Text.of("Nearest possible base near X"+closestbaseX+" x Z"+closestbaseZ));
                    basenotifticks=0;
                }
            }
            }
        }

        if (mc.isInSingleplayer()==true){
            String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
            serverip=array[array.length-2];
            world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
        } else {
            serverip = mc.getCurrentServerEntry().address.replace(':', '_');}
        world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');

        if (autoreload.get()) {
            autoreloadticks++;
            if (autoreloadticks==removedelay.get()*20){
                baseChunks.clear();
                if (load.get()){
                    loadData();
                }
            } else if (autoreloadticks>=removedelay.get()*20){
                autoreloadticks=0;
            }
        }
        //autoreload when entering different dimensions
        if (reloadworld<10){
            reloadworld++;
        }
        if (reloadworld==3){
            if (worldleaveremove.get()){
                baseChunks.clear();
            }
            loadData();
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (baseChunksLineColor.get().a > 5 || baseChunksSideColor.get().a > 5){
            synchronized (baseChunks) {
                for (ChunkPos c : baseChunks) {
                    if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), renderDistance.get()*16)) {
                        render(new Box(c.getStartPos().add(7, renderHeightYbottom.get(), 7), c.getStartPos().add(8, renderHeightY.get(), 8)), baseChunksSideColor.get(), baseChunksLineColor.get(), shapeMode.get(), event);
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
        if (event.packet instanceof ChunkDataS2CPacket && mc.world != null) {
            ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;

            basepos = new ChunkPos(packet.getX(), packet.getZ());

            if (mc.world.getChunkManager().getChunk(packet.getX(), packet.getZ()) == null) {
                WorldChunk chunk = new WorldChunk(mc.world, basepos);
                try {
                    chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()));
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }

                if (Blawcks1.get().size()>0 || Blawcks2.get().size()>0 || Blawcks3.get().size()>0 || Blawcks4.get().size()>0 || Blawcks5.get().size()>0 || Blawcks6.get().size()>0){
                    try {
                for (int x = 0; x < 16; x++) {
                    for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockState blerks = chunk.getBlockState(new BlockPos(x, y, z));
                            blockposi=new BlockPos(x, y, z);
                            if (!(blerks.getBlock()==Blocks.AIR)){
                                if (!(blerks.getBlock()==Blocks.STONE)){
                                    if (!(blerks.getBlock()==Blocks.DEEPSLATE) && !(blerks.getBlock()==Blocks.DIRT) && !(blerks.getBlock()==Blocks.GRASS_BLOCK) && !(blerks.getBlock()==Blocks.WATER) && !(blerks.getBlock()==Blocks.SAND) && !(blerks.getBlock()==Blocks.GRAVEL)  && !(blerks.getBlock()==Blocks.BEDROCK)&& !(blerks.getBlock()==Blocks.NETHERRACK) && !(blerks.getBlock()==Blocks.LAVA)){
                                        if (Blawcks1.get().size()>0){
                                        for (int b = 0; b < Blawcks1.get().size(); b++){
                                            if (blerks.getBlock()==Blawcks1.get().get(b)) {
                                                blockpositions1.add(blockposi);
                                                found1= blockpositions1.size();
                                            }
                                        }
                                        }
                                        if (Blawcks2.get().size()>0){
                                            for (int b = 0; b < Blawcks2.get().size(); b++){
                                                if (blerks.getBlock()==Blawcks2.get().get(b)) {
                                                    blockpositions2.add(blockposi);
                                                    found2= blockpositions2.size();
                                                }
                                            }
                                        }
                                        if (Blawcks3.get().size()>0){
                                            for (int b = 0; b < Blawcks3.get().size(); b++){
                                                if (blerks.getBlock()==Blawcks3.get().get(b)) {
                                                    blockpositions3.add(blockposi);
                                                    found3= blockpositions3.size();
                                                }
                                            }
                                        }
                                        if (Blawcks4.get().size()>0){
                                            for (int b = 0; b < Blawcks4.get().size(); b++){
                                                if (blerks.getBlock()==Blawcks4.get().get(b)) {
                                                    blockpositions4.add(blockposi);
                                                    found4= blockpositions4.size();
                                                }
                                            }
                                        }
                                        if (Blawcks5.get().size()>0){
                                            for (int b = 0; b < Blawcks5.get().size(); b++){
                                                if (blerks.getBlock()==Blawcks5.get().get(b)) {
                                                    blockpositions5.add(blockposi);
                                                    found5= blockpositions5.size();
                                                }
                                            }
                                        }
                                        if (Blawcks6.get().size()>0){
                                            for (int b = 0; b < Blawcks6.get().size(); b++){
                                                if (blerks.getBlock()==Blawcks6.get().get(b)) {
                                                    blockpositions6.add(blockposi);
                                                    found6= blockpositions6.size();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (Blawcks1.get().size()>0)checkingchunk1=true;
                            if (Blawcks2.get().size()>0)checkingchunk2=true;
                            if (Blawcks3.get().size()>0)checkingchunk3=true;
                            if (Blawcks4.get().size()>0)checkingchunk4=true;
                            if (Blawcks5.get().size()>0)checkingchunk5=true;
                            if (Blawcks6.get().size()>0)checkingchunk6=true;
                        }
                    }
                }
                        //CheckList 1
                        if (Blawcks1.get().size()>0){
                            if (checkingchunk1==true && found1>=blowkfind1.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData();
                                    }
                                    ChatUtils.sendMsg(Text.of("(List1)Possible build located near X"+basepos.getCenterX()+" x Z"+basepos.getCenterZ()));
                                }
                                blockpositions1.clear();
                                found1 = 0;
                                checkingchunk1=false;
                            } else if (checkingchunk1==true && found1<blowkfind1.get()){
                                blockpositions1.clear();
                                found1 = 0;
                                checkingchunk1=false;
                            }
                        }

                        //CheckList 2
                        if (Blawcks2.get().size()>0){
                            if (checkingchunk2==true && found2>=blowkfind2.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData();
                                    }
                                    ChatUtils.sendMsg(Text.of("(List2)Possible build located near X"+basepos.getCenterX()+" x Z"+basepos.getCenterZ()));
                                }
                                blockpositions2.clear();
                                found2 = 0;
                                checkingchunk2=false;
                            } else if (checkingchunk2==true && found2<blowkfind2.get()){
                                blockpositions2.clear();
                                found2 = 0;
                                checkingchunk2=false;
                            }
                        }

                        //CheckList 3
                        if (Blawcks3.get().size()>0){
                            if (checkingchunk3==true && found3>=blowkfind3.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData();
                                    }
                                    ChatUtils.sendMsg(Text.of("(List3)Possible build located near X"+basepos.getCenterX()+" x Z"+basepos.getCenterZ()));
                                }
                                blockpositions3.clear();
                                found3 = 0;
                                checkingchunk3=false;
                            } else if (checkingchunk3==true && found3<blowkfind3.get()){
                                blockpositions3.clear();
                                found3 = 0;
                                checkingchunk3=false;
                            }
                        }

                        //CheckList 4
                        if (Blawcks4.get().size()>0){
                            if (checkingchunk4==true && found4>=blowkfind4.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData();
                                    }
                                    ChatUtils.sendMsg(Text.of("(List4)Possible build located near X"+basepos.getCenterX()+" x Z"+basepos.getCenterZ()));
                                }
                                blockpositions4.clear();
                                found4 = 0;
                                checkingchunk4=false;
                            } else if (checkingchunk4==true && found4<blowkfind4.get()){
                                blockpositions4.clear();
                                found4 = 0;
                                checkingchunk4=false;
                            }
                        }

                        //CheckList 5
                        if (Blawcks5.get().size()>0){
                            if (checkingchunk5==true && found5>=blowkfind5.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData();
                                    }
                                    ChatUtils.sendMsg(Text.of("(List5)Possible build located near X"+basepos.getCenterX()+" x Z"+basepos.getCenterZ()));
                                }
                                blockpositions5.clear();
                                found5 = 0;
                                checkingchunk5=false;
                            } else if (checkingchunk5==true && found5<blowkfind5.get()){
                                blockpositions5.clear();
                                found5 = 0;
                                checkingchunk5=false;
                            }
                        }

                        //CheckList 6
                        if (Blawcks6.get().size()>0){
                            if (checkingchunk6==true && found6>=blowkfind6.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData();
                                    }
                                    ChatUtils.sendMsg(Text.of("(List6)Possible build located near X"+basepos.getCenterX()+" x Z"+basepos.getCenterZ()));
                                }
                                blockpositions6.clear();
                                found6 = 0;
                                checkingchunk6=false;
                            } else if (checkingchunk6==true && found6<blowkfind6.get()){
                                blockpositions6.clear();
                                found6 = 0;
                                checkingchunk6=false;
                            }
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        } else {}
    }
    private void loadData() {
        try {
            List<String> allLines = Files.readAllLines(Paths.get("BaseChunks/"+serverip+"/"+world+"/BaseChunkData.txt"));

            for (String line : allLines) {
                String s = line;
                String[] array = s.split(", ");
                int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]",""));
                int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]",""));
                basepos = new ChunkPos(X,Z);
                baseChunks.add(basepos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void saveBaseChunkData() {
        try {
            new File("BaseChunks/"+serverip+"/"+world).mkdirs();
            FileWriter writer = new FileWriter("BaseChunks/"+serverip+"/"+world+"/BaseChunkData.txt", true);
            writer.write(String.valueOf(basepos));
            writer.write("\r\n");   // write new line
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean filterBlocks(Block block) {
        return isNaturalLagCausingBlock(block);
    }
    private boolean isNaturalLagCausingBlock(Block block) {
        return  block instanceof Block &&
                !(block ==Blocks.AIR) &&
                !(block ==Blocks.STONE) &&
                !(block ==Blocks.DIRT) &&
                !(block ==Blocks.GRASS_BLOCK) &&
                !(block ==Blocks.SAND) &&
                !(block ==Blocks.GRAVEL) &&
                !(block ==Blocks.DEEPSLATE) &&
                !(block ==Blocks.WATER) &&
                !(block ==Blocks.BEDROCK) &&
                !(block ==Blocks.NETHERRACK) &&
                !(block ==Blocks.LAVA);
    }
}