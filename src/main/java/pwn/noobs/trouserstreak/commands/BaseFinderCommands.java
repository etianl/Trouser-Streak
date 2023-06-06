package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import pwn.noobs.trouserstreak.modules.BaseFinder;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaseFinderCommands extends Command {
    public BaseFinderCommands() {
        super("base", "Extra functionality for the BaseFinder module.");
    }
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        BaseFinder b=new BaseFinder();
        builder.executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                if (b.closestbaseX<1000000000 && b.closestbaseZ<1000000000){
                    b.findnearestbase=true;
                    ChatUtils.sendMsg(Text.of("#Nearest possible base at X"+b.closestbaseX+" x Z"+b.closestbaseZ));
                    b.findnearestbase=false;
                    return SINGLE_SUCCESS;
                }else {
                    error("No Bases Logged Yet.");
                    return SINGLE_SUCCESS;
                }
            }
        });
        builder.then(literal("add").executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                b.AddCoordX= mc.player.getChunkPos().x;
                b.AddCoordZ= mc.player.getChunkPos().z;
                ChatUtils.sendMsg(Text.of("Base near X"+mc.player.getChunkPos().getCenterX()+", Z"+mc.player.getChunkPos().getCenterZ()+" added to the BaseFinder."));
                return SINGLE_SUCCESS;}
        }));
        builder.then(literal("add").then(argument("x",FloatArgumentType.floatArg()).then(argument("z",FloatArgumentType.floatArg()).executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
            float X = FloatArgumentType.getFloat(ctx, "x");
            float Z = FloatArgumentType.getFloat(ctx, "z");
            b.AddCoordX= Math.floorDiv((int) X,16);
            b.AddCoordZ= Math.floorDiv((int) Z,16);
            ChatUtils.sendMsg(Text.of("Base near X"+X+", Z"+Z+" added to the BaseFinder."));
            return SINGLE_SUCCESS;}
        }))));
        builder.then(literal("rmv").executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
            b.RemoveCoordX= mc.player.getChunkPos().x;
            b.RemoveCoordZ= mc.player.getChunkPos().z;
            ChatUtils.sendMsg(Text.of("Base near X"+mc.player.getChunkPos().getCenterX()+", Z"+mc.player.getChunkPos().getCenterZ()+" removed from the BaseFinder."));
            return SINGLE_SUCCESS;}
        }));
        builder.then(literal("rmv").then(argument("x",FloatArgumentType.floatArg()).then(argument("z",FloatArgumentType.floatArg()).executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                float X = FloatArgumentType.getFloat(ctx, "x");
                float Z = FloatArgumentType.getFloat(ctx, "z");
                b.RemoveCoordX= Math.floorDiv((int) X,16);
                b.RemoveCoordZ= Math.floorDiv((int) Z,16);
                ChatUtils.sendMsg(Text.of("Base near X"+X+", Z"+Z+" removed from the BaseFinder."));
                return SINGLE_SUCCESS;}
        }))));
    }
}
