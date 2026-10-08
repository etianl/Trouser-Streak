package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class CubePrimer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAuto = settings.createGroup("Cube Automation");

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Switch to your previous slot after using the items.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> insertBlock = sgGeneral.add(new BoolSetting.Builder()
            .name("insert-block")
            .description("Inserts a block into the cube after priming to make it unkillable. Magma Blocks make it burn people. Soul Soil and Soul Sand make it hard to move.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<Item>> blocks = sgGeneral.add(new ItemListSetting.Builder()
            .name("blocks")
            .description("Blocks to insert into sulfur cubes.")
            .defaultValue(Items.MAGMA_BLOCK, Items.SOUL_SAND, Items.SOUL_SOIL)
            .filter(item -> BuiltInRegistries.ITEM.wrapAsHolder(item).is(ItemTags.SULFUR_CUBE_SWALLOWABLE))
            .visible(insertBlock::get)
            .build()
    );
    private final Setting<Boolean> dumpAllCubes = sgAuto.add(new BoolSetting.Builder()
            .name("cube-hotbar-dumper")
            .description("If you place one Sulfur Cube bucket, automatically select the next for placing.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> dumptickDelay = sgAuto.add(new IntSetting.Builder()
            .name("dumper-tick-delay")
            .description("Delay in ticks between bucket swap/dumps.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .visible(dumpAllCubes::get)
            .build()
    );
    private final Setting<Boolean> tAura = sgAuto.add(new BoolSetting.Builder()
            .name("TNT-insertion-aura")
            .description("Automatically insert TNT into empty Cubes within range.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> tnttickDelay = sgAuto.add(new IntSetting.Builder()
            .name("TNT-tick-delay")
            .description("Delay in ticks between TNT insertion attempts.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .visible(tAura::get)
            .build()
    );
    private final Setting<Boolean> sAura = sgAuto.add(new BoolSetting.Builder()
            .name("shears-aura")
            .description("Automatically shear Cubes that contain a block other than TNT within range.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> sheartickDelay = sgAuto.add(new IntSetting.Builder()
            .name("shears-tick-delay")
            .description("Delay in ticks between shearing attempts.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .visible(sAura::get)
            .build()
    );
    private final Setting<Boolean> pAura = sgAuto.add(new BoolSetting.Builder()
            .name("ignition-aura")
            .description("Automatically ignite TNT Sulfur Cubes within range.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> ignitiontickDelay = sgAuto.add(new IntSetting.Builder()
            .name("ignition-tick-delay")
            .description("Delay in ticks between ignition attempts.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .visible(pAura::get)
            .build()
    );
    private enum Modes {
        PreferFlintAndSteel, PreferFireCharge, FireAspectOnly
    }
    private final Setting<Modes> mode = sgAuto.add(new EnumSetting.Builder<Modes>()
            .name("ignition-method")
            .description("Choose which ignition method will be used first")
            .defaultValue(Modes.PreferFlintAndSteel)
            .visible(pAura::get)
            .build()
    );
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing arm")
            .defaultValue(true)
            .visible(() -> tAura.get() || pAura.get() || sAura.get())
            .build()
    );
    private final Setting<Boolean> rotateToTarget = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate to Target")
            .description("Sends a look packet aimed at the target before doing things. Helps hit registration on servers that check facing direction.")
            .defaultValue(true)
            .visible(() -> tAura.get() || pAura.get() || sAura.get())
            .build()
    );
    private final Setting<Boolean> stopfuckingupthefarm = sgAuto.add(new BoolSetting.Builder()
            .name("disable-near-farm")
            .description("Do not run TNT and Ignition and Shears Aura code when near a specified coordinate.")
            .defaultValue(true)
            .visible(() -> tAura.get() || pAura.get() || sAura.get())
            .build()
    );
    private final Setting<Double> farmDist = sgAuto.add(new DoubleSetting.Builder()
            .name("safe-distance-from-farm")
            .description("Cubes will not be affected by TNT and Ignition and Shears Aura within this distance of the Farm Coordinate")
            .defaultValue(420)
            .min(0)
            .sliderRange(0, 1000)
            .visible(() -> stopfuckingupthefarm.get() && (tAura.get() || pAura.get() || sAura.get()))
            .build()
    );
    private final Setting<BlockPos> farmCoordinates = sgAuto.add(new BlockPosSetting.Builder()
            .name("farm-coordinate")
            .description("Roughly the center of the Sulfur Cube production.")
            .defaultValue(new BlockPos(694206767, 694206767, 694206767))
            .visible(() -> stopfuckingupthefarm.get() && (tAura.get() || pAura.get() || sAura.get()))
            .build()
    );
    private final Setting<Double> reach = sgAuto.add(new DoubleSetting.Builder()
            .name("Reach (blocks)")
            .description("Cube must be within this range")
            .defaultValue(5.5)
            .min(1)
            .sliderRange(1, 10)
            .visible(() -> tAura.get() || pAura.get() || sAura.get())
            .build()
    );
    public final Setting<Boolean> lagpause = sgAuto.add(new BoolSetting.Builder()
            .name("Pause if No Ticks")
            .description("Pause TNT aura and Ignition aura and Shears aura if server is not ticking")
            .defaultValue(true)
            .visible(() -> tAura.get() || pAura.get() || sAura.get())
            .build()
    );
    private final Setting<Double> lag = sgAuto.add(new DoubleSetting.Builder()
            .name("Seconds until pause")
            .description("Pause TNT aura and Ignition aura if server is lagging for this many seconds.")
            .min(0)
            .sliderRange(0, 10)
            .defaultValue(1)
            .visible(() -> (tAura.get() || pAura.get() || sAura.get()) && lagpause.get())
            .build()
    );

    public CubePrimer() {
        super(Trouser.Main, "CubePrimer", "Use a Flint and Steel, Fire Charge, or Fire Aspect enchanted item on a TNT Sulfur Cube while Shears are in your hotbar to make it primed and ready to explode the moment it absorbs a TNT item. If it absorbs any other dropped block in this state it will become unkillable.");
    }

    private int dumperTicks;
    private int tntTicks;
    private int primerTicks;
    private int shearTicks;
    private int pendingSwapSlot = -1;
    private boolean interacting;
    private boolean sendingTNTPacket;
    private boolean tntErrorShown;
    private boolean shearsErrorShown;
    private boolean ignitionErrorShown;

    @Override
    public void onActivate() {
        if (chatFeedback){
            info("Use a Flint and Steel or Fire Charge or Fire Aspect enchanted item while Shears are in your hotbar to prime a TNT Sulfur Cube.");
        }
        dumperTicks = 0;
        tntTicks = 0;
        primerTicks = 0;
        shearTicks = 0;
        pendingSwapSlot = -1;
        interacting = false;
        sendingTNTPacket = false;
        tntErrorShown = false;
        shearsErrorShown = false;
        ignitionErrorShown = false;
    }
    //Cube Primer
    @EventHandler
    private void onPacket(PacketEvent.Send event) {
        if (sendingTNTPacket) return;
        if (event.packet instanceof ServerboundInteractPacket interactPacket) {
            if (interactPacket.hand() == null) return;
            if (mc.player == null || mc.level == null) return;

            if (!(mc.level.getEntity(interactPacket.entityId()) instanceof SulfurCube sulfurCube) || interacting) return;
            if (sulfurCube.isDeadOrDying() || sulfurCube.isBaby() || sulfurCube.isPrimed()) return;
            if (sulfurCube.getBodyArmorItem().getItem() != Items.TNT) return;
            InteractionHand hand = interactPacket.hand();
            if (hand == null) return;

            ItemStack stack = mc.player.getItemInHand(hand);
            if (stack.getItem() != Items.FLINT_AND_STEEL && stack.getItem() != Items.FIRE_CHARGE) return;

            primeCube(sulfurCube, event);
        } else if (event.packet instanceof ServerboundAttackPacket attackPacket) {
            if (mc.player == null || mc.level == null) return;

            if (!(mc.level.getEntity(attackPacket.entityId()) instanceof SulfurCube sulfurCube)) return;
            if (sulfurCube.isDeadOrDying() || sulfurCube.isBaby() || sulfurCube.isPrimed()) return;
            if (sulfurCube.getBodyArmorItem().getItem() != Items.TNT) return;

            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getEnchantments().keySet().stream()
                    .noneMatch(enchantment ->
                            enchantment.unwrapKey()
                                    .map(Enchantments.FIRE_ASPECT::equals)
                                    .orElse(false))) {
                return;
            }

            primeCube(sulfurCube, event);
        }
    }
    private void primeCube(SulfurCube sulfurCube, PacketEvent.Send event) {
        FindItemResult shearsResult = InvUtils.findInHotbar(Items.SHEARS);

        if (!shearsResult.found()) {
            if (chatFeedback) error("You need Shears in your hotbar to finish the priming method.");
            if (event != null) event.cancel();
            return;
        }

        FindItemResult blockResult = null;

        if (insertBlock.get()) {
            for (Item block : blocks.get()) {
                blockResult = InvUtils.findInHotbar(block);
                if (blockResult.found()) break;
            }
            if (blockResult == null || !blockResult.found()) {
                if (chatFeedback) error("Block for Sulfur Cube not found.");
                if (event != null) event.cancel();
                return;
            }
        }

        int previousSlot = mc.player.getInventory().getSelectedSlot();
        try {
            interacting = true;
            InvUtils.swap(shearsResult.slot(), false);
            sendCubePacket(sulfurCube);
            if (insertBlock.get() && blockResult != null && blockResult.found()) {
                InvUtils.swap(blockResult.slot(), false);
                sendCubePacket(sulfurCube);
            }
        } finally {
            interacting = false;
            if (swapBack.get()) InvUtils.swap(previousSlot, false);
        }
    }
    //Cube Automation
    @EventHandler
    private void onInteract(InteractBlockEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!dumpAllCubes.get()) return;
        if (event.hand != InteractionHand.MAIN_HAND) return;

        ItemStack clickedStack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (clickedStack.getItem() != Items.SULFUR_CUBE_BUCKET
                && clickedStack.getItem() != Items.BUCKET) return;

        if (pendingSwapSlot != -1) return;

        pendingSwapSlot = findNextCubeSlot();
        dumperTicks = 0;
    }

    private int findNextCubeSlot() {
        int currentSlot = mc.player.getInventory().getSelectedSlot();

        for (int i = currentSlot + 1; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.SULFUR_CUBE_BUCKET)
                return i;
        }
        for (int i = 0; i < currentSlot; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.SULFUR_CUBE_BUCKET)
                return i;
        }
        return -1;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        if (pendingSwapSlot != -1) {
            dumperTicks++;

            if (dumperTicks >= dumptickDelay.get()) {
                int slot = pendingSwapSlot;

                if (slot >= 0 && slot < 9 && mc.player.getInventory().getItem(slot).getItem() == Items.SULFUR_CUBE_BUCKET) {
                    InvUtils.swap(slot, false);
                }

                pendingSwapSlot = -1;
                dumperTicks = 0;
            }
        } else {
            dumperTicks = 0;
        }

        if (!pAura.get() && !tAura.get() && !sAura.get()) return;

        float timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();

        if (lagpause.get() && timeSinceLastTick >= lag.get()) return;

        boolean nearFarm = false;
        if (stopfuckingupthefarm.get()) {
            BlockPos farm = farmCoordinates.get();
            double distSq = mc.player.position().distanceToSqr(
                    farm.getX() + 0.5,
                    farm.getY() + 0.5,
                    farm.getZ() + 0.5
            );
            double safeDist = farmDist.get();
            nearFarm = distSq <= safeDist * safeDist;
        }

        if (nearFarm) return;

        if (tAura.get()) {
            tntTicks++;
        } else {
            tntTicks = 0;
        }

        if (pAura.get()) {
            primerTicks++;
        } else {
            primerTicks = 0;
        }

        if (sAura.get()) {
            shearTicks++;
        } else {
            shearTicks = 0;
        }

        boolean tntReady = tAura.get() && tntTicks >= tnttickDelay.get();
        boolean ignitionReady = pAura.get() && primerTicks >= ignitiontickDelay.get();
        boolean shearsReady = sAura.get() && shearTicks >= sheartickDelay.get();

        if (!tntReady && !ignitionReady && !shearsReady) return;

        FindItemResult tntResult = tntReady ? InvUtils.findInHotbar(Items.TNT) : null;
        FindItemResult shearResult = shearsReady ? InvUtils.findInHotbar(Items.SHEARS) : null;
        FindItemResult ignitionResult;
        if (ignitionReady) {
            if (mode.get() == Modes.FireAspectOnly) {
                ignitionResult = InvUtils.findInHotbar(itemStack ->
                        itemStack.getEnchantments().keySet().stream()
                                .anyMatch(enchantment ->
                                        enchantment.unwrapKey()
                                                .map(Enchantments.FIRE_ASPECT::equals)
                                                .orElse(false))
                );
            } else {
                ignitionResult = mode.get() == Modes.PreferFlintAndSteel
                        ? InvUtils.findInHotbar(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE)
                        : InvUtils.findInHotbar(Items.FIRE_CHARGE, Items.FLINT_AND_STEEL);
            }
        } else {
            ignitionResult = null;
        }

        FindItemResult blockResultForIgnition = null;

        if (ignitionReady && insertBlock.get()) {
            for (Item block : blocks.get()) {
                blockResultForIgnition = InvUtils.findInHotbar(block);
                if (blockResultForIgnition.found()) break;
            }
        }

        double range = reach.get();
        double rangeSq = range * range;

        AABB searchBox = mc.player.getBoundingBox().inflate(range);

        List<SulfurCube> cubes = mc.level.getEntitiesOfClass(
                SulfurCube.class,
                searchBox,
                cube -> !cube.isBaby()
                        && !cube.isDeadOrDying()
                        && !cube.isPrimed()
        );

        cubes.removeIf(cube -> cube.distanceToSqr(mc.player) > rangeSq);

        if (cubes.isEmpty()) {
            if (tAura.get()) tntTicks = 0;
            if (pAura.get()) primerTicks = 0;
            if (sAura.get()) shearTicks = 0;
            return;
        }

        if (tntReady && !tntResult.found()) {
            if (chatFeedback && !tntErrorShown) {
                error("You need TNT in your hotbar for TNT Aura.");
                tntErrorShown = true;
            }
            tntTicks = 0;
            tntReady = false;
        } else if (tntReady) {
            tntErrorShown = false;
        }

        if (shearsReady && !shearResult.found()) {
            if (chatFeedback && !shearsErrorShown) {
                error("You need Shears in your hotbar for Shears Aura.");
                shearsErrorShown = true;
            }
            shearTicks = 0;
            shearsReady = false;
        } else if (shearsReady) {
            shearsErrorShown = false;
        }

        if (ignitionReady && !ignitionResult.found()) {
            if (chatFeedback && !ignitionErrorShown) {
                error("You need an ignition method in your hotbar for Ignition Aura.");
                ignitionErrorShown = true;
            }
            primerTicks = 0;
            ignitionReady = false;
        } else if (ignitionReady) {
            ignitionErrorShown = false;
        }

        if (ignitionReady && insertBlock.get() && (blockResultForIgnition == null || !blockResultForIgnition.found())) {
            if (chatFeedback && !ignitionErrorShown) {
                error("Block for Sulfur Cube not found for Ignition Aura.");
                ignitionErrorShown = true;
            }
            primerTicks = 0;
            ignitionReady = false;
        } else if (ignitionReady && insertBlock.get() && blockResultForIgnition != null && blockResultForIgnition.found()) {
            ignitionErrorShown = false;
        }

        if (!tntReady && !ignitionReady && !shearsReady) return;

        for (SulfurCube sulfurCube : cubes) {
            if (sulfurCube.distanceToSqr(mc.player) > rangeSq) continue;

            if (tntReady && sulfurCube.getBodyArmorItem().isEmpty()) {
                int previousSlot = mc.player.getInventory().getSelectedSlot();
                sendingTNTPacket = true;
                try {
                    if (rotateToTarget.get()) rotateTo(sulfurCube);
                    InvUtils.swap(tntResult.slot(), false);
                    if (swing.get()) {
                        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                    sendCubePacket(sulfurCube);
                } finally {
                    sendingTNTPacket = false;
                    if (swapBack.get()) InvUtils.swap(previousSlot, false);
                }
                tntTicks = 0;
                return;
            }

            if (shearsReady && sulfurCube.getBodyArmorItem().getItem() != Items.TNT && !sulfurCube.getBodyArmorItem().isEmpty()) {
                int previousSlot = mc.player.getInventory().getSelectedSlot();
                try {
                    if (rotateToTarget.get()) rotateTo(sulfurCube);
                    InvUtils.swap(shearResult.slot(), false);
                    if (swing.get()) {
                        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                    sendCubePacket(sulfurCube);
                } finally {
                    if (swapBack.get()) InvUtils.swap(previousSlot, false);
                }
                shearTicks = 0;
                return;
            }

            if (ignitionReady && sulfurCube.getBodyArmorItem().getItem() == Items.TNT) {
                int previousSlot = mc.player.getInventory().getSelectedSlot();
                try {
                    if (rotateToTarget.get()) rotateTo(sulfurCube);
                    InvUtils.swap(ignitionResult.slot(), false);
                    if (swing.get()) {
                        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                    if (mode.get() == Modes.FireAspectOnly){
                        mc.getConnection().send(new ServerboundAttackPacket(
                                sulfurCube.getId()
                        ));
                    } else sendCubePacket(sulfurCube);
                } finally {
                    if (swapBack.get()) InvUtils.swap(previousSlot, false);
                }
                primerTicks = 0;
                return;
            }
        }
    }

    private void rotateTo(SulfurCube cube) {
        Vec3 delta = cube.getBoundingBox().getCenter()
                .subtract(mc.player.getEyePosition());

        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));

        ServerboundMovePlayerPacket packet =
                new ServerboundMovePlayerPacket.Rot(
                        yaw,
                        pitch,
                        mc.player.onGround(),
                        mc.player.horizontalCollision
                );

        ((IServerboundMovePlayerPacket) packet).meteor$setTag(1337);
        mc.player.connection.send(packet);
    }
    private void sendCubePacket(SulfurCube sulfurCube) {
        if (mc.player == null || mc.getConnection() == null) return;

        mc.getConnection().send(new ServerboundInteractPacket(
                sulfurCube.getId(),
                InteractionHand.MAIN_HAND,
                sulfurCube.position(),
                mc.player.isShiftKeyDown()
        ));
    }
}