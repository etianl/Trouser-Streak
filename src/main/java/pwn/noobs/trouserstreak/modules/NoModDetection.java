package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class NoModDetection extends Module {
    public NoModDetection() {
        super(
                Trouser.Main,
                "NoModDetection",
                "Prevents sign text from resolving client-side translation and keybind components."
        );
    }
}