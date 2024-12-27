package pwn.noobs.trouserstreak.modules;

import pwn.noobs.trouserstreak.Trouser;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;

public class AttributeSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder().name("target-slot").description("The hotbar slot to swap to when attacking.").sliderRange(0, 8).defaultValue(0).build());
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder().name("swap-back").description("Swap back to the original slot after a short delay.").defaultValue(true).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("swap-back-delay").description("Delay in ticks before swapping back to the previous slot.").sliderRange(1, 20).defaultValue(1).visible(swapBack::get).build());
    private int prevSlot = -1;
    private int dDelay = 0;
    public AttributeSwap() {
        super(Trouser.Main, "Attribute Swap", "Tries the Atrribute Swap bug");
    }
    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (swapBack.get()) {
            prevSlot = mc.player.getInventory().selectedSlot;
        }
        InvUtils.swap(targetSlot.get(), false);
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
