package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class DupeModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    public DupeModule() {
        super(Trouser.Main, "1.17InventoryDupe", "Enable/Disable 1.17 InvDupe button.");
    }

}
