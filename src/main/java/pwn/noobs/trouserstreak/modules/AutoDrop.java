package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

import meteordevelopment.orbit.EventHandler;
public class AutoDrop extends Module {
    public AutoDrop() {super(Trouser.Main, "auto-drop", "Drops the stack in your selected slot automatically");}

    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.player.dropSelectedItem(true);
    }
}
