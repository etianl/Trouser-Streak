package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GarbageCleanerCommand extends Command {
    public GarbageCleanerCommand() {
        super("cleanram", "Clears garbage from RAM."); // Courtesy of youtube.com/@ogmur
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            {
                ChatUtils.sendMsg(Component.nullToEmpty("Cleaning RAM."));}
            System.gc();
            {
                ChatUtils.sendMsg(Component.nullToEmpty("RAM Cleared."));}
            return SINGLE_SUCCESS;
        });
    }
}