package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

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
            .defaultValue(3)
            .min(0)
            .sliderRange(0, 10)
            .build()
    );
    public final Setting<Integer> placelavatickdelay = sgLAVA.add(new IntSetting.Builder()
            .name("Lava Placement Tick Delay")
            .description("Tick Delay for lava placement")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .visible(() -> mode.get() == Mode.LAVA)
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
    public final Setting<Integer> placefiretickdelay = sgFIRE.add(new IntSetting.Builder()
            .name("Fire Placement Tick Delay")
            .description("Tick Delay for fire placement")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .visible(() -> mode.get() == Mode.FIRE)
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
            .visible(lavaeverything::get)
            .build()
    );
    private final Setting<Boolean> ignorebelowplayer = sgBurnEverything.add(new BoolSetting.Builder()
            .name("Burn Only Above Player Y Level")
            .description("Lava or set fire to only the blocks above your Y level.")
            .defaultValue(true)
            .visible(lavaeverything::get)
            .build()
    );
    private final Setting<List<Block>> skippableBlox = sgBurnEverything.add(new BlockListSetting.Builder()
            .name("Blocks to Skip")
            .description("Skips burning these blocks.")
            .defaultValue(Blocks.SHORT_GRASS, Blocks.TALL_GRASS)
            .visible(lavaeverything::get)
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
    private int placementTicks = 0;
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player != null && !mc.player.isAlive() || PlayerUtils.getGameMode() == GameType.SPECTATOR) return;
        if (pauseOnLag.get() && TickRate.INSTANCE.getTimeSinceLastTick() >= 1f) return;
        float originalYaw = mc.player.getYRot();
        float originalPitch = mc.player.getXRot();
        placementTicks++;
        // Convert the Iterable to a List and then stream it
        List<Entity> targetedEntities = new ArrayList<>();
        for (Entity entity : this.mc.level.entitiesForRendering()) {
            targetedEntities.add(entity);
        }

        // Sort entities based on distance to the player
        List<Entity> sortedEntities = targetedEntities.stream()
                .filter(entity -> entity instanceof Entity && entity != mc.player
                        && (entities.get().contains(entity.getType()) || (trollfriends.get() && entity instanceof Player && !Friends.get().isFriend((Player) entity))))
                .sorted(Comparator.comparingDouble(entity -> mc.player.position().distanceTo(entity.position())))
                .collect(Collectors.toList());

        // Limit the number of targets based on the maxtargets setting
        int targets = 0;
        if (!lavaeverything.get()){
            for (Entity entity : sortedEntities) {
                if (targets >= maxtargets.get()) {
                    break;
                }
                if (entity instanceof Entity && entity != mc.player) {
                    if (!entities.get().contains(entity.getType()) || (!trollfriends.get() && entity instanceof Player && Friends.get().isFriend((Player) entity)))
                        continue;
                    Entity targetEntity = entity;
                    Vec3 targetPos = targetEntity.position();

                    double distance = mc.player.position().distanceTo(entity.position());

                    if (mode.get() == Mode.LAVA || (mode.get() == Mode.FIRE && !ignorewalls.get())) {
                        BlockHitResult blockHitResult = mc.level.clip(new ClipContext(
                                mc.player.getEyePosition(1.0f),
                                targetPos,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.ANY,
                                mc.player
                        ));

                        if (blockHitResult.getType() == HitResult.Type.MISS) {
                            if (distance <= range.get() && distance > noburnrange.get()) {
                                BlockPos targetBlockPos = BlockPos.containing(targetPos);

                                if (mc.level.getBlockState(targetBlockPos).getBlock() != Blocks.WATER && mc.level.getBlockState(targetBlockPos).getBlock() != Blocks.LAVA) {
                                    Block blockBelow = mc.level.getBlockState(targetBlockPos.below()).getBlock();
                                    if (mode.get() == Mode.LAVA) {
                                        if (nolavaburning.get() && !entity.isOnFire() && placementTicks >= placelavatickdelay.get()){
                                            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
                                            placeLava();
                                            placementTicks=0;
                                        }
                                        else if (!nolavaburning.get() && placementTicks >= placelavatickdelay.get()){
                                            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
                                            placeLava();
                                            placementTicks=0;
                                        }
                                    } else if ((!mc.player.isShiftKeyDown() &&
                                            !(blockBelow instanceof AbstractFurnaceBlock ||
                                                    blockBelow instanceof SignBlock ||
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
                                                    (PermissionUtils.getPermissionLevel(mc.player) >= 2 && blockBelow instanceof CommandBlock) ||
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
                                                    blockBelow instanceof CeilingHangingSignBlock ||
                                                    blockBelow instanceof HopperBlock ||
                                                    blockBelow instanceof LecternBlock ||
                                                    blockBelow instanceof LeverBlock ||
                                                    (PermissionUtils.getPermissionLevel(mc.player) >= 2 && blockBelow instanceof LightBlock) ||
                                                    blockBelow instanceof LoomBlock ||
                                                    blockBelow instanceof NoteBlock ||
                                                    blockBelow instanceof RedStoneOreBlock ||
                                                    blockBelow instanceof RedStoneWireBlock ||
                                                    blockBelow instanceof RepeaterBlock ||
                                                    blockBelow instanceof RespawnAnchorBlock ||
                                                    blockBelow instanceof ShulkerBoxBlock ||
                                                    blockBelow instanceof SmithingTableBlock ||
                                                    blockBelow instanceof StonecutterBlock ||
                                                    blockBelow instanceof SweetBerryBushBlock ||
                                                    blockBelow instanceof TntBlock ||
                                                    blockBelow instanceof TrapDoorBlock ||
                                                    blockBelow instanceof WallHangingSignBlock) &&
                                            !blockHasOnUseMethod(mc.level.getBlockState(targetBlockPos).getBlock()) && mode.get() == Mode.FIRE) ||
                                            mc.player.isShiftKeyDown() && mode.get() == Mode.FIRE) {
                                        if (placementTicks >= placefiretickdelay.get()){
                                            if (!norotate.get())
                                                mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
                                            if (noburnburning.get() && !entity.isOnFire()) placeFire(targetBlockPos);
                                            else if (!noburnburning.get()) placeFire(targetBlockPos);
                                            placementTicks=0;
                                        }
                                    }
                                }

                            }
                        }
                    } else if (mode.get() == Mode.FIRE && ignorewalls.get()){
                        if (distance <= range.get() && distance > noburnrange.get()) {
                            BlockPos targetBlockPos = BlockPos.containing(targetPos);

                            Block blockBelow = mc.level.getBlockState(targetBlockPos.below()).getBlock();

                            if ((!mc.player.isShiftKeyDown() &&
                                    !(blockBelow instanceof AbstractFurnaceBlock ||
                                            blockBelow instanceof SignBlock ||
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
                                            (PermissionUtils.getPermissionLevel(mc.player) >= 2 && blockBelow instanceof CommandBlock) ||
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
                                            blockBelow instanceof CeilingHangingSignBlock ||
                                            blockBelow instanceof HopperBlock ||
                                            blockBelow instanceof LecternBlock ||
                                            blockBelow instanceof LeverBlock ||
                                            (PermissionUtils.getPermissionLevel(mc.player) >= 2 && blockBelow instanceof LightBlock) ||
                                            blockBelow instanceof LoomBlock ||
                                            blockBelow instanceof NoteBlock ||
                                            blockBelow instanceof RedStoneOreBlock ||
                                            blockBelow instanceof RedStoneWireBlock ||
                                            blockBelow instanceof RepeaterBlock ||
                                            blockBelow instanceof RespawnAnchorBlock ||
                                            blockBelow instanceof ShulkerBoxBlock ||
                                            blockBelow instanceof SmithingTableBlock ||
                                            blockBelow instanceof StonecutterBlock ||
                                            blockBelow instanceof SweetBerryBushBlock ||
                                            blockBelow instanceof TntBlock ||
                                            blockBelow instanceof TrapDoorBlock ||
                                            blockBelow instanceof WallHangingSignBlock) &&
                                    mc.level.getBlockState(targetBlockPos).getBlock() != Blocks.WATER &&
                                    mc.level.getBlockState(targetBlockPos).getBlock() != Blocks.LAVA &&
                                    !blockHasOnUseMethod(mc.level.getBlockState(targetBlockPos).getBlock())) ||
                                    (mc.player.isShiftKeyDown() &&
                                            mc.level.getBlockState(targetBlockPos).getBlock() != Blocks.WATER &&
                                            mc.level.getBlockState(targetBlockPos).getBlock() != Blocks.LAVA)) {
                                if (placementTicks >= placefiretickdelay.get()){
                                    if (!norotate.get())
                                        mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
                                    if (noburnburning.get() && !entity.isOnFire()) placeFire(targetBlockPos);
                                    else if (!noburnburning.get()) placeFire(targetBlockPos);
                                    placementTicks=0;
                                }
                            }
                        }
                    }
                }
                targets++;
            }
        }

        if (lavaeverything.get()) {
            BlockPos playerPos = mc.player.blockPosition();

            for (int x = (int) -Math.round(range.get()+1); x <= range.get()+1; x++) {
                for (int y = (int) -Math.round(range.get()+1); y <= range.get()+1; y++) {
                    for (int z = (int) -Math.round(range.get()+1); z <= range.get()+1; z++) {

                        BlockPos blockPos = playerPos.offset(x, y, z);
                        double distance = mc.player.position().distanceTo(blockPos.getCenter());
                        if (distance <= range.get() && distance > noburnrange.get()) {
                            if (mc.level.getBlockState(blockPos).getBlock() != Blocks.AIR && mc.level.getBlockState(blockPos).getBlock() != Blocks.WATER && mc.level.getBlockState(blockPos).getBlock() != Blocks.LAVA) {

                                if (burnflammableonly.get() && !mc.level.getBlockState(blockPos).ignitedByLava()) continue;
                                if (ignorebelowplayer.get() && blockPos.getY()<mc.player.getBlockY()+3) continue;
                                if (skippableBlox.get().contains(mc.level.getBlockState(blockPos).getBlock())) continue;

                                // Check if the block has not had lava placed on it
                                if (!lavaPlaced.contains(blockPos)) {
                                    if (mode.get() == Mode.LAVA && placementTicks >= placelavatickdelay.get()) {
                                        mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                                        placeLava();
                                        lavaPlaced.add(blockPos);
                                    } else if (mode.get() == Mode.FIRE) {
                                        Block blockBelow = mc.level.getBlockState(blockPos.below()).getBlock();

                                        if ((!mc.player.isShiftKeyDown() &&
                                                !(blockBelow instanceof AbstractFurnaceBlock ||
                                                        blockBelow instanceof SignBlock ||
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
                                                        (PermissionUtils.getPermissionLevel(mc.player) >= 2 && blockBelow instanceof CommandBlock) ||
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
                                                        blockBelow instanceof CeilingHangingSignBlock ||
                                                        blockBelow instanceof HopperBlock ||
                                                        blockBelow instanceof LecternBlock ||
                                                        blockBelow instanceof LeverBlock ||
                                                        (PermissionUtils.getPermissionLevel(mc.player) >= 2 && blockBelow instanceof LightBlock) ||
                                                        blockBelow instanceof LoomBlock ||
                                                        blockBelow instanceof NoteBlock ||
                                                        blockBelow instanceof RedStoneOreBlock ||
                                                        blockBelow instanceof RedStoneWireBlock ||
                                                        blockBelow instanceof RepeaterBlock ||
                                                        blockBelow instanceof RespawnAnchorBlock ||
                                                        blockBelow instanceof ShulkerBoxBlock ||
                                                        blockBelow instanceof SmithingTableBlock ||
                                                        blockBelow instanceof StonecutterBlock ||
                                                        blockBelow instanceof SweetBerryBushBlock ||
                                                        blockBelow instanceof TntBlock ||
                                                        blockBelow instanceof TrapDoorBlock ||
                                                        blockBelow instanceof WallHangingSignBlock) &&
                                                !blockHasOnUseMethod(mc.level.getBlockState(blockPos).getBlock())) || mc.player.isShiftKeyDown()) {
                                            if (placementTicks >= placefiretickdelay.get()){
                                                if (!norotate.get())mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                                                placeFire(blockPos.above());
                                                placementTicks=0;
                                                lavaPlaced.add(blockPos);
                                            }
                                        }
                                    };
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
            mc.player.setYRot(originalYaw);
            mc.player.setXRot(originalPitch);
        }
    }
    private boolean blockHasOnUseMethod(Block block) {
        try {
            block.getClass().getDeclaredMethod("onUse", BlockState.class, Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class);
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
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(findItemResult.slot());
        mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prevSlot);
    }
    private void placeFire(BlockPos targetBlockPos) {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);;
        if (fireMode.get() == FireMode.FIRE_CHARGE) {
            findItemResult = InvUtils.findInHotbar(Items.FIRE_CHARGE);
        }

        if (!findItemResult.found() || mc.player == null || mc.gameMode == null) {
            return; // Exit if the required item is not found
        }
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(findItemResult.slot());
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.UP, targetBlockPos.below(), false));
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.DOWN, targetBlockPos.below(), false));
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.NORTH, targetBlockPos.below(), false));
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.SOUTH, targetBlockPos.below(), false));
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.EAST, targetBlockPos.below(), false));
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()), Direction.WEST, targetBlockPos.below(), false));

        mc.player.getInventory().setSelectedSlot(prevSlot);
    }
    private void pickUpLavaOnTick() {
        if (mc.player == null || mc.level == null) return;
        BlockPos playerPos = mc.player.blockPosition();
        for (int x = (int) -Math.round(range.get()+1); x <= range.get()+1; x++) {
            for (int y = (int) -Math.round(range.get()+1); y <= range.get()+1; y++) {
                for (int z = (int) -Math.round(range.get()+1); z <= range.get()+1; z++) {
                    BlockPos blockPos = playerPos.offset(x, y, z);
                    BlockState blockState = mc.level.getBlockState(blockPos);
                    double distance = mc.player.position().distanceTo(blockPos.getCenter());
                    if (distance <= range.get()) {
                        if (blockState.getFluidState().is(Fluids.LAVA)) {
                            // Perform a raycast to check for obstructions
                            BlockHitResult blockHitResult = mc.level.clip(new ClipContext(
                                    mc.player.getEyePosition(1.0f),
                                    new Vec3(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()),
                                    ClipContext.Block.COLLIDER,
                                    ClipContext.Fluid.NONE,
                                    mc.player
                            ));

                            if (blockHitResult.getType() == HitResult.Type.MISS) {
                                mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()));
                                pickupLiquid();
                            }
                        }
                    }
                }
            }
        }
    }
    private void extinguishFireOnTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        BlockPos playerPos = mc.player.blockPosition();
        for (int x = (int) -Math.round(range.get()+1); x <= range.get()+1; x++) {
            for (int y = (int) -Math.round(range.get()+1); y <= range.get()+1; y++) {
                for (int z = (int) -Math.round(range.get()+1); z <= range.get()+1; z++) {
                    BlockPos blockPos = playerPos.offset(x, y, z);
                    BlockState blockState = mc.level.getBlockState(blockPos);
                    double distance = mc.player.position().distanceTo(blockPos.getCenter());
                    if (distance <= range.get()) {
                        if (blockState.getBlock() == Blocks.FIRE) {
                            if (!ignorewalls.get()){
                                // Perform a raycast to check for obstructions
                                BlockHitResult blockHitResult = mc.level.clip(new ClipContext(
                                        mc.player.getEyePosition(1.0f),
                                        new Vec3(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()),
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        mc.player
                                ));

                                if (blockHitResult.getType() == HitResult.Type.MISS) {
                                    if (!norotate.get()) mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()));
                                    mc.gameMode.startDestroyBlock(blockPos, Direction.DOWN);
                                }
                            } else if (ignorewalls.get()){
                                if (!norotate.get()) mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()));
                                mc.gameMode.startDestroyBlock(blockPos, Direction.DOWN);
                            }
                        }
                    }
                }
            }
        }
    }
    private void pickupLiquid() {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.BUCKET);
        if (!findItemResult.found() || mc.player == null || mc.gameMode == null) {
            return;
        }
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(findItemResult.slot());
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prevSlot);
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