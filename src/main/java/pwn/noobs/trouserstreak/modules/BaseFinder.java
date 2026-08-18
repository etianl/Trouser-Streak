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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
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
    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDetectors = settings.createGroup("Block Detectors");
    private final SettingGroup sgEDetectors = settings.createGroup("Entity Detectors");
    private final SettingGroup sglists = settings.createGroup("Blocks To Check For");
    private final SettingGroup sgCdata = settings.createGroup("Saved Base Data");
    private final SettingGroup sgcacheCdata = settings.createGroup("Cached Base Data");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup locationLogs = settings.createGroup("Location Logs");

    // general
    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("Chat feedback")
            .description("Displays info for you.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> displaycoords = sgGeneral.add(new BoolSetting.Builder()
            .name("DisplayCoords")
            .description("Displays coords of bases in chat.")
            .defaultValue(true)
            .build()
    );
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
            .visible(skybuildfind::get)
            .build());
    private final Setting<Boolean> bedrockfind = sgDetectors.add(new BoolSetting.Builder()
            .name("Bedrock Finder")
            .description("If Bedrock Blocks higher than they can naturally generate in the Overworld or Nether, flag chunk as possible build.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> bedrockint = sgDetectors.add(new IntSetting.Builder()
            .name("Bedrock Y Threshold")
            .description("If bedrock higher than this many blocks above minimum build limit, flag chunk as possible build.")
            .min(-64)
            .sliderRange(-64, 384)
            .defaultValue(4)
            .visible(bedrockfind::get)
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
        BuiltInRegistries.ENTITY_TYPE.forEach(entityType -> {
            if (entityType.getCategory() == MobCategory.CREATURE) {
                creatures.add(entityType);
            }
        });
        return creatures;
    }
    private final Setting<Integer> animalsFoundThreshold = sgEDetectors.add(new IntSetting.Builder()
            .name("Entity Cluster Threshold")
            .description("Once this many entities are found in a chunk trigger it as being a base.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(14)
            .build());
    private final Setting<Integer> bsefndtickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("Base Found Message Tick Delay")
            .description("Delays the allowance of Base Found messages to reduce spam.")
            .min(0)
            .sliderRange(0,300)
            .defaultValue(5)
            .build());
    private final Setting<Boolean> list1Activar = sglists.add(new BoolSetting.Builder()
            .name("List #1 Activate")
            .description("Activates checks for List #1")
            .defaultValue(true)
            .build());
    private final Setting<List<Block>> Blawcks1 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #1 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(
                    block("crafter"), block("spruce_sapling"), block("oak_sapling"), block("birch_sapling"), block("jungle_sapling"), block("cherry_sapling"), block("bamboo_sapling"),
                    block("cherry_button"), block("cherry_door"), block("cherry_fence"), block("cherry_fence_gate"), block("cherry_planks"), block("cherry_pressure_plate"), block("cherry_stairs"), block("cherry_wood"), block("cherry_trapdoor"), block("cherry_slab"),
                    block("mangrove_planks"), block("mangrove_button"), block("mangrove_door"), block("mangrove_fence"), block("mangrove_fence_gate"), block("mangrove_stairs"), block("mangrove_slab"), block("mangrove_trapdoor"),
                    block("birch_door"), block("birch_fence_gate"), block("birch_button"), block("acacia_button"), block("dark_oak_button"), block("polished_blackstone_button"), block("spruce_button"),
                    block("bamboo_block"), block("bamboo_button"), block("bamboo_door"), block("bamboo_fence"), block("bamboo_fence_gate"), block("bamboo_mosaic"), block("bamboo_mosaic_slab"), block("bamboo_mosaic_stairs"), block("bamboo_planks"), block("bamboo_pressure_plate"), block("bamboo_slab"), block("bamboo_stairs"), block("bamboo_trapdoor"), block("chiseled_bookshelf"),
                    block("black_concrete"), block("blue_concrete"), block("cyan_concrete"), block("brown_concrete"), block("orange_concrete"), block("magenta_concrete"), block("light_blue_concrete"), block("yellow_concrete"), block("lime_concrete"), block("pink_concrete"), block("gray_concrete"), block("light_gray_concrete"), block("purple_concrete"), block("green_concrete"),
                    block("black_concrete_powder"), block("blue_concrete_powder"), block("cyan_concrete_powder"), block("brown_concrete_powder"), block("white_concrete_powder"), block("orange_concrete_powder"), block("magenta_concrete_powder"), block("light_blue_concrete_powder"), block("yellow_concrete_powder"), block("lime_concrete_powder"), block("pink_concrete_powder"), block("gray_concrete_powder"), block("light_gray_concrete_powder"), block("purple_concrete_powder"), block("green_concrete_powder"), block("red_concrete_powder"),
                    block("purple_terracotta"), block("magenta_terracotta"), block("pink_terracotta"), block("magenta_glazed_terracotta"), block("pink_glazed_terracotta"), block("gray_glazed_terracotta"), block("blue_glazed_terracotta"), block("brown_glazed_terracotta"), block("green_glazed_terracotta"),
                    block("oxidized_copper"), block("cut_copper"), block("exposed_cut_copper"), block("weathered_cut_copper"), block("cut_copper_slab"), block("cut_copper_stairs"), block("exposed_cut_copper_slab"), block("exposed_cut_copper_stairs"), block("weathered_cut_copper_slab"), block("weathered_cut_copper_stairs"), block("oxidized_cut_copper_slab"), block("oxidized_cut_copper_stairs"), block("copper_bulb"), block("exposed_copper_bulb"), block("weathered_copper_bulb"), block("oxidized_copper_bulb"), block("chiseled_copper"), block("exposed_chiseled_copper"), block("weathered_chiseled_copper"), block("oxidized_chiseled_copper"), block("copper_door"), block("exposed_copper_door"), block("weathered_copper_door"), block("oxidized_copper_door"), block("copper_grate"), block("exposed_copper_grate"), block("weathered_copper_grate"), block("oxidized_copper_grate"), block("copper_trapdoor"), block("exposed_copper_trapdoor"), block("weathered_copper_trapdoor"),
                    block("waxed_exposed_copper"), block("waxed_weathered_copper"), block("waxed_exposed_cut_copper"), block("waxed_weathered_cut_copper"), block("waxed_exposed_cut_copper_slab"), block("waxed_exposed_cut_copper_stairs"), block("waxed_weathered_cut_copper_slab"), block("waxed_weathered_cut_copper_stairs"), block("waxed_exposed_chiseled_copper"), block("waxed_weathered_chiseled_copper"), block("waxed_exposed_copper_door"), block("waxed_weathered_copper_door"), block("waxed_exposed_copper_grate"), block("waxed_weathered_copper_grate"), block("waxed_copper_trapdoor"), block("waxed_exposed_copper_trapdoor"), block("waxed_weathered_copper_trapdoor"),
                    block("soul_torch"), block("soul_wall_torch"), block("potted_mangrove_propagule"), block("potted_azalea"), block("potted_cherry_sapling"), block("potted_fern"), block("potted_acacia_sapling"), block("potted_warped_fungus"), block("potted_warped_roots"), block("potted_crimson_fungus"), block("potted_crimson_roots"), block("potted_oak_sapling"), block("potted_wither_rose"), block("wither_rose"),
                    block("cake"), block("candle_cake"), block("blue_candle_cake"), block("black_candle_cake"), block("brown_candle_cake"), block("cyan_candle_cake"), block("gray_candle_cake"), block("green_candle_cake"), block("light_blue_candle_cake"), block("light_gray_candle_cake"), block("lime_candle_cake"), block("magenta_candle_cake"), block("orange_candle_cake"), block("pink_candle_cake"), block("purple_candle_cake"), block("red_candle_cake"), block("white_candle_cake"), block("yellow_candle_cake"),
                    block("blue_candle"), block("black_candle"), block("brown_candle"), block("cyan_candle"), block("gray_candle"), block("green_candle"), block("light_blue_candle"), block("light_gray_candle"), block("lime_candle"), block("magenta_candle"), block("orange_candle"), block("pink_candle"), block("purple_candle"), block("yellow_candle"),
                    block("smooth_red_sandstone"), block("chiseled_red_sandstone"), block("cut_red_sandstone"), block("smooth_red_sandstone_slab"), block("smooth_red_sandstone_stairs"), block("cut_red_sandstone_slab"), block("red_sandstone_slab"), block("red_sandstone_stairs"), block("red_sandstone_wall"),
                    block("andesite_stairs"), block("andesite_slab"), block("andesite_wall"), block("polished_andesite_slab"), block("polished_andesite_stairs"), block("polished_granite_slab"), block("polished_granite_stairs"), block("polished_diorite_slab"), block("polished_diorite_stairs"),
                    block("tuff_slab"), block("tuff_stairs"), block("tuff_wall"), block("tuff_brick_slab"), block("tuff_brick_stairs"), block("tuff_brick_wall"),
                    block("cracked_nether_bricks"), block("chiseled_nether_bricks"), block("red_nether_bricks"), block("nether_brick_slab"), block("nether_brick_wall"), block("red_nether_bricks"), block("red_nether_brick_slab"), block("red_nether_brick_stairs"), block("red_nether_brick_wall"),
                    block("orange_stained_glass"), block("light_blue_stained_glass"), block("yellow_stained_glass"), block("lime_stained_glass"), block("pink_stained_glass"), block("cyan_stained_glass"), block("purple_stained_glass"), block("blue_stained_glass"), block("green_stained_glass"), block("red_stained_glass"),
                    block("crimson_pressure_plate"), block("crimson_button"), block("crimson_door"), block("crimson_fence"), block("crimson_fence_gate"), block("crimson_planks"), block("crimson_sign"), block("crimson_wall_sign"), block("crimson_slab"), block("crimson_stairs"), block("crimson_trapdoor"),
                    block("warped_pressure_plate"), block("warped_button"), block("warped_door"), block("warped_fence"), block("warped_fence_gate"), block("warped_planks"), block("warped_sign"), block("warped_wall_sign"), block("warped_slab"), block("warped_stairs"), block("warped_trapdoor"),
                    block("scaffolding"), block("cherry_sign"), block("cherry_wall_sign"), block("oak_sign"), block("spruce_sign"), block("acacia_sign"), block("acacia_wall_sign"), block("birch_sign"), block("birch_wall_sign"), block("dark_oak_sign"), block("dark_oak_wall_sign"), block("jungle_sign"), block("jungle_wall_sign"), block("mangrove_sign"), block("mangrove_wall_sign"), block("slime_block"), block("sponge"), block("tinted_glass"),
                    block("acacia_hanging_sign"), block("acacia_wall_hanging_sign"), block("bamboo_hanging_sign"), block("bamboo_wall_hanging_sign"), block("birch_hanging_sign"), block("birch_wall_hanging_sign"), block("cherry_hanging_sign"), block("cherry_wall_hanging_sign"), block("crimson_hanging_sign"), block("crimson_wall_hanging_sign"), block("dark_oak_hanging_sign"), block("dark_oak_wall_hanging_sign"), block("jungle_hanging_sign"), block("jungle_wall_hanging_sign"), block("mangrove_hanging_sign"), block("mangrove_wall_hanging_sign"), block("oak_hanging_sign"), block("oak_wall_hanging_sign"), block("spruce_hanging_sign"), block("spruce_wall_hanging_sign"), block("warped_hanging_sign"), block("warped_wall_hanging_sign"),
                    block("chiseled_quartz_block"), block("quartz_pillar"), block("quartz_bricks"), block("quartz_stairs"), block("ochre_froglight"), block("pearlescent_froglight"), block("verdant_froglight"), block("petrified_oak_slab"),
                    block("stripped_bamboo_block"), block("stripped_cherry_log"), block("stripped_cherry_wood"), block("stripped_acacia_wood"), block("birch_wood"), block("stripped_birch_log"), block("stripped_birch_wood"), block("crimson_hyphae"), block("stripped_crimson_hyphae"), block("stripped_crimson_stem"), block("dark_oak_wood"), block("stripped_dark_oak_log"), block("stripped_dark_oak_wood"), block("stripped_jungle_log"), block("stripped_jungle_wood"), block("stripped_mangrove_log"), block("stripped_mangrove_wood"), block("warped_hyphae"), block("stripped_warped_hyphae"), block("stripped_warped_stem"),
                    block("shulker_box"), block("black_shulker_box"), block("blue_shulker_box"), block("brown_shulker_box"), block("cyan_shulker_box"), block("gray_shulker_box"), block("green_shulker_box"), block("light_blue_shulker_box"), block("light_gray_shulker_box"), block("lime_shulker_box"), block("magenta_shulker_box"), block("orange_shulker_box"), block("pink_shulker_box"), block("purple_shulker_box"), block("red_shulker_box"), block("white_shulker_box"), block("yellow_shulker_box"),
                    block("lava_cauldron"), block("powder_snow_cauldron"), block("activator_rail"), block("beacon"), block("beehive"), block("repeating_command_block"), block("command_block"), block("chain_command_block"), block("emerald_block"), block("iron_block"), block("netherite_block"), block("raw_gold_block"), block("conduit"), block("daylight_detector"), block("detector_rail"), block("dried_kelp_block"), block("dropper"), block("enchanting_table"),
                    block("piglin_head"), block("piglin_wall_head"), block("creeper_head"), block("creeper_wall_head"), block("dragon_wall_head"), block("dragon_head"), block("player_head"), block("player_wall_head"), block("zombie_head"), block("zombie_wall_head"), block("skeleton_wall_skull"), block("wither_skeleton_skull"), block("wither_skeleton_wall_skull"), block("heavy_core"),
                    block("honey_block"), block("honeycomb_block"), block("jukebox"), block("lightning_rod"), block("lodestone"), block("observer"), block("powered_rail"), block("heavy_weighted_pressure_plate"), block("light_weighted_pressure_plate"), block("polished_blackstone_pressure_plate"), block("birch_pressure_plate"), block("jungle_pressure_plate"), block("dark_oak_pressure_plate"), block("mangrove_pressure_plate"), block("crimson_pressure_plate"), block("warped_pressure_plate"), block("respawn_anchor"), block("calibrated_sculk_sensor"), block("sniffer_egg"),
                    block("resin_block"), block("resin_bricks"), block("resin_brick_slab"), block("resin_brick_wall"), block("resin_brick_stairs"), block("chiseled_resin_bricks"), block("potted_closed_eyeblossom"), block("potted_open_eyeblossom"), block("potted_pale_oak_sapling"), block("pale_oak_sapling"), block("pale_oak_button"), block("pale_oak_door"), block("pale_oak_fence"), block("pale_oak_fence_gate"), block("pale_oak_planks"), block("pale_oak_pressure_plate"), block("pale_oak_hanging_sign"), block("pale_oak_sign"), block("pale_oak_wall_sign"), block("pale_oak_wall_hanging_sign"), block("pale_oak_slab"), block("pale_oak_stairs"), block("pale_oak_trapdoor"), block("pale_oak_wood"), block("stripped_pale_oak_wood"),
                    block("copper_bars"),block("waxed_copper_bars"),block("exposed_copper_bars"),block("waxed_exposed_copper_bars"),block("weathered_copper_bars"),block("waxed_weathered_copper_bars"),block("oxidized_copper_bars"),block("waxed_oxidized_copper_bars"), block("copper_chain"),block("waxed_copper_chain"),block("exposed_copper_chain"),block("waxed_exposed_copper_chain"),block("weathered_copper_chain"),block("waxed_weathered_copper_chain"),block("oxidized_copper_chain"),block("waxed_oxidized_copper_chain"), block("copper_lantern"), block("waxed_copper_lantern"), block("exposed_copper_lantern"), block("waxed_exposed_copper_lantern"), block("weathered_copper_lantern"), block("waxed_weathered_copper_lantern"), block("oxidized_copper_lantern"), block("waxed_oxidized_copper_lantern"),
                    block("copper_chest"),block("exposed_copper_chest"),block("oxidized_copper_chest"),block("weathered_copper_chest"), block("waxed_copper_chest"),block("waxed_exposed_copper_chest"),block("waxed_oxidized_copper_chest"),block("waxed_weathered_copper_chest"), block("copper_golem_statue"), block("exposed_copper_golem_statue"), block("weathered_copper_golem_statue"), block("oxidized_copper_golem_statue"), block("waxed_copper_golem_statue"), block("waxed_exposed_copper_golem_statue"), block("waxed_weathered_copper_golem_statue"), block("waxed_oxidized_copper_golem_statue"), block("copper_torch"), block("copper_wall_torch"),
                    block("oak_shelf"), block("dark_oak_shelf"), block("pale_oak_shelf"), block("acacia_shelf"), block("bamboo_shelf"), block("birch_shelf"), block("cherry_shelf"), block("crimson_shelf"), block("jungle_shelf"), block("mangrove_shelf"), block("spruce_shelf"), block("warped_shelf")
            )
            .visible(list1Activar::get)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<Boolean> list2Activar = sglists.add(new BoolSetting.Builder()
            .name("List #2 Activate")
            .description("Activates checks for List #2")
            .defaultValue(true)
            .build());
    private final Setting<List<Block>> Blawcks2 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #2 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(block("spruce_wall_sign"), block("polished_diorite"), block("note_block"), block("mangrove_wood"), block("weathered_copper"))
            .visible(list2Activar::get)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<Boolean> list3Activar = sglists.add(new BoolSetting.Builder()
            .name("List #3 Activate")
            .description("Activates checks for List #3")
            .defaultValue(true)
            .build());
    private final Setting<List<Block>> Blawcks3 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #3 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(block("crafting_table"), block("brewing_stand"), block("ender_chest"), block("smooth_quartz"), block("redstone_block"), block("diamond_block"), block("brown_stained_glass"))
            .visible(list3Activar::get)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<Boolean> list4Activar = sglists.add(new BoolSetting.Builder()
            .name("List #4 Activate")
            .description("Activates checks for List #4")
            .defaultValue(true)
            .build());
    private final Setting<List<Block>> Blawcks4 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #4 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(block("oak_wall_sign"), block("trapped_chest"), block("iron_trapdoor"), block("lapis_block"))
            .visible(list4Activar::get)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<Boolean> list5Activar = sglists.add(new BoolSetting.Builder()
            .name("List #5 Activate")
            .description("Activates checks for List #5")
            .defaultValue(true)
            .build());
    private final Setting<List<Block>> Blawcks5 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #5 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(block("quartz_block"), block("furnace"), block("black_bed"), block("gray_bed"), block("light_blue_bed"), block("light_gray_bed"), block("pink_bed"), block("red_bed"), block("white_bed"), block("yellow_bed"), block("orange_bed"), block("blue_bed"), block("cyan_bed"), block("green_bed"), block("lime_bed"), block("purple_bed"), block("magenta_bed"), block("brown_bed"), block("white_concrete"))
            .visible(list5Activar::get)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<Boolean> list6Activar = sglists.add(new BoolSetting.Builder()
            .name("List #6 Activate")
            .description("Activates checks for List #6")
            .defaultValue(true)
            .build());
    private final Setting<List<Block>> Blawcks6 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #6 (Default)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue(block("redstone_torch"), block("hopper"))
            .visible(list6Activar::get)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<Boolean> list7Activar = sglists.add(new BoolSetting.Builder()
            .name("List #7 Activate")
            .description("Activates checks for List #7")
            .defaultValue(true)
            .build());
    private final Setting<List<Block>> Blawcks7 = sglists.add(new BlockListSetting.Builder()
            .name("Block List #7 (Extra Custom)")
            .description("If the total amount of any of these found is greater than the Number specified, throw a base location.")
            .defaultValue()
            .visible(list7Activar::get)
            .filter(this::filterBlocks)
            .build()
    );
    private final Setting<Integer> blowkfind1 = sglists.add(new IntSetting.Builder()
            .name("(List #1) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(1)
            .visible(list1Activar::get)
            .build());
    private final Setting<Integer> blowkfind2 = sglists.add(new IntSetting.Builder()
            .name("(List #2) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(6)
            .visible(list2Activar::get)
            .build());
    private final Setting<Integer> blowkfind3 = sglists.add(new IntSetting.Builder()
            .name("(List #3) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(4)
            .visible(list3Activar::get)
            .build());
    private final Setting<Integer> blowkfind4 = sglists.add(new IntSetting.Builder()
            .name("(List #4) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(2)
            .visible(list4Activar::get)
            .build());
    private final Setting<Integer> blowkfind5 = sglists.add(new IntSetting.Builder()
            .name("(List #5) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(12)
            .visible(list5Activar::get)
            .build());
    private final Setting<Integer> blowkfind6 = sglists.add(new IntSetting.Builder()
            .name("(List #6) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(12)
            .visible(list6Activar::get)
            .build());
    private final Setting<Integer> blowkfind7 = sglists.add(new IntSetting.Builder()
            .name("(List #7) Number Of Blocks to Find")
            .description("How many blocks it takes, from of any of the listed blocks to throw a base location.")
            .min(1)
            .sliderRange(1,100)
            .defaultValue(1)
            .visible(list7Activar::get)
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
            .visible(load::get)
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
        WTable table1 = theme.table();
        WTable table = theme.table();
        WButton nearestB = table1.add(theme.button("NearestBase")).expandX().minWidth(100).widget();
        nearestB.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else {
                findnearestbaseticks=1;
            }
        };
        table1.row();
        WButton adddata = table1.add(theme.button("AddBase")).expandX().minWidth(100).widget();
        adddata.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else {
                if (!baseChunks.contains(new ChunkPos(mc.player.chunkPosition().x(), mc.player.chunkPosition().z()))){
                    baseChunks.add(new ChunkPos(mc.player.chunkPosition().x(), mc.player.chunkPosition().z()));
                    try {
                        Path baseDir = FabricLoader.getInstance().getGameDir()
                                .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
                        Files.createDirectories(baseDir);
                        Path filePath = baseDir.resolve("BaseChunkData.txt");
                        ChunkPos chunkPos = new ChunkPos(mc.player.chunkPosition().x(), mc.player.chunkPosition().z());
                        String data = chunkPos + System.lineSeparator();
                        Files.write(filePath, data.getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                ChatUtils.sendMsg(Component.nullToEmpty("Base near X"+mc.player.chunkPosition().getMiddleBlockX()+", Z"+mc.player.chunkPosition().getMiddleBlockZ()+" added to the BaseFinder."));
            }
        };
        table1.row();
        WButton deldata = table1.add(theme.button("RemoveBase")).expandX().minWidth(100).widget();
        deldata.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else {
                if (baseChunks.contains(new ChunkPos(mc.player.chunkPosition().x(), mc.player.chunkPosition().z()))){
                    baseChunks.remove(new ChunkPos(mc.player.chunkPosition().x(), mc.player.chunkPosition().z()));
                    try {
                        Path baseDir = FabricLoader.getInstance().getGameDir()
                                .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
                        Files.createDirectories(baseDir);
                        Path filePath = baseDir.resolve("BaseChunkData.txt");
                        Files.deleteIfExists(filePath);
                        List<String> chunkDataLines = baseChunks.stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                        Files.write(filePath, chunkDataLines, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                ChatUtils.sendMsg(Component.nullToEmpty("Base near X"+mc.player.chunkPosition().getMiddleBlockX()+", Z"+mc.player.chunkPosition().getMiddleBlockZ()+" removed from the BaseFinder."));
            }
        };
        table1.row();
        WButton dellastdata = table1.add(theme.button("RemoveLastBase")).expandX().minWidth(100).widget();
        dellastdata.action = () -> {
            if(isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and push the button again.");
            } else if(isBaseFinderModuleOn!=0 && (LastBaseFound.x()==2000000000 || LastBaseFound.z()==2000000000)){
                error("Please find a base and run the command again.");
            } else {
                if (baseChunks.contains(new ChunkPos(LastBaseFound.x(), LastBaseFound.z()))){
                    baseChunks.remove(new ChunkPos(LastBaseFound.x(), LastBaseFound.z()));
                    try {
                        Path baseDir = FabricLoader.getInstance().getGameDir()
                                .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
                        Files.createDirectories(baseDir);
                        Path filePath = baseDir.resolve("BaseChunkData.txt");
                        Files.deleteIfExists(filePath);
                        List<String> chunkDataLines = baseChunks.stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                        Files.write(filePath, chunkDataLines, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                ChatUtils.sendMsg(Component.nullToEmpty("Base near X"+LastBaseFound.getMiddleBlockX()+", Z"+LastBaseFound.getMiddleBlockZ()+" removed from the BaseFinder."));
                LastBaseFound= new ChunkPos(2000000000, 2000000000);
            }
        };
        table1.row();
        WButton deletedata = table1.add(theme.button("**DELETE ALL BASE DATA**")).expandX().minWidth(100).widget();
        deletedata.action = () -> {
            if (!(mc.level==null) && mc.level.hasChunk(mc.player.chunkPosition().x(),mc.player.chunkPosition().z())){
                if (deletewarning==0) error("PRESS AGAIN WITHIN 5s TO DELETE ALL BASE DATA FOR THIS DIMENSION.");
                deletewarningTicks=0;
                deletewarning++;
            }
        };
        table1.row();
        List<LoggedBase> sortedBases = new ArrayList<>(loggedBases);
        sortedBases.sort(Comparator.comparingInt(a -> a.y));
        var list = theme.verticalList();
        list.add(table1);
        var clear = list.add(theme.button("Clear Logged Positions")).widget();
        if(!sortedBases.isEmpty()) list.add(table);
        clear.action = () -> {
            loggedBases.clear();
            loggedBasePositions.clear();
            table.clear();
            saveJsonLog();
            saveCsvLog();
        };
        for(LoggedBase lb : sortedBases) {
            table.add(theme.label("Pos: " + lb.x + ", " + lb.y + ", " + lb.z));
            WButton gotoBtn = table.add(theme.button("Goto")).widget();
            gotoBtn.action = () -> { meteordevelopment.meteorclient.pathing.PathManagers.get().moveTo(new BlockPos(lb.x, lb.y, lb.z), true); };
            var delete = table.add(theme.button("-")).widget();
            delete.action = () -> {
                loggedBases.remove(lb);
                loggedBasePositions.remove(new ChunkPos((lb.x - 8) / 16, (lb.z - 8) / 16));
                table.clear();
                for(LoggedBase l : loggedBases) {
                    table.add(theme.label("Pos: " + l.x + ", " + l.y + ", " + l.z));
                    WButton gotoBtn2 = table.add(theme.button("Goto")).widget();
                    gotoBtn2.action = () -> { meteordevelopment.meteorclient.pathing.PathManagers.get().moveTo(new BlockPos(l.x, l.y, l.z), true); };
                    var delete2 = table.add(theme.button("-")).widget();
                    delete2.action = () -> {
                        loggedBases.remove(l);
                        loggedBasePositions.remove(new ChunkPos((l.x - 8) / 16, (l.z - 8) / 16));
                        table.clear();
                        for(LoggedBase l2 : loggedBases) {
                            table.add(theme.label("Pos: " + l2.x + ", " + l2.y + ", " + l2.z));
                            WButton gotoBtn3 = table.add(theme.button("Goto")).widget();
                            gotoBtn3.action = () -> { meteordevelopment.meteorclient.pathing.PathManagers.get().moveTo(new BlockPos(l2.x, l2.y, l2.z), true); };
                            var delete3 = table.add(theme.button("-")).widget();
                            delete3.action = () -> {
                                loggedBases.remove(l2);
                                loggedBasePositions.remove(new ChunkPos((l2.x - 8) / 16, (l2.z - 8) / 16));
                            };
                            table.row();
                        }
                        saveJsonLog();
                        saveCsvLog();
                    };
                    table.row();
                }
                saveJsonLog();
                saveCsvLog();
            };
            table.row();
        }
        return list;
    }

    // render
    public final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
            .name("Render-Distance(Chunks)")
            .description("How many chunks from the character to render the detected chunks with bases.")
            .defaultValue(128)
            .min(6)
            .sliderRange(6,128)
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
            .visible(trcr::get)
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
            .visible(trcr::get)
            .build()
    );
    private final Setting<Boolean> locLogging = locationLogs.add(new BoolSetting.Builder()
            .name("Enable Location Logging")
            .description("Logs the locations of detected spawners to a table in this options menu.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> locLoggingCSV = locationLogs.add(new BoolSetting.Builder()
            .name("Log to CSV file")
            .description("Logs the locations of detected spawners to a csv file.")
            .defaultValue(false)
            .build()
    );
    private ExecutorService taskExecutor;
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
    private ChunkPos closestBase = new ChunkPos(2000000000, 2000000000);
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
        super(Trouser.baseHunting,"BaseFinder", "Estimates if a build or base may be in the chunk based on the blocks it contains.");
    }
    private void clearChunkData() {
        baseChunks.clear();
        basedistance=2000000000;
        closestBase = new ChunkPos(2000000000, 2000000000);
        LastBaseFound = new ChunkPos(2000000000, 2000000000);
    }
    @Override
    public void onActivate() {
        taskExecutor = Executors.newCachedThreadPool();
        isBaseFinderModuleOn=1;
        if (save.get())saveDataWasOn = true;
        else if (!save.get())saveDataWasOn = false;
        if (autoreload.get()) {
            clearChunkData();
        }
        if (save.get() || load.get()) {
            if (mc.isLocalServer()){
                Path worldPath = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
                Path savesDir = worldPath.getParent();
                if (savesDir != null) {
                    Path worldDir = savesDir.getFileName();
                    serverip = (worldDir != null ? worldDir.toString() : "singleplayer")
                            .replaceAll("[^a-zA-Z0-9._-]", "_");
                } else {
                    serverip = "singleplayer";
                }
            } else {
                serverip = mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9._\\-]", "_");
            }
            world= mc.level.dimension().identifier().toString().replaceAll("[^a-zA-Z0-9._\\-]", "_");
            if (save.get()) {
                try {
                    Path baseDir = FabricLoader.getInstance().getGameDir()
                            .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
                    Files.createDirectories(baseDir);
                } catch (IOException e) {
                    e.printStackTrace();
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
        taskExecutor.shutdownNow();
        isBaseFinderModuleOn=0;
        autoreloadticks=0;
        loadingticks=0;
        worldchange=false;
        justenabledsavedata = 0;
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
        world = mc.level.dimension().identifier().toString().replaceAll("[^a-zA-Z0-9._\\-]", "_");

        if (basefound && basefoundspamTicks < bsefndtickdelay.get()) basefoundspamTicks++;
        else if (basefoundspamTicks >= bsefndtickdelay.get()) {
            basefound = false;
            basefoundspamTicks = 0;
        }
        if (deletewarningTicks <= 100) deletewarningTicks++;
        if (deletewarning>=2){
            if (mc.isLocalServer()){
                Path worldPath = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
                Path savesDir = worldPath.getParent();
                if (savesDir != null) {
                    Path worldDir = savesDir.getFileName();
                    serverip = (worldDir != null ? worldDir.toString() : "singleplayer")
                            .replaceAll("[^a-zA-Z0-9._-]", "_");
                } else {
                    serverip = "singleplayer";
                }
            } else {
                serverip = mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9._\\-]", "_");
            }
            clearChunkData();
            try {
                Path baseDir = FabricLoader.getInstance().getGameDir()
                        .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
                Path filePath = baseDir.resolve("BaseChunkData.txt");
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                e.printStackTrace();
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
                    if (basedistance > Math.sqrt(Math.pow(baseChunks.stream().toList().get(b).x() - mc.player.chunkPosition().x(), 2) + Math.pow(baseChunks.stream().toList().get(b).z() - mc.player.chunkPosition().z(), 2))) {
                        closestBase = new ChunkPos(baseChunks.stream().toList().get(b).x(), baseChunks.stream().toList().get(b).z());
                        basedistance = Math.sqrt(Math.pow(baseChunks.stream().toList().get(b).x() - mc.player.chunkPosition().x(), 2) + Math.pow(baseChunks.stream().toList().get(b).z() - mc.player.chunkPosition().z(), 2));
                    }
                }
                basedistance = 2000000000;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (findnearestbaseticks == 1) {
            if (closestBase.x() < 1000000000 && closestBase.z() < 1000000000)
                ChatUtils.sendMsg(Component.nullToEmpty("#Nearest possible base at X" + closestBase.x() * 16 + " x Z" + closestBase.z() * 16));
            if (!(closestBase.x() < 1000000000 && closestBase.z() < 1000000000))
                error("No Bases Logged Yet.");
            findnearestbaseticks = 0;
        }

        if (save.get() || load.get()) {
            if (mc.isLocalServer()) {
                Path worldPath = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
                Path savesDir = worldPath.getParent();
                if (savesDir != null) {
                    Path worldDir = savesDir.getFileName();
                    serverip = (worldDir != null ? worldDir.toString() : "singleplayer")
                            .replaceAll("[^a-zA-Z0-9._-]", "_");
                } else {
                    serverip = "singleplayer";
                }
            } else {
                serverip = mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9._\\-]", "_");
            }
            world = mc.level.dimension().identifier().toString().replaceAll("[^a-zA-Z0-9._\\-]", "_");
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
        if (load.get() && worldchange) {
            if (worldleaveremove.get()) {
                clearChunkData();
            }
            loadData();
            worldchange = false;
        }
        if (!save.get()) saveDataWasOn = false;
        if (save.get() && justenabledsavedata <= 2 && !saveDataWasOn) {
            justenabledsavedata++;
            if (justenabledsavedata == 1) {
                synchronized (baseChunks) {
                    for (ChunkPos chunk : baseChunks) {
                        saveBaseChunkData(chunk);
                    }
                }
            }
        }

        if (entityScanTicks < entityScanDelay.get()) entityScanTicks++;
        if (entityScanTicks >= entityScanDelay.get() && (pearlFinder.get() || frameFinder.get() || villagerFinder.get() || nameFinder.get() || boatFinder.get() || entityClusterFinder.get())) {
            if (mc.level == null) return;

            int renderDistance = mc.options.renderDistance().get();
            ChunkPos playerChunkPos = mc.player.chunkPosition();
            for (int chunkX = playerChunkPos.x() - renderDistance; chunkX <= playerChunkPos.x() + renderDistance; chunkX++) {
                for (int chunkZ = playerChunkPos.z() - renderDistance; chunkZ <= playerChunkPos.z() + renderDistance; chunkZ++) {
                    LevelChunk chunk = mc.level.getChunk(chunkX, chunkZ);
                    if (chunk != null && chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
                        AABB chunkBox = new AABB(
                                chunk.getPos().getMinBlockX(), mc.level.getMinY(), chunk.getPos().getMinBlockZ(),
                                chunk.getPos().getMaxBlockX() + 1, mc.level.getMaxY(), chunk.getPos().getMaxBlockZ() + 1
                        );
                        if (!baseChunks.contains(chunk.getPos())) {
                            AtomicInteger animalsFound = new AtomicInteger();
                            mc.level.getEntitiesOfClass(Entity.class, chunkBox, entity -> true).forEach(entity -> {
                                if ((entity instanceof ItemFrame || entity instanceof GlowItemFrame) && frameFinder.get()) {
                                    ItemFrame itemFrame = (ItemFrame) entity;
                                    Item heldItem = itemFrame.getItem().getItem();
                                    if (heldItem != Items.ELYTRA) {
                                        baseChunks.add(chunk.getPos());
                                        if (save.get()) {
                                            saveBaseChunkData(chunk.getPos());
                                        }
                                        if (basefoundspamTicks == 0) {
                                            if (chatFeedback.get()){
                                                if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("Item Frame located near X" + entity.position().x() + ", Y" + entity.position().y() + ", Z" + entity.position().z()));
                                                else ChatUtils.sendMsg(Component.nullToEmpty("Item Frame located!"));
                                            }
                                            LastBaseFound = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
                                            basefound = true;
                                        }
                                    }
                                } else if (entity instanceof ThrownEnderpearl && pearlFinder.get()) {
                                    baseChunks.add(chunk.getPos());
                                    if (save.get()) {
                                        saveBaseChunkData(chunk.getPos());
                                    }
                                    if (basefoundspamTicks == 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("Ender Pearl located near X" + entity.position().x() + ", Y" + entity.position().y() + ", Z" + entity.position().z()));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("Ender Pearl located!"));
                                        }
                                        LastBaseFound = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
                                        basefound = true;
                                    }
                                } else if (entity instanceof Villager && villagerFinder.get()) {
                                    if (((Villager) entity).getVillagerData().level() > 1) {
                                        baseChunks.add(chunk.getPos());
                                        if (save.get()) {
                                            saveBaseChunkData(chunk.getPos());
                                        }
                                        if (basefoundspamTicks == 0) {
                                            if (chatFeedback.get()){
                                                if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("Illegal Villager located near X" + entity.position().x() + ", Y" + entity.position().y() + ", Z" + entity.position().z()));
                                                else ChatUtils.sendMsg(Component.nullToEmpty("Illegal Villager located!"));
                                            }
                                            LastBaseFound = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
                                            basefound = true;
                                        }
                                    }
                                } else if (entity.hasCustomName() && nameFinder.get()) {
                                    baseChunks.add(chunk.getPos());
                                    if (save.get()) {
                                        saveBaseChunkData(chunk.getPos());
                                    }
                                    if (basefoundspamTicks == 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("NameTagged Entity located near X" + entity.position().x() + ", Y" + entity.position().y() + ", Z" + entity.position().z()));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("NameTagged Entity located!"));
                                        }
                                        LastBaseFound = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
                                        basefound = true;
                                    }
                                } else if ((entity instanceof ChestBoat || entity instanceof Boat) && boatFinder.get()) {
                                    baseChunks.add(chunk.getPos());
                                    if (save.get()) {
                                        saveBaseChunkData(chunk.getPos());
                                    }
                                    if (basefoundspamTicks == 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("Illegal Boat located near X" + entity.position().x() + ", Y" + entity.position().y() + ", Z" + entity.position().z()));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("Illegal Boat located!"));
                                        }
                                        LastBaseFound = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
                                        basefound = true;
                                    }
                                } else if (entitieslist.get().contains(entity.getType()) && entityClusterFinder.get()) {
                                    animalsFound.getAndIncrement();
                                }
                            });
                            if (animalsFound.get() >= animalsFoundThreshold.get() && entityClusterFinder.get()){
                                baseChunks.add(chunk.getPos());
                                if (save.get()) {
                                    saveBaseChunkData(chunk.getPos());
                                }
                                if (basefoundspamTicks == 0) {
                                    if (chatFeedback.get()){
                                        if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("Illegal amount of entities located near X" + chunk.getPos().getMiddleBlockX() + ", Z" + chunk.getPos().getMiddleBlockZ()));
                                        else ChatUtils.sendMsg(Component.nullToEmpty("Illegal amount of entities located!"));
                                    }
                                    LastBaseFound = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
                                    basefound = true;
                                }
                            }
                        }
                    }
                }
            }
            entityScanTicks = 0;
        }
        if (removerenderdist.get()) removeChunksOutsideRenderDistance();
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        int topY = renderHeightY.get();
        int bottomY = renderHeightYbottom.get();
        int midpoint = (topY + bottomY) / 2;
        BlockPos playerPos = new BlockPos(mc.player.getBlockX(), midpoint, mc.player.getBlockZ());
        if (baseChunksLineColor.get().a > 5 || baseChunksSideColor.get().a > 5){
            if (!nearesttrcr.get()){
                synchronized (baseChunks) {
                    for (ChunkPos c : baseChunks) {
                        if (playerPos.closerThan(new BlockPos(c.getMiddleBlockX(), midpoint, c.getMiddleBlockZ()), renderDistance.get()*16)) {
                            render(new AABB(new Vec3(c.getWorldPosition().getX()+7, c.getWorldPosition().getY()+renderHeightYbottom.get(), c.getWorldPosition().getZ()+7), new Vec3(c.getWorldPosition().getX()+8, c.getWorldPosition().getY()+renderHeightY.get(), c.getWorldPosition().getZ()+8)), baseChunksSideColor.get(), baseChunksLineColor.get(),ShapeMode.Sides, event);
                        }
                    }
                }
            } else if (nearesttrcr.get()){
                synchronized (baseChunks) {
                    for (ChunkPos c : baseChunks) {
                        if (playerPos.closerThan(new BlockPos(c.getMiddleBlockX(), midpoint, c.getMiddleBlockZ()), renderDistance.get()*16)) {
                            render(new AABB(new Vec3(c.getWorldPosition().getX()+7, c.getWorldPosition().getY()+renderHeightYbottom.get(), c.getWorldPosition().getZ()+7), new Vec3(c.getWorldPosition().getX()+8, c.getWorldPosition().getY()+renderHeightY.get(), c.getWorldPosition().getZ()+8)), baseChunksSideColor.get(), baseChunksLineColor.get(),ShapeMode.Sides, event);
                        }
                    }
                }
                render2(new AABB(new Vec3(closestBase.getWorldPosition().getX()+7, closestBase.getWorldPosition().getY()+renderHeightYbottom.get(), closestBase.getWorldPosition().getZ()+7), new Vec3 (closestBase.getWorldPosition().getX()+8, closestBase.getWorldPosition().getY()+renderHeightY.get(), closestBase.getWorldPosition().getZ()+8)), baseChunksSideColor.get(), baseChunksLineColor.get(),ShapeMode.Sides, event);
            }
        }
    }

    private void render(AABB box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        if (trcr.get() && Math.abs(box.minX-RenderUtils.center.x)<=trcrdist.get()*16 && Math.abs(box.minZ-RenderUtils.center.z)<=trcrdist.get()*16)
            if (!nearesttrcr.get())
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX+0.5, box.minY+((box.maxY-box.minY)/2), box.minZ+0.5, lines);
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, new Color(0,0,0,0), shapeMode, 0);
    }
    private void render2(AABB box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        if (trcr.get() && Math.abs(box.minX-RenderUtils.center.x)<=trcrdist.get()*16 && Math.abs(box.minZ-RenderUtils.center.z)<=trcrdist.get()*16)
            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, box.minX+0.5, box.minY+((box.maxY-box.minY)/2), box.minZ+0.5, lines);
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, new Color(0,0,0,0), shapeMode, 0);
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof ServerboundMovePlayerPacket) return; //this keeps getting cast to the chunkdata for no reason
        if (!(event.packet instanceof ServerboundMovePlayerPacket) && event.packet instanceof ClientboundLevelChunkWithLightPacket packet && mc.level != null) {

            basepos = new ChunkPos(packet.getX(), packet.getZ());

            if (mc.level.getChunkSource().getChunkForLighting(packet.getX(), packet.getZ()) == null) {
                LevelChunk chunk = new LevelChunk(mc.level, basepos);
                try {
                    Map<Heightmap.Types, long[]> heightmaps = new EnumMap<>(Heightmap.Types.class);

                    Heightmap.Types type = Heightmap.Types.MOTION_BLOCKING;
                    long[] emptyHeightmapData = new long[37];
                    heightmaps.put(type, emptyHeightmapData);

                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        chunk.replaceWithPacketData(packet.getChunkData().getReadBuffer(), heightmaps,
                                packet.getChunkData().getBlockEntitiesTagsConsumer(packet.getX(), packet.getZ()));
                    }, taskExecutor);
                    future.join();
                } catch (CompletionException e) {e.printStackTrace();}

                if (bubblesFinder.get() || spawner.get() || signFinder.get() || portalFinder.get() || roofDetector.get() || bedrockfind.get() || skybuildfind.get() || !Blawcks1.get().isEmpty() || !Blawcks2.get().isEmpty() || !Blawcks3.get().isEmpty() || !Blawcks4.get().isEmpty() || !Blawcks5.get().isEmpty() || !Blawcks6.get().isEmpty() || !Blawcks7.get().isEmpty()){
                    int Ymin = mc.level.getMinY()+minY.get();
                    int Ymax = mc.level.getMaxY()-maxY.get();
                    try {
                        Set<BlockPos> blockpositions1 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions2 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions3 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions4 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions5 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions6 = Collections.synchronizedSet(new HashSet<>());
                        Set<BlockPos> blockpositions7 = Collections.synchronizedSet(new HashSet<>());
                        LevelChunkSection[] sections = chunk.getSections();
                        int Y = mc.level.getMinY();
                        for (LevelChunkSection section: sections){
                            if (section == null || section.hasOnlyAir()) {
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
                                        if (blerks.getBlock()!=block("air") && blerks.getBlock()!=block("stone")){
                                            if (!(blerks.getBlock()==block("deepslate")) && !(blerks.getBlock()==block("dirt")) && !(blerks.getBlock()==block("grass_block")) && !(blerks.getBlock()==block("water")) && !(blerks.getBlock()==block("sand")) && !(blerks.getBlock()==block("gravel"))  && !(blerks.getBlock()==block("bedrock"))&& !(blerks.getBlock()==block("netherrack")) && !(blerks.getBlock()==block("lava"))){
                                                if (signFinder.get() && blerks.getBlock() instanceof StandingSignBlock || blerks.getBlock() instanceof CeilingHangingSignBlock) {
                                                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                                                        Boolean signtextfound = false;
                                                        if (blockEntity instanceof SignBlockEntity){
                                                            SignText signText = ((SignBlockEntity) blockEntity).getFrontText();
                                                            SignText signText2 = ((SignBlockEntity) blockEntity).getBackText();
                                                            Component[] lines = signText.getMessages(false);
                                                            Component[] lines2 = signText2.getMessages(false);
                                                            int i = 0;
                                                            for (Component line : lines) {
                                                                if (line.tryCollapseToString().length() != 0 && (line.getString() != "<----" && i == 1) && (line.getString() != "---->" && i == 2)){ //handling for arrows is for igloos
                                                                    signtextfound = true;
                                                                    if (signtextfound) break;
                                                                }
                                                                i++;
                                                            }
                                                            for (Component line2 : lines2) {
                                                                if (signtextfound) break;
                                                                if (line2.tryCollapseToString().length() != 0){
                                                                    signtextfound = true;
                                                                    if (signtextfound) break;
                                                                }
                                                            }
                                                        } else if (blockEntity instanceof HangingSignBlockEntity) {
                                                            SignText signText = ((HangingSignBlockEntity) blockEntity).getFrontText();
                                                            SignText signText2 = ((HangingSignBlockEntity) blockEntity).getBackText();
                                                            Component[] lines = signText.getMessages(false);
                                                            Component[] lines2 = signText2.getMessages(false);
                                                            for (Component line : lines) {
                                                                if (line.tryCollapseToString().length() != 0){ //handling for arrows is for igloos
                                                                    signtextfound = true;
                                                                    if (signtextfound) break;
                                                                }
                                                            }
                                                            for (Component line2 : lines2) {
                                                                if (signtextfound) break;
                                                                if (line2.tryCollapseToString().length() != 0){
                                                                    signtextfound = true;
                                                                    if (signtextfound) break;
                                                                }
                                                            }
                                                        }
                                                        if (signtextfound && !baseChunks.contains(basepos)){
                                                            baseChunks.add(basepos);
                                                            if (save.get()) {
                                                                saveBaseChunkData(basepos);
                                                            }
                                                            if (basefoundspamTicks==0){
                                                                if (chatFeedback.get()){
                                                                    if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("Written Sign located near X"+blockEntity.getBlockPos().getX()+", Y"+blockEntity.getBlockPos().getY()+", Z"+blockEntity.getBlockPos().getZ()));
                                                                    else ChatUtils.sendMsg(Component.nullToEmpty("Written Sign located!"));
                                                                }
                                                                LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
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
                                                            if (chatFeedback.get()){
                                                                if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(Skybuild)Possible build located near X"+basepos.getMiddleBlockX()+", Y"+currentY+", Z"+basepos.getMiddleBlockZ()));
                                                                else ChatUtils.sendMsg(Component.nullToEmpty("(Skybuild)Possible build located!"));
                                                            }
                                                            LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (bubblesFinder.get() && blerks.getBlock() instanceof BubbleColumnBlock && !blerks.getValue(BubbleColumnBlock.DRAG_DOWN)) {
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            if (chatFeedback.get()){
                                                                if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(Bubble Column)Possible build located near X"+basepos.getMiddleBlockX()+", Y"+currentY+", Z"+basepos.getMiddleBlockZ()));
                                                                else ChatUtils.sendMsg(Component.nullToEmpty("(Bubble Column)Possible build located!"));
                                                            }
                                                            LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (portalFinder.get() && (blerks.getBlock()==block("nether_portal") || blerks.getBlock()==block("end_portal"))) {
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            if (chatFeedback.get()){
                                                                if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(Open Portal)Possible build located near X"+basepos.getMiddleBlockX()+", Y"+currentY+", Z"+basepos.getMiddleBlockZ()));
                                                                else ChatUtils.sendMsg(Component.nullToEmpty("(Open Portal)Possible build located!"));
                                                            }
                                                            LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (bedrockfind.get() && blerks.getBlock()==block("bedrock") && ((currentY>mc.level.getMinY()+bedrockint.get() && mc.level.dimension() == Level.OVERWORLD) || (currentY>mc.level.getMinY()+bedrockint.get() && (currentY < 123 || currentY > 127) && mc.level.dimension() == Level.NETHER))) {
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            if (chatFeedback.get()){
                                                                if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(Unnatural Bedrock)Possible build located near X"+basepos.getMiddleBlockX()+", Y"+currentY+", Z"+basepos.getMiddleBlockZ()));
                                                                else ChatUtils.sendMsg(Component.nullToEmpty("(Unnatural Bedrock)Possible build located!"));
                                                            }
                                                            LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (roofDetector.get() && blerks.getBlock()!=block("red_mushroom") && blerks.getBlock()!=block("brown_mushroom") && currentY>=128 && mc.level.dimension() == Level.NETHER){
                                                    if (!baseChunks.contains(basepos)){
                                                        baseChunks.add(basepos);
                                                        if (save.get()) {
                                                            saveBaseChunkData(basepos);
                                                        }
                                                        if (basefoundspamTicks==0){
                                                            if (chatFeedback.get()){
                                                                if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(Nether Roof)Possible build located near X"+basepos.getMiddleBlockX()+", Y"+currentY+", Z"+basepos.getMiddleBlockZ()));
                                                                else ChatUtils.sendMsg(Component.nullToEmpty("(Nether Roof)Possible build located!"));
                                                            }
                                                            LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                                            basefound=true;
                                                        }
                                                    }
                                                }
                                                if (spawner.get()){
                                                    if (blerks.getBlock()==block("spawner")){
                                                        spawnerY=currentY;
                                                        spawnerfound=true;
                                                    }
                                                    //dungeon MOSSY_COBBLESTONE, mineshaft COBWEB, fortress NETHER_BRICK_FENCE, stronghold STONE_BRICK_STAIRS, bastion CHAIN
                                                    if (mc.level.dimension() == Level.OVERWORLD && (blerks.getBlock()==block("mossy_cobblestone") || blerks.getBlock()==block("cobweb") || blerks.getBlock()==block("stone_brick_stairs") || blerks.getBlock()==block("budding_amethyst")))spawnernaturalblocks=true;
                                                    else if (mc.level.dimension() == Level.NETHER && (blerks.getBlock()==block("nether_brick_fence") || blerks.getBlock()==block("iron_chain")))spawnernaturalblocks=true;
                                                }
                                                if (list1Activar.get() && !Blawcks1.get().isEmpty()){
                                                    if (Blawcks1.get().contains(blerks.getBlock())) {
                                                        blockpositions1.add(blockposi);
                                                        found1= blockpositions1.size();
                                                        lastblockfound1=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (list2Activar.get() && !Blawcks2.get().isEmpty()){
                                                    if (Blawcks2.get().contains(blerks.getBlock())) {
                                                        blockpositions2.add(blockposi);
                                                        found2= blockpositions2.size();
                                                        lastblockfound2=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (list3Activar.get() && !Blawcks3.get().isEmpty()){
                                                    if (Blawcks3.get().contains(blerks.getBlock())) {
                                                        blockpositions3.add(blockposi);
                                                        found3= blockpositions3.size();
                                                        lastblockfound3=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (list4Activar.get() && !Blawcks4.get().isEmpty()){
                                                    if (Blawcks4.get().contains(blerks.getBlock())) {
                                                        blockpositions4.add(blockposi);
                                                        found4= blockpositions4.size();
                                                        lastblockfound4=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (list5Activar.get() && !Blawcks5.get().isEmpty()){
                                                    if (Blawcks5.get().contains(blerks.getBlock())) {
                                                        blockpositions5.add(blockposi);
                                                        found5= blockpositions5.size();
                                                        lastblockfound5=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (list6Activar.get() && !Blawcks6.get().isEmpty()){
                                                    if (Blawcks6.get().contains(blerks.getBlock())) {
                                                        blockpositions6.add(blockposi);
                                                        found6= blockpositions6.size();
                                                        lastblockfound6=blerks.getBlock().toString();
                                                    }
                                                }
                                                if (list7Activar.get() && !Blawcks7.get().isEmpty()){
                                                    if (Blawcks7.get().contains(blerks.getBlock())) {
                                                        blockpositions7.add(blockposi);
                                                        found7= blockpositions7.size();
                                                        lastblockfound7=blerks.getBlock().toString();
                                                    }
                                                }
                                            }
                                        }
                                        if (!Blawcks1.get().isEmpty())checkingchunk1=true;
                                        if (!Blawcks2.get().isEmpty())checkingchunk2=true;
                                        if (!Blawcks3.get().isEmpty())checkingchunk3=true;
                                        if (!Blawcks4.get().isEmpty())checkingchunk4=true;
                                        if (!Blawcks5.get().isEmpty())checkingchunk5=true;
                                        if (!Blawcks6.get().isEmpty())checkingchunk6=true;
                                        if (!Blawcks7.get().isEmpty())checkingchunk7=true;
                                    }
                                }
                            }
                            Y+=16;
                        }
                        //CheckList 1
                        if (!Blawcks1.get().isEmpty()){
                            if (checkingchunk1 && found1>=blowkfind1.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(List1)Possible build located near X" + basepos.getMiddleBlockX() + ", Y" + blockpositions1.stream().toList().get(0).getY() + ", Z" + basepos.getMiddleBlockZ() + " (" + lastblockfound1 + ")"));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("(List1)Possible build located!"+" ("+lastblockfound1+")"));
                                        }
                                        LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                        basefound=true;
                                    }
                                }
                                blockpositions1.clear();
                                found1 = 0;
                                checkingchunk1=false;
                            } else if (checkingchunk1 && found1<blowkfind1.get()){
                                blockpositions1.clear();
                                found1 = 0;
                                checkingchunk1=false;
                            }
                        }

                        //CheckList 2
                        if (!Blawcks2.get().isEmpty()){
                            if (checkingchunk2 && found2>=blowkfind2.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(List2)Possible build located near X" + basepos.getMiddleBlockX() + ", Y" + blockpositions2.stream().toList().get(0).getY() + ", Z" + basepos.getMiddleBlockZ() + " (" + lastblockfound2 + ")"));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("(List2)Possible build located!"+" ("+lastblockfound2+")"));
                                        }
                                        LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                        basefound=true;
                                    }
                                }
                                blockpositions2.clear();
                                found2 = 0;
                                checkingchunk2=false;
                            } else if (checkingchunk2 && found2<blowkfind2.get()){
                                blockpositions2.clear();
                                found2 = 0;
                                checkingchunk2=false;
                            }
                        }

                        //CheckList 3
                        if (!Blawcks3.get().isEmpty()){
                            if (checkingchunk3 && found3>=blowkfind3.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(List3)Possible build located near X" + basepos.getMiddleBlockX() + ", Y" + blockpositions3.stream().toList().get(0).getY() + ", Z" + basepos.getMiddleBlockZ() + " (" + lastblockfound3 + ")"));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("(List3)Possible build located!"+" ("+lastblockfound3+")"));
                                        }
                                        LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                        basefound=true;
                                    }
                                }
                                blockpositions3.clear();
                                found3 = 0;
                                checkingchunk3=false;
                            } else if (checkingchunk3 && found3<blowkfind3.get()){
                                blockpositions3.clear();
                                found3 = 0;
                                checkingchunk3=false;
                            }
                        }

                        //CheckList 4
                        if (!Blawcks4.get().isEmpty()){
                            if (checkingchunk4 && found4>=blowkfind4.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(List4)Possible build located near X" + basepos.getMiddleBlockX() + ", Y" + blockpositions4.stream().toList().get(0).getY() + ", Z" + basepos.getMiddleBlockZ() + " (" + lastblockfound4 + ")"));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("(List4)Possible build located!"+" ("+lastblockfound4+")"));
                                        }
                                        LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                        basefound=true;
                                    }
                                }
                                blockpositions4.clear();
                                found4 = 0;
                                checkingchunk4=false;
                            } else if (checkingchunk4 && found4<blowkfind4.get()){
                                blockpositions4.clear();
                                found4 = 0;
                                checkingchunk4=false;
                            }
                        }

                        //CheckList 5
                        if (!Blawcks5.get().isEmpty()){
                            if (checkingchunk5 && found5>=blowkfind5.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(List5)Possible build located near X"+basepos.getMiddleBlockX()+", Y"+blockpositions5.stream().toList().get(0).getY()+", Z"+basepos.getMiddleBlockZ()+" ("+lastblockfound5+")"));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("(List5)Possible build located!"+" ("+lastblockfound5+")"));
                                        }
                                        LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                        basefound=true;
                                    }
                                }
                                blockpositions5.clear();
                                found5 = 0;
                                checkingchunk5=false;
                            } else if (checkingchunk5 && found5<blowkfind5.get()){
                                blockpositions5.clear();
                                found5 = 0;
                                checkingchunk5=false;
                            }
                        }

                        //CheckList 6
                        if (!Blawcks6.get().isEmpty()){
                            if (checkingchunk6 && found6>=blowkfind6.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(List6)Possible build located near X"+basepos.getMiddleBlockX()+", Y"+blockpositions6.stream().toList().get(0).getY()+", Z"+basepos.getMiddleBlockZ()+" ("+lastblockfound6+")"));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("(List6)Possible build located!"+" ("+lastblockfound6+")"));
                                        }
                                        LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                        basefound=true;
                                    }
                                }
                                blockpositions6.clear();
                                found6 = 0;
                                checkingchunk6=false;
                            } else if (checkingchunk6 && found6<blowkfind6.get()){
                                blockpositions6.clear();
                                found6 = 0;
                                checkingchunk6=false;
                            }
                        }

                        //CheckList 7
                        if (!Blawcks7.get().isEmpty()){
                            if (checkingchunk7 && found7>=blowkfind7.get()) {
                                if (!baseChunks.contains(basepos)){
                                    baseChunks.add(basepos);
                                    if (save.get()) {
                                        saveBaseChunkData(basepos);
                                    }
                                    if (basefoundspamTicks== 0) {
                                        if (chatFeedback.get()){
                                            if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("(List7)Possible build located near X"+basepos.getMiddleBlockX()+", Y"+blockpositions7.stream().toList().get(0).getY()+", Z"+basepos.getMiddleBlockZ()+" ("+lastblockfound7+")"));
                                            else ChatUtils.sendMsg(Component.nullToEmpty("(List7)Possible build located!"+" ("+lastblockfound7+")"));
                                        }
                                        LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                                        basefound=true;
                                    }
                                }
                                blockpositions7.clear();
                                found7 = 0;
                                checkingchunk7=false;
                            } else if (checkingchunk7 && found7<blowkfind7.get()){
                                blockpositions7.clear();
                                found7 = 0;
                                checkingchunk7=false;
                            }
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                if (spawnerfound && !spawnernaturalblocks){
                    if (!baseChunks.contains(basepos)){
                        baseChunks.add(basepos);
                        if (save.get()) {
                            saveBaseChunkData(basepos);
                        }
                        if (basefoundspamTicks== 0) {
                            if (chatFeedback.get()){
                                if (displaycoords.get())ChatUtils.sendMsg(Component.nullToEmpty("Possible modified spawner located near X"+basepos.getMiddleBlockX()+", Y"+spawnerY+", Z"+basepos.getMiddleBlockZ()));
                                else ChatUtils.sendMsg(Component.nullToEmpty("Possible modified spawner located!"));
                            }
                            LastBaseFound= new ChunkPos(basepos.x(), basepos.z());
                            basefound=true;
                        }
                    }
                    spawnerfound=false;
                    spawnernaturalblocks=false;
                } else if ((spawnerfound && spawnernaturalblocks) || (!spawnerfound && spawnernaturalblocks) || (!spawnerfound && !spawnernaturalblocks)){
                    spawnerfound=false;
                    spawnernaturalblocks=false;
                }
            }
        }
    }
    private void loadData() {
        Path baseDir = FabricLoader.getInstance().getGameDir()
                .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
        Path filePath = baseDir.resolve("BaseChunkData.txt");

        try {
            if (!Files.exists(filePath)) return;
            List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

            for (String line : allLines) {
                String s = line;
                String[] array = s.split(",");
                int X = Integer.parseInt(array[0].trim());
                int Z = Integer.parseInt(array[1].trim());
                basepos = new ChunkPos(X, Z);
                baseChunks.add(basepos);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void saveBaseChunkData(ChunkPos basepos) {
        taskExecutor.submit(() -> {
            try {
                Path baseDir = FabricLoader.getInstance().getGameDir()
                        .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
                Files.createDirectories(baseDir);
                Path filePath = baseDir.resolve("BaseChunkData.txt");
                String data = basepos.x() + "," + basepos.z() + System.lineSeparator();
                Files.write(filePath, data.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean filterBlocks(Block block) {
        return isNaturalLagCausingBlock(block);
    }
    private boolean isNaturalLagCausingBlock(Block block) {
        return  block instanceof Block &&
                !(block ==block("air")) &&
                !(block ==block("stone")) &&
                !(block ==block("dirt")) &&
                !(block ==block("grass_block")) &&
                !(block ==block("sand")) &&
                !(block ==block("gravel")) &&
                !(block ==block("deepslate")) &&
                !(block ==block("water")) &&
                !(block ==block("netherrack")) &&
                !(block ==block("lava"));
    }
    private void removeChunksOutsideRenderDistance() {
        int topY = renderHeightY.get();
        int bottomY = renderHeightYbottom.get();
        int midpoint = (topY + bottomY) / 2;
        BlockPos playerPos = new BlockPos(mc.player.getBlockX(), midpoint, mc.player.getBlockZ());
        double renderDistanceBlocks = renderDistance.get() * 16;

        removeChunksOutsideRenderDistance(baseChunks, playerPos, renderDistanceBlocks, midpoint);
        if (!playerPos.closerThan(new BlockPos(closestBase.getMiddleBlockX(), midpoint, closestBase.getMiddleBlockZ()), renderDistanceBlocks))
            closestBase = new ChunkPos(2000000000, 2000000000);
    }
    private void removeChunksOutsideRenderDistance(Set<ChunkPos> chunkSet, BlockPos playerPos, double renderDistanceBlocks, int midpoint) {
        List<ChunkPos> chunksToRemove = new ArrayList<>();
        for (ChunkPos c : chunkSet) {
            if (!playerPos.closerThan(new BlockPos(c.getMiddleBlockX(), midpoint, c.getMiddleBlockZ()), renderDistanceBlocks)) {
                chunksToRemove.add(c);
            }
        }
        chunkSet.removeAll(chunksToRemove);
    }

    private final List<LoggedBase> loggedBases = new ArrayList<>();
    private final Set<ChunkPos> loggedBasePositions = new HashSet<>();
    private static final com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        for (ChunkPos pos : new ArrayList<>(baseChunks)) {
            if (!loggedBasePositions.contains(pos) && (locLogging.get() || locLoggingCSV.get())) {
                loggedBasePositions.add(pos);
                int x = pos.getMiddleBlockX();
                int z = pos.getMiddleBlockZ();
                int y = (renderHeightY.get() + renderHeightYbottom.get()) / 2;
                loggedBases.add(new LoggedBase(x, y, z));
                if (locLogging.get()) saveJsonLog();
                if (locLoggingCSV.get()) saveCsvLog();
            }
        }
    }

    private void saveCsvLog() {
        try {
            File file = getCsvFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            writer.write("X,Y,Z\n");
            for(LoggedBase lb : loggedBases) {
                lb.write(writer);
            }
            writer.close();
        } catch (IOException e) {e.printStackTrace();}
    }

    private void saveJsonLog() {
        try {
            File file = getJsonFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            gson.toJson(loggedBases, writer);
            writer.close();
        } catch (IOException e) {e.printStackTrace();}
    }
    private File getJsonFile() {
        Path baseDir = FabricLoader.getInstance().getGameDir()
                .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
        return baseDir.resolve("bases.json").toFile();
    }

    private File getCsvFile() {
        Path baseDir = FabricLoader.getInstance().getGameDir()
                .resolve("TrouserStreak").resolve("BaseChunks").resolve(serverip).resolve(world);
        return baseDir.resolve("bases.csv").toFile();
    }

    private static class LoggedBase {
        public int x;
        public int y;
        public int z;
        public LoggedBase(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public void write(java.io.Writer writer) throws java.io.IOException {
            writer.write(x + "," + y + "," + z + "\n");
        }
        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            LoggedBase that = (LoggedBase) o;
            return x == that.x && y == that.y && z == that.z;
        }
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
}
