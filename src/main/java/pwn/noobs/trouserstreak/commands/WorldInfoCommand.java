package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class WorldInfoCommand extends Command {
    public WorldInfoCommand() {
        super("world", "Tells you the coordinates of each world border, and the spawn location.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            Scoreboard scoreboard = mc.world.getScoreboard();
            Collection<ScoreHolder> scoreHolders = scoreboard.getKnownScoreHolders();
            StringBuilder namesBuilder = new StringBuilder();
            for (ScoreHolder holder : scoreHolders) {
                namesBuilder.append(holder.getNameForScoreboard()).append(", ");
            }
            String getKnownPlayers = namesBuilder.toString();
            int chunkX = (int) mc.player.getX() >> 4;
            int chunkZ = (int) mc.player.getZ() >> 4;
            WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);

            boolean foundAnyOre = false;
            boolean isNewGeneration = false;
            for (int x = 0; x < 16; x++) {
                for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                    for (int z = 0; z < 16; z++) {
                        if (!foundAnyOre && isOreBlock(chunk.getBlockState(new BlockPos(x, y, z)).getBlock()) && mc.world.getRegistryKey().getValue().toString().toLowerCase().contains("overworld")) {
                            foundAnyOre = true;
                        }
                        if (!isNewGeneration && y < 256 && y >= 0 && (chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.COPPER_ORE || chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.DEEPSLATE_COPPER_ORE) && mc.world.getRegistryKey().getValue().toString().toLowerCase().contains("overworld")) {
                            isNewGeneration = true;
                        }
                    }
                }
            }

            if (!isNewGeneration) {
                ChatUtils.sendMsg(Text.of("This chunk is pre 1.17 generation!"));
            } else {
                ChatUtils.sendMsg(Text.of("This chunk is new generation! (post-1.17)"));
            }
            ChatUtils.sendMsg(Text.of("East World Border X: "+(int) mc.world.getWorldBorder().getBoundEast()+", West World Border X: "+(int) mc.world.getWorldBorder().getBoundWest()+", South World Border Z: "+(int) mc.world.getWorldBorder().getBoundSouth()+", North World Border Z: "+(int) mc.world.getWorldBorder().getBoundNorth()));
            ChatUtils.sendMsg(Text.of("WorldSpawn Location: x"+mc.world.getLevelProperties().getSpawnX()+" y"+mc.world.getLevelProperties().getSpawnY()+" z"+mc.world.getLevelProperties().getSpawnZ()));
            ChatUtils.sendMsg(Text.of("Difficulty: "+mc.world.getDifficulty().toString()));
            ChatUtils.sendMsg(Text.of("Simulation Distance (chunks): "+mc.world.getSimulationDistance()));
            ChatUtils.sendMsg(Text.of("Day Count: "+Math.floor(mc.world.getTime()/24000)));
            ChatUtils.sendMsg(Text.of("DO_DAYLIGHT_CYCLE: "+mc.world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)));
            ChatUtils.sendMsg(Text.of("KnownPlayers (Names with a period are bedrock players): "+getKnownPlayers));
            return SINGLE_SUCCESS;
        });
        builder.then(literal("save").executes(ctx -> {
            if (!mc.player.getMainHandStack().isEmpty()){
                Scoreboard scoreboard = mc.world.getScoreboard();
                Collection<ScoreHolder> scoreHolders = scoreboard.getKnownScoreHolders();
                StringBuilder namesBuilder = new StringBuilder();
                for (ScoreHolder holder : scoreHolders) {
                    namesBuilder.append(holder.getNameForScoreboard()).append(", ");
                }

                String getKnownPlayers = namesBuilder.toString();
                int chunkX = (int) mc.player.getX() >> 4;
                int chunkZ = (int) mc.player.getZ() >> 4;
                WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);

                boolean foundAnyOre = false;
                boolean isNewGeneration = false;
                for (int x = 0; x < 16; x++) {
                    for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                        for (int z = 0; z < 16; z++) {
                            if (!foundAnyOre && isOreBlock(chunk.getBlockState(new BlockPos(x, y, z)).getBlock()) && mc.world.getRegistryKey().getValue().toString().toLowerCase().contains("overworld")) {
                                foundAnyOre = true;
                            }
                            if (!isNewGeneration && y < 256 && y >= 0 && (chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.COPPER_ORE || chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.DEEPSLATE_COPPER_ORE) && mc.world.getRegistryKey().getValue().toString().toLowerCase().contains("overworld")) {
                                isNewGeneration = true;
                            }
                        }
                    }
                }

                if (!isNewGeneration) {
                    ChatUtils.sendMsg(Text.of("This chunk is pre 1.17 generation!"));
                } else {
                    ChatUtils.sendMsg(Text.of("This chunk is new generation! (post-1.17)"));
                }
                ChatUtils.sendMsg(Text.of("East World Border X: "+(int) mc.world.getWorldBorder().getBoundEast()+", West World Border X: "+(int) mc.world.getWorldBorder().getBoundWest()+", South World Border Z: "+(int) mc.world.getWorldBorder().getBoundSouth()+", North World Border Z: "+(int) mc.world.getWorldBorder().getBoundNorth()));
                ChatUtils.sendMsg(Text.of("WorldSpawn Location: x"+mc.world.getLevelProperties().getSpawnX()+" y"+mc.world.getLevelProperties().getSpawnY()+" z"+mc.world.getLevelProperties().getSpawnZ()));
                ChatUtils.sendMsg(Text.of("Difficulty: "+mc.world.getDifficulty().toString()));
                ChatUtils.sendMsg(Text.of("Simulation Distance (chunks): "+mc.world.getSimulationDistance()));
                ChatUtils.sendMsg(Text.of("Day Count: "+Math.floor(mc.world.getTime()/24000)));
                ChatUtils.sendMsg(Text.of("DO_DAYLIGHT_CYCLE: "+mc.world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)));
                ChatUtils.sendMsg(Text.of("KnownPlayers (Names with a period are bedrock players): "+getKnownPlayers));

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
                    if (!isNewGeneration) {
                        writer.write("This chunk is pre 1.17 generation!");
                        writer.write("\r\n");   // write new line
                    } else {
                        writer.write("This chunk is new generation! (post-1.17)");
                        writer.write("\r\n");   // write new line
                    }
                    writer.write("East World Border X: "+(int) mc.world.getWorldBorder().getBoundEast()+", West World Border X: "+(int) mc.world.getWorldBorder().getBoundWest()+", South World Border Z: "+(int) mc.world.getWorldBorder().getBoundSouth()+", North World Border Z: "+(int) mc.world.getWorldBorder().getBoundNorth());
                    writer.write("\r\n");   // write new line
                    writer.write("WorldSpawn Location: x"+mc.world.getLevelProperties().getSpawnX()+" y"+mc.world.getLevelProperties().getSpawnY()+" z"+mc.world.getLevelProperties().getSpawnZ());
                    writer.write("\r\n");   // write new line
                    writer.write("Difficulty: "+mc.world.getDifficulty().toString());
                    writer.write("\r\n");   // write new line
                    writer.write("Simulation Distance (chunks): "+mc.world.getSimulationDistance());
                    writer.write("\r\n");   // write new line
                    writer.write("Day Count: "+Math.floor(mc.world.getTime()/24000));
                    writer.write("\r\n");   // write new line
                    writer.write("DO_DAYLIGHT_CYCLE: "+mc.world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE));
                    writer.write("\r\n");   // write new line
                    writer.write("KnownPlayers (Names with a period are bedrock players): "+getKnownPlayers);
                    writer.write("\r\n");   // write new line
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else error("No item in main hand.");
            return SINGLE_SUCCESS;
        }));
    }
    private boolean isOreBlock(Block block) {
        return block == Blocks.COAL_ORE
                || block == Blocks.COPPER_ORE
                || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.IRON_ORE
                || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.GOLD_ORE
                || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.LAPIS_ORE
                || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.DIAMOND_ORE
                || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.REDSTONE_ORE
                || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.EMERALD_ORE
                || block == Blocks.DEEPSLATE_EMERALD_ORE;
    }
}