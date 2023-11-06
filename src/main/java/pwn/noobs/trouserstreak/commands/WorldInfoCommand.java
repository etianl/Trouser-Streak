package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class WorldInfoCommand extends Command {
    public WorldInfoCommand() {
        super("world", "Tells you the coordinates of each world border, and the spawn location.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.sendMsg(Text.of("East World Border X: "+(int) mc.world.getWorldBorder().getBoundEast()+", West World Border X: "+(int) mc.world.getWorldBorder().getBoundWest()+", South World Border Z: "+(int) mc.world.getWorldBorder().getBoundSouth()+", North World Border Z: "+(int) mc.world.getWorldBorder().getBoundNorth()));
            ChatUtils.sendMsg(Text.of("Default WorldSpawn Location (May be different if changed): "+mc.world.getSpawnPos()));
            ChatUtils.sendMsg(Text.of("Difficulty: "+mc.world.getDifficulty().toString()));
            ChatUtils.sendMsg(Text.of("Simulation Distance (chunks): "+mc.world.getSimulationDistance()));
            ChatUtils.sendMsg(Text.of("Day Count: "+Math.floor(mc.world.getTime()/24000)));
            ChatUtils.sendMsg(Text.of("GameRules: "+mc.world.getGameRules().toNbt().toString()));
            ChatUtils.sendMsg(Text.of("KnownPlayers (Names with a period are bedrock players): "+mc.world.getScoreboard().getKnownPlayers()));
            return SINGLE_SUCCESS;
        });
        builder.then(literal("save").executes(ctx -> {
            if (!mc.player.getMainHandStack().isEmpty()){
                ChatUtils.sendMsg(Text.of("East World Border X: "+(int) mc.world.getWorldBorder().getBoundEast()+", West World Border X: "+(int) mc.world.getWorldBorder().getBoundWest()+", South World Border Z: "+(int) mc.world.getWorldBorder().getBoundSouth()+", North World Border Z: "+(int) mc.world.getWorldBorder().getBoundNorth()));
                ChatUtils.sendMsg(Text.of("Default WorldSpawn Location (May be different if changed): "+mc.world.getSpawnPos()));
                ChatUtils.sendMsg(Text.of("Difficulty: "+mc.world.getDifficulty().toString()));
                ChatUtils.sendMsg(Text.of("Simulation Distance (chunks): "+mc.world.getSimulationDistance()));
                ChatUtils.sendMsg(Text.of("Day Count: "+Math.floor(mc.world.getTime()/24000)));
                ChatUtils.sendMsg(Text.of("GameRules: "+mc.world.getGameRules().toNbt().toString()));
                ChatUtils.sendMsg(Text.of("KnownPlayers (Names with a period are bedrock players): "+mc.world.getScoreboard().getKnownPlayers()));

                String serverip;
                if (mc.isInSingleplayer()==true){
                    String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
                    serverip=array[array.length-2];
                } else {
                    serverip = mc.getCurrentServerEntry().address.replace(':', '_');
                }

                if (!Files.exists(Paths.get("SavedWorldInfo/"+serverip+"/WorldInfoData.txt"))){
                    File file = new File("SavedWorldInfo/"+serverip+"/WorldInfoData.txt");
                    try {
                        file.createNewFile();
                    } catch (IOException e) {}
                }
                try {
                    new File("SavedWorldInfo/"+serverip+"/").mkdirs();
                    FileWriter writer = new FileWriter("SavedWorldInfo/"+serverip+"/WorldInfoData.txt", true);
                    writer.write("East World Border X: "+(int) mc.world.getWorldBorder().getBoundEast()+", West World Border X: "+(int) mc.world.getWorldBorder().getBoundWest()+", South World Border Z: "+(int) mc.world.getWorldBorder().getBoundSouth()+", North World Border Z: "+(int) mc.world.getWorldBorder().getBoundNorth());
                    writer.write("\r\n");   // write new line
                    writer.write("Default WorldSpawn Location (May be different if changed): "+mc.world.getSpawnPos());
                    writer.write("\r\n");   // write new line
                    writer.write("Difficulty: "+mc.world.getDifficulty().toString());
                    writer.write("\r\n");   // write new line
                    writer.write("Simulation Distance (chunks): "+mc.world.getSimulationDistance());
                    writer.write("\r\n");   // write new line
                    writer.write("Day Count: "+Math.floor(mc.world.getTime()/24000));
                    writer.write("\r\n");   // write new line
                    writer.write("GameRules: "+mc.world.getGameRules().toNbt().toString());
                    writer.write("\r\n");   // write new line
                    writer.write("KnownPlayers (Names with a period are bedrock players): "+mc.world.getScoreboard().getKnownPlayers());
                    writer.write("\r\n");   // write new line
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else error("No item in main hand.");
            return SINGLE_SUCCESS;
        }));
    }
}