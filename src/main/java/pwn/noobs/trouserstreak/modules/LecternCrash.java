package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class LecternCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    public LecternCrash() {
        super(Trouser.Main, "LecternCrash", "Enable/Disable LecternCrash button");
    }

}
