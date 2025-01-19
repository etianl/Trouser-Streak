/*Thank you to [DonKisser](https://github.com/DonKisser) for making this module for us!
        Their inspiration was this Youtube video by @scilangaming:
        https://www.youtube.com/watch?v=q99eqD_fBqo*/

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import pwn.noobs.trouserstreak.modules.addon.TrouserModule;

public class AttributeSwap extends TrouserModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder()
                    .name("target-slot")
                    .description("The hotbar slot to swap to when attacking.")
                    .sliderRange(1, 9)
                    .defaultValue(1)
                    .min(1)
                    .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
                    .name("swap-back")
                    .description("Swap back to the original slot after a short delay.")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("swap-back-delay")
            .description("Delay in ticks before swapping back to the previous slot.")
            .sliderRange(1, 20)
            .defaultValue(1)
            .min(1)
            .visible(swapBack::get)
            .build()
    );
    private int prevSlot = -1;
    private int dDelay = 0;

    public AttributeSwap() {
        super("attribute-swap", "Swaps attributes of the main hand item with the target slot on attack");
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (swapBack.get()) {
            prevSlot = mc.player.getInventory().selectedSlot;
        }
        InvUtils.swap(targetSlot.get() - 1, false);
        if (swapBack.get() && prevSlot != -1) {
            dDelay = delay.get();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (dDelay > 0) {
            dDelay--;
            if (dDelay == 0 && prevSlot != -1) {
                InvUtils.swap(prevSlot, false);
                prevSlot = -1;
            }
        }
    }
}