//written by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.entity.EntityType;
import org.joml.Matrix3x2fStack;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatsHud extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSync = settings.createGroup("Sync");
    private final SettingGroup sgOrder = settings.createGroup("Order and Formatting");
    private final SettingGroup sgStats = settings.createGroup("Stats");
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the HUD text (1.0 = normal, 0.5 = half size, 2.0 = double size).")
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 10.0)
            .build());
    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
            .name("text-color")
            .description("Color of the HUD text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build());
    private final Setting<Boolean> textshadow = sgGeneral.add(new BoolSetting.Builder()
            .name("text-shadow")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> showBackground = sgGeneral.add(new BoolSetting.Builder()
            .name("background")
            .description("Show a background behind the HUD.")
            .defaultValue(true)
            .build());
    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Background color.")
            .defaultValue(new SettingColor(25, 25, 25, 100))
            .visible(showBackground::get)
            .build());
    private final Setting<Integer> padding = sgGeneral.add(new IntSetting.Builder()
            .name("padding")
            .description("Background padding around text.")
            .defaultValue(2)
            .min(0)
            .sliderRange(0, 10)
            .visible(showBackground::get)
            .build());
    private final Setting<Double> posX = sgGeneral.add(new DoubleSetting.Builder()
            .name("pos-x")
            .description("X position of the HUD.")
            .defaultValue(10)
            .min(0)
            .sliderRange(0, 2000)
            .build());
    private final Setting<Double> posY = sgGeneral.add(new DoubleSetting.Builder()
            .name("pos-y")
            .description("Y position of the HUD.")
            .defaultValue(10)
            .min(0)
            .sliderRange(0, 2000)
            .build());
    private final Setting<Integer> updateInterval = sgGeneral.add(new IntSetting.Builder()
            .name("update-interval")
            .description("Update stats every X ticks (20 ticks = 1 second).")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 200)
            .build());
    private final Setting<Boolean> autoSync = sgSync.add(new BoolSetting.Builder()
            .name("auto-sync")
            .description("Automatically request stats from server by sending a packet.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> syncDelay = sgSync.add(new IntSetting.Builder()
            .name("sync-delay")
            .description("Delay between sync packets (seconds).")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 300)
            .visible(autoSync::get)
            .build());
    private final Setting<List<String>> statOrder = sgOrder.add(new StringListSetting.Builder()
            .name("stat-order")
            .description("Order stats appear in HUD. Default: playtime, distancetravelled, blocksbroken, mobskilled, playerkills, itemscrafted, itemsused, itemspickedup, deaths, timesincedeath, timesincesleep")
            .defaultValue(List.of(
                    "playtime", "distancetravelled", "blocksbroken", "mobskilled", "playerkills",
                    "itemscrafted", "itemsused", "itemspickedup", "deaths",
                    "timesincedeath", "timesincesleep"
            ))
            .build());
    private final Setting<Boolean> showRates = sgOrder.add(new BoolSetting.Builder()
            .name("hourly-rates")
            .description("Show hourly rates for stats. Based on total play time.")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> showPlayTime = sgStats.add(new BoolSetting.Builder()
            .name("play-time")
            .description("Show total play time.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> showDistance = sgStats.add(new BoolSetting.Builder()
            .name("distance-travelled")
            .description("Show distance travelled.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> showBlocks = sgStats.add(new BoolSetting.Builder()
            .name("blocks-broken")
            .description("Show blocks broken count.")
            .defaultValue(true)
            .build());
    private enum bModes {
        Count,
        DoNotCount
    }
    private final Setting<bModes> blocksCountMode = sgStats.add(new EnumSetting.Builder<bModes>()
            .name("blocks-broken-count-mode")
            .description("The mode for counting Blocks.")
            .defaultValue(bModes.DoNotCount)
            .visible(showBlocks::get)
            .build());
    private final Setting<List<Block>> blocks = sgStats.add(new BlockListSetting.Builder()
            .name("blocks")
            .visible(showBlocks::get)
            .build());
    private final Setting<Boolean> showMobs = sgStats.add(new BoolSetting.Builder()
            .name("mobs-killed")
            .description("Show total mobs killed.")
            .defaultValue(true)
            .build());
    private enum eModes {
        Count,
        DoNotCount
    }
    private final Setting<eModes> mobsCountMode = sgStats.add(new EnumSetting.Builder<eModes>()
            .name("mobs-killed-count-mode")
            .description("The mode for counting Mobs.")
            .defaultValue(eModes.DoNotCount)
            .visible(showMobs::get)
            .build());
    private final Setting<Set<EntityType<?>>> mobs = sgStats.add(new EntityTypeListSetting.Builder()
            .name("mobs")
            .visible(showMobs::get)
            .build());
    private final Setting<Boolean> showPvpKills = sgStats.add(new BoolSetting.Builder()
            .name("players-killed").defaultValue(true).build());
    private final Setting<Boolean> showItemsCrafted = sgStats.add(new BoolSetting.Builder()
            .name("items-crafted")
            .description("Show items crafted count.")
            .defaultValue(true)
            .build());
    private enum cModes {
        Count,
        DoNotCount
    }
    private final Setting<cModes> craftedCountMode = sgStats.add(new EnumSetting.Builder<cModes>()
            .name("items-crafted-count-mode")
            .description("The mode for counting crafted Items.")
            .defaultValue(cModes.DoNotCount)
            .visible(showItemsCrafted::get)
            .build());

    private final Setting<List<Item>> craftedItems = sgStats.add(new ItemListSetting.Builder()
            .name("items-crafted")
            .visible(showItemsCrafted::get)
            .build());
    private final Setting<Boolean> showItemsUsed = sgStats.add(new BoolSetting.Builder()
            .name("items-used")
            .description("Show items used count.")
            .defaultValue(true)
            .build());
    private enum uModes {
        Count,
        DoNotCount
    }
    private final Setting<uModes> usedCountMode = sgStats.add(new EnumSetting.Builder<uModes>()
            .name("items-used-count-mode")
            .description("The mode for counting used Items.")
            .defaultValue(uModes.DoNotCount)
            .visible(showItemsUsed::get)
            .build());
    private final Setting<List<Item>> usedItems = sgStats.add(new ItemListSetting.Builder()
            .name("items-used")
            .visible(showItemsUsed::get)
            .build());
    private final Setting<Boolean> showItemsPicked = sgStats.add(new BoolSetting.Builder()
            .name("items-picked-up")
            .description("Show items picked up count.")
            .defaultValue(true)
            .build());
    private enum iModes {
        Count,
        DoNotCount
    }
    private final Setting<iModes> itemsCountMode = sgStats.add(new EnumSetting.Builder<iModes>()
            .name("items-picked-up-count-mode")
            .description("The mode for counting Items.")
            .defaultValue(iModes.DoNotCount)
            .visible(showItemsPicked::get)
            .build());
    private final Setting<List<Item>> items = sgStats.add(new ItemListSetting.Builder()
            .name("items-picked-up")
            .visible(showItemsPicked::get)
            .build());
    private final Setting<Boolean> showDeaths = sgStats.add(new BoolSetting.Builder()
            .name("deaths").defaultValue(true).build());
    private final Setting<Boolean> showTimeSinceDeath = sgStats.add(new BoolSetting.Builder()
            .name("time-since-death")
            .description("Show time since last death.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> showTimeSinceSleep = sgStats.add(new BoolSetting.Builder()
            .name("time-since-sleep")
            .description("Show time since last sleep.")
            .defaultValue(true)
            .build());
    private final List<Block> allBlocks;
    private final List<Item> allItems;
    private final List<EntityType<?>> allEntities;

    public StatsHud() {
        super(Trouser.Main, "StatsHud", "Displays player statistics on screen.");
        allBlocks = Registries.BLOCK.stream().toList();
        allItems = Registries.ITEM.stream().toList();
        allEntities = Registries.ENTITY_TYPE.stream().toList();
    }

    private static final String KEY_PLAYTIME = "playtime";
    private static final String KEY_DISTANCE = "distancetravelled";
    private static final String KEY_BLOCKS = "blocksbroken";
    private static final String KEY_MOBS = "mobskilled";
    private static final String KEY_PVP = "playerkills";
    private static final String KEY_ITEMSCRAFTED = "itemscrafted";
    private static final String KEY_ITEMSUSED = "itemsused";
    private static final String KEY_ITEMSPICKED = "itemspickedup";
    private static final String KEY_DEATHS = "deaths";
    private static final String KEY_TIMESINCEDEATH = "timesincedeath";
    private static final String KEY_TIMESINCESLEEP = "timesincesleep";

    private int tickCounter = 0;
    private final Map<String, String> cachedStatLines = new ConcurrentHashMap<>();
    private int updateTickCounter = 0;

    @Override
    public void onActivate() {
        ClientStatusC2SPacket getStats = new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS);
        mc.getNetworkHandler().sendPacket(getStats);
        tickCounter = 0;
        updateTickCounter = 0;
        computeAllStats();
    }
    @Override
    public void onDeactivate() {
        cachedStatLines.clear();
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        updateTickCounter++;

        tickCounter++;
        if (autoSync.get() && mc.getNetworkHandler() != null && tickCounter >= syncDelay.get() * 20) {
            ClientStatusC2SPacket getStats = new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS);
            mc.getNetworkHandler().sendPacket(getStats);
            tickCounter = 0;
        }

        if (updateTickCounter >= updateInterval.get()) {
            computeAllStats();
            updateTickCounter = 0;
        }
    }
    private void computeAllStats() {
        if (mc.player == null || mc.player.getStatHandler() == null) return;

        StatHandler statHandler = mc.player.getStatHandler();
        Map<String, String> newStats = new LinkedHashMap<>();
        double hours = getHoursPlayed(statHandler);

        if (showPlayTime.get()) {
            long playTime = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            long hrs = playTime / 72000L;
            long mins = (playTime % 72000) / 1200L;
            newStats.put(KEY_PLAYTIME, String.format("Play Time: %dh %dm", hrs, mins));
        }

        if (showDistance.get()) {
            int totalDistanceCm = 0;
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SWIM_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_UNDER_WATER_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ON_WATER_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.AVIATE_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.BOAT_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.MINECART_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PIG_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.HORSE_ONE_CM));
            totalDistanceCm += statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.STRIDER_ONE_CM));
            double km = totalDistanceCm / 100000.0;
            newStats.put(KEY_DISTANCE, String.format("Distance travelled: %.1fkm", km));
        }

        if (showBlocks.get()) {
            int totalBlocksBroken = 0;
            List<Block> blockList = blocks.get();
            bModes blockMode = blocksCountMode.get();

            Block singleBlock = null;
            if (blockMode == bModes.Count && !blockList.isEmpty()) {
                singleBlock = blockList.get(0);
            }

            for (Block block : allBlocks) {
                boolean shouldCount;
                if (blockMode == bModes.Count) {
                    shouldCount = blockList.isEmpty() || blockList.contains(block);
                } else {
                    shouldCount = !blockList.contains(block);
                }
                if (shouldCount) {
                    Stat<Block> stat = Stats.MINED.getOrCreateStat(block);
                    totalBlocksBroken += statHandler.getStat(stat);
                }
            }

            String blocksText;
            if (singleBlock != null) {
                String blockName = singleBlock.getName().getString();
                blocksText = String.format("%s broken: %d",
                        blockName != null ? blockName : "Unknown Block", totalBlocksBroken);
            } else {
                blocksText = String.format("Blocks broken: %d", totalBlocksBroken);
            }
            if (showRates.get() && hours > 0) {
                blocksText += String.format(" (%.0f/h)", totalBlocksBroken / hours);
            }
            newStats.put(KEY_BLOCKS, blocksText);
        }

        if (showMobs.get()) {
            int totalMobsKilled = 0;
            List<EntityType<?>> mobList = new ArrayList<>(mobs.get());
            eModes mobMode = mobsCountMode.get();

            EntityType<?> singleMob = null;
            if (mobMode == eModes.Count && !mobList.isEmpty()) {
                singleMob = mobList.get(0);
            }

            for (EntityType<?> entityType : allEntities) {
                boolean shouldCount;
                if (mobMode == eModes.Count) {
                    shouldCount = mobList.isEmpty() || mobList.contains(entityType);
                } else {
                    shouldCount = !mobList.contains(entityType);
                }
                if (shouldCount) {
                    Stat<EntityType<?>> stat = Stats.KILLED.getOrCreateStat(entityType);
                    totalMobsKilled += statHandler.getStat(stat);
                }
            }

            String mobsText;
            if (singleMob != null) {
                String mobName = singleMob.getName().getString();
                mobsText = String.format("%s killed: %d",
                        mobName != null ? mobName : "Unknown Mob", totalMobsKilled);
            } else {
                mobsText = String.format("Mobs killed: %d", totalMobsKilled);
            }
            if (showRates.get() && hours > 0) {
                mobsText += String.format(" (%.0f/h)", totalMobsKilled / hours);
            }
            newStats.put(KEY_MOBS, mobsText);
        }

        if (showPvpKills.get()) {
            int pvpKills = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAYER_KILLS));
            String text = String.format("Players killed: %d", pvpKills);
            if (showRates.get() && hours > 0) {
                text += String.format(" (%.1f/h)", pvpKills / hours);
            }
            newStats.put(KEY_PVP, text);
        }

        if (showItemsCrafted.get()) {
            int totalItemsCrafted = 0;
            List<Item> craftedItemList = new ArrayList<>(craftedItems.get());
            cModes craftedMode = craftedCountMode.get();

            for (Item item : allItems) {
                boolean shouldCount;
                if (craftedMode == cModes.Count) {
                    shouldCount = craftedItemList.isEmpty() || craftedItemList.contains(item);
                } else {
                    shouldCount = !craftedItemList.contains(item);
                }
                if (shouldCount) {
                    Stat<Item> stat = Stats.CRAFTED.getOrCreateStat(item);
                    totalItemsCrafted += statHandler.getStat(stat);
                }
            }

            String craftedText;
            if (craftedMode == cModes.Count && craftedItemList.size() == 1) {
                Item singleItem = craftedItemList.get(0);
                String itemName = singleItem.getName().getString();
                craftedText = String.format("%s crafted: %d",
                        itemName != null ? itemName : "Unknown Item", totalItemsCrafted);
            } else {
                craftedText = String.format("Items crafted: %d", totalItemsCrafted);
            }
            if (showRates.get() && hours > 0) {
                craftedText += String.format(" (%.0f/h)", totalItemsCrafted / hours);
            }
            newStats.put(KEY_ITEMSCRAFTED, craftedText);
        }

        if (showItemsUsed.get()) {
            int totalItemsUsed = 0;
            List<Item> usedItemList = new ArrayList<>(usedItems.get());
            uModes usedMode = usedCountMode.get();

            for (Item item : allItems) {
                boolean shouldCount;
                if (usedMode == uModes.Count) {
                    shouldCount = usedItemList.isEmpty() || usedItemList.contains(item);
                } else {
                    shouldCount = !usedItemList.contains(item);
                }
                if (shouldCount) {
                    Stat<Item> stat = Stats.USED.getOrCreateStat(item);
                    totalItemsUsed += statHandler.getStat(stat);
                }
            }

            String usedText;
            if (usedMode == uModes.Count && usedItemList.size() == 1) {
                Item singleItem = usedItemList.get(0);
                String itemName = singleItem.getName().getString();
                usedText = String.format("%s used: %d",
                        itemName != null ? itemName : "Unknown Item", totalItemsUsed);
            } else {
                usedText = String.format("Items used: %d", totalItemsUsed);
            }
            if (showRates.get() && hours > 0) {
                usedText += String.format(" (%.0f/h)", totalItemsUsed / hours);
            }
            newStats.put(KEY_ITEMSUSED, usedText);
        }

        if (showItemsPicked.get()) {
            int totalItemsPicked = 0;
            List<Item> itemList = new ArrayList<>(items.get());
            iModes itemMode = itemsCountMode.get();

            for (Item item : allItems) {
                boolean shouldCount;
                if (itemMode == iModes.Count) {
                    shouldCount = itemList.isEmpty() || itemList.contains(item);
                } else {
                    shouldCount = !itemList.contains(item);
                }
                if (shouldCount) {
                    Stat<Item> stat = Stats.PICKED_UP.getOrCreateStat(item);
                    totalItemsPicked += statHandler.getStat(stat);
                }
            }

            String itemsText;
            if (itemMode == iModes.Count && itemList.size() == 1) {
                Item singleItem = itemList.get(0);
                String itemName = singleItem.getName().getString();
                itemsText = String.format("%s picked up: %d",
                        itemName != null ? itemName : "Unknown Item", totalItemsPicked);
            } else {
                itemsText = String.format("Items picked up: %d", totalItemsPicked);
            }
            if (showRates.get() && hours > 0) {
                itemsText += String.format(" (%.0f/h)", totalItemsPicked / hours);
            }
            newStats.put(KEY_ITEMSPICKED, itemsText);
        }

        int deaths = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS));
        if (showDeaths.get()) {
            String text = String.format("Deaths: %d", deaths);
            if (showRates.get() && hours > 0) {
                text += String.format(" (%.1f/h)", deaths / hours);
            }
            newStats.put(KEY_DEATHS, text);
        }

        if (showTimeSinceDeath.get() && deaths >= 1) {
            int ticksSinceDeath = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_DEATH));
            long totalSeconds = ticksSinceDeath / 20;
            long hrs = totalSeconds / 3600;
            long mins = (totalSeconds % 3600) / 60;
            long secs = totalSeconds % 60;
            String timeText = hrs > 0 ?
                    String.format("Since death: %dh %02dm %02ds", hrs, mins, secs) :
                    String.format("Since death: %dm %02ds", mins, secs);
            newStats.put(KEY_TIMESINCEDEATH, timeText);
        }

        if (showTimeSinceSleep.get()) {
            int ticksSinceSleep = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
            long totalSeconds = ticksSinceSleep / 20;
            long hrs = totalSeconds / 3600;
            long mins = (totalSeconds % 3600) / 60;
            long secs = totalSeconds % 60;
            String timeText = hrs > 0 ?
                    String.format("Since last sleep: %dh %02dm %02ds", hrs, mins, secs) :
                    String.format("Since last sleep: %dm %02ds", mins, secs);
            newStats.put(KEY_TIMESINCESLEEP, timeText);
        }
        cachedStatLines.clear();
        cachedStatLines.putAll(newStats);
    }
    private double getHoursPlayed(StatHandler statHandler) {
        long playTime = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
        return playTime / 72000.0; // 72000 ticks = 1 hour
    }
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (cachedStatLines.isEmpty()) return;

        DrawContext context = event.drawContext;
        double guiScale = mc.options.getGuiScale().getValue();
        double x = posX.get() / guiScale;
        double y = posY.get() / guiScale;
        double customScale = scale.get();

        List<String> statLines = statOrder.get().stream()
                .filter(cachedStatLines::containsKey)
                .map(cachedStatLines::get)
                .toList();

        if (statLines.isEmpty()) return;

        double unscaledMaxWidth = 0;
        for (String line : statLines) {
            unscaledMaxWidth = Math.max(unscaledMaxWidth, mc.textRenderer.getWidth(line));
        }

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale((float) customScale, (float) customScale, (float) customScale);

        if (showBackground.get()) {
            int bgColor = backgroundColor.get().getPacked();
            context.fill(
                    (int)(x - padding.get()),
                    (int)(y - padding.get()),
                    (int)(x + (unscaledMaxWidth + 2 * padding.get())),
                    (int)(y + (statLines.size() * mc.textRenderer.fontHeight + 2 * padding.get())),
                    bgColor
            );
        }

        double yOffset = 0;
        for (String line : statLines) {
            int tc = textColor.get().getPacked();
            context.drawText(
                    mc.textRenderer,
                    line,
                    (int)x,
                    (int)(y + yOffset),
                    tc,
                    textshadow.get()
            );
            yOffset += mc.textRenderer.fontHeight;
        }

        matrices.pop();
    }
}