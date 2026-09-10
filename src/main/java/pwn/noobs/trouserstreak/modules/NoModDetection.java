package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class NoModDetection extends Module {
    public NoModDetection() {
        super(Trouser.Main, "NoModDetection", "Modifies some translation keys to use a fallback instead. This may allow you to play on servers that do not allow mods.");
    }
}