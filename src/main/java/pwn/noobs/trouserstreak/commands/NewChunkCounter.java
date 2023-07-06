package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
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
        NewerNewChunks n=new NewerNewChunks();
        builder.then(literal("NewChunks").executes(ctx -> {
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks = n.newchunksfound;
            ChatUtils.sendMsg(Text.of(chunks+"  NewChunk locations have been saved by NewerNewChunks in this dimension."));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("OldChunks").executes(ctx -> {
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks = n.oldchunksfound;
            ChatUtils.sendMsg(Text.of(chunks+"  OldChunk locations have been saved by NewerNewChunks in this dimension."));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("FlowBelowY0Chunks").executes(ctx -> {
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks = n.olderoldchunksfound;
            ChatUtils.sendMsg(Text.of(chunks+"  FlowBelowY0Chunk locations have been saved by NewerNewChunks in this dimension."));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("LightingExploitChunks").executes(ctx -> {
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks = n.tickexploitchunksfound;
            ChatUtils.sendMsg(Text.of(chunks+"  Lighting/TickExploitChunk locations have been saved by NewerNewChunks in this dimension."));
            return SINGLE_SUCCESS;
        }));
        builder.executes(ctx -> {
            n.chunkcounterticks=0;
            n.chunkcounter=true;
            int chunks1 = n.newchunksfound;
            int chunks2 = n.olderoldchunksfound;
            int chunks4 = n.tickexploitchunksfound;
            int chunks3 = n.oldchunksfound;
            ChatUtils.sendMsg(Text.of("New: "+chunks1+" | FlowBelowY=0: "+chunks2+" | Lighting/TickExploitChunk: "+chunks4+" | Old: "+chunks3+" | Chunk locations have been saved by NewerNewChunks in this dimension."));
            return SINGLE_SUCCESS;
        });
    }
}
