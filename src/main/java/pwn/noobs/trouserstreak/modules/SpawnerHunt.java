package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpawnerHunt extends Module {
    private static final int PICKUP_TIMEOUT_TICKS = 200;
    private static final double PICKUP_SEARCH_RANGE = 8.0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<String> mobFilter = sgGeneral.add(new StringSetting.Builder()
        .name("mob-filter")
        .description("Only targets spawners whose mob id exactly matches this value.")
        .defaultValue("minecraft:skeleton")
        .build()
    );

    private final Setting<Boolean> useBaritone = sgAutomation.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("Uses Meteor's path manager (Baritone when available) to route to the nearest matching spawner.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreY = sgAutomation.add(new BoolSetting.Builder()
        .name("ignore-y")
        .description("When enabled, pathing only targets X/Z and ignores Y.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> dynamicReroute = sgAutomation.add(new BoolSetting.Builder()
        .name("dynamic-reroute")
        .description("Automatically reroutes when a newly detected spawner is meaningfully closer.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> rerouteAdvantage = sgAutomation.add(new DoubleSetting.Builder()
        .name("reroute-advantage")
        .description("How many blocks closer a new spawner must be before rerouting.")
        .defaultValue(5.0)
        .min(0)
        .sliderMax(32)
        .build()
    );

    private final Setting<Integer> repathDelay = sgAutomation.add(new IntSetting.Builder()
        .name("repath-delay")
        .description("Ticks between path refreshes while traveling.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> autoMine = sgAutomation.add(new BoolSetting.Builder()
        .name("auto-mine")
        .description("Automatically starts mining the target spawner when in range.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> requireSilkTouch = sgAutomation.add(new BoolSetting.Builder()
        .name("require-silk-touch")
        .description("Only mines using a Silk Touch pickaxe unless disabled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> verifySpawnerPickup = sgAutomation.add(new BoolSetting.Builder()
        .name("verify-spawner-pickup")
        .description("Verifies that mined spawners were picked up and attempts to collect dropped spawner items if needed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> exploreWhenNoSpawners = sgAutomation.add(new BoolSetting.Builder()
        .name("explore-when-no-spawners")
        .description("Randomly explores until a matching spawner is detected.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> exploreRadius = sgAutomation.add(new IntSetting.Builder()
        .name("explore-radius")
        .description("Radius in blocks used when picking random exploration destinations.")
        .defaultValue(96)
        .min(16)
        .sliderRange(16, 512)
        .visible(exploreWhenNoSpawners::get)
        .build()
    );

    private final Setting<Integer> exploreRepathDelay = sgAutomation.add(new IntSetting.Builder()
        .name("explore-repath-delay")
        .description("Ticks before selecting a new random exploration destination.")
        .defaultValue(100)
        .min(10)
        .sliderRange(10, 400)
        .visible(exploreWhenNoSpawners::get)
        .build()
    );

    private final Setting<Double> mineRange = sgAutomation.add(new DoubleSetting.Builder()
        .name("mine-range")
        .description("Distance in blocks at which the module starts mining the current target.")
        .defaultValue(1.5)
        .min(1)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> tracers = sgRender.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draws a tracer from your eye position to each matching spawner.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> box = sgRender.add(new BoolSetting.Builder()
        .name("box")
        .description("Draws a 1x1x1 box around each matching spawner.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> tracerColor = sgRender.add(new ColorSetting.Builder()
        .name("tracer-color")
        .description("Tracer color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> boxColor = sgRender.add(new ColorSetting.Builder()
        .name("box-color")
        .description("Box color.")
        .defaultValue(new SettingColor(255, 80, 80, 75))
        .build()
    );

    private final List<BlockPos> matchingSpawners = new ArrayList<>();
    private final Map<BlockPos, String> fallbackEntityIdCache = new HashMap<>();

    private BlockPos currentTarget;
    private BlockPos explorationTarget;
    private BlockPos pendingPickupTarget;
    private boolean pathOwnedByModule;
    private boolean warnedBaritoneUnavailable;
    private int expectedSpawnerItemCount;
    private int exploreTicks;
    private int pickupTicks;
    private int pickupPathRefreshTicks;
    private int ticksSincePathRefresh;
    private int silkWarningCooldown;

    public SpawnerHunt() {
        super(Trouser.baseHunting, "SpawnerHunt", "Routes to and mines mob spawners filtered by mob type.");
    }

    @Override
    public void onDeactivate() {
        matchingSpawners.clear();
        fallbackEntityIdCache.clear();
        currentTarget = null;
        clearExploration();
        clearPickupVerification();
        warnedBaritoneUnavailable = false;
        ticksSincePathRefresh = 0;
        silkWarningCooldown = 0;
        stopOwnedPathing();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null) {
            matchingSpawners.clear();
            fallbackEntityIdCache.clear();
            currentTarget = null;
            clearExploration();
            clearPickupVerification();
            stopOwnedPathing();
            return;
        }

        if (silkWarningCooldown > 0) silkWarningCooldown--;

        if (!verifySpawnerPickup.get() && pendingPickupTarget != null) {
            clearPickupVerification();
            stopOwnedPathing();
        }

        if (verifySpawnerPickup.get() && handlePickupVerification()) return;

        updateMatchingSpawners();

        if (!useBaritone.get()) {
            currentTarget = null;
            stopOwnedPathing();
            return;
        }

        if (!BaritoneUtils.IS_AVAILABLE) {
            currentTarget = null;
            stopOwnedPathing();

            if (!warnedBaritoneUnavailable) {
                Trouser.LOG.warn("[SpawnerHunt] Baritone path manager is not available.");
                warnedBaritoneUnavailable = true;
            }

            return;
        }

        warnedBaritoneUnavailable = false;

        if (matchingSpawners.isEmpty()) {
            currentTarget = null;

            if (exploreWhenNoSpawners.get()) {
                handleExploration();
            } else {
                clearExploration();
                stopOwnedPathing();
            }

            return;
        }

        clearExploration();

        BlockPos nearest = findNearestSpawner();
        if (nearest == null) {
            currentTarget = null;
            stopOwnedPathing();
            return;
        }

        if (currentTarget == null || !matchingSpawners.contains(currentTarget)) {
            currentTarget = nearest;
            pathToCurrentTarget();
        } else if (dynamicReroute.get() && shouldReroute(nearest)) {
            currentTarget = nearest;
            pathToCurrentTarget();
        } else if (!isWithinMineRange(currentTarget)) {
            ticksSincePathRefresh++;
            if (!PathManagers.get().isPathing() || ticksSincePathRefresh >= repathDelay.get()) {
                pathToCurrentTarget();
            }
        }

        if (currentTarget != null && isWithinMineRange(currentTarget)) {
            stopOwnedPathing();

            if (autoMine.get()) {
                int beforeMineSpawnerCount = verifySpawnerPickup.get() ? countSpawnerItemsInInventory() : -1;
                mineTargetSpawner(currentTarget);

                if (!mc.level.getBlockState(currentTarget).is(Blocks.SPAWNER)) {
                    if (verifySpawnerPickup.get()) beginPickupVerification(currentTarget, beforeMineSpawnerCount);
                    currentTarget = null;
                }
            }
        }
    }

    private boolean handlePickupVerification() {
        if (pendingPickupTarget == null) return false;

        if (mc.level == null || mc.player == null) {
            clearPickupVerification();
            stopOwnedPathing();
            return false;
        }

        if (countSpawnerItemsInInventory() > expectedSpawnerItemCount) {
            clearPickupVerification();
            stopOwnedPathing();
            return false;
        }

        pickupTicks++;

        if (pickupTicks >= PICKUP_TIMEOUT_TICKS) {
            Trouser.LOG.warn("[SpawnerHunt] Timed out trying to confirm pickup for mined spawner at {}.", pendingPickupTarget.toShortString());
            clearPickupVerification();
            stopOwnedPathing();
            return false;
        }

        ItemEntity drop = findNearestSpawnerDrop();
        if (drop == null) {
            return true;
        }

        if (BaritoneUtils.IS_AVAILABLE) {
            pickupPathRefreshTicks++;

            if (!PathManagers.get().isPathing() || pickupPathRefreshTicks >= repathDelay.get()) {
                PathManagers.get().moveTo(drop.blockPosition(), false);
                pathOwnedByModule = true;
                pickupPathRefreshTicks = 0;
            }
        }

        return true;
    }

    private void handleExploration() {
        if (mc.player == null || !BaritoneUtils.IS_AVAILABLE) return;

        exploreTicks++;

        if (explorationTarget == null || !PathManagers.get().isPathing() || exploreTicks >= exploreRepathDelay.get()) {
            explorationTarget = createRandomExploreTarget();
            PathManagers.get().moveTo(explorationTarget, true);
            pathOwnedByModule = true;
            exploreTicks = 0;
        }
    }

    private BlockPos createRandomExploreTarget() {
        if (mc.player == null) return BlockPos.ZERO;

        double angle = Math.random() * (Math.PI * 2);
        int radius = exploreRadius.get();
        double distance = 8 + Math.random() * Math.max(1, radius - 8);

        int x = (int) Math.floor(mc.player.getX() + Math.cos(angle) * distance);
        int y = mc.player.getBlockY();
        int z = (int) Math.floor(mc.player.getZ() + Math.sin(angle) * distance);

        return new BlockPos(x, y, z);
    }

    private void clearExploration() {
        explorationTarget = null;
        exploreTicks = 0;
    }

    private void beginPickupVerification(BlockPos target, int countBeforeMine) {
        pendingPickupTarget = target.immutable();
        expectedSpawnerItemCount = Math.max(0, countBeforeMine);
        pickupTicks = 0;
        pickupPathRefreshTicks = 0;
    }

    private void clearPickupVerification() {
        pendingPickupTarget = null;
        expectedSpawnerItemCount = 0;
        pickupTicks = 0;
        pickupPathRefreshTicks = 0;
    }

    private int countSpawnerItemsInInventory() {
        if (mc.player == null) return 0;

        int count = 0;

        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isSpawnerItem(stack)) count += stack.getCount();
        }

        return count;
    }

    private ItemEntity findNearestSpawnerDrop() {
        if (mc.level == null || mc.player == null || pendingPickupTarget == null) return null;

        AABB searchBox = new AABB(pendingPickupTarget).inflate(PICKUP_SEARCH_RANGE);
        List<ItemEntity> drops = mc.level.getEntitiesOfClass(ItemEntity.class, searchBox, item -> isSpawnerItem(item.getItem()));

        ItemEntity nearest = null;
        double bestDistSq = Double.MAX_VALUE;

        for (ItemEntity drop : drops) {
            double distSq = mc.player.distanceToSqr(drop.getX(), drop.getY(), drop.getZ());
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = drop;
            }
        }

        return nearest;
    }

    private boolean isSpawnerItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() == Blocks.SPAWNER.asItem();
    }

    private void updateMatchingSpawners() {
        String filter = mobFilter.get().trim();
        matchingSpawners.clear();

        if (filter.isEmpty()) return;

        Set<BlockPos> seenSpawners = new HashSet<>();

        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (!(blockEntity instanceof SpawnerBlockEntity spawner)) continue;

            BlockPos pos = spawner.getBlockPos().immutable();
            seenSpawners.add(pos);

            String entityId = resolveEntityId(spawner, pos);
            if (entityId != null && entityId.equals(filter)) matchingSpawners.add(pos);
        }

        fallbackEntityIdCache.keySet().removeIf(pos -> !seenSpawners.contains(pos));
    }

    private BlockPos findNearestSpawner() {
        if (mc.player == null || matchingSpawners.isEmpty()) return null;

        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos pos : matchingSpawners) {
            double distSq = squaredDistanceTo(pos);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = pos;
            }
        }

        return nearest;
    }

    private boolean shouldReroute(BlockPos candidate) {
        if (candidate == null || currentTarget == null || candidate.equals(currentTarget)) return false;

        double currentDistSq = squaredDistanceTo(currentTarget);
        double candidateDistSq = squaredDistanceTo(candidate);
        double advantageSq = rerouteAdvantage.get() * rerouteAdvantage.get();

        return candidateDistSq + advantageSq < currentDistSq;
    }

    private double squaredDistanceTo(BlockPos pos) {
        if (mc.player == null) return Double.MAX_VALUE;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        return mc.player.distanceToSqr(x, y, z);
    }

    private boolean isWithinMineRange(BlockPos pos) {
        double range = mineRange.get();
        return squaredDistanceTo(pos) <= range * range;
    }

    private void pathToCurrentTarget() {
        if (currentTarget == null || !BaritoneUtils.IS_AVAILABLE) return;

        PathManagers.get().moveTo(currentTarget, ignoreY.get());
        pathOwnedByModule = true;
        ticksSincePathRefresh = 0;
    }

    private void stopOwnedPathing() {
        if (!pathOwnedByModule) return;
        if (BaritoneUtils.IS_AVAILABLE) PathManagers.get().stop();
        pathOwnedByModule = false;
    }

    private void mineTargetSpawner(BlockPos pos) {
        if (mc.level == null || !mc.level.getBlockState(pos).is(Blocks.SPAWNER)) return;

        FindItemResult tool = findMiningTool();

        if (requireSilkTouch.get() && !tool.found()) {
            if (silkWarningCooldown == 0) {
                Trouser.LOG.info("[SpawnerHunt] Reached spawner at {} but no Silk Touch pickaxe is in hotbar.", pos.toShortString());
                silkWarningCooldown = 40;
            }
            return;
        }

        if (tool.found() && !tool.isMainHand()) {
            InvUtils.swap(tool.slot(), false);
        }

        BlockUtils.breakBlock(pos, true);
    }

    private FindItemResult findMiningTool() {
        if (requireSilkTouch.get()) {
            return InvUtils.findInHotbar(this::isSilkTouchPickaxe);
        }

        return InvUtils.findInHotbar(stack -> stack.is(ItemTags.PICKAXES));
    }

    private boolean isSilkTouchPickaxe(ItemStack stack) {
        if (mc.level == null || !stack.is(ItemTags.PICKAXES)) return false;

        var enchantmentRegistry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantmentRegistry.getOrThrow(Enchantments.SILK_TOUCH), stack) > 0;
    }

    private String resolveEntityId(SpawnerBlockEntity spawner, BlockPos pos) {
        String fromSpawner = readEntityIdFromSpawner(spawner);
        if (fromSpawner != null) {
            fallbackEntityIdCache.put(pos, fromSpawner);
            return fromSpawner;
        }

        return fallbackEntityIdCache.get(pos);
    }

    private String readEntityIdFromSpawner(SpawnerBlockEntity spawner) {
        try {
            if (spawner.getSpawner().nextSpawnData == null) return null;
            CompoundTag entityTag = spawner.getSpawner().nextSpawnData.getEntityToSpawn();
            if (entityTag == null || !entityTag.contains("id")) return null;
            return entityTag.getString("id").orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.level == null || mc.player == null || matchingSpawners.isEmpty() || RenderUtils.center == null) return;

        for (BlockPos pos : matchingSpawners) {
            if (box.get()) {
                event.renderer.box(pos, boxColor.get(), boxColor.get(), ShapeMode.Both, 0);
            }

            if (tracers.get()) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5;

                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x, y, z, tracerColor.get());
            }
        }
    }
}
