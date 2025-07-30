package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import pwn.noobs.trouserstreak.Trouser;

public class InfiniteElytra extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> elytraOnTicks = sgGeneral.add(new IntSetting.Builder()
            .name("elytra-on-ticks")
            .description("Number of ticks to keep Elytra equipped (on).")
            .defaultValue(15)
            .min(1)
            .sliderRange(1, 200)
            .build()
    );

    private final Setting<Integer> elytraOffTicks = sgGeneral.add(new IntSetting.Builder()
            .name("elytra-off-ticks")
            .description("Number of ticks to keep Elytra unequipped (off).")
            .defaultValue(2)
            .min(1)
            .sliderRange(1, 200)
            .build()
    );
    private final Setting<Boolean> fireRockets = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-fire-rockets")
            .description("Number of ticks between rocket fires.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> timebetweenfires = sgGeneral.add(new IntSetting.Builder()
            .name("time-between-rockets")
            .description("Number of ticks between rocket fires.")
            .defaultValue(40)
            .min(1)
            .sliderRange(1, 200)
            .build()
    );
    private int tickCounter = 0;
    private boolean playerWasFlying = false;
    private boolean glidingTime = false;
    private int timeBetweenRockets = 0;
    private int ticksSinceLastRocket = 0;

    public InfiniteElytra() {
        super(Trouser.Main, "InfiniteElytra", "Automatically toggles Elytra on/off to conserve durability and auto-uses rockets maintaining flight.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (!playerWasFlying) playerWasFlying = mc.player.isGliding();
        if (!playerWasFlying) return;

        tickCounter++;

        if (fireRockets.get()) {
            timeBetweenRockets = timebetweenfires.get();
            ticksSinceLastRocket++;
        }

        int totalCycleTicks = elytraOnTicks.get() + elytraOffTicks.get();
        int currentPhaseTick = tickCounter % totalCycleTicks;

        if (currentPhaseTick == 0) {
            tickCounter = 0;
        }

        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        if (currentPhaseTick < elytraOnTicks.get()) {
            glidingTime = true;
            if (chestStack.getItem() != Items.ELYTRA) {
                for (int i = 0; i < mc.player.getInventory().getMainStacks().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getMainStacks().get(i);
                    if (stack.getItem() == Items.ELYTRA) {
                        InvUtils.move().from(i).toArmor(2);
                        break;
                    }
                }
            }

            if (chestStack.getItem() == Items.ELYTRA && !mc.player.isOnGround() && !mc.player.isGliding()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        } else {
            glidingTime = false;
            if (chestStack.getItem() == Items.ELYTRA) {
                int emptySlot = -1;
                for (int i = 0; i < 36; i++) {
                    if (mc.player.getInventory().getStack(i).isEmpty()) {
                        emptySlot = i;
                        break;
                    }
                }

                if (emptySlot != -1) {
                    InvUtils.move().fromArmor(2).to(emptySlot);
                }
            }
        }
        if (glidingTime && fireRockets.get()) {
            if (ticksSinceLastRocket >= timeBetweenRockets) {
                int rocketSlot = -1;

                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack.getItem() == Items.FIREWORK_ROCKET) {
                        rocketSlot = i;
                        break;
                    }
                }

                if (rocketSlot != -1) {
                    int currentSlot = mc.player.getInventory().selectedSlot;

                    if (rocketSlot != currentSlot) {
                        mc.player.getInventory().selectedSlot = rocketSlot;
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        mc.player.getInventory().selectedSlot = currentSlot;
                    } else {
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    }
                }
            }
        }
    }
    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        if (mc.player == null) return;

        ItemStack stack = mc.player.getStackInHand(event.hand);

        if (stack != null && stack.getItem() == Items.FIREWORK_ROCKET) {
            ticksSinceLastRocket = 0;
        }
    }
}