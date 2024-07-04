package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

//credits to DAM for the sauce
public class BungeeSpoofer extends Module {
    private final SettingGroup specialGroup = settings.createGroup("Credits to DAMcraft, maker of ServerSeeker.");

    public Setting<String> spoofedAddress = specialGroup.add(new StringSetting.Builder()
            .name("spoofed-address")
            .description("The spoofed IP address that will be sent to the server.")
            .defaultValue("127.0.0.1")
            .filter((text, c) -> (text + c).matches("^[0-9a-f\\\\.:]{0,45}$"))
            .build()
    );

    public BungeeSpoofer() {
        super(Trouser.Main, "BungeeSpoof", "Allows you to join servers with an exposed bungeecord backend. ONLY ENABLE THIS IF YOU ACTUALLY WANT TO JOIN A BUNGEESPOOFABLE SERVER!");
    }
}