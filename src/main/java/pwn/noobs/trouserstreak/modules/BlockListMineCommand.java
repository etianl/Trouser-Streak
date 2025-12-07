package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
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
                    Blocks.HONEY_BLOCK, Blocks.HONEYCOMB_BLOCK, Blocks.JUKEBOX, Blocks.LIGHTNING_ROD, Blocks.LODESTONE, Blocks.OBSERVER, Blocks.POWERED_RAIL, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE, Blocks.JUNGLE_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE, Blocks.MANGROVE_PRESSURE_PLATE, Blocks.CRIMSON_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE, Blocks.RESPAWN_ANCHOR, Blocks.CALIBRATED_SCULK_SENSOR, Blocks.SNIFFER_EGG,
                    Blocks.RESIN_BLOCK, Blocks.RESIN_BRICKS, Blocks.RESIN_BRICK_SLAB, Blocks.RESIN_BRICK_WALL, Blocks.RESIN_BRICK_STAIRS, Blocks.CHISELED_RESIN_BRICKS, Blocks.POTTED_CLOSED_EYEBLOSSOM, Blocks.POTTED_OPEN_EYEBLOSSOM, Blocks.POTTED_PALE_OAK_SAPLING, Blocks.PALE_OAK_SAPLING, Blocks.PALE_OAK_BUTTON, Blocks.PALE_OAK_DOOR, Blocks.PALE_OAK_FENCE, Blocks.PALE_OAK_FENCE_GATE, Blocks.PALE_OAK_PLANKS, Blocks.PALE_OAK_PRESSURE_PLATE, Blocks.PALE_OAK_HANGING_SIGN, Blocks.PALE_OAK_SIGN, Blocks.PALE_OAK_WALL_SIGN, Blocks.PALE_OAK_WALL_HANGING_SIGN, Blocks.PALE_OAK_SLAB, Blocks.PALE_OAK_STAIRS, Blocks.PALE_OAK_TRAPDOOR, Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD,
                    Blocks.COPPER_BARS.unaffected(),Blocks.COPPER_BARS.waxed(),Blocks.COPPER_BARS.exposed(),Blocks.COPPER_BARS.waxedExposed(),Blocks.COPPER_BARS.weathered(),Blocks.COPPER_BARS.waxedWeathered(),Blocks.COPPER_BARS.oxidized(),Blocks.COPPER_BARS.waxedOxidized(), Blocks.COPPER_CHAINS.unaffected(),Blocks.COPPER_CHAINS.waxed(),Blocks.COPPER_CHAINS.exposed(),Blocks.COPPER_CHAINS.waxedExposed(),Blocks.COPPER_CHAINS.weathered(),Blocks.COPPER_CHAINS.waxedWeathered(),Blocks.COPPER_CHAINS.oxidized(),Blocks.COPPER_CHAINS.waxedOxidized(), Blocks.COPPER_LANTERNS.unaffected(), Blocks.COPPER_LANTERNS.waxed(), Blocks.COPPER_LANTERNS.exposed(), Blocks.COPPER_LANTERNS.waxedExposed(), Blocks.COPPER_LANTERNS.weathered(), Blocks.COPPER_LANTERNS.waxedWeathered(), Blocks.COPPER_LANTERNS.oxidized(), Blocks.COPPER_LANTERNS.waxedOxidized(),
                    Blocks.COPPER_CHEST,Blocks.EXPOSED_COPPER_CHEST,Blocks.OXIDIZED_COPPER_CHEST,Blocks.WEATHERED_COPPER_CHEST, Blocks.WAXED_COPPER_CHEST,Blocks.WAXED_EXPOSED_COPPER_CHEST,Blocks.WAXED_OXIDIZED_COPPER_CHEST,Blocks.WAXED_WEATHERED_COPPER_CHEST, Blocks.COPPER_GOLEM_STATUE, Blocks.EXPOSED_COPPER_GOLEM_STATUE, Blocks.WEATHERED_COPPER_GOLEM_STATUE, Blocks.OXIDIZED_COPPER_GOLEM_STATUE, Blocks.WAXED_COPPER_GOLEM_STATUE, Blocks.WAXED_EXPOSED_COPPER_GOLEM_STATUE, Blocks.WAXED_WEATHERED_COPPER_GOLEM_STATUE, Blocks.WAXED_OXIDIZED_COPPER_GOLEM_STATUE, Blocks.COPPER_TORCH, Blocks.COPPER_WALL_TORCH,
                    Blocks.OAK_SHELF, Blocks.DARK_OAK_SHELF, Blocks.PALE_OAK_SHELF, Blocks.ACACIA_SHELF, Blocks.BAMBOO_SHELF, Blocks.BIRCH_SHELF, Blocks.CHERRY_SHELF, Blocks.CRIMSON_SHELF, Blocks.JUNGLE_SHELF, Blocks.MANGROVE_SHELF, Blocks.SPRUCE_SHELF, Blocks.WARPED_SHELF
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

        Chunk playerchunk = mc.world.getChunk(mc.player.getBlockPos());
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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

        Chunk playerchunk2 = mc.world.getChunk(new BlockPos(mc.player.getBlockX()+16, mc.player.getBlockY(), mc.player.getBlockZ()));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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

        Chunk playerchunk3 = mc.world.getChunk(new BlockPos(mc.player.getBlockX()-16, mc.player.getBlockY(), mc.player.getBlockZ()));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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

        Chunk playerchunk4 = mc.world.getChunk(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()+16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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

        Chunk playerchunk5 = mc.world.getChunk(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()-16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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

        Chunk playerchunk6 = mc.world.getChunk(new BlockPos(mc.player.getBlockX()-16, mc.player.getBlockY(), mc.player.getBlockZ()-16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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

        Chunk playerchunk7 = mc.world.getChunk(new BlockPos(mc.player.getBlockX()+16, mc.player.getBlockY(), mc.player.getBlockZ()+16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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

        Chunk playerchunk8 = mc.world.getChunk(new BlockPos(mc.player.getBlockX()-16, mc.player.getBlockY(), mc.player.getBlockZ()+16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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

        Chunk playerchunk9 = mc.world.getChunk(new BlockPos(mc.player.getBlockX()+16, mc.player.getBlockY(), mc.player.getBlockZ()-16));
        for (int x = 0; x < 16; x++) {
            for (int y = mc.player.getBlockY()-range.get(); y < mc.player.getBlockY()+range.get(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (y >= mc.world.getBottomY() && y <= mc.world.getTopYInclusive()) {
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
            mc.inGameHud.getChatHud().addToMessageHistory("#mine " + blockListString);
            ChatUtils.sendMsg(Text.of("Press T, then the up key, then ENTER to execute the #mine command. **REQUIRES BARITONE**"));
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