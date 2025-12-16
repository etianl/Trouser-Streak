package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.PlayerListEntry;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

import java.util.concurrent.CopyOnWriteArrayList;

//credits to ogmur (https://www.youtube.com/@Ogmur) for the idea, etianl for writing and aaaasdfghjkllll for fixing
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
    public final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Doesn't teleport you to friends.")
            .defaultValue(true)
            .build()
    );
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton deletedata = table.add(theme.button("RESET CURRENT PLAYER")).expandX().minWidth(100).widget();
        deletedata.action = () -> currentplayer = 0;
        table.row();
        return table;
    }
    public OPplayerTPmodule() {
        super(Trouser.operator, "OPplayerTPmodule", "**REQUIRES OP** Teleports you to each player on the server with a button press if keybound, or teleport people to you.");
    }

    public static int currentplayer = 0;
    private CopyOnWriteArrayList<PlayerListEntry> players;

    @Override
    public void onActivate() {
        if (mc.player != null && mc.world != null && notOP.get() && PermissionUtils.getPermissionLevel(mc.player) < 2 && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have permission level 2 or higher");
            return;
        }
        players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
        for(PlayerListEntry player : players) {
            if(player.getProfile().name().equals(mc.player.getName().getLiteralString())) players.remove(player);
            if(Friends.get().isFriend(player) && ignoreFriends.get()) players.remove(player);
        }
        if(currentplayer < players.size()) currentplayer++;
        if(players.isEmpty()) {
            error("No other players online.");
            currentplayer = 0;
            toggle();
            return;
        }
        if(currentplayer >= players.size()) currentplayer = players.size();
        if(tp2u.get()) ChatUtils.sendPlayerMsg("/tp " + players.get(currentplayer - 1).getProfile().name() + " " + mc.player.getName().getLiteralString());
        if(!tp2u.get()) ChatUtils.sendPlayerMsg("/tp " + mc.player.getName().getLiteralString() + " " + players.get(currentplayer - 1).getProfile().name());
        if(currentplayer >= players.size()) currentplayer = 0;
        toggle();
    }
}