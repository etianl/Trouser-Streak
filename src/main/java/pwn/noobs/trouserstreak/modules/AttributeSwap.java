// Credits to https://github.com/DonKisser for the first pull request and idea. -Dedicate

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import pwn.noobs.trouserstreak.Trouser;

public class AttributeSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Swap back to the original slot after a short delay.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("swap-back-delay")
            .description("Delay in ticks before swapping back to the previous slot.")
            .sliderRange(1, 20)
            .defaultValue(1)
            .visible(swapBack::get)
            .build());

    private int prevSlot = -1;
    private int dDelay = 0;

    // Cannot be used for tools they need a way bigger delay which will not be useful
    public AttributeSwap() {
        super(Trouser.Main, "Attribute Swap", "Prevents items from breaking while attacking by swapping at 1 durability");
    }

    private boolean isLowDurability() {
        ItemStack currentStack = mc.player.getMainHandStack();
        return currentStack.getDamage() >= currentStack.getMaxDamage() - 1;
    }

    private boolean isWeapon(ItemStack stack) {
        String itemId = stack.getItem().toString().toLowerCase();
        return itemId.contains("sword") ||
                itemId.contains("axe") ||
                itemId.contains("mace");
    }

    private int findSafeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !isWeapon(stack)) {
                return i;
            }
        }
        return -1;
    }

    private void trySwap() {
        if (isLowDurability()) {
            int safeSlot = findSafeSlot();
            if (safeSlot != -1) {
                if (swapBack.get()) {
                    prevSlot = mc.player.getInventory().selectedSlot;
                }
                InvUtils.swap(safeSlot, false);
                if (swapBack.get() && prevSlot != -1) {
                    dDelay = delay.get();
                }
            }
        }
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        trySwap();
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