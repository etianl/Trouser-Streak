package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import pwn.noobs.trouserstreak.Trouser;
import java.util.List;
//credits to ogmur (https://www.youtube.com/@Ogmur) for the idea and etianl for writing
public class OPplayerTPmodule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> tp2u = sgGeneral.add(new BoolSetting.Builder()
            .name("TP Players to you instead")
            .description("Turn this on to teleport the players to you.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> notOP = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle Module if not OP")
            .description("Turn this off to prevent the bug of module always being turned off when you join server.")
            .defaultValue(false)
            .build()
    );
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton deletedata = table.add(theme.button("RESET CURRENT PLAYER")).expandX().minWidth(100).widget();
        deletedata.action = () -> {
            currentplayer = 0;
        };
        table.row();
        return table;
    }
    public static int currentplayer = 0;
    public OPplayerTPmodule() {
        super(Trouser.Main, "OPplayerTPmodule", "**REQUIRES OP** Teleports you to each player on the server with a button press if keybound, or teleport people to you.");
    }
    private List<AbstractClientPlayerEntity> players = null;

    @Override
    public void onActivate() {
        if (notOP.get() && !(mc.player.hasPermissionLevel(4)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have OP");
        }
        players = mc.world.getPlayers();
        if (currentplayer<players.size()) currentplayer++;
        if (players.get(currentplayer-1) == mc.player) {
            //skip past yourself
            currentplayer++;
        }
        if (players.size()==1){
            error("No other players online.");
            currentplayer=0;
            toggle();
            return;
        }
        if (currentplayer>=players.size()) currentplayer=players.size();
        error("Player "+currentplayer+" of "+players.size());
        if (tp2u.get()) ChatUtils.sendPlayerMsg("/tp "+mc.player.getName().getString()+" "+players.get(currentplayer-1).getName().getString());
        else if (!tp2u.get()) ChatUtils.sendPlayerMsg("/tp "+players.get(currentplayer-1).getName().getString()+" "+mc.player.getName().getString());
        error(String.valueOf(players.get(currentplayer-1).getName().getString()));
        if (currentplayer>=players.size()) currentplayer=0;
        toggle();
    }
}