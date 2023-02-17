package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.item.*;
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
            .sliderMin(1)
                    .sliderMax(9)
            .defaultValue(1)
                    .visible(() -> dropthisslot.get())
            .build());


    public AutoDrop() {super(Trouser.Main, "auto-drop", "Drops the stack in your selected slot automatically");}
    private int ticks=0;
    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        ticks++;
        if (tool.get() == true){
        if (!(mc.player.getMainHandStack().getItem() instanceof BucketItem || mc.player.getMainHandStack().getItem() instanceof FlintAndSteelItem || mc.player.getMainHandStack().getItem() instanceof ToolItem || mc.player.getMainHandStack().getItem() instanceof ShearsItem)) {
            if (dropthisslot.get() && !mc.player.getInventory().getStack(dropslot.get()-1).isEmpty()){
                if (ticks==1){mc.player.getInventory().selectedSlot = dropslot.get()-1;}
                else if (ticks==2){mc.player.dropSelectedItem(true);}
                else if (ticks>=2){ticks=0;}
            } else if (!dropthisslot.get()) mc.player.dropSelectedItem(true);
        }else if (mc.player.getMainHandStack().getItem() instanceof BucketItem || mc.player.getMainHandStack().getItem() instanceof FlintAndSteelItem || mc.player.getMainHandStack().getItem() instanceof ToolItem || mc.player.getMainHandStack().getItem() instanceof ShearsItem){}
        }
        else {
            if (dropthisslot.get() && !mc.player.getInventory().getStack(dropslot.get()-1).isEmpty()){
                if (ticks==1){mc.player.getInventory().selectedSlot = dropslot.get()-1;}
                else if (ticks==2){mc.player.dropSelectedItem(true);}
                else if (ticks>=2){ticks=0;}
            } else if (!dropthisslot.get()) mc.player.dropSelectedItem(true);
        }

    }
}
