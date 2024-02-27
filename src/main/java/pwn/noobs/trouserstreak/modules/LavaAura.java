package pwn.noobs.trouserstreak.modules;

import com.sun.jdi.event.BreakpointEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;
import java.util.stream.Collectors;

public class LavaAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLAVA = settings.createGroup("LAVA Options");
    private final SettingGroup sgFIRE = settings.createGroup("FIRE Options");
    private final SettingGroup sgBurnEverything = settings.createGroup("BurnEverything Options");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Selects the mode for placing around entities - Lava or Fire.")
            .defaultValue(Mode.LAVA)
            .build()
    );
    private final Setting<FireMode> fireMode = sgGeneral.add(new EnumSetting.Builder<FireMode>()
            .name("fire-mode")
            .description("Selects the fire mode for placing fire - Flint and Steel or Fire Charge.")
            .defaultValue(FireMode.FLINT_AND_STEEL)
            .visible(() -> mode.get() == Mode.FIRE)
            .build()
    );
    public final Setting<Boolean> noburnburning = sgFIRE.add(new BoolSetting.Builder()
            .name("No Burn Already Burning")
            .description("Do not burn already burning entities")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.FIRE)
            .build()
    );
    public final Setting<Boolean> nolavaburning = sgLAVA.add(new BoolSetting.Builder()
            .name("No Lava Already Burning")
            .description("Do not Lava already burning entities")
            .defaultValue(false)
            .visible(() -> mode.get() == Mode.LAVA)
            .build()
    );
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to Lava.")
            .defaultValue(EntityType.PLAYER, EntityType.VILLAGER)
            .build()
    );
    public final Setting<Boolean> trollfriends = sgGeneral.add(new BoolSetting.Builder()
            .name("Lava/Burn Friends")
            .description("Lava bucket your friends too")
            .defaultValue(false)
            .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Lava/Fire placement range.")
            .defaultValue(6)
            .min(2)
            .sliderRange(2, 10)
            .build()
    );
    private final Setting<Double> noburnrange = sgGeneral.add(new DoubleSetting.Builder()
            .name("Dont Burn Range")
            .description("Range around player to not burn.")
            .defaultValue(2.25)
            .min(0)
            .sliderRange(0, 10)
            .build()
    );
    public final Setting<Boolean> pickup = sgLAVA.add(new BoolSetting.Builder()
            .name("Pickup Lava")
            .description("pickup lava after placing")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.LAVA)
            .build()
    );
    public final Setting<Integer> pickuptickdelay = sgLAVA.add(new IntSetting.Builder()
            .name("Lava Pickup Tick Delay")
            .description("Tick Delay for lava pickup")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .visible(() -> pickup.get() && mode.get() == Mode.LAVA)
            .build()
    );
    public final Setting<Boolean> extinguish = sgFIRE.add(new BoolSetting.Builder()
            .name("Extinguish Fire")
            .description("extinguish fire after placing")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.FIRE)
            .build()
    );
    public final Setting<Integer> extinguishtickdelay = sgFIRE.add(new IntSetting.Builder()
            .name("Extinguish Fire Tick Delay")
            .description("Tick Delay for Extinguish Fire")
            .defaultValue(5)
            .min(0)
            .sliderMax(20)
            .visible(() -> extinguish.get() && mode.get() == Mode.FIRE)
            .build()
    );
    public final Setting<Boolean> norotate = sgFIRE.add(new BoolSetting.Builder()
            .name("No Rotations")
            .description("do not rotate to the target to burn them")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.FIRE)
            .build()
    );
    public final Setting<Boolean> ignorewalls = sgFIRE.add(new BoolSetting.Builder()
            .name("Ignore Walls")
            .description("burn things through walls")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.FIRE)
            .build()
    );
    public final Setting<Integer> maxtargets = sgGeneral.add(new IntSetting.Builder()
            .name("Max Targets")
            .description("Maximum targets to lava at a time")
            .defaultValue(6)
            .min(1)
            .sliderMax(20)
            .build()
    );
    private final Setting<Boolean> lavaeverything = sgBurnEverything.add(new BoolSetting.Builder()
            .name("Lava/Burn-Everything")
            .description("Lava or set fire to all the blocks. Creative mode recommended.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> burnflammableonly = sgBurnEverything.add(new BoolSetting.Builder()
            .name("Target Flammable Only")
            .description("Lava or set fire to only the flammable blocks.")
            .defaultValue(true)
            .visible(() -> lavaeverything.get())
            .build()
    );
    private final Setting<Boolean> ignorebelowplayer = sgBurnEverything.add(new BoolSetting.Builder()
            .name("Burn Only Above Player Y Level")
            .description("Lava or set fire to only the blocks above your Y level.")
            .defaultValue(true)
            .visible(() -> lavaeverything.get())
            .build()
    );
    private final Setting<List<Block>> skippableBlox = sgBurnEverything.add(new BlockListSetting.Builder()
            .name("Blocks to Skip")
            .description("Skips burning these blocks.")
            .defaultValue(Blocks.SHORT_GRASS, Blocks.TALL_GRASS)
            .visible(() -> lavaeverything.get())
            .build()
    );
    private final Setting<Boolean> pauseOnLag = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-lag")
            .description("Pauses if the server is lagging.")
            .defaultValue(true)
            .build()
    );

    public LavaAura() {
        super(Trouser.Main, "lava-aura", "Places lava buckets around you repeatedly.");
    }
    private Set<BlockPos> lavaPlaced = new HashSet<>();
    private int ticks = 0;
    private int fireticks = 0;
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;
        if (pauseOnLag.get() && TickRate.INSTANCE.getTimeSinceLastTick() >= 1f) return;
        float originalYaw = mc.player.getYaw();
        float originalPitch = mc.player.getPitch();

        // Convert the Iterable to a List and then stream it
        List<Entity> targetedEntities = new ArrayList<>();
        for (Entity entity : this.mc.world.getEntities()) {
            targetedEntities.add(entity);
        }

        // Sort entities based on distance to the player
        List<Entity> sortedEntities = targetedEntities.stream()
                .filter(entity -> entity instanceof Entity && entity != mc.player
                        && (entities.get().contains(entity.getType()) || (trollfriends.get() && entity instanceof PlayerEntity && !Friends.get().isFriend((PlayerEntity) entity))))
                .sorted(Comparator.comparingDouble(entity -> mc.player.getPos().distanceTo(entity.getPos())))
                .collect(Collectors.toList());

        // Limit the number of targets based on the maxtargets setting
        int targets = 0;
        if (!lavaeverything.get()){
            for (Entity entity : sortedEntities) {
                if (targets >= maxtargets.get()) {
                    break;
                }
                if (entity instanceof Entity && entity != mc.player) {
                    if (!entities.get().contains(entity.getType()) || (!trollfriends.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)))
                        continue;
                    Entity targetEntity = entity;
                    Vec3d targetPos = targetEntity.getPos();

                    double distance = mc.player.getPos().distanceTo(entity.getPos());

                    if (mode.get() == Mode.LAVA || (mode.get() == Mode.FIRE && !ignorewalls.get())) {
                        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(
                                mc.player.getCameraPosVec(1.0f),
                                targetPos,
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.ANY,
                                mc.player
                        ));

                        if (blockHitResult.getType() == HitResult.Type.MISS) {
                            if (distance <= range.get() && distance > noburnrange.get()) {
                                BlockPos targetBlockPos = BlockPos.ofFloored(targetPos);

                                if (mc.world.getBlockState(targetBlockPos).getBlock() != Blocks.WATER && mc.world.getBlockState(targetBlockPos).getBlock() != Blocks.LAVA) {
                                        Block blockBelow = mc.world.getBlockState(targetBlockPos.down()).getBlock();
                                        if (mode.get() == Mode.LAVA) {
                                            mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos);
                                            if (nolavaburning.get() && !entity.isOnFire()) placeLava();
                                            else if (!nolavaburning.get()) placeLava();
                                        } else if ((!mc.player.isSneaking() &&
                                                !(blockBelow instanceof AbstractFurnaceBlock ||
                                                        blockBelow instanceof AbstractSignBlock ||
                                                        blockBelow instanceof AnvilBlock ||
                                                        blockBelow instanceof BarrelBlock ||
                                                        blockBelow instanceof BeaconBlock ||
                                                        blockBelow instanceof BedBlock ||
                                                        blockBelow instanceof BellBlock ||
                                                        blockBelow instanceof BrewingStandBlock ||
                                                        blockBelow instanceof ButtonBlock ||
                                                        blockBelow instanceof CakeBlock ||
                                                        blockBelow instanceof CampfireBlock ||
                                                        blockBelow instanceof CandleBlock ||
                                                        blockBelow instanceof CandleCakeBlock ||
                                                        blockBelow instanceof CartographyTableBlock ||
                                                        blockBelow instanceof ChestBlock ||
                                                        (mc.player.hasPermissionLevel(2) && blockBelow instanceof CommandBlock) ||
                                                        blockBelow instanceof ComparatorBlock ||
                                                        blockBelow instanceof CrafterBlock ||
                                                        blockBelow instanceof CraftingTableBlock ||
                                                        blockBelow instanceof DaylightDetectorBlock ||
                                                        blockBelow instanceof DecoratedPotBlock ||
                                                        blockBelow instanceof DispenserBlock ||
                                                        blockBelow instanceof DoorBlock ||
                                                        blockBelow instanceof DragonEggBlock ||
                                                        blockBelow instanceof EnchantingTableBlock ||
                                                        blockBelow instanceof EnderChestBlock ||
                                                        blockBelow instanceof FenceBlock ||
                                                        blockBelow instanceof FenceGateBlock ||
                                                        blockBelow instanceof GrindstoneBlock ||
                                                        blockBelow instanceof HangingSignBlock ||
                                                        blockBelow instanceof HopperBlock ||
                                                        blockBelow instanceof LecternBlock ||
                                                        blockBelow instanceof LeverBlock ||
                                                        (mc.player.hasPermissionLevel(2) && blockBelow instanceof LightBlock) ||
                                                        blockBelow instanceof LoomBlock ||
                                                        blockBelow instanceof NoteBlock ||
                                                        blockBelow instanceof RedstoneOreBlock ||
                                                        blockBelow instanceof RedstoneWireBlock ||
                                                        blockBelow instanceof RepeaterBlock ||
                                                        blockBelow instanceof RespawnAnchorBlock ||
                                                        blockBelow instanceof ShulkerBoxBlock ||
                                                        blockBelow instanceof SmithingTableBlock ||
                                                        blockBelow instanceof StonecutterBlock ||
                                                        blockBelow instanceof SweetBerryBushBlock ||
                                                        blockBelow instanceof TntBlock ||
                                                        blockBelow instanceof TrapdoorBlock ||
                                                        blockBelow instanceof WallHangingSignBlock) &&
                                                !blockHasOnUseMethod(mc.world.getBlockState(targetBlockPos).getBlock()) && mode.get() == Mode.FIRE) ||
                                                mc.player.isSneaking() && mode.get() == Mode.FIRE) {
                                            if (!norotate.get())
                                                mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos);
                                            if (noburnburning.get() && !entity.isOnFire()) placeFire(targetBlockPos);
                                            else if (!noburnburning.get()) placeFire(targetBlockPos);
                                        }
                                    }

                            }
                        }
                    } else if (mode.get() == Mode.FIRE && ignorewalls.get()){
                        if (distance <= range.get() && distance > noburnrange.get()) {
                            BlockPos targetBlockPos = BlockPos.ofFloored(targetPos);

                                Block blockBelow = mc.world.getBlockState(targetBlockPos.down()).getBlock();

                                if ((!mc.player.isSneaking() &&
                                        !(blockBelow instanceof AbstractFurnaceBlock ||
                                        blockBelow instanceof AbstractSignBlock ||
                                        blockBelow instanceof AnvilBlock ||
                                        blockBelow instanceof BarrelBlock ||
                                        blockBelow instanceof BeaconBlock ||
                                        blockBelow instanceof BedBlock ||
                                        blockBelow instanceof BellBlock ||
                                        blockBelow instanceof BrewingStandBlock ||
                                        blockBelow instanceof ButtonBlock ||
                                        blockBelow instanceof CakeBlock ||
                                        blockBelow instanceof CampfireBlock ||
                                        blockBelow instanceof CandleBlock ||
                                        blockBelow instanceof CandleCakeBlock ||
                                        blockBelow instanceof CartographyTableBlock ||
                                        blockBelow instanceof ChestBlock ||
                                                (mc.player.hasPermissionLevel(2) && blockBelow instanceof CommandBlock) ||
                                        blockBelow instanceof ComparatorBlock ||
                                        blockBelow instanceof CrafterBlock ||
                                        blockBelow instanceof CraftingTableBlock ||
                                        blockBelow instanceof DaylightDetectorBlock ||
                                        blockBelow instanceof DecoratedPotBlock ||
                                        blockBelow instanceof DispenserBlock ||
                                        blockBelow instanceof DoorBlock ||
                                        blockBelow instanceof DragonEggBlock ||
                                        blockBelow instanceof EnchantingTableBlock ||
                                        blockBelow instanceof EnderChestBlock ||
                                        blockBelow instanceof FenceBlock ||
                                        blockBelow instanceof FenceGateBlock ||
                                        blockBelow instanceof GrindstoneBlock ||
                                        blockBelow instanceof HangingSignBlock ||
                                        blockBelow instanceof HopperBlock ||
                                        blockBelow instanceof LecternBlock ||
                                        blockBelow instanceof LeverBlock ||
                                                (mc.player.hasPermissionLevel(2) && blockBelow instanceof LightBlock) ||
                                        blockBelow instanceof LoomBlock ||
                                        blockBelow instanceof NoteBlock ||
                                        blockBelow instanceof RedstoneOreBlock ||
                                        blockBelow instanceof RedstoneWireBlock ||
                                        blockBelow instanceof RepeaterBlock ||
                                        blockBelow instanceof RespawnAnchorBlock ||
                                        blockBelow instanceof ShulkerBoxBlock ||
                                        blockBelow instanceof SmithingTableBlock ||
                                        blockBelow instanceof StonecutterBlock ||
                                        blockBelow instanceof SweetBerryBushBlock ||
                                        blockBelow instanceof TntBlock ||
                                        blockBelow instanceof TrapdoorBlock ||
                                        blockBelow instanceof WallHangingSignBlock) &&
                                        mc.world.getBlockState(targetBlockPos).getBlock() != Blocks.WATER &&
                                        mc.world.getBlockState(targetBlockPos).getBlock() != Blocks.LAVA &&
                                        !blockHasOnUseMethod(mc.world.getBlockState(targetBlockPos).getBlock())) ||
                                        (mc.player.isSneaking() &&
                                                mc.world.getBlockState(targetBlockPos).getBlock() != Blocks.WATER &&
                                                mc.world.getBlockState(targetBlockPos).getBlock() != Blocks.LAVA)) {
                                    if (!norotate.get())
                                        mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos);
                                    if (noburnburning.get() && !entity.isOnFire()) placeFire(targetBlockPos);
                                    else if (!noburnburning.get()) placeFire(targetBlockPos);
                                }
                        }
                    }
                }
                targets++;
            }
        }

        if (lavaeverything.get()) {
            BlockPos playerPos = mc.player.getBlockPos();

            for (int x = (int) -Math.round(range.get()+1); x <= range.get()+1; x++) {
                for (int y = (int) -Math.round(range.get()+1); y <= range.get()+1; y++) {
                    for (int z = (int) -Math.round(range.get()+1); z <= range.get()+1; z++) {

                        BlockPos blockPos = playerPos.add(x, y, z);
                        double distance = mc.player.getPos().distanceTo(blockPos.toCenterPos());
                        if (distance <= range.get() && distance > noburnrange.get()) {
                            if (mc.world.getBlockState(blockPos).getBlock() != Blocks.AIR && mc.world.getBlockState(blockPos).getBlock() != Blocks.WATER && mc.world.getBlockState(blockPos).getBlock() != Blocks.LAVA) {

                                if (burnflammableonly.get() && !mc.world.getBlockState(blockPos).isBurnable()) continue;
                                if (ignorebelowplayer.get() && blockPos.getY()<mc.player.getBlockY()+3) continue;
                                if (skippableBlox.get().contains(mc.world.getBlockState(blockPos).getBlock())) continue;

                                // Check if the block has not had lava placed on it
                                if (!lavaPlaced.contains(blockPos)) {
                                    if (mode.get() == Mode.LAVA) {
                                        mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                                        placeLava();
                                    } else if (mode.get() == Mode.FIRE) {
                                        Block blockBelow = mc.world.getBlockState(blockPos.down()).getBlock();

                                        if ((!mc.player.isSneaking() &&
                                                !(blockBelow instanceof AbstractFurnaceBlock ||
                                                        blockBelow instanceof AbstractSignBlock ||
                                                        blockBelow instanceof AnvilBlock ||
                                                        blockBelow instanceof BarrelBlock ||
                                                        blockBelow instanceof BeaconBlock ||
                                                        blockBelow instanceof BedBlock ||
                                                        blockBelow instanceof BellBlock ||
                                                        blockBelow instanceof BrewingStandBlock ||
                                                        blockBelow instanceof ButtonBlock ||
                                                        blockBelow instanceof CakeBlock ||
                                                        blockBelow instanceof CampfireBlock ||
                                                        blockBelow instanceof CandleBlock ||
                                                        blockBelow instanceof CandleCakeBlock ||
                                                        blockBelow instanceof CartographyTableBlock ||
                                                        blockBelow instanceof ChestBlock ||
                                                        (mc.player.hasPermissionLevel(2) && blockBelow instanceof CommandBlock) ||
                                                        blockBelow instanceof ComparatorBlock ||
                                                        blockBelow instanceof CrafterBlock ||
                                                        blockBelow instanceof CraftingTableBlock ||
                                                        blockBelow instanceof DaylightDetectorBlock ||
                                                        blockBelow instanceof DecoratedPotBlock ||
                                                        blockBelow instanceof DispenserBlock ||
                                                        blockBelow instanceof DoorBlock ||
                                                        blockBelow instanceof DragonEggBlock ||
                                                        blockBelow instanceof EnchantingTableBlock ||
                                                        blockBelow instanceof EnderChestBlock ||
                                                        blockBelow instanceof FenceBlock ||
                                                        blockBelow instanceof FenceGateBlock ||
                                                        blockBelow instanceof GrindstoneBlock ||
                                                        blockBelow instanceof HangingSignBlock ||
                                                        blockBelow instanceof HopperBlock ||
                                                        blockBelow instanceof LecternBlock ||
                                                        blockBelow instanceof LeverBlock ||
                                                        (mc.player.hasPermissionLevel(2) && blockBelow instanceof LightBlock) ||
                                                        blockBelow instanceof LoomBlock ||
                                                        blockBelow instanceof NoteBlock ||
                                                        blockBelow instanceof RedstoneOreBlock ||
                                                        blockBelow instanceof RedstoneWireBlock ||
                                                        blockBelow instanceof RepeaterBlock ||
                                                        blockBelow instanceof RespawnAnchorBlock ||
                                                        blockBelow instanceof ShulkerBoxBlock ||
                                                        blockBelow instanceof SmithingTableBlock ||
                                                        blockBelow instanceof StonecutterBlock ||
                                                        blockBelow instanceof SweetBerryBushBlock ||
                                                        blockBelow instanceof TntBlock ||
                                                        blockBelow instanceof TrapdoorBlock ||
                                                        blockBelow instanceof WallHangingSignBlock) &&
                                                !blockHasOnUseMethod(mc.world.getBlockState(blockPos).getBlock())) || mc.player.isSneaking()) {
                                                    if (!norotate.get())mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                                                    placeFire(blockPos.up());
                                                }
                                    };

                                    // Add the block to the set to indicate that lava has been placed on it
                                    lavaPlaced.add(blockPos);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (mode.get() == Mode.LAVA && pickup.get() && !lavaeverything.get()){
            if (ticks<pickuptickdelay.get()){
                ticks++;
            } else if (ticks>=pickuptickdelay.get()){
                pickUpLavaOnTick();
                ticks=0;
            }
        }
        else if (mode.get() == Mode.FIRE && extinguish.get() && !lavaeverything.get()){
            if (fireticks<extinguishtickdelay.get()){
                fireticks++;
            } else if (fireticks>=extinguishtickdelay.get()){
                extinguishFireOnTick();
                fireticks=0;
            }
        }
        if (mode.get() == Mode.LAVA || (mode.get() == Mode.FIRE && !norotate.get())){
            mc.player.setYaw(originalYaw);
            mc.player.setPitch(originalPitch);
        }
    }
    private boolean blockHasOnUseMethod(Block block) {
        try {
            block.getClass().getDeclaredMethod("onUse", BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockHitResult.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    private void placeLava() {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.LAVA_BUCKET);
        if (!findItemResult.found()) {
            return;
        }
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = findItemResult.slot();
        mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prevSlot;
    }
    private void placeFire(BlockPos targetBlockPos) {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);;
        if (fireMode.get() == FireMode.FIRE_CHARGE) {
            findItemResult = InvUtils.findInHotbar(Items.FIRE_CHARGE);
        }

        if (!findItemResult.found()) {
            return; // Exit if the required item is not found
        }
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = findItemResult.slot();
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.UP, targetBlockPos.down(), false));
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.DOWN, targetBlockPos.down(), false));
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.NORTH, targetBlockPos.down(), false));
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.SOUTH, targetBlockPos.down(), false));
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.EAST, targetBlockPos.down(), false));
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.WEST, targetBlockPos.down(), false));

        mc.player.getInventory().selectedSlot = prevSlot;
    }
    private void pickUpLavaOnTick() {
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = (int) -Math.round(range.get()+1); x <= range.get()+1; x++) {
            for (int y = (int) -Math.round(range.get()+1); y <= range.get()+1; y++) {
                for (int z = (int) -Math.round(range.get()+1); z <= range.get()+1; z++) {
                    BlockPos blockPos = playerPos.add(x, y, z);
                    BlockState blockState = mc.world.getBlockState(blockPos);
                    double distance = mc.player.getPos().distanceTo(blockPos.toCenterPos());
                    if (distance <= range.get()) {
                        if (blockState.getBlock() == Blocks.LAVA) {
                            // Perform a raycast to check for obstructions
                            BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(
                                    mc.player.getCameraPosVec(1.0f),
                                    new Vec3d(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()),
                                    RaycastContext.ShapeType.COLLIDER,
                                    RaycastContext.FluidHandling.NONE,
                                    mc.player
                            ));

                            if (blockHitResult.getType() == HitResult.Type.MISS) {
                                mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()));
                                pickupLiquid();
                            }
                        }
                    }
                }
            }
        }
    }
    private void extinguishFireOnTick() {
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = (int) -Math.round(range.get()+1); x <= range.get()+1; x++) {
            for (int y = (int) -Math.round(range.get()+1); y <= range.get()+1; y++) {
                for (int z = (int) -Math.round(range.get()+1); z <= range.get()+1; z++) {
                    BlockPos blockPos = playerPos.add(x, y, z);
                    BlockState blockState = mc.world.getBlockState(blockPos);
                    double distance = mc.player.getPos().distanceTo(blockPos.toCenterPos());
                    if (distance <= range.get()) {
                        if (blockState.getBlock() == Blocks.FIRE) {
                            if (!ignorewalls.get()){
                                // Perform a raycast to check for obstructions
                                BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(
                                        mc.player.getCameraPosVec(1.0f),
                                        new Vec3d(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()),
                                        RaycastContext.ShapeType.COLLIDER,
                                        RaycastContext.FluidHandling.NONE,
                                        mc.player
                                ));

                                if (blockHitResult.getType() == HitResult.Type.MISS) {
                                    if (!norotate.get()) mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()));
                                    mc.interactionManager.attackBlock(blockPos, Direction.DOWN);
                                }
                            } else if (ignorewalls.get()){
                                if (!norotate.get()) mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()));
                                mc.interactionManager.attackBlock(blockPos, Direction.DOWN);
                            }
                        }
                    }
                }
            }
        }
    }
    private void pickupLiquid() {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.BUCKET);
        if (!findItemResult.found()) {
            return;
        }
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = findItemResult.slot();
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prevSlot;
    }
    public enum Mode {
        LAVA,
        FIRE
    }
    public enum FireMode {
        FLINT_AND_STEEL,
        FIRE_CHARGE
    }
}