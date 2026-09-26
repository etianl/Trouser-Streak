package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        PreferFlintAndSteel, PreferFireCharge
    }
    private final Setting<Modes> mode = sgAuto.add(new EnumSetting.Builder<Modes>()
            .name("ignition-method")
            .description("Choose which ignition method will be used first")
            .defaultValue(Modes.PreferFlintAndSteel)
            .visible(pAura::get)
            .build());
    private final Setting<Boolean> stopfuckingupthefarm = sgAuto.add(new BoolSetting.Builder()
            .name("disable-near-farm")
            .description("Do not run TNT and Ignition Aura code when near a specified coordinate.")
            .defaultValue(true)
            .visible(() -> tAura.get() || pAura.get())
            .build()
    );
    private final Setting<Double> farmDist = sgAuto.add(new DoubleSetting.Builder()
            .name("safe-distance-from-farm")
            .description("Cubes will not be affected by TNT and Ignition aura within this distance of the Farm Coordinate")
            .defaultValue(420)
            .min(0)
            .sliderRange(0, 1000)
            .visible(() -> stopfuckingupthefarm.get() && (tAura.get() || pAura.get()))
            .build()
    );
    private final Setting<BlockPos> farmCoordinates = sgAuto.add(new BlockPosSetting.Builder()
            .name("farm-coordinate")
            .description("Roughly the center of the Sulfur Cube production.")
            .defaultValue(new BlockPos(694206767, 694206767, 694206767))
            .visible(() -> stopfuckingupthefarm.get() && (tAura.get() || pAura.get()))
            .build()
    );
    private final Setting<Integer> maxEntities = sgAuto.add(new IntSetting.Builder()
            .name("max-entities")
            .description("Max amount of entities to try to do things to per tick.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .visible(() -> tAura.get() || pAura.get())
            .build()
    );
    private final Setting<Double> reach = sgAuto.add(new DoubleSetting.Builder()
            .name("Reach (blocks)")
            .description("Cube must be within this range")
            .defaultValue(5.5)
            .min(1)
            .sliderRange(1, 10)
            .visible(() -> tAura.get() || pAura.get())
            .build()
    );

    public CubePrimer() {
        super(Trouser.Main, "CubePrimer", "Use a Flint and Steel or Fire Charge on a TNT Sulfur Cube while Shears are in your hotbar to make it primed and ready to explode the moment it absorbs a TNT item. If it absorbs any other dropped block in this state it will become unkillable.");
    }

    private int dumperTicks;
    private int tntTicks;
    private int primerTicks;
    private int pendingSwapSlot = -1;
    private boolean interacting;

    @Override
    public void onActivate() {
        if (chatFeedback)info("Use a Flint and Steel or Fire Charge while Shears are in your hotbar to prime a TNT Sulfur Cube.");
        dumperTicks = 0;
        tntTicks = 0;
        primerTicks = 0;
        pendingSwapSlot = -1;
        interacting = false;
    }
    //Cube Primer
    @EventHandler
    private void onPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundInteractPacket interactPacket)) return;

        if (interactPacket.hand() == null) return;
        if (mc.player == null || mc.level == null) return;

        if (!(mc.level.getEntity(interactPacket.entityId()) instanceof SulfurCube sulfurCube) || interacting) return;
        if (sulfurCube.getBodyArmorItem().getItem() != Items.TNT) return;
        InteractionHand hand = interactPacket.hand();
        if (hand == null) return;

        ItemStack stack = mc.player.getItemInHand(hand);
        if (stack.getItem() != Items.FLINT_AND_STEEL && stack.getItem() != Items.FIRE_CHARGE) return;

        FindItemResult shearsResult = InvUtils.findInHotbar(Items.SHEARS);

        if (!shearsResult.found()) {
            if (chatFeedback)error("You need Shears in your hotbar to finish the priming method.");
            event.cancel();
            return;
        }

        FindItemResult blockResult = null;

        if (insertBlock.get()){
            for (Item block : blocks.get()){
                blockResult = InvUtils.findInHotbar(block);
                if (blockResult.found()) break;
            }
            if (blockResult == null || !blockResult.found()) {
                if (chatFeedback)error("Block for Sulfur Cube not found.");
                event.cancel();
                return;
            }
        }

        int previousslot = -1;
        try {
            interacting = true;
            previousslot = mc.player.getInventory().getSelectedSlot();
            InvUtils.swap(shearsResult.slot(), false);
            mc.getConnection().send(new ServerboundInteractPacket(
                    sulfurCube.getId(),
                    mc.player.getUsedItemHand(),
                    sulfurCube.position(),
                    true
            ));
            if (insertBlock.get() && blockResult.found()){
                InvUtils.swap(blockResult.slot(), false);
                mc.getConnection().send(new ServerboundInteractPacket(
                        sulfurCube.getId(),
                        mc.player.getUsedItemHand(),
                        sulfurCube.position(),
                        true
                ));
            }
        } finally {
            interacting = false;
            if (swapBack.get()) InvUtils.swap(previousslot, false);
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
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;


        if (pendingSwapSlot != -1) {
            dumperTicks++;

            if (dumperTicks >= dumptickDelay.get()) {
                int slot = pendingSwapSlot;

                if (slot >= 0
                        && slot < 9
                        && mc.player.getInventory().getItem(slot).getItem()
                        == Items.SULFUR_CUBE_BUCKET) {
                    InvUtils.swap(slot, false);
                }

                pendingSwapSlot = -1;
                dumperTicks = 0;
            }
        } else {
            dumperTicks = 0;
        }

        if (!pAura.get() && !tAura.get()) return;

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

        double range = reach.get();

        Iterable<Entity> entities = mc.level.entitiesForRendering();
        int processed = 0;

        boolean tnterror = false;
        boolean ignitionerror = false;
        for (Entity entity : entities) {
            if (processed >= maxEntities.get()) break;
            if (!(entity instanceof SulfurCube sulfurCube)) continue;
            if (sulfurCube.distanceToSqr(mc.player) > range * range) continue;

            if (tAura.get()
                    && sulfurCube.getBodyArmorItem().isEmpty()
                    && tntTicks >= tnttickDelay.get()) {

                FindItemResult tntResult = InvUtils.findInHotbar(Items.TNT);

                if (!tntResult.found() && !tnterror) {
                    if (chatFeedback) error("You need a TNT in your hotbar.");
                    tnterror = true;
                    continue;
                }

                int previousSlot = mc.player.getInventory().getSelectedSlot();

                try {
                    InvUtils.swap(tntResult.slot(), false);

                    mc.getConnection().send(new ServerboundInteractPacket(
                            sulfurCube.getId(),
                            mc.player.getUsedItemHand(),
                            sulfurCube.position(),
                            true
                    ));
                } finally {
                    if (swapBack.get()) InvUtils.swap(previousSlot, false);
                }

                tntTicks = 0;
                processed++;
                continue;
            }

            if (pAura.get()
                    && sulfurCube.getBodyArmorItem().getItem() == Items.TNT
                    && primerTicks >= ignitiontickDelay.get()) {

                FindItemResult ignitionResult = mode.get() == Modes.PreferFlintAndSteel
                        ? InvUtils.findInHotbar(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE)
                        : InvUtils.findInHotbar(Items.FIRE_CHARGE, Items.FLINT_AND_STEEL);

                if (!ignitionResult.found() && !ignitionerror) {
                    if (chatFeedback) error("You need an ignition method in your hotbar.");
                    ignitionerror = true;
                    continue;
                }

                int previousSlot = mc.player.getInventory().getSelectedSlot();

                try {
                    InvUtils.swap(ignitionResult.slot(), false);

                    InteractionHand hand = mc.player.getUsedItemHand();
                    Vec3 hitPos = sulfurCube.position();

                    mc.getConnection().send(new ServerboundInteractPacket(
                            sulfurCube.getId(),
                            hand,
                            hitPos,
                            true
                    ));
                } finally {
                    if (swapBack.get()) InvUtils.swap(previousSlot, false);
                }

                primerTicks = 0;
                processed++;
            }
        }
    }
}