package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.world.level.block.LevelEvent;
import pwn.noobs.trouserstreak.Trouser;

public class WorldEventCoords extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> dragon = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect DRAGON_DEATH")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> portal = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect END_PORTAL_SPAWN")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> wither = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect WITHER_BOSS_SPAWN")
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
        if (!(event.packet instanceof ClientboundLevelEventPacket packet)) return;

        if (!packet.isGlobalEvent()) return;

        BlockPos pos = packet.getPos();
        int eventId = packet.getType();
        int data = packet.getData();
        String eventString = "unknown global event";
        if (eventId == LevelEvent.SOUND_DRAGON_DEATH) {
            eventString = "DRAGON_DEATH";
            if (!dragon.get()) return;
        }
        if (eventId == LevelEvent.SOUND_END_PORTAL_SPAWN) {
            eventString = "END_PORTAL_SPAWN";
            if (!portal.get()) return;
        }
        if (eventId == LevelEvent.SOUND_WITHER_BOSS_SPAWN) {
            eventString = "WITHER_BOSS_SPAWN";
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