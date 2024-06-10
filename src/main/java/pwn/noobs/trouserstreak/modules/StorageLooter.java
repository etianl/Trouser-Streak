//Made by etianl
package pwn.noobs.trouserstreak.modules;

import net.minecraft.component.type.FoodComponents;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.lang.reflect.Field;
import java.util.*;

public class StorageLooter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoLoot = settings.createGroup("Auto Open Options");
    private final SettingGroup sgAutoSteal = settings.createGroup("Steal On Tick Options");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final Setting<Boolean> disconnectdisable = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> stopLoot = sgGeneral.add(new BoolSetting.Builder()
            .name("Stop Looting on Full Inv")
            .description("Disables looting when inventory is full")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> swapStacks = sgGeneral.add(new BoolSetting.Builder()
            .name("Swap Lesser Stacks for Better Stacks")
            .description("Applies to the Food, Block, And Wood item lists as well as equipment selected in the ")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> moveOverlimitToContainer = sgGeneral.add(new BoolSetting.Builder()
            .name("Move Overlimit Items to Container")
            .description("If enabled, items over the set limits will be moved to the container if there are empty slots.")
            .defaultValue(true)
            .build());
    private final Setting<List<Item>> containerList = sgGeneral.add(new ItemListSetting.Builder()
            .name("Containers to loot from")
            .description("Select the containers to loot.")
            .defaultValue(Arrays.asList(Items.CHEST, Items.TRAPPED_CHEST, Items.BARREL, Items.SHULKER_BOX, Items.HOPPER, Items.DISPENSER, Items.DROPPER, Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER, Items.BREWING_STAND, Items.CHEST_MINECART))
            .filter(this::isValidContainer)
            .build());
    private final Setting<Integer> maxClicksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("Max Clicks Per Tick")
            .description("Maximum number of clicks per tick when moving items.")
            .defaultValue(100)
            .sliderRange(1, 500)
            .build());
    private final Setting<Boolean> autoloot = sgAutoLoot.add(new BoolSetting.Builder()
            .name("Auto Open Storage Containers")
            .description("Opens and loots storage containers within reach automatically.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> delay = sgAutoLoot.add(new IntSetting.Builder()
            .name("delay between opens")
            .description("Delay in ticks between opening chests.")
            .defaultValue(5)
            .sliderRange(0, 20)
            .min(0)
            .visible(() -> autoloot.get())
            .build());
    private final Setting<Integer> opendelay = sgAutoLoot.add(new IntSetting.Builder()
            .name("held open delay")
            .description("Delay in ticks for how long the chest is held open.")
            .defaultValue(10)
            .sliderRange(0, 20)
            .min(0)
            .visible(() -> autoloot.get())
            .build());
    private final Setting<Modes> mode = sgAutoLoot.add(new EnumSetting.Builder<Modes>()
            .name("Reach Shape")
            .description("the shape of your reach")
            .defaultValue(Modes.Sphere)
            .visible(() -> autoloot.get())
            .build());
    private final Setting<Double> spherereach = sgAutoLoot.add(new DoubleSetting.Builder()
            .name("Sphere Range")
            .description("Your Range, in blocks.")
            .defaultValue(5)
            .sliderRange(1, 5)
            .min(1)
            .visible(() -> mode.get() == Modes.Sphere && autoloot.get())
            .build());
    private final Setting<Integer> boxreach = sgAutoLoot.add(new IntSetting.Builder()
            .name("Box Range")
            .description("Your Range, in blocks.")
            .defaultValue(4)
            .sliderRange(1, 4)
            .min(1)
            .visible(() -> mode.get() == Modes.Box && autoloot.get())
            .build());
    private final Setting<Boolean> autosteal = sgAutoSteal.add(new BoolSetting.Builder()
            .name("Steal On Tick")
            .description("Steals the items on tick from already open containers.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> autoStealDelay = sgAutoSteal.add(new IntSetting.Builder()
            .name("AutoSteal Delay")
            .description("Delay in ticks between stealing items from open containers.")
            .defaultValue(1)
            .sliderRange(0, 20)
            .build());
    private final Setting<Integer> minLootableStackSize = sgItems.add(new IntSetting.Builder()
            .name("Min Lootable Stack Size %")
            .description("Minimum percentage to a full stack that is eligible for looting (0-100)")
            .defaultValue(0)
            .sliderRange(0, 100)
            .build());
    // Items
    private final Setting<List<Item>> itemList = sgItems.add(new ItemListSetting.Builder()
            .name("Items with limits to loot")
            .description("Select the items to loot.")
            .defaultValue(Arrays.asList(
                    Items.BUCKET,
                    Items.LAVA_BUCKET,
                    Items.WATER_BUCKET,
                    Items.CRAFTING_TABLE,
                    Items.STICK,
                    Items.TNT,
                    Items.GUNPOWDER,
                    Items.SAND,
                    Items.IRON_INGOT,
                    Items.IRON_BLOCK,
                    Items.DIAMOND,
                    Items.DIAMOND_BLOCK,
                    Items.BEACON,
                    Items.FLINT,
                    Items.WITHER_SKELETON_SKULL,
                    Items.SOUL_SAND,
                    Items.NAME_TAG,
                    Items.FLINT_AND_STEEL,
                    Items.DIAMOND_SWORD,
                    Items.DIAMOND_SHOVEL,
                    Items.DIAMOND_PICKAXE,
                    Items.DIAMOND_AXE,
                    Items.DIAMOND_HOE,
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS,
                    Items.DIAMOND_BOOTS,
                    Items.SHULKER_BOX
                    ))
            .filter(this::isValidLootItem)
            .build()
    );
    private final Setting<Boolean> hidelimits = sgItems.add(new BoolSetting.Builder()
            .name("Hide Item Limits Setting")
            .description("Prevent menu too big.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<String>> limitList = sgItems.add(new StringListSetting.Builder()
            .name("Item Limits")
            .description("List of Limits for Items. Order the limits in order of the Loot Items list and put a note with the limit for the item.")
            .defaultValue(Arrays.asList(
                    "12 buckets", "2 lavabucket", "2 waterbucket", "64 crafting tables", "64 sticks", "256 TNT", "64 gunpowder", "64 sand", "64 iron", "64 ironblocks",
                    "64 diamonds", "64 diamondblocks", "64 flint", "64 beacon", "64 spooky skulls", "64 soul sand", "64 nametag", "3 flint and steel", "1 sword", "1 shovel", "3 pickaxe", "1 axe", "1 hoe", "1 helmet", "1 chestplate", "1 leggings", "1 boots", "2 shulker"
            ))
            .visible(() -> !hidelimits.get())
            .build()
    );
    private final Setting<List<Item>> blockItemList = sgItems.add(new ItemListSetting.Builder()
            .name("Blocks to Loot")
            .description("Select the blocks to loot.")
            .defaultValue(Arrays.asList(
                    // Stone blocks
                    Items.GRASS_BLOCK, Items.DIRT, Items.COARSE_DIRT, Items.BEDROCK, Items.BARRIER, Items.COBBLESTONE, Items.STONE, Items.ANDESITE, Items.POLISHED_ANDESITE, Items.GRANITE, Items.POLISHED_GRANITE, Items.DIORITE, Items.POLISHED_DIORITE, Items.SANDSTONE, Items.CHISELED_SANDSTONE, Items.CUT_SANDSTONE, Items.SMOOTH_SANDSTONE, Items.RED_SANDSTONE, Items.CHISELED_RED_SANDSTONE, Items.CUT_RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE, Items.STONE_BRICKS, Items.CRACKED_STONE_BRICKS, Items.CHISELED_STONE_BRICKS, Items.MOSSY_STONE_BRICKS, Items.SMOOTH_STONE, Items.TUFF, Items.CALCITE, Items.DRIPSTONE_BLOCK,

                    // Brick blocks
                    Items.BRICKS, Items.NETHER_BRICKS, Items.CRACKED_NETHER_BRICKS, Items.CHISELED_NETHER_BRICKS, Items.RED_NETHER_BRICKS, Items.PRISMARINE, Items.PRISMARINE_BRICKS, Items.DARK_PRISMARINE, Items.PURPUR_BLOCK, Items.PURPUR_PILLAR, Items.END_STONE, Items.END_STONE_BRICKS, Items.BLACKSTONE, Items.POLISHED_BLACKSTONE, Items.POLISHED_BLACKSTONE_BRICKS, Items.CHISELED_POLISHED_BLACKSTONE, Items.CRACKED_POLISHED_BLACKSTONE_BRICKS, Items.GILDED_BLACKSTONE, Items.DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES, Items.CHISELED_DEEPSLATE, Items.CRACKED_DEEPSLATE_BRICKS, Items.CRACKED_DEEPSLATE_TILES, Items.MUD_BRICKS,

                    // Quartz blocks
                    Items.QUARTZ_BLOCK, Items.QUARTZ_PILLAR, Items.SMOOTH_QUARTZ, Items.CHISELED_QUARTZ_BLOCK,

                    // Terracotta blocks
                    Items.TERRACOTTA, Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA, Items.LIGHT_BLUE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA, Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA, Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA, Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA, Items.BLACK_TERRACOTTA,

                    // Concrete blocks
                    Items.WHITE_CONCRETE, Items.ORANGE_CONCRETE, Items.MAGENTA_CONCRETE, Items.LIGHT_BLUE_CONCRETE, Items.YELLOW_CONCRETE, Items.LIME_CONCRETE, Items.PINK_CONCRETE, Items.GRAY_CONCRETE, Items.LIGHT_GRAY_CONCRETE, Items.CYAN_CONCRETE, Items.PURPLE_CONCRETE, Items.BLUE_CONCRETE, Items.BROWN_CONCRETE, Items.GREEN_CONCRETE, Items.RED_CONCRETE, Items.BLACK_CONCRETE,

                    // Glass blocks
                    Items.GLASS, Items.TINTED_GLASS, Items.WHITE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS, Items.LIME_STAINED_GLASS, Items.PINK_STAINED_GLASS, Items.GRAY_STAINED_GLASS, Items.LIGHT_GRAY_STAINED_GLASS, Items.CYAN_STAINED_GLASS, Items.PURPLE_STAINED_GLASS, Items.BLUE_STAINED_GLASS, Items.BROWN_STAINED_GLASS, Items.GREEN_STAINED_GLASS, Items.RED_STAINED_GLASS, Items.BLACK_STAINED_GLASS,

                    // Miscellaneous blocks
                    Items.OBSIDIAN, Items.CRYING_OBSIDIAN, Items.GLOWSTONE, Items.SEA_LANTERN, Items.JACK_O_LANTERN, Items.HONEYCOMB_BLOCK, Items.BONE_BLOCK, Items.NETHERRACK, Items.BASALT, Items.POLISHED_BASALT, Items.SMOOTH_BASALT, Items.SOUL_SOIL, Items.AMETHYST_BLOCK, Items.COPPER_BLOCK, Items.EXPOSED_COPPER, Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER, Items.CUT_COPPER, Items.EXPOSED_CUT_COPPER, Items.WEATHERED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER, Items.WAXED_COPPER_BLOCK, Items.WAXED_EXPOSED_COPPER, Items.WAXED_WEATHERED_COPPER, Items.WAXED_OXIDIZED_COPPER, Items.WAXED_CUT_COPPER, Items.WAXED_EXPOSED_CUT_COPPER, Items.WAXED_WEATHERED_CUT_COPPER, Items.WAXED_OXIDIZED_CUT_COPPER, Items.MOSS_BLOCK, Items.PACKED_MUD, Items.SHROOMLIGHT
            ))
            .filter(this::isValidBlockItem)
            .build()
    );
    private final Setting<Integer> blockItemListLimit = sgItems.add(new IntSetting.Builder()
            .name("Block Limit")
            .description("Limit on amount of blocks to loot.")
            .defaultValue(384)
            .sliderRange(0, 2304)
            .min(0)
            .build()
    );
    private final Setting<List<Item>> woodItemList = sgItems.add(new ItemListSetting.Builder()
            .name("Wood Items to Loot")
            .description("Select the wood items to loot.")
            .defaultValue(Arrays.asList(
                    Items.OAK_LOG, Items.BIRCH_LOG, Items.CHERRY_LOG, Items.DARK_OAK_LOG, Items.JUNGLE_LOG,Items.MANGROVE_LOG, Items.SPRUCE_LOG, Items.CRIMSON_STEM, Items.WARPED_STEM,
                    Items.STRIPPED_OAK_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_CHERRY_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_MANGROVE_LOG, Items.STRIPPED_SPRUCE_LOG, Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM,
                    Items.OAK_WOOD, Items.OAK_WOOD, Items.BIRCH_WOOD, Items.CHERRY_WOOD, Items.DARK_OAK_WOOD, Items.JUNGLE_WOOD, Items.MANGROVE_WOOD, Items.SPRUCE_WOOD, Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE,
                    Items.STRIPPED_OAK_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.STRIPPED_CHERRY_WOOD, Items.STRIPPED_DARK_OAK_WOOD, Items.STRIPPED_JUNGLE_WOOD, Items.STRIPPED_MANGROVE_WOOD, Items.STRIPPED_SPRUCE_WOOD, Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE,
                    Items.BAMBOO_BLOCK, Items.STRIPPED_BAMBOO_BLOCK,
                    Items.OAK_PLANKS, Items.BIRCH_PLANKS, Items.CHERRY_PLANKS, Items.DARK_OAK_PLANKS, Items.JUNGLE_PLANKS, Items.MANGROVE_PLANKS, Items.SPRUCE_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS
            ))
            .filter(this::isValidWoodItem)
            .build()
    );
    private final Setting<Integer> woodItemListLimit = sgItems.add(new IntSetting.Builder()
            .name("Wood Items Limit")
            .description("Limit on amount of wood items to loot.")
            .defaultValue(64)
            .sliderRange(0, 2304)
            .min(0)
            .build()
    );
    private final Setting<List<Item>> foodItemList = sgItems.add(new ItemListSetting.Builder()
            .name("Food Items to Loot")
            .description("Select the food items to loot.")
            .defaultValue(Arrays.asList(
                    Items.COOKED_PORKCHOP,
                    Items.COOKED_BEEF,
                    Items.COOKED_CHICKEN,
                    Items.COOKED_MUTTON,
                    Items.COOKED_RABBIT,
                    Items.COOKED_COD,
                    Items.COOKED_SALMON,
                    Items.GOLDEN_CARROT,
                    Items.GOLDEN_APPLE,
                    Items.ENCHANTED_GOLDEN_APPLE,
                    Items.BAKED_POTATO,
                    Items.BREAD,
                    Items.PUMPKIN_PIE
            ))
            .filter(this::isValidFoodItem)
            .build()
    );
    private final Setting<Integer> foodItemListLimit = sgItems.add(new IntSetting.Builder()
            .name("Food Items Limit")
            .description("Limit on amount of food items to loot.")
            .defaultValue(64)
            .sliderRange(0, 2304)
            .min(0)
            .build()
    );

    private final Setting<List<Item>> miscItemList = sgItems.add(new ItemListSetting.Builder()
            .name("Miscellaneous items to Loot")
            .description("Select the miscellaneous items to loot.")
            .build()
    );
    private final Setting<Integer> miscItemListLimit = sgItems.add(new IntSetting.Builder()
            .name("Misc Items Limit")
            .description("Limit on amount of misc items to loot.")
            .defaultValue(64)
            .sliderRange(0, 2304)
            .min(0)
            .build()
    );
    private int lastSwappedPlayerSlot = -666;
    private int lastSwappedContainerSlot = -666;
    private final Set<BlockPos> processedChests = new HashSet<>();
    private final Set<Integer> openedEntities = new HashSet<>();
    private final Map<BlockPos, Integer> chestsToProcess = new HashMap<>();
    private BlockPos lastInteractedBlockPos = null;
    private boolean isChestOpen = false;
    private boolean inventoryFullErrorSent = false;
    private int totalClicksThisTick = 0;
    private int ticks;
    private int autoStealTicks;
    private double reach;

    public StorageLooter() {
        super(Trouser.Main, "Storage Looter", "Steals stuff from containers around you.");
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            inventoryFullErrorSent = false;
            ticks = 0;
            autoStealTicks = 0;
            processedChests.clear();
            chestsToProcess.clear();
            openedEntities.clear();
            lastSwappedPlayerSlot = -666;
            lastSwappedContainerSlot = -666;
            if (disconnectdisable.get()) toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        inventoryFullErrorSent = false;
        ticks = 0;
        autoStealTicks = 0;
        processedChests.clear();
        chestsToProcess.clear();
        openedEntities.clear();
        lastSwappedPlayerSlot = -666;
        lastSwappedContainerSlot = -666;
        if (disconnectdisable.get()) toggle();
    }

    @Override
    public void onActivate() {
        inventoryFullErrorSent = false;
        ticks = 0;
        autoStealTicks = 0;
        processedChests.clear();
        chestsToProcess.clear();
        openedEntities.clear();
        lastSwappedPlayerSlot = -666;
        lastSwappedContainerSlot = -666;
    }

    @Override
    public void onDeactivate() {
        inventoryFullErrorSent = false;
        ticks = 0;
        autoStealTicks = 0;
        processedChests.clear();
        chestsToProcess.clear();
        openedEntities.clear();
        lastSwappedPlayerSlot = -666;
        lastSwappedContainerSlot = -666;
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractBlockC2SPacket) {
            lastInteractedBlockPos = ((PlayerInteractBlockC2SPacket) event.packet).getBlockHitResult().getBlockPos();
        }
        else if (event.packet instanceof PlayerInteractEntityC2SPacket) {
            PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket) event.packet;
            Entity entity = mc.world.getEntityById(getEntityId(packet));
            if (entity != null) {
                lastInteractedBlockPos = entity.getBlockPos();
            }
        }
    }
    private int getEntityId(PlayerInteractEntityC2SPacket packet) {
        try {
            Field entityIdField = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
            entityIdField.setAccessible(true);
            return entityIdField.getInt(packet);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if the field cannot be accessed
    }
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (!hasEnoughFreeSlots() && stopLoot.get()) return;
        updateReach();
        int bottomlimit = (int) (mc.player.getBlockY() - Math.round(Math.ceil(reach)));
        if (!isContainerScreen(mc.player.currentScreenHandler)) autoStealTicks = 0;
        if (autosteal.get() && lastInteractedBlockPos != null) {
            BlockState blockState = mc.world.getBlockState(lastInteractedBlockPos);
            Block block = blockState.getBlock();

            if (isValidContainerBlock(block) && containerList.get().contains(block.asItem())) {
                if (mc.player.currentScreenHandler != null && isContainerScreen(mc.player.currentScreenHandler)) {
                    if (autoStealTicks == 0) {
                        processContainerItems();
                    }
                    if (autoStealTicks<autoStealDelay.get()) {
                        autoStealTicks++;
                    }
                    else if (autoStealTicks>=autoStealDelay.get()){
                        processContainerItems();
                        autoStealTicks = 0;
                    }
                    for (Entity entity : mc.world.getEntities()) {
                        if (entity.getBlockPos().equals(lastInteractedBlockPos) && entity instanceof ChestMinecartEntity && containerList.get().contains(Items.CHEST_MINECART)) {
                            if (autoStealTicks == 0) {
                                processContainerItems();
                            }
                            if (autoStealTicks<autoStealDelay.get()) {
                                autoStealTicks++;
                            }
                            else if (autoStealTicks>=autoStealDelay.get()){
                                processContainerItems();
                                autoStealTicks = 0;
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (ticks < delay.get())ticks++;
        if (ticks >= delay.get() && autoloot.get()) {
            List<BlockPos> blocks = getBlocksInRange(bottomlimit);
            blocks.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(mc.player.getPos())));

            for (BlockPos blockPos : blocks) {
                BlockState blockState = mc.world.getBlockState(blockPos);
                Block block = blockState.getBlock();

                if (isValidContainerBlock(block) && !processedChests.contains(blockPos) && !isChestOpen) {
                    if (containerList.get().contains(block.asItem())) {
                        if (block instanceof ChestBlock) {
                            DoubleBlockProperties.Type chestType = ChestBlock.getDoubleBlockType(blockState);
                            if (chestType == DoubleBlockProperties.Type.SINGLE || chestType == DoubleBlockProperties.Type.FIRST) {
                                openContainer(blockPos, block);
                            }
                        } else {
                            openContainer(blockPos, block);
                        }
                    }
                }

                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof ChestMinecartEntity && containerList.get().contains(Items.CHEST_MINECART)) {
                        if (!openedEntities.contains(entity.getId()) && !processedChests.contains(entity.getBlockPos()) && !isChestOpen) {
                            mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
                            chestsToProcess.put(entity.getBlockPos(), opendelay.get());
                            processedChests.add(entity.getBlockPos());
                            openedEntities.add(entity.getId()); // Add the entity ID to the set
                            isChestOpen = true;
                        }
                    }
                }
            }

            ticks = 0;
        }

        processChestsAfterDelay();
    }

    private boolean hasEnoughFreeSlots() {
        int freeSlots = 0;
        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.isEmpty()) {
                freeSlots++;
            }
        }
        if (freeSlots < 1) {
            if (!inventoryFullErrorSent) {
                error("Inventory is full!");
                inventoryFullErrorSent = true;
            }
            return false;
        } else {
            return true;
        }
    }

    private void updateReach() {
        reach = mode.get() == Modes.Sphere ? spherereach.get() : boxreach.get();
    }

    private boolean isContainerScreen(ScreenHandler screenHandler) {
        return screenHandler instanceof GenericContainerScreenHandler
                || screenHandler instanceof ShulkerBoxScreenHandler
                || screenHandler instanceof HopperScreenHandler
                || screenHandler instanceof Generic3x3ContainerScreenHandler
                || screenHandler instanceof FurnaceScreenHandler
                || screenHandler instanceof AbstractFurnaceScreenHandler
                || screenHandler instanceof BlastFurnaceScreenHandler
                || screenHandler instanceof SmokerScreenHandler
                || screenHandler instanceof BrewingStandScreenHandler;
    }

    private void processContainerItems() {
        int playerInvStart = mc.player.currentScreenHandler.slots.size() - 36;

        if (!itemList.get().isEmpty()) {
            processItemList(itemList.get(), playerInvStart);
        }
        if (!miscItemList.get().isEmpty()) {
            processItemList(miscItemList.get(), playerInvStart);
        }
        if (!woodItemList.get().isEmpty()) {
            processItemList(woodItemList.get(), playerInvStart);
        }
        if (!blockItemList.get().isEmpty()) {
            processItemList(blockItemList.get(), playerInvStart);
        }
        if (!foodItemList.get().isEmpty()) {
            processItemList(foodItemList.get(), playerInvStart);
        }

        if (moveOverlimitToContainer.get()) {
            moveExcessItemsToContainer(playerInvStart);
        }

        if (swapStacks.get()) {
            swapSmallerStacksForBigger(playerInvStart);
        }

        totalClicksThisTick = 0; // Reset the total clicks for the next tick
    }

    private void moveExcessItemsToContainer(int playerInvStart) {
        for (int i = playerInvStart; i < mc.player.currentScreenHandler.slots.size(); i++) {
            ItemStack playerStack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (!playerStack.isEmpty()) {
                Item playerItem = playerStack.getItem();
                int maxStackSize = playerItem.getMaxCount();
                int maxCount = getMaxCount(playerItem);
                List<Item> itemlist = getItemList(playerItem);

                int currentCount = getItemCount(playerItem, itemlist);
                if (itemList.get().contains(playerItem)) currentCount = getCurrentItemCount(playerItem);

                // Round up the limit to the nearest full stack size
                if (maxCount < maxStackSize) {
                    maxCount = maxStackSize;
                } else if (maxCount % maxStackSize != 0) {
                    maxCount = (maxCount / maxStackSize + 1) * maxStackSize;
                }

                if (currentCount > maxCount) {
                    int excessAmount = currentCount - maxCount;
                    for (int j = 0; j < playerInvStart; j++) {
                        ItemStack containerStack = mc.player.currentScreenHandler.getSlot(j).getStack();
                        if (containerStack.isEmpty()) {
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                            totalClicksThisTick++;
                            excessAmount -= playerStack.getCount();
                            if (excessAmount <= 0) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private List<Item> getItemList(Item item) {
        if (itemList.get().contains(item)) {
            return itemList.get();
        } else if (miscItemList.get().contains(item)) {
            return miscItemList.get();
        } else if (woodItemList.get().contains(item)) {
            return woodItemList.get();
        } else if (blockItemList.get().contains(item)) {
            return blockItemList.get();
        } else if (foodItemList.get().contains(item)) {
            return foodItemList.get();
        }
        return Collections.emptyList();
    }

    private void processItemList(List<Item> itemList, int playerInvStart) {
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem();
            String itemName = item.toString().toLowerCase();
            if (((itemName.contains("shulker_box") && itemList.contains(Items.SHULKER_BOX)) ||
                    (itemName.contains("_pickaxe") && itemList.contains(Items.DIAMOND_PICKAXE)) ||
                    (itemName.contains("_sword") && itemList.contains(Items.DIAMOND_SWORD)) ||
                    (itemName.contains("_shovel") && itemList.contains(Items.DIAMOND_SHOVEL)) ||
                    (itemName.contains("_axe") && itemList.contains(Items.DIAMOND_AXE)) ||
                    (itemName.contains("_hoe") && itemList.contains(Items.DIAMOND_HOE)) ||
                    (itemName.contains("_helmet") && itemList.contains(Items.DIAMOND_HELMET)) ||
                    (itemName.contains("_chestplate") && itemList.contains(Items.DIAMOND_CHESTPLATE)) ||
                    (itemName.contains("_leggings") && itemList.contains(Items.DIAMOND_LEGGINGS)) ||
                    (itemName.contains("_boots") && itemList.contains(Items.DIAMOND_BOOTS)) ||
                    itemList.contains(item)) && i < playerInvStart) {
                moveItemsWithLimit(i, item, getMaxCount(item), itemList);
            }
        }
    }

    private void moveItemsWithLimit(int slotIndex, Item item, int maxCount, List<Item> itemlist) {
        int currentCount = getItemCount(item, itemlist);
        if (itemlist==itemList.get()) currentCount = getCurrentItemCount(item);
        if (currentCount < maxCount) {
            int amountToMove = maxCount - currentCount;
            moveItems(slotIndex, amountToMove);
        }
    }

    private List<BlockPos> getBlocksInRange(int bottomlimit) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = (int) (mc.player.getBlockX() - Math.round(Math.ceil(reach))); x <= mc.player.getBlockX() + Math.round(Math.ceil(reach)); x++) {
            for (int y = bottomlimit; y <= (mc.player.getBlockY() + 1) + Math.round(Math.ceil(reach)); y++) {
                for (int z = (int) (mc.player.getBlockZ() - Math.round(Math.ceil(reach))); z <= mc.player.getBlockZ() + Math.round(Math.ceil(reach)); z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    Vec3d playerPos1 = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()).toCenterPos();
                    Vec3d playerPos2 = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY() + 1, mc.player.getBlockZ()).toCenterPos();
                    double distance1 = playerPos1.distanceTo(blockPos.toCenterPos());
                    double distance2 = playerPos2.distanceTo(blockPos.toCenterPos());
                    if (mode.get() == Modes.Sphere && (distance1 <= reach || distance2 <= reach)) {
                        blocks.add(blockPos);
                    } else if (mode.get() == Modes.Box) {
                        blocks.add(blockPos);
                    }
                }
            }
        }
        return blocks;
    }

    private boolean isValidContainerBlock(Block block) {
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock || block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof DropperBlock || block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock || block instanceof SmokerBlock || block instanceof BrewingStandBlock;
    }

    private void openContainer(BlockPos blockPos, Block block) {
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(blockPos.toCenterPos(), Direction.UP, blockPos, true));
        chestsToProcess.put(blockPos, opendelay.get());
        processedChests.add(blockPos);
        isChestOpen = true;
    }

    private void processChestsAfterDelay() {
        chestsToProcess.entrySet().removeIf(entry -> {
            int delay = entry.getValue();
            if (delay <= 0) {
                if (mc.player.currentScreenHandler != null) {
                    processContainerItems();
                    mc.player.closeHandledScreen();
                    isChestOpen = false;
                }
                return true;
            } else {
                entry.setValue(delay - 1);
                return false;
            }
        });
    }

    private void swapSmallerStacksForBigger(int playerInvStart) {
        for (int i = playerInvStart; i < mc.player.currentScreenHandler.slots.size(); i++) {
            ItemStack playerStack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (!playerStack.isEmpty()) {
                Item playerItem = playerStack.getItem();
                int playerStackSize = playerStack.getCount();

                for (int j = 0; j < playerInvStart; j++) {
                    ItemStack containerStack = mc.player.currentScreenHandler.getSlot(j).getStack();
                    if (!containerStack.isEmpty()) {
                        Item containerItem = containerStack.getItem();
                        int containerStackSize = containerStack.getCount();

                        if ((containerStackSize > playerStackSize && isSameItemList(playerItem, containerItem) && Math.round(((double)containerStackSize / containerStack.getItem().getMaxCount()) * 100) >= minLootableStackSize.get()) ||
                                (isBetterItem(containerItem, playerItem) && isSameItemList(playerItem, containerItem))) {
                            swapItems(i, j);
                        }
                    }
                }
            }
        }
    }

    private boolean isBetterItem(Item item1, Item item2) {
        List<Item> itemQualityOrder = Arrays.asList(
                Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD,
                Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE,
                Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL,
                Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE,
                Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE,
                Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET,
                Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE,
                Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS,
                Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS
        );

        return itemQualityOrder.indexOf(item1) > itemQualityOrder.indexOf(item2);
    }

    private boolean isSameItemList(Item item1, Item item2) {
        return (blockItemList.get().contains(item1) && blockItemList.get().contains(item2)) ||
                (foodItemList.get().contains(item1) && foodItemList.get().contains(item2)) ||
                (woodItemList.get().contains(item1) && woodItemList.get().contains(item2)) ||
                (item1.toString().toLowerCase().contains("_pickaxe") && item2.toString().toLowerCase().contains("_pickaxe")) ||
                (item1.toString().toLowerCase().contains("_sword") && item2.toString().toLowerCase().contains("_sword")) ||
                (item1.toString().toLowerCase().contains("_shovel") && item2.toString().toLowerCase().contains("_shovel")) ||
                (item1.toString().toLowerCase().contains("_axe") && item2.toString().toLowerCase().contains("_axe")) ||
                (item1.toString().toLowerCase().contains("_hoe") && item2.toString().toLowerCase().contains("_hoe")) ||
                (item1.toString().toLowerCase().contains("_helmet") && item2.toString().toLowerCase().contains("_helmet")) ||
                (item1.toString().toLowerCase().contains("_chestplate") && item2.toString().toLowerCase().contains("_chestplate")) ||
                (item1.toString().toLowerCase().contains("_leggings") && item2.toString().toLowerCase().contains("_leggings")) ||
                (item1.toString().toLowerCase().contains("_boots") && item2.toString().toLowerCase().contains("_boots"));
    }

    private void swapItems(int playerSlotIndex, int containerSlotIndex) {
        if (mc.player.currentScreenHandler != null) {
            // Check if we're trying to swap the same slots back and forth
            if (lastSwappedPlayerSlot != -666 && lastSwappedContainerSlot != -666 &&
                    lastSwappedPlayerSlot == playerSlotIndex &&
                    lastSwappedContainerSlot == containerSlotIndex) {
                return; // Don't swap the same slots back and forth
            }

            int maxClicks = maxClicksPerTick.get();

            // Perform the swap if we have enough clicks left
            if (totalClicksThisTick + 3 <= maxClicks) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, playerSlotIndex, 0, SlotActionType.QUICK_MOVE, mc.player);
                totalClicksThisTick++;

                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, containerSlotIndex, 0, SlotActionType.PICKUP, mc.player);
                totalClicksThisTick++;

                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, playerSlotIndex, 0, SlotActionType.PICKUP, mc.player);
                totalClicksThisTick++;

                lastSwappedPlayerSlot = playerSlotIndex;
                lastSwappedContainerSlot = containerSlotIndex;
            }
        }
    }

    private void moveItems(int slotIndex, int amountToMove) {
        int maxClicks = maxClicksPerTick.get();

        if (mc.player.currentScreenHandler != null && slotIndex >= 0 && slotIndex < mc.player.currentScreenHandler.slots.size()) {
            ItemStack sourceStack = mc.player.currentScreenHandler.getSlot(slotIndex).getStack();
            if (!sourceStack.isEmpty() && Math.round(((double)sourceStack.getCount() / sourceStack.getItem().getMaxCount()) * 100) >= minLootableStackSize.get()) {
                amountToMove = Math.min(amountToMove, sourceStack.getCount());

                // Calculate the number of clicks needed
                int clicksNeeded = (int) Math.ceil((double) amountToMove / sourceStack.getMaxCount());

                for (int i = 0; i < clicksNeeded; i++) {
                    if (totalClicksThisTick >= maxClicks) {
                        return; // Stop moving items if the maximum clicks per tick is reached
                    }
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotIndex, 0, SlotActionType.QUICK_MOVE, mc.player);
                    totalClicksThisTick++;
                }
            }
        }
    }
    private int getCurrentItemCount(Item item) {
        int count = 0;
        String itemName = item.toString().toLowerCase();
        for (ItemStack stack : mc.player.getInventory().main) {
            if (isSameItem(stack.getItem(), item, itemName)) {
                count += stack.getCount();
            }
        }
        if (isSameItem(mc.player.getOffHandStack().getItem(), item, itemName)) {
            count += mc.player.getOffHandStack().getCount();
        }
        for (ItemStack armorStack : mc.player.getArmorItems()) {
            if (isSameItem(armorStack.getItem(), item, itemName)) {
                count += armorStack.getCount();
            }
        }
        return count;
    }
    private int getMaxCount(Item item) {
        int maxCount = 1;
        String itemName = item.toString().toLowerCase();

        if (itemName.contains("_pickaxe") && itemList.get().contains(Items.DIAMOND_PICKAXE)) {
            int index = itemList.get().indexOf(Items.DIAMOND_PICKAXE);
            return getLimitFromList(index);
        } else if (itemName.contains("_sword") && itemList.get().contains(Items.DIAMOND_SWORD)) {
            int index = itemList.get().indexOf(Items.DIAMOND_SWORD);
            return getLimitFromList(index);
        } else if (itemName.contains("_shovel") && itemList.get().contains(Items.DIAMOND_SHOVEL)) {
            int index = itemList.get().indexOf(Items.DIAMOND_SHOVEL);
            return getLimitFromList(index);
        } else if (itemName.contains("_axe") && itemList.get().contains(Items.DIAMOND_AXE)) {
            int index = itemList.get().indexOf(Items.DIAMOND_AXE);
            return getLimitFromList(index);
        } else if (itemName.contains("_hoe") && itemList.get().contains(Items.DIAMOND_HOE)) {
            int index = itemList.get().indexOf(Items.DIAMOND_HOE);
            return getLimitFromList(index);
        } else if (itemName.contains("_helmet") && itemList.get().contains(Items.DIAMOND_HELMET)) {
            int index = itemList.get().indexOf(Items.DIAMOND_HELMET);
            return getLimitFromList(index);
        } else if (itemName.contains("_chestplate") && itemList.get().contains(Items.DIAMOND_CHESTPLATE)) {
            int index = itemList.get().indexOf(Items.DIAMOND_CHESTPLATE);
            return getLimitFromList(index);
        } else if (itemName.contains("_leggings") && itemList.get().contains(Items.DIAMOND_LEGGINGS)) {
            int index = itemList.get().indexOf(Items.DIAMOND_LEGGINGS);
            return getLimitFromList(index);
        } else if (itemName.contains("_boots") && itemList.get().contains(Items.DIAMOND_BOOTS)) {
            int index = itemList.get().indexOf(Items.DIAMOND_BOOTS);
            return getLimitFromList(index);
        } else if (itemName.contains("shulker_box") && itemList.get().contains(Items.SHULKER_BOX)) {
            int index = itemList.get().indexOf(Items.SHULKER_BOX);
            return getLimitFromList(index);
        } else {
            // Check if the item is in the main item list
            int index = itemList.get().indexOf(item);
            if (index != -1) {
                return getLimitFromList(index);
            }
        }

        if (miscItemList.get().contains(item)) {
            return miscItemListLimit.get();
        }
        if (foodItemList.get().contains(item)) {
            return foodItemListLimit.get();
        }
        if (woodItemList.get().contains(item)) {
            return woodItemListLimit.get();
        }
        if (blockItemList.get().contains(item)) {
            return blockItemListLimit.get();
        }

        return maxCount;
    }

    private int getLimitFromList(int index) {
        int maxCount = 1;
        if (index != -1) {
            try {
                String limitString = limitList.get().get(index).replaceAll("[^0-9]", "").trim();
                if (!limitString.isEmpty()) {
                    maxCount = Integer.parseInt(limitString);
                }
            } catch (Exception ignored) {}
        }
        return maxCount;
    }

    private int getItemCount(Item item, List<Item> itemList) {
        int count = 0;
        String itemName = item.toString().toLowerCase();

        for (ItemStack stack : mc.player.getInventory().main) {
            Item stackItem = stack.getItem();
            if (isSameItem(stackItem, item, itemName)) {
                if (stack.isStackable()) {
                    count += stack.getCount();
                } else {
                    count++;
                }
            } else if (itemList.contains(stackItem)) {
                if (stack.isStackable()) {
                    count += stack.getCount();
                } else {
                    count++;
                }
            }
        }
        for (ItemStack stack : mc.player.getArmorItems()) {
            Item stackItem = stack.getItem();
            if (isSameItem(stackItem, item, itemName)) {
                if (stack.isStackable()) {
                    count += stack.getCount();
                } else {
                    count++;
                }
            } else if (itemList.contains(stackItem)) {
                if (stack.isStackable()) {
                    count += stack.getCount();
                } else {
                    count++;
                }
            }
        }

        ItemStack offhandStack = mc.player.getOffHandStack();
        Item offhandItem = offhandStack.getItem();
        if (isSameItem(offhandItem, item, itemName)) {
            if (offhandStack.isStackable()) {
                count += offhandStack.getCount();
            } else {
                count++;
            }
        } else if (itemList.contains(offhandItem)) {
            if (offhandStack.isStackable()) {
                count += offhandStack.getCount();
            } else {
                count++;
            }
        }

        return count;
    }
    private boolean isSameItem(Item stackItem, Item item, String itemName) {
        return stackItem == item || (itemName.contains("shulker_box") && stackItem.toString().toLowerCase().contains("shulker_box")) ||
                (itemName.contains("_pickaxe") && stackItem.toString().toLowerCase().contains("_pickaxe")) ||
                (itemName.contains("_sword") && stackItem.toString().toLowerCase().contains("_sword")) ||
                (itemName.contains("_shovel") && stackItem.toString().toLowerCase().contains("_shovel")) ||
                (itemName.contains("_axe") && stackItem.toString().toLowerCase().contains("_axe")) ||
                (itemName.contains("_hoe") && stackItem.toString().toLowerCase().contains("_hoe")) ||
                (itemName.contains("_helmet") && stackItem.toString().toLowerCase().contains("_helmet")) ||
                (itemName.contains("_chestplate") && stackItem.toString().toLowerCase().contains("_chestplate")) ||
                (itemName.contains("_leggings") && stackItem.toString().toLowerCase().contains("_leggings")) ||
                (itemName.contains("_boots") && stackItem.toString().toLowerCase().contains("_boots"));
    }

    private boolean isValidContainer(Item item) {
        return item == Items.CHEST
                || item == Items.TRAPPED_CHEST
                || item == Items.BARREL
                || item == Items.SHULKER_BOX
                || item == Items.HOPPER
                || item == Items.DISPENSER
                || item == Items.DROPPER
                || item == Items.FURNACE
                || item == Items.BLAST_FURNACE
                || item == Items.SMOKER
                || item == Items.BREWING_STAND
                || item == Items.CHEST_MINECART;
    }

    public enum Modes {
        Sphere, Box
    }

    //item list filters
    private static final Set<String> FOOD_ITEMS = new HashSet<>();

    static {
        try {
            Field[] fields = FoodComponents.class.getFields();
            for (Field field : fields) {
                FOOD_ITEMS.add(field.getName().toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isValidFoodItem(Item item) {
        String itemName = item.toString().toLowerCase();
        return FOOD_ITEMS.contains(itemName);
    }
    private boolean isValidBlockItem(Item item) {
        return item instanceof BlockItem;
    }
    private boolean isValidWoodItem(Item item) {
        String itemName = item.toString().toLowerCase();
        return itemName.contains("_wood") || itemName.contains("log") || itemName.contains("plank") || itemName.contains("hyphae") || itemName.contains("bamboo_block");
    }
    private boolean isValidLootItem(Item item) {
        if (item instanceof BlockItem) {
            Block block = ((BlockItem) item).getBlock();
            if (block instanceof ShulkerBoxBlock) {
                // Exclude colored shulker boxes
                return block == Blocks.SHULKER_BOX;
            }
        }

        List<Item> diamondItems = Arrays.asList(
                Items.DIAMOND_SWORD,
                Items.DIAMOND_SHOVEL,
                Items.DIAMOND_PICKAXE,
                Items.DIAMOND_AXE,
                Items.DIAMOND_HOE,
                Items.DIAMOND_HELMET,
                Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS,
                Items.DIAMOND_BOOTS
        );

        if (item instanceof ToolItem || item instanceof ArmorItem) {
            return diamondItems.contains(item);
        }

        return true;
    }
}