package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AntiHunger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import pwn.noobs.trouserstreak.Trouser;

public class ItemTractorBeam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public final Setting<Integer> multiply = sgGeneral.add(new IntSetting.Builder()
            .name("Multiplier")
            .description("Higher values make success more likely at the cost of more hunger.")
            .defaultValue(90)
            .sliderRange(1,150)
            .min(1)
            .build()
    );
    public ItemTractorBeam() {
        super(Trouser.Main, "ItemTractorBeam", "Sucks up items from a very far distance using hunger points. Only works well for items on the same Y level. Only works in Vanilla.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        boolean antihungerWasEnabled = false;
        if (Modules.get().get(AntiHunger.class).isActive()){
            Modules.get().get(AntiHunger.class).toggle();
            antihungerWasEnabled = true;
        }
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        for (int i = 0; i < multiply.get(); i++) {
            sendmovementpackets();
        }
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        if (antihungerWasEnabled) Modules.get().get(AntiHunger.class).toggle();
        toggle();
    }
    // 0.000000001
    private static void sendmovementpackets(){
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.00000000000001, mc.player.getZ(), true, mc.player.horizontalCollision));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.00000000000001, mc.player.getZ(), false, mc.player.horizontalCollision));
    }
}