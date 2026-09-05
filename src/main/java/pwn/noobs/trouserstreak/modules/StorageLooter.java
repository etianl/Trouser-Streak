//Made by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.food.Foods;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
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
            .description("Applies to the Food, Block, And Wood item lists as well as equipment selected in the Items with limits list.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> moveOverlimitToContainer = sgGeneral.add(new BoolSetting.Builder()
            .name("Move Overlimit Items to Container")
            .description("If enabled, items over the set limits will be moved to the container if there are empty slots.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> moveJunkToContainer = sgGeneral.add(new BoolSetting.Builder()
            .name("Move Junk Items to Container")
            .description("If enabled, items over the set limits will be moved to the container if there are empty slots.")
            .defaultValue(true)
            .build());
    private final Setting<List<Item>> junkItemList = sgGeneral.add(new ItemListSetting.Builder()
            .name("Junk Items")
            .description("Select the items to get rid of.")
            .defaultValue(Arrays.asList(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("rotten_flesh")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("poisonous_potato")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("spider_eye")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("fermented_spider_eye")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("phantom_membrane")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("breeze_rod")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("nautilus_shell")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("string")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lily_pad")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("beetroot")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("beetroot_seeds")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("melon_seeds")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pumpkin_seeds")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("wheat_seeds")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sugar")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("short_grass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tall_grass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("seagrass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sea_pickle")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("fern")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dead_bush")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("vine")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brown_mushroom")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_mushroom")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("warped_fungus")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crimson_fungus")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("nether_wart")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("ghast_tear")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bowl")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("feather")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sugar_cane")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cactus")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cocoa_beans")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("rabbit_hide")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("rabbit_foot")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("glow_lichen")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sculk_vein")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("glow_item_frame")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("item_frame")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("painting")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("paper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("clock")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("compass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pufferfish")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tropical_fish")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("glistering_melon_slice")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("magma_cream")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lead")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("saddle")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("carrot_on_a_stick")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("warped_fungus_on_a_stick")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("frogspawn")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("turtle_egg")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("turtle_scute")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sniffer_egg")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("armadillo_scute")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("goat_horn")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("book")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("writable_book")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("written_book")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brush"))
            ))
            .visible(moveJunkToContainer::get)
            .build()
    );
    private final Setting<Boolean> nonitemlistjunk = sgGeneral.add(new BoolSetting.Builder()
            .name("Items not on a List are Junk")
            .description("If enabled, items not listed in the item lists (excluding the junk list) are treated as junk items.")
            .defaultValue(false)
            .visible(moveJunkToContainer::get)
            .build());
    private final Setting<List<Item>> containerList = sgGeneral.add(new ItemListSetting.Builder()
            .name("Containers to loot from")
            .description("Select the containers to loot.")
            .defaultValue(Arrays.asList(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chest")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("trapped_chest")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("barrel")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shulker_box")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("hopper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dispenser")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dropper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("furnace")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blast_furnace")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("smoker")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brewing_stand")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crafter")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chest_minecart"))))
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
            .visible(autoloot::get)
            .build());
    private final Setting<Integer> opendelay = sgAutoLoot.add(new IntSetting.Builder()
            .name("held open delay")
            .description("Delay in ticks for how long the chest is held open.")
            .defaultValue(10)
            .sliderRange(0, 20)
            .min(0)
            .visible(autoloot::get)
            .build());
    private final Setting<Modes> mode = sgAutoLoot.add(new EnumSetting.Builder<Modes>()
            .name("Reach Shape")
            .description("the shape of your reach")
            .defaultValue(Modes.Sphere)
            .visible(autoloot::get)
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
    private final Setting<Boolean> swing = sgAutoLoot.add(new BoolSetting.Builder()
            .name("Swing Hand")
            .description("Do or Do Not swing hand when opening chests.")
            .defaultValue(false)
            .visible(autoloot::get)
            .build()
    );
    private final Setting<Boolean> rotate = sgAutoLoot.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Faces the containers being opened server side.")
            .defaultValue(false)
            .visible(autoloot::get)
            .build()
    );
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
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bucket")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lava_bucket")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("water_bucket")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crafting_table")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stick")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tnt")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gunpowder")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sand")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_ingot")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_block")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_block")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("beacon")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("flint")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("wither_skeleton_skull")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("soul_sand")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("name_tag")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("flint_and_steel")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_sword")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_shovel")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_pickaxe")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_axe")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_hoe")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_helmet")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_chestplate")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_leggings")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_boots")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shulker_box"))
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
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("grass_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dirt")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("coarse_dirt")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bedrock")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("barrier")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cobblestone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("andesite")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("polished_andesite")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("granite")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("polished_granite")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diorite")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("polished_diorite")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sandstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chiseled_sandstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cut_sandstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("smooth_sandstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_sandstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chiseled_red_sandstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cut_red_sandstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("smooth_red_sandstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stone_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cracked_stone_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chiseled_stone_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("mossy_stone_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("smooth_stone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tuff")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("calcite")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dripstone_block")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("nether_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cracked_nether_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chiseled_nether_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_nether_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("prismarine")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("prismarine_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dark_prismarine")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("purpur_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("purpur_pillar")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("end_stone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("end_stone_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blackstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("polished_blackstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("polished_blackstone_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chiseled_polished_blackstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cracked_polished_blackstone_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gilded_blackstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cobbled_deepslate")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("polished_deepslate")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_tiles")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chiseled_deepslate")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cracked_deepslate_bricks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cracked_deepslate_tiles")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("mud_bricks")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("quartz_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("quartz_pillar")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("smooth_quartz")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chiseled_quartz_block")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("orange_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("magenta_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("light_blue_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("yellow_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lime_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pink_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gray_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("light_gray_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cyan_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("purple_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blue_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brown_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("green_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_terracotta")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("black_terracotta")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("orange_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("magenta_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("light_blue_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("yellow_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lime_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pink_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gray_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("light_gray_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cyan_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("purple_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blue_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brown_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("green_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_concrete")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("black_concrete")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tinted_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("orange_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("magenta_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("light_blue_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("yellow_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lime_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pink_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gray_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("light_gray_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cyan_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("purple_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blue_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brown_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("green_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_stained_glass")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("black_stained_glass")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("obsidian")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crying_obsidian")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("glowstone")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sea_lantern")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("jack_o_lantern")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("honeycomb_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bone_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("netherrack")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("basalt")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("polished_basalt")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("smooth_basalt")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("soul_soil")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("amethyst_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("exposed_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("weathered_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("oxidized_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cut_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("exposed_cut_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("weathered_cut_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("oxidized_cut_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_copper_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_exposed_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_weathered_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_oxidized_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_cut_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_exposed_cut_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_weathered_cut_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_oxidized_cut_copper")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("moss_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("packed_mud")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shroomlight"))
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
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("oak_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("birch_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cherry_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dark_oak_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("jungle_log")),net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("mangrove_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("spruce_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crimson_stem")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("warped_stem")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_oak_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_birch_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_cherry_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_dark_oak_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_jungle_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_mangrove_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_spruce_log")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_crimson_stem")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_warped_stem")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("oak_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("oak_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("birch_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cherry_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dark_oak_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("jungle_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("mangrove_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("spruce_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crimson_hyphae")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("warped_hyphae")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_oak_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_birch_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_cherry_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_dark_oak_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_jungle_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_mangrove_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_spruce_wood")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_crimson_hyphae")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_warped_hyphae")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bamboo_block")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("stripped_bamboo_block")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("oak_planks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("birch_planks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cherry_planks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dark_oak_planks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("jungle_planks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("mangrove_planks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("spruce_planks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crimson_planks")), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("warped_planks"))
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
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cooked_porkchop")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cooked_beef")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cooked_chicken")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cooked_mutton")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cooked_rabbit")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cooked_cod")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cooked_salmon")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("golden_carrot")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("golden_apple")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("enchanted_golden_apple")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("baked_potato")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("bread")),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pumpkin_pie"))
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
    private final Set<BlockPos> processedChests = new HashSet<>();
    private final Set<Integer> openedEntities = new HashSet<>();
    private final Map<BlockPos, Integer> chestsToProcess = new HashMap<>();
    private BlockPos lastInteractedBlockPos = null;
    private boolean isChestOpen = false;
    private boolean inventoryFullErrorSent = false;
    private int totalClicksThisTick = 0;
    private int maxClicks = 0;
    private int ticks;
    private int autoStealTicks;
    private double reach;
    private float originalYaw;
    private float originalPitch;

    public StorageLooter() {
        super(Trouser.Main, "Storage-Looter", "Steals stuff from containers around you.");
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            resetValues();
            if (disconnectdisable.get()) toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        resetValues();
        if (disconnectdisable.get()) toggle();
    }

    @Override
    public void onActivate() {
        resetValues();
    }

    @Override
    public void onDeactivate() {
        resetValues();
    }

    private void resetValues() {
        inventoryFullErrorSent = false;
        ticks = 0;
        autoStealTicks = 0;
        processedChests.clear();
        chestsToProcess.clear();
        openedEntities.clear();
        isChestOpen = false;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundUseItemOnPacket) {
            lastInteractedBlockPos = ((ServerboundUseItemOnPacket) event.packet).getHitResult().getBlockPos();
        }
        else if (event.packet instanceof ServerboundInteractPacket packet) {
            Entity entity = mc.level.getEntity(getEntityId(packet));
            if (entity != null) {
                lastInteractedBlockPos = entity.blockPosition();
            }
        }
    }
    private int getEntityId(ServerboundInteractPacket packet) {
        try {
            Field entityIdField = ServerboundInteractPacket.class.getDeclaredField("entityId");
            entityIdField.setAccessible(true);
            return entityIdField.getInt(packet);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if the field cannot be accessed
    }
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if ((!hasEnoughFreeSlots() && stopLoot.get()) || mc.player == null || mc.level == null) return;
        updateReach();
        int bottomlimit = (int) (mc.player.getBlockY() - Math.round(Math.ceil(reach)));
        if (!isContainerScreen(mc.player.containerMenu)) autoStealTicks = 0;
        if (autosteal.get() && lastInteractedBlockPos != null) {
            BlockState blockState = mc.level.getBlockState(lastInteractedBlockPos);
            Block block = blockState.getBlock();

            if (isValidContainerBlock(block) && containerList.get().contains(block.asItem())) {
                if (mc.player.containerMenu != null && isContainerScreen(mc.player.containerMenu)) {
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
                }
            }
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity.blockPosition().equals(lastInteractedBlockPos) && entity instanceof MinecartChest && containerList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chest_minecart")))) {
                    if (mc.player.containerMenu != null && isContainerScreen(mc.player.containerMenu)) {
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
        if (ticks < delay.get())ticks++;
        if (mc.gameMode == null) return;
        if (ticks >= delay.get() && autoloot.get()) {
            List<BlockPos> blocks = getBlocksInRange(bottomlimit);
            blocks.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(mc.player.position())));

            for (BlockPos blockPos : blocks) {
                BlockState blockState = mc.level.getBlockState(blockPos);
                Block block = blockState.getBlock();

                if (isValidContainerBlock(block) && !processedChests.contains(blockPos) && !isChestOpen) {
                    if (containerList.get().contains(block.asItem())) {
                        if (block instanceof ChestBlock) {
                            DoubleBlockCombiner.BlockType chestType = ChestBlock.getBlockType(blockState);
                            if (chestType == DoubleBlockCombiner.BlockType.SINGLE || chestType == DoubleBlockCombiner.BlockType.FIRST) {
                                openContainer(blockPos, block);
                            } else if (chestType == DoubleBlockCombiner.BlockType.SECOND) processedChests.add(blockPos);
                        } else {
                            openContainer(blockPos, block);
                        }
                    }
                }

                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof MinecartChest && containerList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chest_minecart"))) && blocks.contains(entity.blockPosition())) {
                        if (!openedEntities.contains(entity.getId()) && !processedChests.contains(entity.blockPosition()) && !isChestOpen) {
                            if (rotate.get()){
                                originalYaw = mc.player.getYRot();
                                originalPitch = mc.player.getXRot();
                                mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(entity.getX(), entity.getY(), entity.getZ()));
                            }
                            if (swing.get()){
                                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                            EntityHitResult hitResult = new EntityHitResult(entity, Vec3.atCenterOf(entity.blockPosition()));
                            mc.gameMode.interact(mc.player, entity, hitResult, InteractionHand.MAIN_HAND);
                            chestsToProcess.put(entity.blockPosition(), opendelay.get());
                            processedChests.add(entity.blockPosition());
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
        assert mc.player != null;
        int freeSlots = 0;
        for (ItemStack stack : mc.player.getInventory()) {
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

    private boolean isContainerScreen(AbstractContainerMenu screenHandler) {
        return screenHandler instanceof ChestMenu
                || screenHandler instanceof ShulkerBoxMenu
                || screenHandler instanceof HopperMenu
                || screenHandler instanceof DispenserMenu
                || screenHandler instanceof FurnaceMenu
                || screenHandler instanceof AbstractFurnaceMenu
                || screenHandler instanceof BlastFurnaceMenu
                || screenHandler instanceof SmokerMenu
                || screenHandler instanceof BrewingStandMenu
                || screenHandler instanceof CrafterMenu;
    }

    private void processContainerItems() {
        if (mc.player == null) return;
        int playerInvStart = mc.player.containerMenu.slots.size() - 36;
        maxClicks = maxClicksPerTick.get();

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
        if (swapStacks.get() && (mc.player.containerMenu instanceof ChestMenu
                || mc.player.containerMenu instanceof ShulkerBoxMenu
                || mc.player.containerMenu instanceof HopperMenu
                || mc.player.containerMenu instanceof DispenserMenu)) {
            swapSmallerStacksForBigger(playerInvStart);
        }
        if ((moveJunkToContainer.get() || moveOverlimitToContainer.get()) && (mc.player.containerMenu instanceof ChestMenu
                || mc.player.containerMenu instanceof ShulkerBoxMenu
                || mc.player.containerMenu instanceof HopperMenu
                || mc.player.containerMenu instanceof DispenserMenu)) {
            moveExcessItemsToContainer(playerInvStart);
        }

        totalClicksThisTick = 0; // Reset the total clicks for the next tick
    }
    private void moveExcessItemsToContainer(int playerInvStart) {
        if (mc.player == null || mc.gameMode == null) return;
        for (int i = playerInvStart; i < mc.player.containerMenu.slots.size(); i++) {
            ItemStack playerStack = mc.player.containerMenu.getSlot(i).getItem();
            if (!playerStack.isEmpty()) {
                Item playerItem = playerStack.getItem();

                // Check if the item is in the junk item list
                if (moveJunkToContainer.get() && (junkItemList.get().contains(playerItem) || (nonitemlistjunk.get() && (!itemList.get().contains(playerItem) && !blockItemList.get().contains(playerItem)  && !woodItemList.get().contains(playerItem) && !foodItemList.get().contains(playerItem) && !miscItemList.get().contains(playerItem) && !isNonJunkVariant(playerStack, itemList.get()))))) {
                    // Move the entire stack to the container
                    for (int j = 0; j < playerInvStart; j++) {
                        ItemStack containerStack = mc.player.containerMenu.getSlot(j).getItem();
                        if (containerStack.isEmpty()) {
                            if (totalClicksThisTick >= maxClicks) {
                                return; // Stop moving items if the maximum clicks per tick is reached
                            }
                            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
                            totalClicksThisTick++;
                            break; // Exit the inner loop since the stack has been moved
                        }
                    }
                }
                if (moveOverlimitToContainer.get()) {
                    int maxStackSize = playerItem.getDefaultMaxStackSize();
                    int maxCount = getMaxCount(playerItem);
                    List<Item> itemList = getItemList(playerItem);

                    int currentCount = getItemCount(playerItem, itemList);
                    if (itemList == this.itemList.get()) currentCount = getCurrentItemCount(playerItem);

                    // Round up the limit to the nearest full stack size
                    if (maxCount < maxStackSize) {
                        maxCount = maxStackSize;
                    } else if (maxCount % maxStackSize != 0) {
                        maxCount = (maxCount / maxStackSize + 1) * maxStackSize;
                    }

                    if (currentCount > maxCount) {
                        int excessAmount = currentCount - maxCount;
                        for (int j = 0; j < playerInvStart; j++) {
                            ItemStack containerStack = mc.player.containerMenu.getSlot(j).getItem();
                            if (containerStack.isEmpty()) {
                                continue; // Skip empty container slots
                            }

                            Item containerItem = containerStack.getItem();
                            if (isSameItemList(playerItem, containerItem)) {
                                if (totalClicksThisTick >= maxClicks) {
                                    return; // Stop moving items if the maximum clicks per tick is reached
                                }
                                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
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
    }
    private boolean isNonJunkVariant(ItemStack playerItem, List<Item> itemList) {
        String itemName = playerItem.getItem().getDescriptionId().toLowerCase();

        if (itemName.contains("shulker_box")) {
            return itemList.stream().anyMatch(item -> item.getDescriptionId().contains("shulker_box"));
        } else if (itemName.contains("_pickaxe")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_pickaxe")));
        } else if (itemName.contains("_sword")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_sword")));
        } else if (itemName.contains("_shovel")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_shovel")));
        } else if (itemName.contains("_axe")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_axe")));
        } else if (itemName.contains("_hoe")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_hoe")));
        } else if (itemName.contains("_helmet")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_helmet")));
        } else if (itemName.contains("_chestplate")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_chestplate")));
        } else if (itemName.contains("_leggings")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_leggings")));
        } else if (itemName.contains("_boots")) {
            return itemList.stream().anyMatch(item -> item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_boots")));
        }

        return false;
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
        if (mc.player == null) return;
        List<Integer> slotIndices = new ArrayList<>();
        for (int i = 0; i < mc.player.containerMenu.slots.size(); i++) {
            Item item = mc.player.containerMenu.getSlot(i).getItem().getItem();
            String itemName = item.toString().toLowerCase();
            if (((itemName.contains("shulker_box") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shulker_box")))) ||
                    (itemName.contains("_pickaxe") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_pickaxe")))) ||
                    (itemName.contains("_sword") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_sword")))) ||
                    (itemName.contains("_shovel") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_shovel")))) ||
                    (itemName.contains("_axe") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_axe")))) ||
                    (itemName.contains("_hoe") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_hoe")))) ||
                    (itemName.contains("_helmet") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_helmet")))) ||
                    (itemName.contains("_chestplate") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_chestplate")))) ||
                    (itemName.contains("_leggings") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_leggings")))) ||
                    (itemName.contains("_boots") && itemList.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_boots")))) ||
                    itemList.contains(item)) && i < playerInvStart) {
                slotIndices.add(i);
            }
        }

        slotIndices.sort((a, b) -> {
            ItemStack stackA = mc.player.containerMenu.getSlot(a).getItem();
            ItemStack stackB = mc.player.containerMenu.getSlot(b).getItem();
            return compareItems(stackA, stackB);
        });

        for (int slotIndex : slotIndices) {
            Item item = mc.player.containerMenu.getSlot(slotIndex).getItem().getItem();
            moveItemsWithLimit(slotIndex, item, getMaxCount(item), itemList);
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
        assert mc.player != null;
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = (int) (mc.player.getBlockX() - Math.round(Math.ceil(reach))); x <= mc.player.getBlockX() + Math.round(Math.ceil(reach)); x++) {
            for (int y = bottomlimit; y <= (mc.player.getBlockY() + 1) + Math.round(Math.ceil(reach)); y++) {
                for (int z = (int) (mc.player.getBlockZ() - Math.round(Math.ceil(reach))); z <= mc.player.getBlockZ() + Math.round(Math.ceil(reach)); z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    Vec3 playerPos1 = Vec3.atCenterOf(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()));
                    Vec3 playerPos2 = Vec3.atCenterOf(new BlockPos(mc.player.getBlockX(), mc.player.getBlockY() + 1, mc.player.getBlockZ()));
                    double distance1 = playerPos1.distanceTo(Vec3.atCenterOf(blockPos));
                    double distance2 = playerPos2.distanceTo(Vec3.atCenterOf(blockPos));
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
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock || block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof DropperBlock || block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock || block instanceof SmokerBlock || block instanceof BrewingStandBlock || block instanceof CrafterBlock;
    }

    private void openContainer(BlockPos blockPos, Block block) {
        if (mc.player == null) return;
        if (rotate.get()){
            originalYaw = mc.player.getYRot();
            originalPitch = mc.player.getXRot();
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        }
        if (swing.get()){
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(blockPos), Direction.UP, blockPos, true));
        chestsToProcess.put(blockPos, opendelay.get());
        processedChests.add(blockPos);
        isChestOpen = true;
    }

    private void processChestsAfterDelay() {
        if (mc.player == null) return;
        chestsToProcess.entrySet().removeIf(entry -> {
            int delay = entry.getValue();
            if (delay <= 0) {
                if (mc.player.containerMenu != null && isContainerScreen(mc.player.containerMenu)) {
                    processContainerItems();
                    mc.player.closeContainer();
                    if (rotate.get()){
                        mc.player.setYRot(originalYaw);
                        mc.player.setXRot(originalPitch);
                    }
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
        if (mc.player == null) return;
        List<Integer> containerSlotIndices = new ArrayList<>();
        for (int i = 0; i < playerInvStart; i++) {
            ItemStack containerStack = mc.player.containerMenu.getSlot(i).getItem();
            if (!containerStack.isEmpty()) {
                containerSlotIndices.add(i);
            }
        }

        containerSlotIndices.sort((a, b) -> {
            ItemStack stackA = mc.player.containerMenu.getSlot(a).getItem();
            ItemStack stackB = mc.player.containerMenu.getSlot(b).getItem();
            return compareItems(stackA, stackB);
        });

        for (int i = playerInvStart; i < mc.player.containerMenu.slots.size(); i++) {
            ItemStack playerStack = mc.player.containerMenu.getSlot(i).getItem();
            if (!playerStack.isEmpty()) {
                Item playerItem = playerStack.getItem();
                int playerStackSize = playerStack.getCount();

                for (int containerSlotIndex : containerSlotIndices) {
                    ItemStack containerStack = mc.player.containerMenu.getSlot(containerSlotIndex).getItem();
                    Item containerItem = containerStack.getItem();
                    int containerStackSize = containerStack.getCount();

                    if (isSameItemList(playerItem, containerItem)) {
                        if (containerStackSize > playerStackSize && Math.round(((double) containerStackSize / containerStack.getItem().getDefaultMaxStackSize()) * 100) >= minLootableStackSize.get()) {
                            swapItems(i, containerSlotIndex, playerInvStart);
                            break; // Exit the inner loop since the swap has been performed
                        } else if (compareItems(playerStack, containerStack) > 0) {
                            swapItems(i, containerSlotIndex, playerInvStart);
                            break; // Exit the inner loop since the swap has been performed
                        }
                    }
                }
            }
        }
    }

    private int compareItems(ItemStack stack1, ItemStack stack2) {
        int score1 = getItemScore(stack1);
        int score2 = getItemScore(stack2);

        return Integer.compare(score2, score1); // Reverse the order to sort in descending order
    }
    public static ArrayList<ItemStack> getArmorItems(LivingEntity livingEntity) {
        ArrayList<ItemStack> armorItems = new ArrayList<>();
        armorItems.add(livingEntity.getItemBySlot(EquipmentSlot.HEAD));
        armorItems.add(livingEntity.getItemBySlot(EquipmentSlot.CHEST));
        armorItems.add(livingEntity.getItemBySlot(EquipmentSlot.LEGS));
        armorItems.add(livingEntity.getItemBySlot(EquipmentSlot.FEET));
        return armorItems;
    }
    public static ArrayList<ItemStack> getHandItems(LivingEntity livingEntity) {
        ArrayList<ItemStack> handItems = new ArrayList<>();
        handItems.add(livingEntity.getItemBySlot(EquipmentSlot.MAINHAND));
        handItems.add(livingEntity.getItemBySlot(EquipmentSlot.OFFHAND));
        return handItems;
    }
    public static boolean isArmor(ItemStack itemStack) {
        return itemStack.is(ItemTags.HEAD_ARMOR) ||
                itemStack.is(ItemTags.CHEST_ARMOR) ||
                itemStack.is(ItemTags.LEG_ARMOR) ||
                itemStack.is(ItemTags.FOOT_ARMOR);
    }
    public static boolean isTool(ItemStack itemStack) {
        return itemStack.is(ItemTags.AXES) ||
                itemStack.is(ItemTags.HOES) ||
                itemStack.is(ItemTags.PICKAXES) ||
                itemStack.is(ItemTags.SHOVELS) ||
                itemStack.getItem() instanceof ShearsItem ||
                itemStack.getItem() instanceof FlintAndSteelItem;
    }
    private int getItemScore(ItemStack stack) {
        assert mc.player != null;
        String itemName = stack.getItem().getDescriptionId().toLowerCase();
        int score = 0;

        if (itemName.contains("wooden") || itemName.contains("golden")) {
            score = 188;
        } else if (itemName.contains("stone") || itemName.contains("leather")) {
            score = 422;
        } else if (itemName.contains("iron") || itemName.contains("chainmail")) {
            score = 575;
        } else if (itemName.contains("diamond")) {
            score = 651;
        } else if (itemName.contains("netherite")) {
            score = 700;
        }

        //durability score
        int durabilityscore = 0;
        int maxDurability = stack.getMaxDamage();
        int currentDurability = maxDurability - stack.getDamageValue();
        durabilityscore = (int) ((double) currentDurability / maxDurability * 100);
        score += durabilityscore;

        //enchantments score
        ItemEnchantments enchantments = stack.getEnchantments();
        int enchantmentscore = 0;
        Registry<Enchantment> enchantmentRegistry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (isArmor(stack)) {
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.PROTECTION)) * 10;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.BLAST_PROTECTION)) * 10;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.FIRE_PROTECTION)) * 10;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.PROJECTILE_PROTECTION)) * 10;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.UNBREAKING)) * 9;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.MENDING)) * 8;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.THORNS)) * 5;
        } else if (stack.is(ItemTags.SWORDS)) {
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.SMITE)) * 10;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.SHARPNESS)) * 10;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.BANE_OF_ARTHROPODS)) * 9;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.FIRE_ASPECT)) * 9;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.UNBREAKING)) * 9;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.MENDING)) * 8;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.LOOTING)) * 5;
        } else if (isTool(stack)) {
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.EFFICIENCY)) * 10;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.UNBREAKING)) * 9;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.MENDING)) * 8;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.SILK_TOUCH)) * 6;
            enchantmentscore += getEnchantmentLevel(enchantments, enchantmentRegistry.getOrThrow(Enchantments.FORTUNE)) * 5;
        }
        score += enchantmentscore;

        return score;
    }

    private int getEnchantmentLevel(ItemEnchantments enchantments, Holder<Enchantment> enchantment) {
        return enchantments.getLevel(enchantment);
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

    private void swapItems(int playerSlotIndex, int containerSlotIndex, int playerInvStart) {
        if (mc.player != null && mc.gameMode != null && isContainerScreen(mc.player.containerMenu)) {
            // Perform the swap if we have enough clicks left
            if (totalClicksThisTick + 3 <= maxClicks) {
                // Move the entire stack to the container
                for (int j = 0; j < playerInvStart; j++) {
                    ItemStack containerStack = mc.player.containerMenu.getSlot(j).getItem();
                    if (containerStack.isEmpty()) {
                        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, playerSlotIndex, 0, ContainerInput.QUICK_MOVE, mc.player);
                        totalClicksThisTick++;

                        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, containerSlotIndex, 0, ContainerInput.PICKUP, mc.player);
                        totalClicksThisTick++;

                        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, playerSlotIndex, 0, ContainerInput.PICKUP, mc.player);
                        totalClicksThisTick++;
                        break; // Exit the inner loop since the stack has been moved
                    }
                }
            }
        }
    }

    private void moveItems(int slotIndex, int amountToMove) {
        if (mc.player != null && mc.gameMode != null && isContainerScreen(mc.player.containerMenu) && slotIndex >= 0 && slotIndex < mc.player.containerMenu.slots.size()) {
            ItemStack sourceStack = mc.player.containerMenu.getSlot(slotIndex).getItem();
            if (!sourceStack.isEmpty() && Math.round(((double)sourceStack.getCount() / sourceStack.getItem().getDefaultMaxStackSize()) * 100) >= minLootableStackSize.get()) {
                amountToMove = Math.min(amountToMove, sourceStack.getCount());

                int clicksNeeded = (int) Math.ceil((double) amountToMove / sourceStack.getMaxStackSize());

                for (int i = 0; i < clicksNeeded; i++) {
                    if (totalClicksThisTick >= maxClicks) {
                        return; // Stop moving items if the maximum clicks per tick is reached
                    }
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slotIndex, 0, ContainerInput.QUICK_MOVE, mc.player);
                    totalClicksThisTick++;
                }
            }
        }
    }
    private int getCurrentItemCount(Item item) {
        assert mc.player != null;
        int count = 0;
        String itemName = item.toString().toLowerCase();
        for (ItemStack stack : mc.player.getInventory()) {
            if (isSameItem(stack.getItem(), item, itemName)) {
                count += stack.getCount();
            }
        }
        if (isSameItem(mc.player.getOffhandItem().getItem(), item, itemName)) {
            count += mc.player.getOffhandItem().getCount();
        }
        for (ItemStack armorStack : getArmorItems(mc.player)) {
            if (isSameItem(armorStack.getItem(), item, itemName)) {
                count += armorStack.getCount();
            }
        }
        return count;
    }
    private int getMaxCount(Item item) {
        int maxCount = 1;
        String itemName = item.toString().toLowerCase();

        if (itemName.contains("_pickaxe") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_pickaxe")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_pickaxe")));
            return getLimitFromList(index);
        } else if (itemName.contains("_sword") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_sword")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_sword")));
            return getLimitFromList(index);
        } else if (itemName.contains("_shovel") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_shovel")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_shovel")));
            return getLimitFromList(index);
        } else if (itemName.contains("_axe") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_axe")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_axe")));
            return getLimitFromList(index);
        } else if (itemName.contains("_hoe") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_hoe")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_hoe")));
            return getLimitFromList(index);
        } else if (itemName.contains("_helmet") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_helmet")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_helmet")));
            return getLimitFromList(index);
        } else if (itemName.contains("_chestplate") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_chestplate")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_chestplate")));
            return getLimitFromList(index);
        } else if (itemName.contains("_leggings") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_leggings")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_leggings")));
            return getLimitFromList(index);
        } else if (itemName.contains("_boots") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_boots")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_boots")));
            return getLimitFromList(index);
        } else if (itemName.contains("shulker_box") && itemList.get().contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shulker_box")))) {
            int index = itemList.get().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shulker_box")));
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
        assert mc.player != null;
        int count = 0;
        String itemName = item.toString().toLowerCase();

        for (ItemStack stack : mc.player.getInventory()) {
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
        for (ItemStack stack : getArmorItems(mc.player)) {
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

        ItemStack offhandStack = mc.player.getOffhandItem();
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
        return item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chest"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("trapped_chest"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("barrel"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shulker_box"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("hopper"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dispenser"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dropper"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("furnace"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blast_furnace"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("smoker"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("brewing_stand"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("crafter"))
                || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chest_minecart"));
    }

    public enum Modes {
        Sphere, Box
    }

    //item list filters
    private static final Set<String> FOOD_ITEMS = new HashSet<>();

    static {
        try {
            Field[] fields = Foods.class.getFields();
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
                return block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shulker_box"));
            }
        }

        List<Item> diamondItems = Arrays.asList(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_sword")),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_shovel")),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_pickaxe")),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_axe")),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_hoe")),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_helmet")),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_chestplate")),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_leggings")),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_boots"))
        );

        if (isTool(item.getDefaultInstance()) || isArmor(item.getDefaultInstance())) {
            return diamondItems.contains(item);
        }

        return true;
    }
}