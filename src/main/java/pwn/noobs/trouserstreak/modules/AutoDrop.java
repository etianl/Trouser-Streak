package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ToolItem;
import pwn.noobs.trouserstreak.Trouser;

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
    private int previousslot=0;

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (tool.get() == true){
        if (!(mc.player.getMainHandStack().getItem() instanceof BucketItem || mc.player.getMainHandStack().getItem() instanceof FlintAndSteelItem || mc.player.getMainHandStack().getItem() instanceof ToolItem || mc.player.getMainHandStack().getItem() instanceof ShearsItem)) {
            if (dropthisslot.get() && !mc.player.getInventory().getStack(dropslot.get()-1).isEmpty()){
                ticks++;
                if (ticks==1){
                    previousslot=mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = dropslot.get()-1;
                }
                else if (ticks==2){mc.player.dropSelectedItem(true);}
                else if (ticks>=2){
                    mc.player.getInventory().selectedSlot = previousslot;
                    ticks=0;
                }
            }
            if (dropthisslot.get() && mc.player.getInventory().getStack(dropslot.get()-1).isEmpty() && ticks>=2){
                mc.player.getInventory().selectedSlot = previousslot;
                ticks=0;
            }
            if (!dropthisslot.get()) mc.player.dropSelectedItem(true);
        }else if (mc.player.getMainHandStack().getItem() instanceof BucketItem || mc.player.getMainHandStack().getItem() instanceof FlintAndSteelItem || mc.player.getMainHandStack().getItem() instanceof ToolItem || mc.player.getMainHandStack().getItem() instanceof ShearsItem){}
        }
        else {
            if (dropthisslot.get() && !mc.player.getInventory().getStack(dropslot.get()-1).isEmpty()){
                ticks++;
                if (ticks==1){
                    previousslot=mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = dropslot.get()-1;
                }
                else if (ticks==2){mc.player.dropSelectedItem(true);}
                else if (ticks>=2){
                    mc.player.getInventory().selectedSlot = previousslot;
                    ticks=0;
                }
            }
            if (dropthisslot.get() && mc.player.getInventory().getStack(dropslot.get()-1).isEmpty() && ticks>=2){
                mc.player.getInventory().selectedSlot = previousslot;
                ticks=0;
            }
            if (!dropthisslot.get()) mc.player.dropSelectedItem(true);
        }

    }
}
