package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pwn.noobs.trouserstreak.Trouser;

public class InstantKill extends Module {
	public static final Logger LOGGER = LogManager.getLogger("instantkill");

	public static final MinecraftClient mc = MinecraftClient.getInstance();

	public static boolean shouldAddVelocity = true;
	public InstantKill() {
		super(Trouser.Main, "InstaKillBow", "Enable/Disable instakill");
	}
	@EventHandler
	public void onInitialize() {
		LOGGER.info("the bow instant kill is real working");
	}

	//the exploit used here is not mine!
	//I dont completely know where it originated, but I'm pretty sure it's 2b2t.
	public static void addVelocityToPlayer(){
		if(shouldAddVelocity){
			mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
			for (int i = 0; i < 200; i++) {
				mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.000000001, mc.player.getZ(), true));
				mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), false));
			}
		}
	}
}
