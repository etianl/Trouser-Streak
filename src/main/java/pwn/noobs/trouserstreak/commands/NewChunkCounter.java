package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import pwn.noobs.trouserstreak.modules.NewerNewChunks;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class NewChunkCounter extends Command {
    public NewChunkCounter() {
        super("newchunkcount", "Counts how many chunks have been saved with NewerNewChunks.");
    }
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("NewChunks").executes(ctx -> {
            NewerNewChunks n=new NewerNewChunks();
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks = n.newchunksfound;
            ChatUtils.sendMsg(Text.of(chunks+"  NewChunk locations have been saved by NewerNewChunks. :D"));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("OldChunks").executes(ctx -> {
            NewerNewChunks n=new NewerNewChunks();
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks = n.oldchunksfound;
            ChatUtils.sendMsg(Text.of(chunks+"  OldChunk locations have been saved by NewerNewChunks. :D"));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("FlowBelowY0Chunks").executes(ctx -> {
            NewerNewChunks n=new NewerNewChunks();
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks = n.olderoldchunksfound;
            ChatUtils.sendMsg(Text.of(chunks+"  FlowBelowY0Chunk locations have been saved by NewerNewChunks. :D"));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("AllChunks").executes(ctx -> {
            NewerNewChunks n=new NewerNewChunks();
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks = n.olderoldchunksfound+n.oldchunksfound+n.newchunksfound;
            ChatUtils.sendMsg(Text.of(chunks+"  Total Chunk locations have been saved by NewerNewChunks. :D"));
            return SINGLE_SUCCESS;
        }));
    }
}
