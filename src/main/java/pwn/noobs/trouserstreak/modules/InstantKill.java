//Note from Saturn5Vfive:
//the exploit used here is not mine!
//I don't completely know where it originated, but I'm pretty sure it's 2b2t.
//note from etianl: I skidded this from Saturn5Vfive to get it into trouser-streak
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import pwn.noobs.trouserstreak.Trouser;

public class InstantKill extends Module {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean shouldAddVelocity = true;
    public static boolean shouldAddVelocity1 = false;
    public static boolean shouldAddVelocity2 = false;
    public static boolean shouldAddVelocity3 = false;
    public static boolean shouldAddVelocity0 = false;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> auto = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoDraw")
            .description("Automatically draws your bow.")
            .defaultValue(false)
            .build()
    );

    public InstantKill() {
        super(Trouser.Main, "InstaKill", "Enable/Disable instakill");
    }

    public static void addVelocityToPlayer() {
        if (mc.player == null) return;
        if (shouldAddVelocity) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            for (int i = 0; i < 100; i++) {
                sendmovementpackets();
            }
        }
        if (shouldAddVelocity1) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            for (int i = 0; i < 150; i++) {
                sendmovementpackets();
            }
        }
        if (shouldAddVelocity2) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            for (int i = 0; i < 200; i++) {
                sendmovementpackets();
            }
        }
        if (shouldAddVelocity3) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            for (int i = 0; i < 300; i++) {
                sendmovementpackets();
            }
        }
        if (shouldAddVelocity0) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            for (int i = 0; i < 50; i++) {
                sendmovementpackets();
            }
        }
    }

    private static void sendmovementpackets() {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.000000001, mc.player.getZ(), true, mc.player.horizontalCollision));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), false, mc.player.horizontalCollision));
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player != null && auto.get() && mc.player.getMainHandStack().getItem() == Items.BOW) {
            if (!mc.player.isUsingItem()) {
                mc.options.useKey.setPressed(true);
            }
        }
    }

    @Override
    public void onDeactivate() {
        mc.options.useKey.setPressed(false);
    }
}