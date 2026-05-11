package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class LoadingTerrainDisconnect extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public final Setting<Integer> x = sgGeneral.add(new IntSetting.Builder()
            .name("x")
            .description("Horizontal position")
            .defaultValue(100)
            .sliderRange(0, 2000)
            .build());
    public final Setting<Integer> y = sgGeneral.add(new IntSetting.Builder()
            .name("y")
            .description("Vertical Position")
            .defaultValue(120)
            .sliderRange(0, 2000)
            .build());
    public LoadingTerrainDisconnect() {
        super(Trouser.Main, "terrain-disconnect", "Adds a disconnect button to the terrain loading screen for stuck loads.");
    }
}