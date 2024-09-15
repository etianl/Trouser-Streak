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
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HangingSignBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/*
    This BaseFinder was made from the newchunks code,
    Newchunks was Ported from: https://github.com/BleachDrinker420/BleachHack/blob/master/BleachHack-Fabric-1.16/src/main/java/bleach/hack/module/mods/NewChunks.java
    Ported for meteor-rejects
    updated and modified by etianll :D
*/
public class BaseFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDetectors = settings.createGroup("Block Detectors");
    private final SettingGroup sgEDetectors = settings.createGroup("Entity Detectors");
    private final SettingGroup sglists = settings.createGroup("Blocks To Check For");
    private final SettingGroup sgCdata = settings.createGroup("Saved Base Data");
    private final SettingGroup sgcacheCdata = settings.createGroup("Cached Base Data");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // general
    private final Setting<Integer> minY = sgGeneral.add(new IntSetting.Builder()
            .name("Detection Y Minimum OffSet")
            .description("Scans blocks above or at this this many blocks from minimum build limit.")
            .min(0)
            .sliderRange(0,319)
            .defaultValue(0)
            .build());
    private final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
            .name("Detection Y Maximum OffSet")
            .description("Scans blocks below or at this this many blocks from maximum build limit.")
            .min(0)
            .sliderRange(0,319)
            .defaultValue(0)
            .build());
    private final Setting<Boolean> signFinder = sgDetectors.add(new BoolSetting.Builder()
            .name("Written Sign Finder")
            .description("Finds signs that have text on them because they are not natural.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> portalFinder = sgDetectors.add(new BoolSetting.Builder()
            .name("Open Portal Finder")
            .description("Finds End/Nether portals that are open because they are usually not natural.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> bubblesFinder = sgDetectors.add(new BoolSetting.Builder()
            .name("Bubble Column Finder")
            .description("Finds bubble column blocks made by soul sand because they are not natural.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> skybuildfind = sgDetectors.add(new BoolSetting.Builder()
            .name("Sky Build Finder")
            .description("If Blocks higher than terrain can naturally generate, flag chunk as possible build.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> skybuildint = sgDetectors.add(new IntSetting.Builder()
            .name("Sky Build Y Threshold")
            .description("If Blocks higher than this Y value, flag chunk as possible build.")
            .min(-64)
            .sliderRange(-64, 319)
            .defaultValue(260)
            .visible(() -> skybuildfind.get())
            .build());
    private final Setting<Boolean> bedrockfind = sgDetectors.add(new BoolSetting.Builder()
            .name("Bedrock Finder")
            .description("If Bedrock Blocks higher than they can naturally generate in the Overworld or Nether, flag chunk as possible build.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> bedrockint = sgDetectors.add(new IntSetting.Builder()
            .name("Bedrock Y Threshold")
            .description("If bedrock higher than this many blocks above minimum build limit, flag chunk as possible build.")
            .min(0)
            .sliderRange(0, 384)
            .defaultValue(4)
            .visible(() -> bedrockfind.get())
            .build());
    private final Setting<Boolean> spawner = sgDetectors.add(new BoolSetting.Builder()
            .name("Unnatural Spawner Finder")
            .description("If a spawner doesn't have the proper natural companion blocks with it in the chunk, flag as possible build.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> roofDetector = sgDetectors.add(new BoolSetting.Builder()
            .name("Nether Roof Build Finder")
            .description("If anything but mushrooms on the nether roof, flag as possible build.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> entityScanDelay = sgEDetectors.add(new IntSetting.Builder()
            .name("Entity Scan Tick Delay")
            .description("Delay between scanning all the entities within render distance.")
            .min(0)
            .sliderRange(0,300)
            .defaultValue(20)
            .build());
    private final Setting<Boolean> frameFinder = sgEDetectors.add(new BoolSetting.Builder()
            .name("Item Frame Finder")
            .description("Finds item frames that do not contain an elytra because they are not natural.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> pearlFinder = sgEDetectors.add(new BoolSetting.Builder()
            .name("Ender Pearl Finder")
            .description("Finds ender pearls entities because they are not natural.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> nameFinder = sgEDetectors.add(new BoolSetting.Builder()
            .name("NameTag Finder")
            .description("Finds mobs with a nametag because they are not natural.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> villagerFinder = sgEDetectors.add(new BoolSetting.Builder()
            .name("Villager Finder")
            .description("Finds villagers with a level greater than 1 because they are not natural.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> boatFinder = sgEDetectors.add(new BoolSetting.Builder()
            .name("Boat Finder")
            .description("Finds villagers with a level greater than 1 because they are not natural.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> entityClusterFinder = sgEDetectors.add(new BoolSetting.Builder()
            .name("Entity Cluster Finder")
            .description("Finds clusters of entities per chunk.")
            .defaultValue(true)
            .build());
    private final Setting<Set<EntityType<?>>> entitieslist = sgEDetectors.add(new EntityTypeListSetting.Builder()
            .name("Entities")
            .description("Select specific entities.")
            .defaultValue(getDefaultCreatures())
            .build()
    );
    private Set<EntityType<?>> getDefaultCreatures() {
        Set<EntityType<?>> creatures = new HashSet<>();
        Registries.ENTITY_TYPE.forEach(entityType -> {
            if (entityType.getSpawnGroup() == SpawnGroup.CREATURE) {
                creatures.add(entityType);
            }
        });
        return creatures;
    }
    private final Setting<Integer> animalsFoundThreshold = sgEDetectors.add(new IntSetting.Builder()
            .name("Entity Cluster Threshold")
            .description("Once this many entities are found in a chunk trigger it as being a base.")
            .min(0)
            .sliderRange(0,100)
            .defaultValue(12)
            .build());
    private final Setting<Integer> bsefndtickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("Base Found Message Tick Delay")
            .description("Delays the allowance of Base Found messages to reduce spam.")
            .min(0)
            .sliderRange(0,300)
            .defaultValue(5)
            .build());
    private final Setting<List<Block>> Blawcks1 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #1 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(
                    Blocks.CRAFTER, Blocks.BLACK_BED, Blocks.GRAY_BED, Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_GRAY_BED, Blocks.PINK_BED,
                    Blocks.SPRUCE_SAPLING, Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.CHERRY_SAPLING, Blocks.BAMBOO_SAPLING,
                    Blocks.CHERRY_BUTTON, Blocks.CHERRY_DOOR, Blocks.CHERRY_FENCE, Blocks.CHERRY_FENCE_GATE, Blocks.CHERRY_PLANKS, Blocks.CHERRY_PRESSURE_PLATE, Blocks.CHERRY_STAIRS, Blocks.CHERRY_WOOD, Blocks.CHERRY_TRAPDOOR, Blocks.CHERRY_SLAB,
                    Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_BUTTON, Blocks.MANGROVE_DOOR, Blocks.MANGROVE_FENCE, Blocks.MANGROVE_FENCE_GATE, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB, Blocks.MANGROVE_TRAPDOOR,
                    Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE_GATE, Blocks.BIRCH_BUTTON, Blocks.ACACIA_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.POLISHED_BLACKSTONE_BUTTON, Blocks.SPRUCE_BUTTON,
                    Blocks.BAMBOO_BLOCK, Blocks.BAMBOO_BUTTON, Blocks.BAMBOO_DOOR, Blocks.BAMBOO_FENCE, Blocks.BAMBOO_FENCE_GATE, Blocks.BAMBOO_MOSAIC, Blocks.BAMBOO_MOSAIC_SLAB, Blocks.BAMBOO_MOSAIC_STAIRS, Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_PRESSURE_PLATE, Blocks.BAMBOO_SLAB, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_TRAPDOOR, Blocks.CHISELED_BOOKSHELF,
                    Blocks.BLACK_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.BROWN_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.GREEN_CONCRETE,
                    Blocks.BLACK_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER, Blocks.CYAN_CONCRETE_POWDER, Blocks.BROWN_CONCRETE_POWDER, Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER, Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER,
                    Blocks.PURPLE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.PINK_GLAZED_TERRACOTTA, Blocks.GRAY_GLAZED_TERRACOTTA, Blocks.BLUE_GLAZED_TERRACOTTA, Blocks.BROWN_GLAZED_TERRACOTTA, Blocks.GREEN_GLAZED_TERRACOTTA,
                    Blocks.OXIDIZED_COPPER, Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER, Blocks.CUT_COPPER_SLAB, Blocks.CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.COPPER_BULB, Blocks.EXPOSED_COPPER_BULB, Blocks.WEATHERED_COPPER_BULB, Blocks.OXIDIZED_COPPER_BULB, Blocks.CHISELED_COPPER, Blocks.EXPOSED_CHISELED_COPPER, Blocks.WEATHERED_CHISELED_COPPER, Blocks.OXIDIZED_CHISELED_COPPER, Blocks.COPPER_DOOR, Blocks.EXPOSED_COPPER_DOOR, Blocks.WEATHERED_COPPER_DOOR, Blocks.OXIDIZED_COPPER_DOOR, Blocks.COPPER_GRATE, Blocks.EXPOSED_COPPER_GRATE, Blocks.WEATHERED_COPPER_GRATE, Blocks.OXIDIZED_COPPER_GRATE, Blocks.COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR,
                    Blocks.WAXED_EXPOSED_COPPER, Blocks.WAXED_WEATHERED_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.WAXED_WEATHERED_CUT_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_CHISELED_COPPER, Blocks.WAXED_WEATHERED_CHISELED_COPPER, Blocks.WAXED_EXPOSED_COPPER_DOOR, Blocks.WAXED_WEATHERED_COPPER_DOOR, Blocks.WAXED_EXPOSED_COPPER_GRATE, Blocks.WAXED_WEATHERED_COPPER_GRATE, Blocks.WAXED_COPPER_TRAPDOOR, Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR,
                    Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.POTTED_MANGROVE_PROPAGULE, Blocks.POTTED_AZALEA_BUSH, Blocks.POTTED_CHERRY_SAPLING, Blocks.POTTED_FERN, Blocks.POTTED_ACACIA_SAPLING, Blocks.POTTED_WARPED_FUNGUS, Blocks.POTTED_WARPED_ROOTS, Blocks.POTTED_CRIMSON_FUNGUS, Blocks.POTTED_CRIMSON_ROOTS, Blocks.POTTED_OAK_SAPLING, Blocks.POTTED_WITHER_ROSE, Blocks.WITHER_ROSE,
                    Blocks.CAKE, Blocks.CANDLE_CAKE, Blocks.BLUE_CANDLE_CAKE, Blocks.BLACK_CANDLE_CAKE, Blocks.BROWN_CANDLE_CAKE, Blocks.CYAN_CANDLE_CAKE, Blocks.GRAY_CANDLE_CAKE, Blocks.GREEN_CANDLE_CAKE, Blocks.LIGHT_BLUE_CANDLE_CAKE, Blocks.LIGHT_GRAY_CANDLE_CAKE, Blocks.LIME_CANDLE_CAKE, Blocks.MAGENTA_CANDLE_CAKE, Blocks.ORANGE_CANDLE_CAKE, Blocks.PINK_CANDLE_CAKE, Blocks.PURPLE_CANDLE_CAKE, Blocks.RED_CANDLE_CAKE, Blocks.WHITE_CANDLE_CAKE, Blocks.YELLOW_CANDLE_CAKE,
                    Blocks.BLUE_CANDLE, Blocks.BLACK_CANDLE, Blocks.BROWN_CANDLE, Blocks.CYAN_CANDLE, Blocks.GRAY_CANDLE, Blocks.GREEN_CANDLE, Blocks.LIGHT_BLUE_CANDLE, Blocks.LIGHT_GRAY_CANDLE, Blocks.LIME_CANDLE, Blocks.MAGENTA_CANDLE, Blocks.ORANGE_CANDLE, Blocks.PINK_CANDLE, Blocks.PURPLE_CANDLE, Blocks.YELLOW_CANDLE,
                    Blocks.SMOOTH_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_STAIRS, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE_WALL,
                    Blocks.ANDESITE_STAIRS, Blocks.ANDESITE_SLAB, Blocks.ANDESITE_WALL, Blocks.POLISHED_ANDESITE_SLAB, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_GRANITE_SLAB, Blocks.POLISHED_GRANITE_STAIRS, Blocks.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE_STAIRS,
                    Blocks.TUFF_SLAB, Blocks.TUFF_STAIRS, Blocks.TUFF_WALL, Blocks.TUFF_BRICK_SLAB, Blocks.TUFF_BRICK_STAIRS, Blocks.TUFF_BRICK_WALL,
                    Blocks.CRACKED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICK_WALL, Blocks.RED_NETHER_BRICKS, Blocks.RED_NETHER_BRICK_SLAB, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICK_WALL,
                    Blocks.ORANGE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.RED_STAINED_GLASS,
                    Blocks.CRIMSON_PRESSURE_PLATE, Blocks.CRIMSON_BUTTON, Blocks.CRIMSON_DOOR, Blocks.CRIMSON_FENCE, Blocks.CRIMSON_FENCE_GATE, Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_SIGN, Blocks.CRIMSON_WALL_SIGN, Blocks.CRIMSON_SLAB, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_TRAPDOOR,
                    Blocks.WARPED_PRESSURE_PLATE, Blocks.WARPED_BUTTON, Blocks.WARPED_DOOR, Blocks.WARPED_FENCE, Blocks.WARPED_FENCE_GATE, Blocks.WARPED_PLANKS, Blocks.WARPED_SIGN, Blocks.WARPED_WALL_SIGN, Blocks.WARPED_SLAB, Blocks.WARPED_STAIRS, Blocks.WARPED_TRAPDOOR,
                    Blocks.SCAFFOLDING, Blocks.CHERRY_SIGN, Blocks.CHERRY_WALL_SIGN, Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.ACACIA_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.DARK_OAK_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.JUNGLE_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.MANGROVE_SIGN, Blocks.MANGROVE_WALL_SIGN, Blocks.SLIME_BLOCK, Blocks.SPONGE, Blocks.TINTED_GLASS,
                    Blocks.ACACIA_HANGING_SIGN, Blocks.ACACIA_WALL_HANGING_SIGN, Blocks.BAMBOO_HANGING_SIGN, Blocks.BAMBOO_WALL_HANGING_SIGN, Blocks.BIRCH_HANGING_SIGN, Blocks.BIRCH_WALL_HANGING_SIGN, Blocks.CHERRY_HANGING_SIGN, Blocks.CHERRY_WALL_HANGING_SIGN, Blocks.CRIMSON_HANGING_SIGN, Blocks.CRIMSON_WALL_HANGING_SIGN, Blocks.DARK_OAK_HANGING_SIGN, Blocks.DARK_OAK_WALL_HANGING_SIGN, Blocks.JUNGLE_HANGING_SIGN, Blocks.JUNGLE_WALL_HANGING_SIGN, Blocks.MANGROVE_HANGING_SIGN, Blocks.MANGROVE_WALL_HANGING_SIGN, Blocks.OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN, Blocks.SPRUCE_HANGING_SIGN, Blocks.SPRUCE_WALL_HANGING_SIGN, Blocks.WARPED_HANGING_SIGN, Blocks.WARPED_WALL_HANGING_SIGN,
                    Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_STAIRS, Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.PETRIFIED_OAK_SLAB,
                    Blocks.STRIPPED_BAMBOO_BLOCK, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_CHERRY_WOOD, Blocks.STRIPPED_ACACIA_WOOD, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_WOOD, Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_STEM, Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_WOOD, Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_WARPED_STEM,
                    Blocks.SHULKER_BOX, Blocks.BLACK_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.WHITE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX,
                    Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON, Blocks.ACTIVATOR_RAIL, Blocks.BEACON, Blocks.BEEHIVE, Blocks.REPEATING_COMMAND_BLOCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.IRON_BLOCK, Blocks.NETHERITE_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.CONDUIT, Blocks.DAYLIGHT_DETECTOR, Blocks.DETECTOR_RAIL, Blocks.DRIED_KELP_BLOCK, Blocks.DROPPER, Blocks.ENCHANTING_TABLE,
                    Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD, Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD, Blocks.DRAGON_WALL_HEAD, Blocks.DRAGON_HEAD, Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD, Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD, Blocks.SKELETON_WALL_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL, Blocks.HEAVY_CORE,
                    Blocks.HONEY_BLOCK, Blocks.HONEYCOMB_BLOCK, Blocks.HOPPER, Blocks.JUKEBOX, Blocks.LIGHTNING_ROD, Blocks.LODESTONE, Blocks.OBSERVER, Blocks.POWERED_RAIL, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE, Blocks.JUNGLE_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE, Blocks.MANGROVE_PRESSURE_PLATE, Blocks.CRIMSON_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE, Blocks.RESPAWN_ANCHOR, Blocks.CALIBRATED_SCULK_SENSOR, Blocks.SNIFFER_EGG
            )
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks2 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #2 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(Blocks.SPRUCE_WALL_SIGN, Blocks.POLISHED_DIORITE, Blocks.NOTE_BLOCK, Blocks.MANGROVE_WOOD, Blocks.WEATHERED_COPPER)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks3 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #3 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(Blocks.CRAFTING_TABLE, Blocks.BREWING_STAND, Blocks.ENDER_CHEST, Blocks.SMOOTH_QUARTZ, Blocks.REDSTONE_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.BROWN_STAINED_GLASS, Blocks.MAGENTA_BED, Blocks.BROWN_BED)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks4 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #4 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(Blocks.OAK_WALL_SIGN, Blocks.TRAPPED_CHEST, Blocks.IRON_TRAPDOOR, Blocks.LAPIS_BLOCK)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks5 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #5 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(Blocks.QUARTZ_BLOCK, Blocks.RED_BED, Blocks.WHITE_BED, Blocks.YELLOW_BED, Blocks.ORANGE_BED, Blocks.BLUE_BED, Blocks.CYAN_BED, Blocks.GREEN_BED, Blocks.LIME_BED, Blocks.PURPLE_BED, Blocks.WHITE_CONCRETE)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks6 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #6 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.FURNACE)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<List<Block>> Blawcks7 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #7 (Extra Custom)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue()
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
            .defaultValue(6)
            .build());
    private final Setting<Integer> blowkfind3 = sglists.add(new IntSetting.Builder()
            .name("(List #3) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(4)
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
            .defaultValue(12)
            .build());
    private final Setting<Integer> blowkfind6 = sglists.add(new IntSetting.Builder()
            .name("(List #6) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(12)
            .build());
    private final Setting<Integer> blowkfind7 = sglists.add(new IntSetting.Builder()
            .name("(List #7) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(1)
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
    private final Setting<Boolean> removerenderdist = sgcacheCdata.add(new BoolSetting.Builder()
            .name("RemoveOutsideRenderDistance")
            .description("Removes the cached chunks when they leave the defined render distance.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> save = sgCdata.add(new BoolSetting.Builder()
            .name("SaveBaseData")
            .description("Saves the cached bases to a file.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> load = sgCdata.add(new BoolSetting.Builder()
            .name("LoadBaseData")
            .description("Loads the saved bases from the file.")
            .defaultValue(false)
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
                findnearestbaseticks=1;
            }
        };
        table.row();
        WButton adddata = table.add(theme.button("AddBase")).expandX().minWidth(100).widget();
        adddata.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else {
                if (!baseChunks.contains(new ChunkPos(mc.player.getChunkPos().x, mc.player.getChunkPos().z))){
                    baseChunks.add(new ChunkPos(mc.player.getChunkPos().x, mc.player.getChunkPos().z));
                    try {
                        Path dirPath = Paths.get("TrouserStreak", "BaseChunks", serverip, world);
                        Files.createDirectories(dirPath);

                        Path filePath = dirPath.resolve("BaseChunkData.txt");
                        ChunkPos chunkPos = new ChunkPos(mc.player.getChunkPos().x, mc.player.getChunkPos().z);
                        String data = chunkPos + System.lineSeparator();

                        Files.write(filePath, data.getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }

                }
                ChatUtils.sendMsg(Text.of("Base near X"+mc.player.getChunkPos().getCenterX()+", Z"+mc.player.getChunkPos().getCenterZ()+" added to the BaseFinder."));
            }
        };
        table.row();
        WButton deldata = table.add(theme.button("RemoveBase")).expandX().minWidth(100).widget();
        deldata.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else {
                if (baseChunks.contains(new ChunkPos(mc.player.getChunkPos().x, mc.player.getChunkPos().z))){
                    baseChunks.remove(new ChunkPos(mc.player.getChunkPos().x, mc.player.getChunkPos().z));
                    try {
                        Path dirPath = Paths.get("TrouserStreak", "BaseChunks", serverip, world);
                        Files.createDirectories(dirPath);
                        Path filePath = dirPath.resolve("BaseChunkData.txt");
                        Files.deleteIfExists(filePath);
                        List<String> chunkDataLines = baseChunks.stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                        Files.write(filePath, chunkDataLines, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }

                }
                ChatUtils.sendMsg(Text.of("Base near X"+mc.player.getChunkPos().getCenterX()+", Z"+mc.player.getChunkPos().getCenterZ()+" removed from the BaseFinder."));
            }
        };
        table.row();
        WButton dellastdata = table.add(theme.button("RemoveLastBase")).expandX().minWidth(100).widget();
        dellastdata.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else if(isBaseFinderModuleOn!=0 && (LastBaseFound.x==2000000000 || LastBaseFound.z==2000000000)){
                error("Please find a base and run the command again.");
            } else {
                if (baseChunks.contains(new ChunkPos(LastBaseFound.x, LastBaseFound.z))){
                    baseChunks.remove(new ChunkPos(LastBaseFound.x, LastBaseFound.z));
                    try {
                        Path dirPath = Paths.get("TrouserStreak", "BaseChunks", serverip, world);
                        Files.createDirectories(dirPath);
                        Path filePath = dirPath.resolve("BaseChunkData.txt");
                        Files.deleteIfExists(filePath);
                        List<String> chunkDataLines = baseChunks.stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                        Files.write(filePath, chunkDataLines, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
                ChatUtils.sendMsg(Text.of("Base near X"+LastBaseFound.getCenterX()+", Z"+LastBaseFound.getCenterZ()+" removed from the BaseFinder."));
                LastBaseFound= new ChunkPos(2000000000, 2000000000);
            }
        };
        table.row();
        WButton deletedata = table.add(theme.button("**DELETE ALL BASE DATA**")).expandX().minWidth(100).widget();
        deletedata.action = () -> {
            if (!(mc.world==null) && mc.world.isChunkLoaded(mc.player.getChunkPos().x,mc.player.getChunkPos().z)){
                if (deletewarning==0) error("PRESS AGAIN WITHIN 5s TO DELETE ALL BASE DATA FOR THIS DIMENSION.");
                deletewarningTicks=0;
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
            .defaultValue(128)
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
    private final Setting<Boolean> trcr = sgRender.add(new BoolSetting.Builder()
            .name("Tracers")
            .description("Show tracers to the base chunks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> nearesttrcr = sgRender.add(new BoolSetting.Builder()
            .name("Tracer to NearestBase Only")
            .description("Show only one tracer to the nearest base chunk.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> trcrdist = sgRender.add(new IntSetting.Builder()
            .name("Tracer Distance (in chunks)")
            .description("How far from the base chunk to still render a tracer.")
            .defaultValue(32)
            .sliderRange(1,1024)
            .visible(() -> trcr.get())
            .build()
    );
    private final Setting<SettingColor> baseChunksSideColor = sgRender.add(new ColorSetting.Builder()
            .name("Base-chunks-waypoint-color")
            .description("Color of the waypoints indicating chunks that may contain bases or builds.")
            .defaultValue(new SettingColor(255, 127, 0, 40, true))
            .build()
    );
    private final Setting<SettingColor> baseChunksLineColor = sgRender.add(new ColorSetting.Builder()
            .name("Base-chunks-tracer-color")
            .description("Color of tracers to the chunks that may contain bases or builds.")
            .defaultValue(new SettingColor(255, 127, 0, 255, true))
            .visible(() -> trcr.get())
            .build()
    );
    private static final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private int basefoundspamTicks=0;
    private boolean basefound=false;
    private int deletewarningTicks=666;
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
    private boolean checkingchunk7=false;
    private int found7 = 0;
    private ChunkPos LastBaseFound = new ChunkPos(2000000000, 2000000000);
    private int closestbaseX=2000000000;
    private int closestbaseZ=2000000000;
    private double basedistance=2000000000;
    private String serverip;
    private String world;
    private ChunkPos basepos;
    private BlockPos blockposi;
    private final Set<ChunkPos> baseChunks = Collections.synchronizedSet(new HashSet<>());
    private static int isBaseFinderModuleOn=0;
    private int autoreloadticks=0;
    private int loadingticks=0;
    private boolean worldchange=false;
    private int justenabledsavedata=0;
    private boolean saveDataWasOn = false;
    private int findnearestbaseticks=0;
    private boolean spawnernaturalblocks=false;
    private boolean spawnerfound=false;
    private int spawnerY;
    private String lastblockfound1;
    private String lastblockfound2;
    private String lastblockfound3;
    private String lastblockfound4;
    private String lastblockfound5;
    private String lastblockfound6;
    private String lastblockfound7;
    private int entityScanTicks;
    public BaseFinder() {
        super(Trouser.Main,"BaseFinder", "Estimates if a build or base may be in the chunk based on the blocks it contains.");
    }
    private void clearChunkData() {
        baseChunks.clear();
        closestbaseX=2000000000;
        closestbaseZ=2000000000;
        basedistance=2000000000;
        LastBaseFound= new ChunkPos(2000000000, 2000000000);
    }
    @Override
    public void onActivate() {
        isBaseFinderModuleOn=1;
        if (save.get())saveDataWasOn = true;
        else if (!save.get())saveDataWasOn = false;
        if (autoreload.get()) {
            clearChunkData();
        }
        if (save.get() || load.get()) {
            if (mc.isInSingleplayer()==true){
                String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
                serverip=array[array.length-2];
                world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
            } else {
                serverip = mc.getCurrentServerEntry().address.replace(':', '_');}
            world= mc.world.getRegistryKey().getValue().toString().replace(':', '_');
            if (save.get()){
                try {
                    Files.createDirectories(Paths.get("TrouserStreak", "BaseChunks", serverip, world));
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
            if (load.get()){
                loadData();
            }
        }
        autoreloadticks=0;
        loadingticks=0;
        worldchange=false;
        justenabledsavedata = 0;
    }

    @Override
    public void onDeactivate() {
        isBaseFinderModuleOn=0;
        autoreloadticks=0;
        loadingticks=0;
        worldchange=false;
        justenabledsavedata = 0;
        if (remove.get()|autoreload.get()) {
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
        if (event.screen instanceof DownloadingTerrainScreen) {
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
        world = mc.world.getRegistryKey().getValue().toString().replace(':', '_');

        if (mc.player.getHealth() == 0) {
            worldchange = true;
        }
        if (basefound == true && basefoundspamTicks < bsefndtickdelay.get()) basefoundspamTicks++;
        else if (basefoundspamTicks >= bsefndtickdelay.get()) {
            basefound = false;
            basefoundspamTicks = 0;
        }
        if (deletewarningTicks <= 100) deletewarningTicks++;
        if (deletewarning>=2){
            if (mc.isInSingleplayer()){
                String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
                serverip=array[array.length-2];
            } else {
                serverip = mc.getCurrentServerEntry().address.replace(':', '_');
            }
            clearChunkData();
            try {
                Files.deleteIfExists(Paths.get("TrouserStreak", "BaseChunks", serverip, world, "BaseChunkData.txt"));
            } catch (IOException e) {
                //e.printStackTrace();
            }
            error("Chunk Data deleted for this Dimension.");
            deletewarning=0;
        }
        if (load.get()) {
            if (loadingticks < 1) {
                loadData();
                loadingticks++;
            }
        } else if (!load.get()) {
            loadingticks = 0;
        }

        try {
            if (baseChunks.stream().toList().size() > 0) {
                for (int b = 0; b < baseChunks.stream().toList().size(); b++) {
                    if (basedistance > Math.sqrt(Math.pow(baseChunks.stream().toList().get(b).x - mc.player.getChunkPos().x, 2) + Math.pow(baseChunks.stream().toList().get(b).z - mc.player.getChunkPos().z, 2))) {
                        closestbaseX = baseChunks.stream().toList().get(b).x;
                        closestbaseZ = baseChunks.stream().toList().get(b).z;
                        basedistance = Math.sqrt(Math.pow(baseChunks.stream().toList().get(b).x - mc.player.getChunkPos().x, 2) + Math.pow(baseChunks.stream().toList().get(b).z - mc.player.getChunkPos().z, 2));
                    }
                }
                basedistance = 2000000000;
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (findnearestbaseticks == 1) {
            if (closestbaseX < 1000000000 && closestbaseZ < 1000000000)
                ChatUtils.sendMsg(Text.of("#Nearest possible base at X" + closestbaseX * 16 + " x Z" + closestbaseZ * 16));
            if (!(closestbaseX < 1000000000 && closestbaseZ < 1000000000))
                error("No Bases Logged Yet.");
            findnearestbaseticks = 0;
        }

        if (save.get() || load.get()) {
            if (mc.isInSingleplayer() == true) {
                String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
                serverip = array[array.length - 2];
                world = mc.world.getRegistryKey().getValue().toString().replace(':', '_');
            } else {
                serverip = mc.getCurrentServerEntry().address.replace(':', '_');
            }
            world = mc.world.getRegistryKey().getValue().toString().replace(':', '_');
        }

        if (autoreload.get()) {
            autoreloadticks++;
            if (autoreloadticks == removedelay.get() * 20) {
                clearChunkData();
                if (load.get()) {
                    loadData();
                }
            } else if (autoreloadticks >= removedelay.get() * 20) {
                autoreloadticks = 0;
            }
        }
        //autoreload when entering different dimensions
        if (load.get() && worldchange == true) {
            if (worldleaveremove.get()) {
                clearChunkData();
            }
            loadData();
            worldchange = false;
        }
        if (!save.get()) saveDataWasOn = false;
        if (save.get() && justenabledsavedata <= 2 && saveDataWasOn == false) {
            justenabledsavedata++;
            if (justenabledsavedata == 1) {
                synchronized (baseChunks) {
                    for (ChunkPos chunk : baseChunks) {
                        saveBaseChunkData(chunk);
                    }
                }
            }
        }
        if (removerenderdist.get()) removeChunksOutsideRenderDistance();

        if (entityScanTicks < entityScanDelay.get()) entityScanTicks++;
        if (entityScanTicks >= entityScanDelay.get() && (pearlFinder.get() || frameFinder.get() || villagerFinder.get() || nameFinder.get() || boatFinder.get() || entityClusterFinder.get())) {
            if (mc.world == null) return;

            int renderDistance = mc.options.getViewDistance().getValue();
            ChunkPos playerChunkPos = new ChunkPos(mc.player.getBlockPos());
            for (int chunkX = playerChunkPos.x - renderDistance; chunkX <= playerChunkPos.x + renderDistance; chunkX++) {
                for (int chunkZ = playerChunkPos.z - renderDistance; chunkZ <= playerChunkPos.z + renderDistance; chunkZ++) {
                    WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
                    if (chunk != null && chunk.getStatus().isAtLeast(ChunkStatus.FULL)) {
                        Box chunkBox = new Box(
                                chunk.getPos().getStartX(), mc.world.getBottomY(), chunk.getPos().getStartZ(),
                                chunk.getPos().getEndX() + 1, mc.world.getTopY(), chunk.getPos().getEndZ() + 1
                        );
                        if (!baseChunks.contains(chunk.getPos())) {
                            AtomicInteger animalsFound = new AtomicInteger();
                            mc.world.getEntitiesByClass(Entity.class, chunkBox, entity -> true).forEach(entity -> {
                                if ((entity instanceof ItemFrameEntity || entity instanceof GlowItemFrameEntity) && frameFinder.get()) {
                                    ItemFrameEntity itemFrame = (ItemFrameEntity) entity;
                                    Item heldItem = itemFrame.getHeldItemStack().getItem();
                                    if (heldItem != Items.ELYTRA) {
                                        baseChunks.add(chunk.getPos());
                                        if (save.get()) {
                                            saveBaseChunkData(chunk.getPos());
                                        }
                                        if (basefoundspamTicks == 0) {
                                            ChatUtils.sendMsg(Text.of("Item Frame located near X" + entity.getPos().getX() + ", Y" + entity.getPos().getY() + ", Z" + entity.getPos().getZ()));
                                            LastBaseFound = new ChunkPos(chunk.getPos().x, chunk.getPos().z);
                                            basefound = true;
                                        }
                                    }
                                } else if (entity instanceof EnderPearlEntity && pearlFinder.get()) {
                                    baseChunks.add(chunk.getPos());
                                    if (save.get()) {
                                        saveBaseChunkData(chunk.getPos());
                                    }
                                    if (basefoundspamTicks == 0) {
                                        ChatUtils.sendMsg(Text.of("Ender Pearl located near X" + entity.getPos().getX() + ", Y" + entity.getPos().getY() + ", Z" + entity.getPos().getZ()));
                                        LastBaseFound = new ChunkPos(chunk.getPos().x, chunk.getPos().z);
                                        basefound = true;
                                    }
                                } else if (entity instanceof VillagerEntity && villagerFinder.get()) {
                                    if (((VillagerEntity) entity).getVillagerData().getLevel() > 1) {
                                        baseChunks.add(chunk.getPos());
                                        if (save.get()) {
                                            saveBaseChunkData(chunk.getPos());
                                        }
                                        if (basefoundspamTicks == 0) {
                                            ChatUtils.sendMsg(Text.of("Illegal Villager located near X" + entity.getPos().getX() + ", Y" + entity.getPos().getY() + ", Z" + entity.getPos().getZ()));
                                            LastBaseFound = new ChunkPos(chunk.getPos().x, chunk.getPos().z);
                                            basefound = true;
                                        }
                                    }
                                } else if (entity.hasCustomName() && nameFinder.get()) {
                                    baseChunks.add(chunk.getPos());
                                    if (save.get()) {
                                        saveBaseChunkData(chunk.getPos());
                                    }
                                    if (basefoundspamTicks == 0) {
                                        ChatUtils.sendMsg(Text.of("NameTagged Entity located near X" + entity.getPos().getX() + ", Y" + entity.getPos().getY() + ", Z" + entity.getPos().getZ()));
                                        LastBaseFound = new ChunkPos(chunk.getPos().x, chunk.getPos().z);
                                        basefound = true;
                                    }
                                } else if ((entity instanceof ChestBoatEntity || entity instanceof BoatEntity) && boatFinder.get()) {
                                    baseChunks.add(chunk.getPos());
                                    if (save.get()) {
                                        saveBaseChunkData(chunk.getPos());
                                    }
                                    if (basefoundspamTicks == 0) {
                                        ChatUtils.sendMsg(Text.of("Illegal Boat located near X" + entity.getPos().getX() + ", Y" + entity.getPos().getY() + ", Z" + entity.getPos().getZ()));
                                        LastBaseFound = new ChunkPos(chunk.getPos().x, chunk.getPos().z);
                                        basefound = true;
                                    }
                                } else if (entitieslist.get().contains(entity) && entityClusterFinder.get()) {
                                    animalsFound.getAndIncrement();
                                }
                            });
                            if (animalsFound.get() >= animalsFoundThreshold.get() && entityClusterFinder.get()){
                                baseChunks.add(chunk.getPos());
                                if (save.get()) {
                                    saveBaseChunkData(chunk.getPos());
                                }
                                if (basefoundspamTicks == 0) {
                                    ChatUtils.sendMsg(Text.of("Illegal amount of entities located near X" + chunk.getPos().getCenterX() + ", Z" + chunk.getPos().getCenterZ()));
                                    LastBaseFound = new ChunkPos(chunk.getPos().x, chunk.getPos().z);
                                    basefound = true;
                                }
                            }
                        }
                    }
                }
            }
            entityScanTicks = 0;
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (baseChunksLineColor.get().a > 5 || baseChunksSideColor.get().a > 5){
            if (!nearesttrcr.get()){
                synchronized (baseChunks) {
                    for (ChunkPos c : baseChunks) {
                        if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), renderDistance.get()*16)) {
                            render(new Box(new Vec3d(c.getStartPos().getX()+7, c.getStartPos().getY()+renderHeightYbottom.get(), c.getStartPos().getZ()+7), new Vec3d(c.getStartPos().getX()+8, c.getStartPos().getY()+renderHeightY.get(), c.getStartPos().getZ()+8)), baseChunksSideColor.get(), baseChunksLineColor.get(),ShapeMode.Sides, event);
                        }
                    }
                }
            } else if (nearesttrcr.get()){
                synchronized (baseChunks) {
                    for (ChunkPos c : baseChunks) {
                        if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), renderDistance.get()*16)) {
                            render(new Box(new Vec3d(c.getStartPos().getX()+7, c.getStartPos().getY()+renderHeightYbottom.get(), c.getStartPos().getZ()+7), new Vec3d(c.getStartPos().getX()+8, c.getStartPos().getY()+renderHeightY.get(), c.getStartPos().getZ()+8)), baseChunksSideColor.get(), baseChunksLineColor.get(),ShapeMode.Sides, event);
                        }
                    }
                }
                render2(new Box(new Vec3d(new ChunkPos(closestbaseX,closestbaseZ).getStartPos().getX()+7, new ChunkPos(closestbaseX,closestbaseZ).getStartPos().getY()+renderHeightYbottom.get(), new ChunkPos(closestbaseX,closestbaseZ).getStartPos().getZ()+7), new Vec3d (new ChunkPos(closestbaseX,closestbaseZ).getStartPos().getX()+8, new ChunkPos(closestbaseX,closestbaseZ).getStartPos().getY()+renderHeightY.get(), new ChunkPos(closestbaseX,closestbaseZ).getStartPos().getZ()+8)), baseChunksSideColor.get(), baseChunksLineColor.get(),ShapeMode.Sides, event);
            }
        }
    }

    private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        if (trcr.get() && Math.abs(box.minX-RenderUtils.center.x)<=trcrdist.get()*16 && Math.abs(box.minZ-RenderUtils.center.z)<=trcrdist.get()*16)
            if (!nearesttrcr.get())
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX+0.5, box.minY+((box.maxY-box.minY)/2), box.minZ+0.5, lines);
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, new Color(0,0,0,0), shapeMode, 0);
    }
    private void render2(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        if (trcr.get() && Math.abs(box.minX-RenderUtils.center.x)<=trcrdist.get()*16 && Math.abs(box.minZ-RenderUtils.center.z)<=trcrdist.get()*16)
            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX+0.5, box.minY+((box.maxY-box.minY)/2), box.minZ+0.5, lines);
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, new Color(0,0,0,0), shapeMode, 0);
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerMoveC2SPacket) return; //this keeps getting cast to the chunkdata for no reason
        if (!(event.packet instanceof PlayerMoveC2SPacket) && event.packet instanceof ChunkDataS2CPacket && mc.world != null) {
            ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;

            basepos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());

            if (mc.world.getChunkManager().getChunk(packet.getChunkX(), packet.getChunkZ()) == null) {
                WorldChunk chunk = new WorldChunk(mc.world, basepos);
                try {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(),
                                packet.getChunkData().getBlockEntities(packet.getChunkX(), packet.getChunkZ()));
                    }, taskExecutor);
                    future.join();
                } catch (CompletionException e) {}

                if (bubblesFinder.get() || spawner.get() || signFinder.get() || portalFinder.get() || roofDetector.get() || bedrockfind.get() || skybuildfind.get() || Blawcks1.get().size()>0 || Blawcks2.get().size()>0 || Blawcks3.get().size()>0 || Blawcks4.get().size()>0 || Blawcks5.get().size()>0 || Blawcks6.get().size()>0 || Blawcks7.get().size()>0){
                    int Ymin = mc.world.getBottomY()+minY.get();
                    int Ymax = mc.world.getTopY()-maxY.get();
                    try {
                        Set<BlockPos> blockpositions1 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions2 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions3 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions4 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions5 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions6 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions7 = Collections.synchronizedSet(new HashSet<>());
                        ChunkSection[] sections = chunk.getSectionArray();
                        int Y = mc.world.getBottomY();
                        for (ChunkSection section: sections){
                            if (section == null || section.isEmpty()) {
                                Y+=16;
                                continue;
                            }
                            for (int x = 0; x < 16; x++) {
                                for (int y = 0; y < 16; y++) {
                                    for (int z = 0; z < 16; z++) {
                                        int currentY = Y + y;
                                        if (currentY <= Ymin || currentY >= Ymax) continue;
                                        blockposi=new BlockPos(x, currentY, z);
                                        BlockState blerks = section.getBlockState(x,y,z);
                                        if (blerks.getBlock()!=Blocks.AIR && blerks.getBlock()!=Blocks.STONE){
                                            if (!(blerks.getBlock()==Blocks.DEEPSLATE) && !(blerks.getBlock()==Blocks.DIRT) && !(blerks.getBlock()==Blocks.GRASS_BLOCK) && !(blerks.getBlock()==Blocks.WATER) && !(blerks.getBlock()==Blocks.SAND) && !(blerks.getBlock()==Blocks.GRAVEL)  && !(blerks.getBlock()==Blocks.BEDROCK)&& !(blerks.getBlock()==Blocks.NETHERRACK) && !(blerks.getBlock()==Blocks.LAVA)){
                                                if (signFinder.get() && blerks.getBlock() instanceof SignBlock || blerks.getBlock() instanceof HangingSignBlock) {
                                                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                                                        Boolean signtextfound = false;
                                                        if (blockEntity instanceof SignBlockEntity){
                                                            SignText signText = ((SignBlockEntity) blockEntity).getFrontText();
                                                            SignText signText2 = ((SignBlockEntity) blockEntity).getBackText();
                                                            Text[] lines = signText.getMessages(false);
                                                            Text[] lines2 = signText2.getMessages(false);
                                                            int i = 0;
                                                            for (Text line : lines) {
                                                                if (line.getLiteralString().length() != 0 && (line.getString() != "<----" && i == 1) && (line.getString() != "---->" && i == 2)){ //handling for arrows is for igloos
                                                                    signtextfound = true;
                                                                    if (signtextfound == true) break;
                                                                }
                                                                i++;
                                                            }
                                                            for (Text line2 : lines2) {
                                                                if (signtextfound == true) break;
                                                                if (line2.getLiteralString().length() != 0){
                                                                    signtextfound = true;
                                                                    if (signtextfound == true) break;
                                                                }
                                                            }
                                                        } else if (blockEntity instanceof HangingSignBlockEntity) {
                                                            SignText signText = ((HangingSignBlockEntity) blockEntity).getFrontText();
                                                            SignText signText2 = ((HangingSignBlockEntity) blockEntity).getBackText();
                                                            Text[] lines = signText.getMessages(false);
                                                            Text[] lines2 = signText2.getMessages(false);
                                                            for (Text line : lines) {
                                                                if (line.getLiteralString().length() != 0){ //handling for arrows is for igloos
                                                                    signtextfound = true;
                                                                    if (signtextfound == true) break;
                                                                }
                                                            }
                                                            for (Text line2 : lines2) {
                                                                if (signtextfound == true) break;
                                                                if (line2.getLiteralString().length() != 0){
                                                                    signtextfound = true;
                                                                    if (signtextfound == true) break;
                                                                }
                                                            }
                                                        }
                                                        if (signtextfound == true && !baseChunks.contains(basepos)){
                                                            baseChunks.add(basepos);
                                                            if (save.get()) {
                                                                saveBaseChunkData(basepos);
                                                            }
                                                            if (basefoundspamTicks==0){
                                                                ChatUtils.sendMsg(Text.of("Written Sign located near X"+blockEntity.getPos().getX()+", Y"+blockEntity.getPos().getY()+", Z"+blockEntity.getPos().getZ()));
                                                                LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                                                basefound=true;
                                                            }
                                                        }
                                                    }
                                                }
                                                if (skybuildfind.get() && currentY>skybuildint.get()) {
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            ChatUtils.sendMsg(Text.of("(Skybuild)Possible build located near X"+basepos.getCenterX()+", Y"+currentY+", Z"+basepos.getCenterZ()));
                                                            LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (bubblesFinder.get() && blerks.getBlock() instanceof BubbleColumnBlock && blerks.get(BubbleColumnBlock.DRAG) == false) {
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            ChatUtils.sendMsg(Text.of("(Bubble Column)Possible build located near X"+basepos.getCenterX()+", Y"+currentY+", Z"+basepos.getCenterZ()));
                                                            LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (portalFinder.get() && (blerks.getBlock()==Blocks.NETHER_PORTAL || blerks.getBlock()==Blocks.END_PORTAL)) {
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            ChatUtils.sendMsg(Text.of("(Open Portal)Possible build located near X"+basepos.getCenterX()+", Y"+currentY+", Z"+basepos.getCenterZ()));
                                                            LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (bedrockfind.get() && blerks.getBlock()==Blocks.BEDROCK && ((currentY>mc.world.getBottomY()+bedrockint.get() && mc.world.getRegistryKey() == World.OVERWORLD) || (currentY>mc.world.getBottomY()+bedrockint.get() && (currentY < 123 || currentY > 127) && mc.world.getRegistryKey() == World.NETHER))) {
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            ChatUtils.sendMsg(Text.of("(Unnatural Bedrock)Possible build located near X"+basepos.getCenterX()+", Y"+currentY+", Z"+basepos.getCenterZ()));
                                                            LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (roofDetector.get() && blerks.getBlock()!=Blocks.RED_MUSHROOM && blerks.getBlock()!=Blocks.BROWN_MUSHROOM && currentY>=128 && mc.world.getRegistryKey() == World.NETHER){
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            ChatUtils.sendMsg(Text.of("(Nether Roof)Possible build located near X"+basepos.getCenterX()+", Y"+currentY+", Z"+basepos.getCenterZ()));
                                                            LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (spawner.get()){
                                                    if (blerks.getBlock()==Blocks.SPAWNER){
                                                        spawnerY=currentY;
                                                        spawnerfound=true;
                                                    }
                                                    //dungeon MOSSY_COBBLESTONE, mineshaft COBWEB, fortress NETHER_BRICK_FENCE, stronghold STONE_BRICK_STAIRS, bastion CHAIN
                                                    if (mc.world.getRegistryKey() == World.OVERWORLD && (blerks.getBlock()==Blocks.MOSSY_COBBLESTONE || blerks.getBlock()==Blocks.COBWEB || blerks.getBlock()==Blocks.STONE_BRICK_STAIRS || blerks.getBlock()==Blocks.BUDDING_AMETHYST))spawnernaturalblocks=true;
                                                    else if (mc.world.getRegistryKey() == World.NETHER && (blerks.getBlock()==Blocks.NETHER_BRICK_FENCE || blerks.getBlock()==Blocks.CHAIN))spawnernaturalblocks=true;
                                                }
                                                if (Blawcks1.get().size()>0){
                                                    if (Blawcks1.get().contains(blerks.getBlock())) {
                                                        blockpositions1.add(blockposi);
                                                        found1= blockpositions1.size();
                                                        lastblockfound1=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (Blawcks2.get().size()>0){
                                                    if (Blawcks2.get().contains(blerks.getBlock())) {
                                                        blockpositions2.add(blockposi);
                                                        found2= blockpositions2.size();
                                                        lastblockfound2=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (Blawcks3.get().size()>0){
                                                    if (Blawcks3.get().contains(blerks.getBlock())) {
                                                        blockpositions3.add(blockposi);
                                                        found3= blockpositions3.size();
                                                        lastblockfound3=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (Blawcks4.get().size()>0){
                                                    if (Blawcks4.get().contains(blerks.getBlock())) {
                                                        blockpositions4.add(blockposi);
                                                        found4= blockpositions4.size();
                                                        lastblockfound4=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (Blawcks5.get().size()>0){
                                                    if (Blawcks5.get().contains(blerks.getBlock())) {
                                                        blockpositions5.add(blockposi);
                                                        found5= blockpositions5.size();
                                                        lastblockfound5=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (Blawcks6.get().size()>0){
                                                    if (Blawcks6.get().contains(blerks.getBlock())) {
                                                        blockpositions6.add(blockposi);
                                                        found6= blockpositions6.size();
                                                        lastblockfound6=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (Blawcks7.get().size()>0){
                                                    if (Blawcks7.get().contains(blerks.getBlock())) {
                                                        blockpositions7.add(blockposi);
                                                        found7= blockpositions7.size();
                                                        lastblockfound7=blerks.getBlock().toString();
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
                                        if (Blawcks7.get().size()>0)checkingchunk7=true;
                                    }
                                }
                            }
                            Y+=16;
                        }
                        //CheckList 1
                        if (Blawcks1.get().size()>0){
                            if (checkingchunk1==true && found1>=blowkfind1.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        ChatUtils.sendMsg(Text.of("(List1)Possible build located near X" + basepos.getCenterX() + ", Y" + blockpositions1.stream().toList().get(0).getY() + ", Z" + basepos.getCenterZ() + " (" + lastblockfound1 + ")"));
                                        LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                        basefound=true;
                                    }
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
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        ChatUtils.sendMsg(Text.of("(List2)Possible build located near X" + basepos.getCenterX() + ", Y" + blockpositions2.stream().toList().get(0).getY() + ", Z" + basepos.getCenterZ() + " (" + lastblockfound2 + ")"));
                                        LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                        basefound=true;
                                    }
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
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        ChatUtils.sendMsg(Text.of("(List3)Possible build located near X" + basepos.getCenterX() + ", Y" + blockpositions3.stream().toList().get(0).getY() + ", Z" + basepos.getCenterZ() + " (" + lastblockfound3 + ")"));
                                        LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                        basefound=true;
                                    }
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
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        ChatUtils.sendMsg(Text.of("(List4)Possible build located near X" + basepos.getCenterX() + ", Y" + blockpositions4.stream().toList().get(0).getY() + ", Z" + basepos.getCenterZ() + " (" + lastblockfound4 + ")"));
                                        LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                        basefound=true;
                                    }
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
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        ChatUtils.sendMsg(Text.of("(List5)Possible build located near X"+basepos.getCenterX()+", Y"+blockpositions5.stream().toList().get(0).getY()+", Z"+basepos.getCenterZ()+" ("+lastblockfound5+")"));
                                        LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                        basefound=true;
                                    }
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
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        ChatUtils.sendMsg(Text.of("(List6)Possible build located near X"+basepos.getCenterX()+", Y"+blockpositions6.stream().toList().get(0).getY()+", Z"+basepos.getCenterZ()+" ("+lastblockfound6+")"));
                                        LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                        basefound=true;
                                    }
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

                        //CheckList 7
                        if (Blawcks7.get().size()>0){
                            if (checkingchunk7==true && found7>=blowkfind7.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        ChatUtils.sendMsg(Text.of("(List7)Possible build located near X"+basepos.getCenterX()+", Y"+blockpositions7.stream().toList().get(0).getY()+", Z"+basepos.getCenterZ()+" ("+lastblockfound7+")"));
                                        LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                                        basefound=true;
                                    }
                                }
                                blockpositions7.clear();
                                found7 = 0;
                                checkingchunk7=false;
                            } else if (checkingchunk7==true && found7<blowkfind7.get()){
                                blockpositions7.clear();
                                found7 = 0;
                                checkingchunk7=false;
                            }
                        }
                    }
                    catch (Exception e){
                        //e.printStackTrace();
                    }
                }
                if (spawnerfound==true && spawnernaturalblocks==false){
                    if (!baseChunks.contains(basepos)){
                        baseChunks.add(basepos);
                        if (save.get()) {
                            saveBaseChunkData(basepos);
                        }
                        if (basefoundspamTicks== 0) {
                            ChatUtils.sendMsg(Text.of("Possible modified spawner located near X"+basepos.getCenterX()+", Y"+spawnerY+", Z"+basepos.getCenterZ()));
                            LastBaseFound= new ChunkPos(basepos.x, basepos.z);
                            basefound=true;
                        }
                    }
                    spawnerfound=false;
                    spawnernaturalblocks=false;
                } else if ((spawnerfound==true && spawnernaturalblocks==true) || (spawnerfound==false && spawnernaturalblocks==true) || (spawnerfound==false && spawnernaturalblocks==false)){
                    spawnerfound=false;
                    spawnernaturalblocks=false;
                }
            }
        }
    }
    private void loadData() {
        try {
            List<String> allLines = Files.readAllLines(Paths.get("TrouserStreak/BaseChunks/"+serverip+"/"+world+"/BaseChunkData.txt"));

            for (String line : allLines) {
                String s = line;
                String[] array = s.split(", ");
                int X = Integer.parseInt(array[0].replaceAll("\\[", "").replaceAll("\\]",""));
                int Z = Integer.parseInt(array[1].replaceAll("\\[", "").replaceAll("\\]",""));
                basepos = new ChunkPos(X,Z);
                baseChunks.add(basepos);
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }
    private void saveBaseChunkData(ChunkPos basepos) {
        Path dirPath = Paths.get("TrouserStreak", "BaseChunks", serverip, world);
        Path filePath = dirPath.resolve("BaseChunkData.txt");
        try {
            Files.createDirectories(dirPath);
            String data = basepos.toString() + System.lineSeparator();
            Files.write(filePath, data.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            //e.printStackTrace();
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
                !(block ==Blocks.NETHERRACK) &&
                !(block ==Blocks.LAVA);
    }
    private void removeChunksOutsideRenderDistance() {
        BlockPos cameraPos = mc.getCameraEntity().getBlockPos();
        double renderDistanceBlocks = renderDistance.get() * 16;

        removeChunksOutsideRenderDistance(baseChunks, cameraPos, renderDistanceBlocks);
    }
    private void removeChunksOutsideRenderDistance(Set<ChunkPos> chunkSet, BlockPos cameraPos, double renderDistanceBlocks) {
        chunkSet.removeIf(chunkPos -> !cameraPos.isWithinDistance(chunkPos.getStartPos(), renderDistanceBlocks));
    }
}