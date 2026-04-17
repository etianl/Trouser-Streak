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

    private final Setting<DropMode> dropMode = sgGeneral.add(new EnumSetting.Builder<DropMode>()
            .name("mode")
            .description("How hotbar slots are dropped each tick.")
            .defaultValue(DropMode.SEQUENTIAL)
            .build()
    );

    private final Setting<Integer> dropsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("drops-per-tick")
            .description("Maximum number of hotbar slots to drop per tick (1-9).")
            .defaultValue(1)
            .min(1)
            .sliderMax(9)
            .build()
    );

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

    private int nextSlot = 0;

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

    @Override
    public void onActivate() {
        nextSlot = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (!dropthisslot.get()) {
            if (tool.get() && isTool(mc.player.getMainHandStack())) return;
            mc.player.dropSelectedItem(true);
            return;
        }

        int savedSlot = mc.player.getInventory().selectedSlot;
        int dropped = 0;
        int limit = Math.min(dropsPerTick.get(), 9);

        if (dropMode.get() == DropMode.BURST) {
            for (int i = 0; i < 9 && dropped < limit; i++) {
                if (!isSlotEnabled(i)) continue;
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                if (tool.get() && isTool(stack)) continue;
                mc.player.getInventory().selectedSlot = i;
                mc.player.dropSelectedItem(true);
                dropped++;
            }
        } else {
            int startSlot = nextSlot;
            for (int i = 0; i < 9 && dropped < limit; i++) {
                int slot = (startSlot + i) % 9;
                if (!isSlotEnabled(slot)) continue;
                ItemStack stack = mc.player.getInventory().getStack(slot);
                if (stack.isEmpty()) continue;
                if (tool.get() && isTool(stack)) continue;
                mc.player.getInventory().selectedSlot = slot;
                mc.player.dropSelectedItem(true);
                nextSlot = (slot + 1) % 9;
                dropped++;
            }
        }

        mc.player.getInventory().selectedSlot = savedSlot;
    }

    public enum DropMode {
        SEQUENTIAL,
        BURST
    }
}