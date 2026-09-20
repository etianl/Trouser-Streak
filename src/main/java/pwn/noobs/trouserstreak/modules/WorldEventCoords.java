package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;
import pwn.noobs.trouserstreak.Trouser;

public class WorldEventCoords extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> dragon = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect ENDER_DRAGON_DIES")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> portal = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect END_PORTAL_OPENED")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> wither = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect WITHER_SPAWNS")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> unknown = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect unknown global events")
            .defaultValue(true)
            .build()
    );
    public WorldEventCoords() {
        super(Trouser.baseHunting, "WorldEventCoords", "Displays the positions of global world events.");
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof WorldEventS2CPacket packet)) return;

        if (!packet.isGlobal()) return;

        BlockPos pos = packet.getPos();
        int eventId = packet.getEventId();
        int data = packet.getData();
        String eventString = "unknown global event";
        if (eventId == WorldEvents.ENDER_DRAGON_DIES) {
            eventString = "ENDER_DRAGON_DIES";
            if (!dragon.get()) return;
        }
        if (eventId == WorldEvents.END_PORTAL_OPENED) {
            eventString = "END_PORTAL_OPENED";
            if (!portal.get()) return;
        }
        if (eventId == WorldEvents.WITHER_SPAWNS) {
            eventString = "WITHER_SPAWNS";
            if (!wither.get()) return;
        }
        if (eventString.equals("unknown global event")) {
            if (!unknown.get()) return;
        }
        info(String.format(
                "§a[WorldEvent] §fEventID: §e%s §f| Pos: §bX:%d Y:%d Z:%d §f| Data: §c%d",
                eventString, pos.getX(), pos.getY(), pos.getZ(), data
        ));
    }
}