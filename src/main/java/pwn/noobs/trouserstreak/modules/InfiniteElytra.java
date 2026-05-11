package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import meteordevelopment.meteorclient.events.world.TickEvent;
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
        if (mc.player == null || mc.level == null) return;

        if (!playerWasFlying) playerWasFlying = mc.player.isFallFlying();
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

        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);

        if (currentPhaseTick < elytraOnTicks.get()) {
            glidingTime = true;
            if (chestStack.getItem() != Items.ELYTRA) {
                for (int i = 0; i < mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getNonEquipmentItems().get(i);
                    if (stack.getItem() == Items.ELYTRA) {
                        InvUtils.move().from(i).toArmor(2);
                        break;
                    }
                }
            }

            if (chestStack.getItem() == Items.ELYTRA && !mc.player.onGround() && !mc.player.isFallFlying()) {
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        } else {
            glidingTime = false;
            if (chestStack.getItem() == Items.ELYTRA) {
                int emptySlot = -1;
                for (int i = 0; i < 36; i++) {
                    if (mc.player.getInventory().getItem(i).isEmpty()) {
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
                    ItemStack stack = mc.player.getInventory().getItem(i);
                    if (stack.getItem() == Items.FIREWORK_ROCKET) {
                        rocketSlot = i;
                        break;
                    }
                }

                if (rocketSlot != -1) {
                    int currentSlot = mc.player.getInventory().getSelectedSlot();

                    if (rocketSlot != currentSlot) {
                        mc.player.getInventory().setSelectedSlot(rocketSlot);
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        mc.player.getInventory().setSelectedSlot(currentSlot);
                    } else {
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    }
                }
            }
        }
    }
    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        if (mc.player == null) return;

        ItemStack stack = mc.player.getItemInHand(event.hand);

        if (stack != null && stack.getItem() == Items.FIREWORK_ROCKET) {
            ticksSinceLastRocket = 0;
        }
    }
}