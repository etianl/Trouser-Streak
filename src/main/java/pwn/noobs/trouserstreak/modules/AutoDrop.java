package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import pwn.noobs.trouserstreak.Trouser;

import meteordevelopment.orbit.EventHandler;
import java.util.ArrayList;
import java.util.List;

public class AutoDrop extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> tool = sgGeneral.add(new BoolSetting.Builder()
            .name("No Throw Tools")
            .description("No Throw tools")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> dropthisslot = sgGeneral.add(new BoolSetting.Builder()
            .name("SpecifySlotToDrop")
            .description("Specifies the slot to drop.")
            .defaultValue(false)
            .build()
    );

    private final List<Setting<Boolean>> slots = new ArrayList<>(9);

    public AutoDrop() {
        super(Trouser.Main, "auto-drop", "Drops the stack in your selected slot automatically");

        for (int i = 0; i < 9; i++) {
            final int slot = i;
            slots.add(sgGeneral.add(new BoolSetting.Builder()
                    .name("slot " + slot)
                    .description("Drop items from hotbar slot " + slot + ".")
                    .defaultValue(false)
                    .visible(dropthisslot::get)
                    .build()
            ));
        }
    }

    private int previousslot = 0;
    private int nextSlot = 0;
    private boolean getprevslot = false;

    public static boolean isTool(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.AXES) ||
                itemStack.isIn(ItemTags.HOES) ||
                itemStack.isIn(ItemTags.PICKAXES) ||
                itemStack.isIn(ItemTags.SHOVELS) ||
                itemStack.getItem() instanceof ShearsItem ||
                itemStack.getItem() instanceof FlintAndSteelItem ||
                itemStack.getItem() instanceof BucketItem;
    }

    private boolean isSlotEnabled(int slot) {
        return slot >= 0 && slot < 9 && slots.get(slot).get();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (!dropthisslot.get()) {
            if (tool.get() && isTool(mc.player.getMainHandStack())) return;
            mc.player.dropSelectedItem(true);
            return;
        }

        if (getprevslot) return;

        for (int i = 0; i < 9; i++) {
            int slot = (nextSlot + i) % 9;
            if (!isSlotEnabled(slot)) continue;

            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            if (tool.get() && isTool(stack)) continue;

            previousslot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
            nextSlot = (slot + 1) % 9;
            getprevslot = true;
            return;
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (getprevslot) {
            mc.player.dropSelectedItem(true);
            mc.player.getInventory().selectedSlot = previousslot;
            getprevslot = false;
        }
    }
}