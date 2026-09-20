package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class CubeAutomation extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> dumpAllCubes = sgGeneral.add(new BoolSetting.Builder()
            .name("cube-hotbar-dumper")
            .description("If you place one Sulfur Cube bucket, automatically select the next for placing.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> tAura = sgGeneral.add(new BoolSetting.Builder()
            .name("TNT-insertion-aura")
            .description("Automatically insert TNT into empty Cubes within range.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> pAura = sgGeneral.add(new BoolSetting.Builder()
            .name("ignition-aura")
            .description("Automatically ignite TNT Sulfur Cubes within range.")
            .defaultValue(true)
            .build()
    );

    public enum Modes {
        PreferFlintAndSteel, PreferFireCharge
    }
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("ignition-method")
            .description("Choose which ignition method will be used first")
            .defaultValue(Modes.PreferFlintAndSteel)
            .visible(pAura::get)
            .build());
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("tick-delay")
            .description("Delay in ticks between tnt insertion/priming attempts.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .visible(() -> tAura.get() || pAura.get())
            .build()
    );
    private final Setting<Integer> maxEntities = sgGeneral.add(new IntSetting.Builder()
            .name("max-entities")
            .description("Max amount of entities to try to do things to per tick.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .visible(() -> tAura.get() || pAura.get())
            .build()
    );
    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
            .name("Reach (blocks)")
            .description("Cube must be within this range")
            .defaultValue(5.5)
            .min(1)
            .sliderRange(1, 10)
            .visible(() -> tAura.get() || pAura.get())
            .build()
    );
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Switch to your previous slot after using the items.")
            .defaultValue(true)
            .visible(() -> tAura.get() || pAura.get())
            .build()
    );

    public CubeAutomation() {
        super(Trouser.Main, "CubeAutomation", "Automatically do things with Sulfur Cubes. Use this module in combination with CubePrimer to make alot of invincible cubes fast.");
    }

    private int previousslot;
    private int automationTicks;
    private int pendingSwapSlot = -1;

    @Override
    public void onActivate() {
        previousslot = -1;
        automationTicks = 0;
        pendingSwapSlot = -1;
    }

    @EventHandler
    private void onInteract(InteractBlockEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!dumpAllCubes.get()) return;
        if (event.hand != InteractionHand.MAIN_HAND) return;

        ItemStack clickedStack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (clickedStack.getItem() != Items.SULFUR_CUBE_BUCKET && clickedStack.getItem() != Items.BUCKET) return;

        pendingSwapSlot = findNextCubeSlot();
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
            InvUtils.swap(pendingSwapSlot, false);
            pendingSwapSlot = -1;
        }
        if (!pAura.get() && !tAura.get()) return;

        automationTicks++;

        if (automationTicks < tickDelay.get()) return;

        automationTicks = 0;

        double range = reach.get();
        Vec3 pos = mc.player.position();

        AABB box = new AABB(
                pos.x - range, pos.y - range, pos.z - range,
                pos.x + range, pos.y + range, pos.z + range
        );

        List<Entity> entities = mc.level.getEntities(mc.player, box);

        int processed = 0;

        for (Entity entity : entities) {
            if (processed >= maxEntities.get()) break;
            if (!(entity instanceof SulfurCube sulfurCube)) continue;
            if (sulfurCube.distanceToSqr(mc.player) > range * range) continue;

            boolean acted = false;

            if (tAura.get() && sulfurCube.getBodyArmorItem().isEmpty()) {
                FindItemResult tntResult = InvUtils.findInHotbar(Items.TNT);

                if (tntResult.found()) {
                    previousslot = mc.player.getInventory().getSelectedSlot();

                    InvUtils.swap(tntResult.slot(), false);

                    mc.getConnection().send(new ServerboundInteractPacket(
                            sulfurCube.getId(),
                            mc.player.getUsedItemHand(),
                            sulfurCube.position(),
                            true
                    ));

                    if (swapBack.get()) {
                        InvUtils.swap(previousslot, false);
                    }

                    acted = true;
                }
            }

            if (!acted && pAura.get() && sulfurCube.getBodyArmorItem().getItem() == Items.TNT) {
                FindItemResult ignitionResult = (mode.get() == Modes.PreferFlintAndSteel)
                        ? InvUtils.findInHotbar(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE)
                        : InvUtils.findInHotbar(Items.FIRE_CHARGE, Items.FLINT_AND_STEEL);

                if (!ignitionResult.found()) {
                    if (chatFeedback) error("You need an ignition method in your hotbar.");
                    return;
                }

                try {
                    previousslot = mc.player.getInventory().getSelectedSlot();
                    InvUtils.swap(ignitionResult.slot(), false);
                    InteractionHand hand = mc.player.getUsedItemHand();
                    EntityHitResult sulfurHit = new EntityHitResult(
                            sulfurCube,
                            sulfurCube.position()
                    );
                    mc.gameMode.interact(mc.player, sulfurCube, sulfurHit, hand);//specifically mc.gameMode.interact because I think that will trigger the onInteract method in CubePrimer.
                } finally {
                    if (swapBack.get()) InvUtils.swap(previousslot, false);
                }

                acted = true;
            }

            if (acted) {
                processed++;
            }
        }
    }
}