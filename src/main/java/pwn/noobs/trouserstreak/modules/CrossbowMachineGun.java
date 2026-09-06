//written by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
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
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

        if (delay.get() > 0) {
            if (timer++ < delay.get()) return;
            timer = 0;
        }

        if (mc.player.getOffhandItem().getItem() != Items.CROSSBOW
                && mc.player.getMainHandItem().getItem() != Items.CROSSBOW
                || !mc.options.keyUse.isDown()) return;

        InteractionHand crossbowHand = mc.player.getMainHandItem().getItem() == Items.CROSSBOW
                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;

        int sequence = correctSequence.get() ? mc.level.blockStatePredictionHandler.currentSequence() : 0;

        mc.getConnection().send(
                new ServerboundUseItemPacket(crossbowHand, sequence, mc.player.getYRot(), mc.player.getXRot())
        );
    }
}