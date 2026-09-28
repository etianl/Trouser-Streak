package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class NoModDetection extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> filterSign = sgGeneral.add(new BoolSetting.Builder()
            .name("Filter Sign Packets")
            .description("Filter Sign Packets.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> filterAnvil = sgGeneral.add(new BoolSetting.Builder()
            .name("Filter Anvil Packets")
            .description("Filter Anvil Packets. May mess with renaming things.")
            .defaultValue(true)
            .build()
    );
    public NoModDetection() {
        super(
                Trouser.Main,
                "NoModDetection",
                "Prevents sign text from resolving client-side translation and keybind components."
        );
    }
}