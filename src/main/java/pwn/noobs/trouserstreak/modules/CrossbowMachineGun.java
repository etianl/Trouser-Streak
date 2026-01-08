//written by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import pwn.noobs.trouserstreak.Trouser;

public class CrossbowMachineGun extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between shots (in ticks)")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> correctSequence = sgGeneral.add(new BoolSetting.Builder()
            .name("correct-sequence")
            .description("Use correct sequence.")
            .defaultValue(true)
            .build()
    );

    private int timer = 0;

    public CrossbowMachineGun() {
        super(Trouser.Main, "crossbow-machine-gun", "Turns your crossbow into a machine gun. Hold right click to activate! Thank you to agreed!");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        if (delay.get() > 0) {
            if (timer++ < delay.get()) return;
            timer = 0;
        }

        if (mc.player.getMainHandStack().getItem() != Items.CROSSBOW || !mc.options.useKey.isPressed()) return;

        int sequence = 0;

        if (correctSequence.get()) {
            sequence = mc.world.pendingUpdateManager.getSequence();
        }

        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence));
    }
}