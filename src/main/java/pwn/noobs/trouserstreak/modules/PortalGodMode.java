package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import pwn.noobs.trouserstreak.Trouser;

public class PortalGodMode extends Module {
    public PortalGodMode() {
        super(Trouser.Main, "PortalGodMode", "Makes you invincible after you walk through a portal. You will not be able to move while invincible until you disable the god mode.");
    }

    private boolean godMode = false;

    @Override
    public void onActivate() {
        godMode=false;
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof LevelLoadingScreen) {
            godMode=true;
        }
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (godMode && event.packet instanceof ServerboundAcceptTeleportationPacket) event.cancel();
    }
}