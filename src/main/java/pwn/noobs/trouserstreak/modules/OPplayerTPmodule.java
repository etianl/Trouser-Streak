package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.Collection;

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
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton deletedata = table.add(theme.button("RESET CURRENT PLAYER")).expandX().minWidth(100).widget();
        deletedata.action = () -> i = 0;
        table.row();
        return table;
    }
    public OPplayerTPmodule() {
        super(Trouser.Main, "OPplayerTPmodule", "**REQUIRES OP** Teleports you to each player on the server with a button press if keybound, or teleport people to you.");
    }

    private int i = 0;

    @Override
    public void onActivate() {
        if (notOP.get() && !(mc.player.hasPermissionLevel(2)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have permission level 2 or higher");
            return;
        }
        Collection<PlayerListEntry> playerListEntries = mc.getNetworkHandler().getPlayerList();
        ArrayList<PlayerListEntry> playerArrayList = new ArrayList<>(playerListEntries);
        if(playerArrayList.get(i).getProfile().getName().equals(mc.player.getName().getLiteralString())) i++;
        if(i-1 >= playerArrayList.size()) i = 0;
        if(playerArrayList.size() == 1) {
            error("No other players online.");
            toggle();
            i = 0;
            return;
        }
        if(tp2u.get()) ChatUtils.sendPlayerMsg("/tp " + playerArrayList.get(i).getProfile().getName() + " " + mc.player.getName().getLiteralString());
        if (!tp2u.get()) ChatUtils.sendPlayerMsg("/tp " + mc.player.getName().getLiteralString() + " " + playerArrayList.get(i).getProfile().getName());
        i++;
        toggle();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        i = 0;
    }

    @Override
    public void onDeactivate() {
        i = 0;
    }
}