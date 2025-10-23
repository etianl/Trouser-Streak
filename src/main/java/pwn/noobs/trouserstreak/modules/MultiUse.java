//Thank you maytrixc for your pull request: https://github.com/etianl/Trouser-Streak/pull/69 I (etianl) just made it work good.
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import pwn.noobs.trouserstreak.Trouser;

public class MultiUse extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("Rate");
    private final Setting<Integer> uses = sgGeneral.add(new IntSetting.Builder()
            .name("Extra uses per use")
            .description("Amount of extra uses per use")
            .defaultValue(1)
            .min(1)
            .sliderMax(10)
            .build()
    );
    public MultiUse() {
        super(Trouser.Main, "multi-use", "Uses an item multiple times per item use");
    }
    @EventHandler
    private void onMouseButton(MouseClickEvent event) {
        if (mc.options.useKey.isPressed()) {
            for (int i = 0; i < uses.get(); i++) {
                mc.doItemUse();
            }
        }
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.options.useKey.isPressed()) {
            for (int i = 0; i < uses.get(); i++) {
                mc.doItemUse();
            }
        }
    }
}