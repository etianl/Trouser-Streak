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


    public AutoDrop() {super(Trouser.Main, "auto-drop", "Drops the stack in your selected slot automatically");}

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (tool.get() == true){
        if (!(mc.player.getMainHandStack().getItem() instanceof BucketItem || mc.player.getMainHandStack().getItem() instanceof FlintAndSteelItem || mc.player.getMainHandStack().getItem() instanceof ToolItem || mc.player.getMainHandStack().getItem() instanceof ShearsItem)) {
            mc.player.dropSelectedItem(true);
        }else if (mc.player.getMainHandStack().getItem() instanceof BucketItem || mc.player.getMainHandStack().getItem() instanceof FlintAndSteelItem || mc.player.getMainHandStack().getItem() instanceof ToolItem || mc.player.getMainHandStack().getItem() instanceof ShearsItem){
        }}
        else {
            mc.player.dropSelectedItem(true);
        }

    }
}
