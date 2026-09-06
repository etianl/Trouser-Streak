package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockListMineCommand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sglists = settings.createGroup("Blocks To Mine");

    // general
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("Unnatural Blocks or Custom Blocklist")
            .description("Blocklists.")
            .defaultValue(Modes.UnnaturalBlocks)
            .build());
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("Block Scan Range (vertical)")
            .description("How far from the player's Y level to scan for matching blocks in the list. (Will only scan within the game's )")
            .sliderRange(0, 384)
            .min(1)
            .defaultValue(30)
            .build());
    private final Setting<List<Block>> Blawcks1 = sglists.add(new BlockListSetting.Builder()
            .name("Unnatural Blocks)")
            .description("Blocks to add to the #mine command. These blocks never spawn naturally. Edit as needed.")
            .defaultValue(
                    Blocks.CRAFTER, Blocks.SPRUCE_SAPLING, Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.CHERRY_SAPLING, Blocks.BAMBOO_SAPLING,
                    Blocks.CHERRY_BUTTON, Blocks.CHERRY_DOOR, Blocks.CHERRY_FENCE, Blocks.CHERRY_FENCE_GATE, Blocks.CHERRY_PLANKS, Blocks.CHERRY_PRESSURE_PLATE, Blocks.CHERRY_STAIRS, Blocks.CHERRY_WOOD, Blocks.CHERRY_TRAPDOOR, Blocks.CHERRY_SLAB,
                    Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_BUTTON, Blocks.MANGROVE_DOOR, Blocks.MANGROVE_FENCE, Blocks.MANGROVE_FENCE_GATE, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB, Blocks.MANGROVE_TRAPDOOR,
                    Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE_GATE, Blocks.BIRCH_BUTTON, Blocks.ACACIA_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.POLISHED_BLACKSTONE_BUTTON, Blocks.SPRUCE_BUTTON,
                    Blocks.BAMBOO_BLOCK, Blocks.BAMBOO_BUTTON, Blocks.BAMBOO_DOOR, Blocks.BAMBOO_FENCE, Blocks.BAMBOO_FENCE_GATE, Blocks.BAMBOO_MOSAIC, Blocks.BAMBOO_MOSAIC_SLAB, Blocks.BAMBOO_MOSAIC_STAIRS, Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_PRESSURE_PLATE, Blocks.BAMBOO_SLAB, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_TRAPDOOR, Blocks.CHISELED_BOOKSHELF,
                    Blocks.CONCRETE.black(), Blocks.CONCRETE.blue(), Blocks.CONCRETE.cyan(), Blocks.CONCRETE.brown(), Blocks.CONCRETE.orange(), Blocks.CONCRETE.magenta(), Blocks.CONCRETE.lightBlue(), Blocks.CONCRETE.yellow(), Blocks.CONCRETE.lime(), Blocks.CONCRETE.pink(), Blocks.CONCRETE.gray(), Blocks.CONCRETE.lightGray(), Blocks.CONCRETE.purple(), Blocks.CONCRETE.green(),
                    Blocks.CONCRETE_POWDER.black(), Blocks.CONCRETE_POWDER.blue(), Blocks.CONCRETE_POWDER.cyan(), Blocks.CONCRETE_POWDER.brown(), Blocks.CONCRETE_POWDER.white(), Blocks.CONCRETE_POWDER.orange(), Blocks.CONCRETE_POWDER.magenta(), Blocks.CONCRETE_POWDER.lightBlue(), Blocks.CONCRETE_POWDER.yellow(), Blocks.CONCRETE_POWDER.lime(), Blocks.CONCRETE_POWDER.pink(), Blocks.CONCRETE_POWDER.gray(), Blocks.CONCRETE_POWDER.lightGray(), Blocks.CONCRETE_POWDER.purple(), Blocks.CONCRETE_POWDER.green(), Blocks.CONCRETE_POWDER.red(),
                    Blocks.DYED_TERRACOTTA.purple(), Blocks.DYED_TERRACOTTA.magenta(), Blocks.DYED_TERRACOTTA.pink(), Blocks.GLAZED_TERRACOTTA.magenta(), Blocks.GLAZED_TERRACOTTA.pink(), Blocks.GLAZED_TERRACOTTA.gray(), Blocks.GLAZED_TERRACOTTA.blue(), Blocks.GLAZED_TERRACOTTA.brown(), Blocks.GLAZED_TERRACOTTA.green(),
                    Blocks.COPPER_BLOCK.weathering().oxidized(), Blocks.CUT_COPPER.weathering().unaffected(), Blocks.CUT_COPPER.weathering().exposed(), Blocks.CUT_COPPER.weathering().weathered(), Blocks.CUT_COPPER_SLAB.weathering().unaffected(), Blocks.CUT_COPPER_STAIRS.weathering().unaffected(), Blocks.CUT_COPPER_SLAB.weathering().exposed(), Blocks.CUT_COPPER_STAIRS.weathering().exposed(), Blocks.CUT_COPPER_SLAB.weathering().weathered(), Blocks.CUT_COPPER_STAIRS.weathering().weathered(), Blocks.CUT_COPPER_SLAB.weathering().oxidized(), Blocks.CUT_COPPER_STAIRS.weathering().oxidized(), Blocks.COPPER_BULB.weathering().unaffected(), Blocks.COPPER_BULB.weathering().exposed(), Blocks.COPPER_BULB.weathering().weathered(), Blocks.COPPER_BULB.weathering().oxidized(), Blocks.CHISELED_COPPER.weathering().unaffected(), Blocks.CHISELED_COPPER.weathering().exposed(), Blocks.CHISELED_COPPER.weathering().weathered(), Blocks.CHISELED_COPPER.weathering().oxidized(), Blocks.COPPER_DOOR.weathering().unaffected(), Blocks.COPPER_DOOR.weathering().exposed(), Blocks.COPPER_DOOR.weathering().weathered(), Blocks.COPPER_DOOR.weathering().oxidized(), Blocks.COPPER_GRATE.weathering().unaffected(), Blocks.COPPER_GRATE.weathering().exposed(), Blocks.COPPER_GRATE.weathering().weathered(), Blocks.COPPER_GRATE.weathering().oxidized(), Blocks.COPPER_TRAPDOOR.weathering().unaffected(), Blocks.COPPER_TRAPDOOR.weathering().exposed(), Blocks.COPPER_TRAPDOOR.weathering().weathered(),
                    Blocks.COPPER_BLOCK.waxed().exposed(), Blocks.COPPER_BLOCK.waxed().weathered(), Blocks.CUT_COPPER.waxed().exposed(), Blocks.CUT_COPPER.waxed().weathered(), Blocks.CUT_COPPER_SLAB.waxed().exposed(), Blocks.CUT_COPPER_STAIRS.waxed().exposed(), Blocks.CUT_COPPER_SLAB.waxed().weathered(), Blocks.CUT_COPPER_STAIRS.waxed().weathered(), Blocks.CHISELED_COPPER.waxed().exposed(), Blocks.CHISELED_COPPER.waxed().weathered(), Blocks.COPPER_DOOR.waxed().exposed(), Blocks.COPPER_DOOR.waxed().weathered(), Blocks.COPPER_GRATE.waxed().exposed(), Blocks.COPPER_GRATE.waxed().weathered(), Blocks.COPPER_TRAPDOOR.waxed().unaffected(), Blocks.COPPER_TRAPDOOR.waxed().exposed(), Blocks.COPPER_TRAPDOOR.waxed().weathered(),
                    Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.POTTED_MANGROVE_PROPAGULE, Blocks.POTTED_AZALEA, Blocks.POTTED_CHERRY_SAPLING, Blocks.POTTED_FERN, Blocks.POTTED_ACACIA_SAPLING, Blocks.POTTED_WARPED_FUNGUS, Blocks.POTTED_WARPED_ROOTS, Blocks.POTTED_CRIMSON_FUNGUS, Blocks.POTTED_CRIMSON_ROOTS, Blocks.POTTED_OAK_SAPLING, Blocks.POTTED_WITHER_ROSE, Blocks.WITHER_ROSE,
                    Blocks.CAKE, Blocks.CANDLE_CAKE, Blocks.DYED_CANDLE_CAKE.blue(), Blocks.DYED_CANDLE_CAKE.black(), Blocks.DYED_CANDLE_CAKE.brown(), Blocks.DYED_CANDLE_CAKE.cyan(), Blocks.DYED_CANDLE_CAKE.gray(), Blocks.DYED_CANDLE_CAKE.green(), Blocks.DYED_CANDLE_CAKE.lightBlue(), Blocks.DYED_CANDLE_CAKE.lightGray(), Blocks.DYED_CANDLE_CAKE.lime(), Blocks.DYED_CANDLE_CAKE.magenta(), Blocks.DYED_CANDLE_CAKE.orange(), Blocks.DYED_CANDLE_CAKE.pink(), Blocks.DYED_CANDLE_CAKE.purple(), Blocks.DYED_CANDLE_CAKE.red(), Blocks.DYED_CANDLE_CAKE.white(), Blocks.DYED_CANDLE_CAKE.yellow(),
                    Blocks.DYED_CANDLE.blue(), Blocks.DYED_CANDLE.black(), Blocks.DYED_CANDLE.brown(), Blocks.DYED_CANDLE.cyan(), Blocks.DYED_CANDLE.gray(), Blocks.DYED_CANDLE.green(), Blocks.DYED_CANDLE.lightBlue(), Blocks.DYED_CANDLE.lightGray(), Blocks.DYED_CANDLE.lime(), Blocks.DYED_CANDLE.magenta(), Blocks.DYED_CANDLE.orange(), Blocks.DYED_CANDLE.pink(), Blocks.DYED_CANDLE.purple(), Blocks.DYED_CANDLE.yellow(),
                    Blocks.SMOOTH_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_STAIRS, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE_WALL,
                    Blocks.ANDESITE_STAIRS, Blocks.ANDESITE_SLAB, Blocks.ANDESITE_WALL, Blocks.POLISHED_ANDESITE_SLAB, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_GRANITE_SLAB, Blocks.POLISHED_GRANITE_STAIRS, Blocks.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE_STAIRS,
                    Blocks.TUFF_SLAB, Blocks.TUFF_STAIRS, Blocks.TUFF_WALL, Blocks.TUFF_BRICK_SLAB, Blocks.TUFF_BRICK_STAIRS, Blocks.TUFF_BRICK_WALL,
                    Blocks.CRACKED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICK_WALL, Blocks.RED_NETHER_BRICKS, Blocks.RED_NETHER_BRICK_SLAB, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICK_WALL,
                    Blocks.STAINED_GLASS.orange(), Blocks.STAINED_GLASS.lightBlue(), Blocks.STAINED_GLASS.yellow(), Blocks.STAINED_GLASS.lime(), Blocks.STAINED_GLASS.pink(), Blocks.STAINED_GLASS.cyan(), Blocks.STAINED_GLASS.purple(), Blocks.STAINED_GLASS.blue(), Blocks.STAINED_GLASS.green(), Blocks.STAINED_GLASS.red(),
                    Blocks.CRIMSON_PRESSURE_PLATE, Blocks.CRIMSON_BUTTON, Blocks.CRIMSON_DOOR, Blocks.CRIMSON_FENCE, Blocks.CRIMSON_FENCE_GATE, Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_SIGN, Blocks.CRIMSON_WALL_SIGN, Blocks.CRIMSON_SLAB, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_TRAPDOOR,
                    Blocks.WARPED_PRESSURE_PLATE, Blocks.WARPED_BUTTON, Blocks.WARPED_DOOR, Blocks.WARPED_FENCE, Blocks.WARPED_FENCE_GATE, Blocks.WARPED_PLANKS, Blocks.WARPED_SIGN, Blocks.WARPED_WALL_SIGN, Blocks.WARPED_SLAB, Blocks.WARPED_STAIRS, Blocks.WARPED_TRAPDOOR,
                    Blocks.SCAFFOLDING, Blocks.CHERRY_SIGN, Blocks.CHERRY_WALL_SIGN, Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.ACACIA_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.DARK_OAK_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.JUNGLE_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.MANGROVE_SIGN, Blocks.MANGROVE_WALL_SIGN, Blocks.SLIME_BLOCK, Blocks.SPONGE, Blocks.TINTED_GLASS,
                    Blocks.ACACIA_HANGING_SIGN, Blocks.ACACIA_WALL_HANGING_SIGN, Blocks.BAMBOO_HANGING_SIGN, Blocks.BAMBOO_WALL_HANGING_SIGN, Blocks.BIRCH_HANGING_SIGN, Blocks.BIRCH_WALL_HANGING_SIGN, Blocks.CHERRY_HANGING_SIGN, Blocks.CHERRY_WALL_HANGING_SIGN, Blocks.CRIMSON_HANGING_SIGN, Blocks.CRIMSON_WALL_HANGING_SIGN, Blocks.DARK_OAK_HANGING_SIGN, Blocks.DARK_OAK_WALL_HANGING_SIGN, Blocks.JUNGLE_HANGING_SIGN, Blocks.JUNGLE_WALL_HANGING_SIGN, Blocks.MANGROVE_HANGING_SIGN, Blocks.MANGROVE_WALL_HANGING_SIGN, Blocks.OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN, Blocks.SPRUCE_HANGING_SIGN, Blocks.SPRUCE_WALL_HANGING_SIGN, Blocks.WARPED_HANGING_SIGN, Blocks.WARPED_WALL_HANGING_SIGN,
                    Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_STAIRS, Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.PETRIFIED_OAK_SLAB,
                    Blocks.STRIPPED_BAMBOO_BLOCK, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_CHERRY_WOOD, Blocks.STRIPPED_ACACIA_WOOD, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_WOOD, Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_STEM, Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_WOOD, Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_WARPED_STEM,
                    Blocks.SHULKER_BOX, Blocks.DYED_SHULKER_BOX.black(), Blocks.DYED_SHULKER_BOX.blue(), Blocks.DYED_SHULKER_BOX.brown(), Blocks.DYED_SHULKER_BOX.cyan(), Blocks.DYED_SHULKER_BOX.gray(), Blocks.DYED_SHULKER_BOX.green(), Blocks.DYED_SHULKER_BOX.lightBlue(), Blocks.DYED_SHULKER_BOX.lightGray(), Blocks.DYED_SHULKER_BOX.lime(), Blocks.DYED_SHULKER_BOX.magenta(), Blocks.DYED_SHULKER_BOX.orange(), Blocks.DYED_SHULKER_BOX.pink(), Blocks.DYED_SHULKER_BOX.purple(), Blocks.DYED_SHULKER_BOX.red(), Blocks.DYED_SHULKER_BOX.white(), Blocks.DYED_SHULKER_BOX.yellow(),
                    Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON, Blocks.ACTIVATOR_RAIL, Blocks.BEACON, Blocks.BEEHIVE, Blocks.REPEATING_COMMAND_BLOCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.IRON_BLOCK, Blocks.NETHERITE_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.CONDUIT, Blocks.DAYLIGHT_DETECTOR, Blocks.DETECTOR_RAIL, Blocks.DRIED_KELP_BLOCK, Blocks.DROPPER, Blocks.ENCHANTING_TABLE,
                    Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD, Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD, Blocks.DRAGON_WALL_HEAD, Blocks.DRAGON_HEAD, Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD, Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD, Blocks.SKELETON_WALL_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL, Blocks.HEAVY_CORE,
                    Blocks.HONEY_BLOCK, Blocks.HONEYCOMB_BLOCK, Blocks.JUKEBOX, Blocks.LODESTONE, Blocks.OBSERVER, Blocks.POWERED_RAIL, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE, Blocks.JUNGLE_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE, Blocks.MANGROVE_PRESSURE_PLATE, Blocks.CRIMSON_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE, Blocks.RESPAWN_ANCHOR, Blocks.CALIBRATED_SCULK_SENSOR, Blocks.SNIFFER_EGG,
                    Blocks.RESIN_BLOCK, Blocks.RESIN_BRICKS, Blocks.RESIN_BRICK_SLAB, Blocks.RESIN_BRICK_WALL, Blocks.RESIN_BRICK_STAIRS, Blocks.CHISELED_RESIN_BRICKS, Blocks.POTTED_CLOSED_EYEBLOSSOM, Blocks.POTTED_OPEN_EYEBLOSSOM, Blocks.POTTED_PALE_OAK_SAPLING, Blocks.PALE_OAK_SAPLING, Blocks.PALE_OAK_BUTTON, Blocks.PALE_OAK_DOOR, Blocks.PALE_OAK_FENCE, Blocks.PALE_OAK_FENCE_GATE, Blocks.PALE_OAK_PLANKS, Blocks.PALE_OAK_PRESSURE_PLATE, Blocks.PALE_OAK_HANGING_SIGN, Blocks.PALE_OAK_SIGN, Blocks.PALE_OAK_WALL_SIGN, Blocks.PALE_OAK_WALL_HANGING_SIGN, Blocks.PALE_OAK_SLAB, Blocks.PALE_OAK_STAIRS, Blocks.PALE_OAK_TRAPDOOR, Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD,
                    Blocks.COPPER_BARS.weathering().unaffected(),Blocks.COPPER_BARS.waxed().unaffected(),Blocks.COPPER_BARS.weathering().exposed(),Blocks.COPPER_BARS.waxed().exposed(),Blocks.COPPER_BARS.weathering().weathered(),Blocks.COPPER_BARS.waxed().weathered(),Blocks.COPPER_BARS.weathering().oxidized(),Blocks.COPPER_BARS.waxed().oxidized(), Blocks.COPPER_CHAIN.weathering().unaffected(),Blocks.COPPER_CHAIN.waxed().unaffected(),Blocks.COPPER_CHAIN.weathering().exposed(),Blocks.COPPER_CHAIN.waxed().exposed(),Blocks.COPPER_CHAIN.weathering().weathered(),Blocks.COPPER_CHAIN.waxed().weathered(),Blocks.COPPER_CHAIN.weathering().oxidized(),Blocks.COPPER_CHAIN.waxed().oxidized(), Blocks.COPPER_LANTERN.weathering().unaffected(), Blocks.COPPER_LANTERN.waxed().unaffected(), Blocks.COPPER_LANTERN.weathering().exposed(), Blocks.COPPER_LANTERN.waxed().exposed(), Blocks.COPPER_LANTERN.weathering().weathered(), Blocks.COPPER_LANTERN.waxed().weathered(), Blocks.COPPER_LANTERN.weathering().oxidized(), Blocks.COPPER_LANTERN.waxed().oxidized(),
                    Blocks.COPPER_CHEST.weathering().unaffected(),Blocks.COPPER_CHEST.weathering().exposed(),Blocks.COPPER_CHEST.weathering().oxidized(),Blocks.COPPER_CHEST.weathering().weathered(), Blocks.COPPER_CHEST.waxed().unaffected(),Blocks.COPPER_CHEST.waxed().exposed(),Blocks.COPPER_CHEST.waxed().oxidized(),Blocks.COPPER_CHEST.waxed().weathered(), Blocks.COPPER_GOLEM_STATUE.weathering().unaffected(), Blocks.COPPER_GOLEM_STATUE.weathering().exposed(), Blocks.COPPER_GOLEM_STATUE.weathering().weathered(), Blocks.COPPER_GOLEM_STATUE.weathering().oxidized(), Blocks.COPPER_GOLEM_STATUE.waxed().unaffected(), Blocks.COPPER_GOLEM_STATUE.waxed().exposed(), Blocks.COPPER_GOLEM_STATUE.waxed().weathered(), Blocks.COPPER_GOLEM_STATUE.waxed().oxidized(), Blocks.COPPER_TORCH, Blocks.COPPER_WALL_TORCH,
                    Blocks.LIGHTNING_ROD.weathering().unaffected(), Blocks.LIGHTNING_ROD.weathering().exposed(), Blocks.LIGHTNING_ROD.weathering().weathered(), Blocks.LIGHTNING_ROD.weathering().oxidized(), Blocks.LIGHTNING_ROD.waxed().unaffected(), Blocks.LIGHTNING_ROD.waxed().exposed(), Blocks.LIGHTNING_ROD.waxed().weathered(), Blocks.LIGHTNING_ROD.waxed().oxidized(),
                    Blocks.OAK_SHELF, Blocks.DARK_OAK_SHELF, Blocks.PALE_OAK_SHELF, Blocks.ACACIA_SHELF, Blocks.BAMBOO_SHELF, Blocks.BIRCH_SHELF, Blocks.CHERRY_SHELF, Blocks.CRIMSON_SHELF, Blocks.JUNGLE_SHELF, Blocks.MANGROVE_SHELF, Blocks.SPRUCE_SHELF, Blocks.WARPED_SHELF,
                    Blocks.GOLDEN_DANDELION, Blocks.POTTED_GOLDEN_DANDELION,
                    Blocks.CINNABAR_SLAB, Blocks.CINNABAR_STAIRS, Blocks.CINNABAR_WALL, Blocks.CINNABAR_BRICKS, Blocks.CINNABAR_BRICK_SLAB, Blocks.CINNABAR_BRICK_STAIRS, Blocks.CINNABAR_BRICK_WALL, Blocks.CHISELED_CINNABAR, Blocks.POLISHED_CINNABAR, Blocks.POLISHED_CINNABAR_SLAB, Blocks.POLISHED_CINNABAR_STAIRS, Blocks.POLISHED_CINNABAR_WALL,
                    Blocks.SULFUR_SLAB, Blocks.SULFUR_STAIRS, Blocks.SULFUR_WALL, Blocks.SULFUR_BRICKS, Blocks.SULFUR_BRICK_SLAB, Blocks.SULFUR_BRICK_STAIRS, Blocks.SULFUR_BRICK_WALL, Blocks.CHISELED_SULFUR, Blocks.POLISHED_SULFUR, Blocks.POLISHED_SULFUR_SLAB, Blocks.POLISHED_SULFUR_STAIRS, Blocks.POLISHED_SULFUR_WALL
            )
            .visible(() -> (mode.get() == Modes.UnnaturalBlocks))
            .build()
    );
    private final Setting<List<Block>> Blawcks2 = sglists.add(new BlockListSetting.Builder()
            .name("Custom Block List")
            .description("Edit as needed.")
            .defaultValue()
            .visible(() -> (mode.get() == Modes.Custom))
            .build()
    );


    public BlockListMineCommand() {
        super(Trouser.Main,"BlockList#MineCommand", "Adds a custom #mine command to your message history containing all the blocks in the blocklist that are in the chunk you are in. Press T then up arrow, then ENTER key to execute the command. BETTER CHAT module is recommended for infinitely long commands.");
    }
    @Override
    public void onActivate() {
        String blockListString = "";
        Set<BlockState> addedBlocks = new HashSet<>();

        ChunkAccess playerchunk = mc.level.getChunk(mc.player.blockPosition());
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        ChunkAccess playerchunk2 = mc.level.getChunk(new BlockPos(mc.player.getBlockX()+16, mc.player.getBlockY(), mc.player.getBlockZ()));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk2.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        ChunkAccess playerchunk3 = mc.level.getChunk(new BlockPos(mc.player.getBlockX()-16, mc.player.getBlockY(), mc.player.getBlockZ()));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk3.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        ChunkAccess playerchunk4 = mc.level.getChunk(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()+16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk4.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        ChunkAccess playerchunk5 = mc.level.getChunk(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()-16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk5.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        ChunkAccess playerchunk6 = mc.level.getChunk(new BlockPos(mc.player.getBlockX()-16, mc.player.getBlockY(), mc.player.getBlockZ()-16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk6.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        ChunkAccess playerchunk7 = mc.level.getChunk(new BlockPos(mc.player.getBlockX()+16, mc.player.getBlockY(), mc.player.getBlockZ()+16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk7.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        ChunkAccess playerchunk8 = mc.level.getChunk(new BlockPos(mc.player.getBlockX()-16, mc.player.getBlockY(), mc.player.getBlockZ()+16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk8.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        ChunkAccess playerchunk9 = mc.level.getChunk(new BlockPos(mc.player.getBlockX()+16, mc.player.getBlockY(), mc.player.getBlockZ()-16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.level.getMinY() && y <= mc.level.getMaxY()) {
                        BlockState blockState = playerchunk9.getBlockState(new BlockPos(x, y, z));
                        if (blockState.getBlock() != Blocks.AIR && ((Blawcks1.get().contains(blockState.getBlock()) && mode.get() == Modes.UnnaturalBlocks) || (mode.get() == Modes.Custom && Blawcks2.get().contains(blockState.getBlock())))) {
                            if (!addedBlocks.contains(blockState)){
                                blockListString += blockState.getBlock().asItem().toString() + " ";
                                addedBlocks.add(blockState);
                            }
                        }
                    }
                }
            }
        }

        if (!blockListString.isEmpty()) {
            String[] blockNames = blockListString.split(" ");
            Set<String> uniqueBlockNames = new HashSet<>(Arrays.asList(blockNames));
            blockListString = String.join(" ", uniqueBlockNames);
            mc.gui.hud.getChat().addRecentChat("#mine " + blockListString);
            ChatUtils.sendMsg(Component.nullToEmpty("Press T, then the up key, then ENTER to execute the #mine command. **REQUIRES BARITONE**"));
        } else if (blockListString.isEmpty()) error("No blocks in the list within range.");
        toggle();
    }
    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        //turn it off if it was on prior to logging in
        toggle();
    }
    public enum Modes {
        Custom, UnnaturalBlocks
    }
}