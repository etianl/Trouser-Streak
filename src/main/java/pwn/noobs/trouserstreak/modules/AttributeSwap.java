/*Thank you to [DonKisser](https://github.com/DonKisser) for making this module for us!
        Their inspiration was this Youtube video by @scilangaming:
        https://www.youtube.com/watch?v=q99eqD_fBqo*/

package pwn.noobs.trouserstreak.modules;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
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
    private final Setting<Boolean> shieldBreaker = sgGeneral.add(new BoolSetting.Builder().name("shield-breaker").description("Swap to an axe from your hotbar when attacking to disable the shield of someone who is blocking.").defaultValue(false).build());
    private final Setting<Boolean> noswap = sgGeneral.add(new BoolSetting.Builder().name("Shield Breaker No Swap").description("Do not attribute swap to another item after shield is broken").defaultValue(false).visible(shieldBreaker::get).build());
    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder().name("target-slot").description("The hotbar slot to swap to when attacking.").sliderRange(1, 9).defaultValue(1).min(1).visible(() -> !(noswap.get() && shieldBreaker.get())).build());
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder().name("swap-back").description("Swap back to the original slot after a short delay.").defaultValue(true).visible(() -> !(noswap.get() && shieldBreaker.get())).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("swap-back-delay").description("Delay in ticks before swapping back to the previous slot.").sliderRange(1, 20).defaultValue(1).min(1).visible(swapBack::get).visible(() -> !(noswap.get() && shieldBreaker.get())).build());

    private int prevSlot = -1;
    private int dDelay = 0;
    public AttributeSwap() {
        super(Trouser.Main, "AttributeSwap", "Swaps attributes of the main hand item with the target slot on attack");
    }
    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (swapBack.get()) {
            prevSlot = mc.player.getInventory().selectedSlot;
        }
        if (shieldBreaker.get()) {
            if (event.entity instanceof PlayerEntity player){
                if (player.isBlocking()){
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = mc.player.getInventory().getMainStacks().get(i);
                        if (stack.getItem() instanceof AxeItem) {
                            InvUtils.swap(i, false);
                            break;
                        }
                    }
                } else if (!noswap.get()) InvUtils.swap(targetSlot.get()-1, false);
            } else InvUtils.swap(targetSlot.get()-1, false);
        }
        else if (!shieldBreaker.get())InvUtils.swap(targetSlot.get()-1, false);
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