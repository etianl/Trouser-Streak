package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import pwn.noobs.trouserstreak.Trouser;

public class Voider extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
        .name("mode")
        .description("the mode")
        .defaultValue(Modes.Air)
        .build());
    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("radius")
        .defaultValue(90)
        .sliderRange(1, 90)
        .build());
    private final Setting<Integer> maxheight = sgGeneral.add(new IntSetting.Builder()
        .name("maxheight")
        .description("maxheight")
        .defaultValue(319)
        .sliderRange(128, 319)
        .build());
    private final Setting<Integer> minheight = sgGeneral.add(new IntSetting.Builder()
        .name("minheight")
        .description("minheight")
        .defaultValue(-64)
        .sliderRange(-64, 128)
        .build());

    public Voider() {
        super(Trouser.Main, "voider", "Deletes the world from the top down");
    }

    int i = maxheight.get();

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (!(mc.player.hasPermissionLevel(4))) {
            toggle();
            error("Must have OP");
        }
        i--;
        switch (mode.get()) {
            case Air -> {
                ChatUtils.sendPlayerMsg("/fill ~-" + radius.get() + " " + i + " ~-" + radius.get() + " ~" + radius.get() + " " + i + " ~" + radius.get() + " air");
                if (i == minheight.get()) {
                    i = maxheight.get();
                    toggle();
                }
            }
            case Water -> {
                ChatUtils.sendPlayerMsg("/fill ~-" + radius.get() + " " + i + " ~-" + radius.get() + " ~" + radius.get() + " " + i + " ~" + radius.get() + " water");
                if (i == minheight.get()) {
                    i = maxheight.get();
                    toggle();
                }
            }
            case Lava -> {
                ChatUtils.sendPlayerMsg("/fill ~-" + radius.get() + " " + i + " ~-" + radius.get() + " ~" + radius.get() + " " + i + " ~" + radius.get() + " lava");
                if (i == minheight.get()) {
                    i = maxheight.get();
                    toggle();
                }
            }
        }
    }
    public enum Modes {
        Air, Water, Lava
    }
}
