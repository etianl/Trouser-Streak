package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import pwn.noobs.trouserstreak.Trouser;

import meteordevelopment.orbit.EventHandler;

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

    private final Setting<Integer> dropslot = sgGeneral.add(new IntSetting.Builder()
            .name("DrotSlot#")
            .description("Drops this Slot if items are in it.")
            .sliderRange(1,9)
            .min(1)
            .max(9)
            .defaultValue(1)
            .visible(dropthisslot::get)
            .build());

    public AutoDrop() {super(Trouser.Main, "auto-drop", "Drops the stack in your selected slot automatically");}
    private int previousslot=0;
    private boolean getprevslot=false;
    public static boolean isTool(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.AXES) ||
                itemStack.isIn(ItemTags.HOES) ||
                itemStack.isIn(ItemTags.PICKAXES) ||
                itemStack.isIn(ItemTags.SHOVELS) ||
                itemStack.getItem() instanceof ShearsItem ||
                itemStack.getItem() instanceof FlintAndSteelItem ||
                itemStack.getItem() instanceof BucketItem;
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (tool.get() && isTool(mc.player.getMainHandStack()))return;
        if (dropthisslot.get() && !mc.player.getInventory().getStack(dropslot.get()-1).isEmpty()){
            previousslot=mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = dropslot.get()-1;
            getprevslot=true;
        }else if (!dropthisslot.get()) mc.player.dropSelectedItem(true);
    }
    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (getprevslot){
            mc.player.dropSelectedItem(true);
            mc.player.getInventory().selectedSlot = previousslot;
            getprevslot=false;
        }
    }
}