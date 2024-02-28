package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GarbageCleanerCommand extends Command {
    public GarbageCleanerCommand() {
        super("cleanram", "Clears garbage from RAM."); // Courtesy of youtube.com/@ogmur
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            {
                ChatUtils.sendMsg(Text.of("Cleaning RAM."));}
            System.gc();
            {
                ChatUtils.sendMsg(Text.of("RAM Cleared."));}
            return SINGLE_SUCCESS;
        });
    }
}   
