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
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class WorldInfoCommand extends Command {
    public WorldInfoCommand() {
        super("world", "Tells you the coordinates of each world border, and the spawn location.");
    }

    private static final Set<Block> NEW_OVERWORLD_BLOCKS = new HashSet<>();
    static {
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.AMETHYST_BLOCK);
        NEW_OVERWORLD_BLOCKS.add(Blocks.AZALEA);
        NEW_OVERWORLD_BLOCKS.add(Blocks.BIG_DRIPLEAF);
        NEW_OVERWORLD_BLOCKS.add(Blocks.BIG_DRIPLEAF_STEM);
        NEW_OVERWORLD_BLOCKS.add(Blocks.SMALL_DRIPLEAF);
        NEW_OVERWORLD_BLOCKS.add(Blocks.CAVE_VINES);
        NEW_OVERWORLD_BLOCKS.add(Blocks.CAVE_VINES_PLANT);
        NEW_OVERWORLD_BLOCKS.add(Blocks.SPORE_BLOSSOM);
        NEW_OVERWORLD_BLOCKS.add(Blocks.COPPER_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_COPPER_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_IRON_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_COAL_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_EMERALD_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_GOLD_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_LAPIS_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.GLOW_LICHEN);
        NEW_OVERWORLD_BLOCKS.add(Blocks.RAW_COPPER_BLOCK);
        NEW_OVERWORLD_BLOCKS.add(Blocks.RAW_IRON_BLOCK);
        NEW_OVERWORLD_BLOCKS.add(Blocks.DRIPSTONE_BLOCK);
        NEW_OVERWORLD_BLOCKS.add(Blocks.MOSS_BLOCK);
        NEW_OVERWORLD_BLOCKS.add(Blocks.POINTED_DRIPSTONE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.SMOOTH_BASALT);
        NEW_OVERWORLD_BLOCKS.add(Blocks.TUFF);
        NEW_OVERWORLD_BLOCKS.add(Blocks.CALCITE);
        NEW_OVERWORLD_BLOCKS.add(Blocks.HANGING_ROOTS);
        NEW_OVERWORLD_BLOCKS.add(Blocks.ROOTED_DIRT);
        NEW_OVERWORLD_BLOCKS.add(Blocks.AZALEA_LEAVES);
        NEW_OVERWORLD_BLOCKS.add(Blocks.FLOWERING_AZALEA_LEAVES);
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
                for (int y = mc.world.getBottomY(); y < mc.world.getTopYInclusive(); y++) {
                    for (int z = 0; z < 16; z++) {
                        if (!foundAnyOre && isOreBlock(chunk.getBlockState(new BlockPos(x, y, z)).getBlock()) && mc.world.getRegistryKey() == World.OVERWORLD) {
                            foundAnyOre = true;
                        }
                        if (!isNewGeneration && y < 260 && y > 5 && NEW_OVERWORLD_BLOCKS.contains(chunk.getBlockState(new BlockPos(x, y, z)).getBlock()) && mc.world.getRegistryKey() == World.OVERWORLD) {
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
            ChatUtils.sendMsg(Text.of("WorldSpawn Location: x"+mc.world.getLevelProperties().getSpawnPoint().getPos().getX()+" y"+mc.world.getLevelProperties().getSpawnPoint().getPos().getY()+" z"+mc.world.getLevelProperties().getSpawnPoint().getPos().getZ()));
            Optional<GlobalPos> deathPos = mc.player.getLastDeathPos();
            if (deathPos.isPresent()) {
                GlobalPos pos = deathPos.get();
                ChatUtils.sendMsg(Text.of(
                        "Last Death Location: x" + pos.pos().getX() +
                                " y" + pos.pos().getY() +
                                " z" + pos.pos().getZ() +
                                " | Dimension: " + pos.dimension().getValue()
                ));
            } else {
                ChatUtils.sendMsg(Text.of("No recorded death location"));
            }
            ChatUtils.sendMsg(Text.of("Difficulty: "+mc.world.getDifficulty().toString()));
            ChatUtils.sendMsg(Text.of("Permission Level: "+mc.player.getPermissionLevel()));
            ChatUtils.sendMsg(Text.of("Simulation Distance (chunks): "+mc.world.getSimulationDistance()));
            ChatUtils.sendMsg(Text.of("Day Count: "+Math.floor(mc.world.getTime()/24000)));
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
                    for (int y = mc.world.getBottomY(); y < mc.world.getTopYInclusive(); y++) {
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
                ChatUtils.sendMsg(Text.of("WorldSpawn Location: x"+mc.world.getLevelProperties().getSpawnPoint().getPos().getX()+" y"+mc.world.getLevelProperties().getSpawnPoint().getPos().getY()+" z"+mc.world.getLevelProperties().getSpawnPoint().getPos().getZ()));
                ChatUtils.sendMsg(Text.of("Difficulty: "+mc.world.getDifficulty().toString()));
                ChatUtils.sendMsg(Text.of("Permission Level: "+mc.player.getPermissionLevel()));
                ChatUtils.sendMsg(Text.of("Simulation Distance (chunks): "+mc.world.getSimulationDistance()));
                ChatUtils.sendMsg(Text.of("Day Count: "+Math.floor(mc.world.getTime()/24000)));
                ChatUtils.sendMsg(Text.of("KnownPlayers (Names with a period are bedrock players): "+getKnownPlayers));

                String serverip;
                if (mc.isInSingleplayer()==true){
                    String[] array = mc.getServer().getSavePath(WorldSavePath.ROOT).toString().replace(':', '_').split("/|\\\\");
                    serverip=array[array.length-2];
                } else {
                    serverip = mc.getCurrentServerEntry().address.replace(':', '_');
                }

                if (!Files.exists(Paths.get("TrouserStreak/SavedWorldInfo/"+serverip+"/WorldInfoData.txt"))){
                    File file = new File("TrouserStreak/SavedWorldInfo/"+serverip+"/WorldInfoData.txt");
                    try {
                        file.createNewFile();
                    } catch (IOException e) {}
                }
                try {
                    new File("TrouserStreak/SavedWorldInfo/"+serverip+"/").mkdirs();
                    FileWriter writer = new FileWriter("TrouserStreak/SavedWorldInfo/"+serverip+"/WorldInfoData.txt", true);
                    if (!isNewGeneration) {
                        writer.write("This chunk is pre 1.17 generation!");
                        writer.write("\r\n");   // write new line
                    } else {
                        writer.write("This chunk is new generation! (post-1.17)");
                        writer.write("\r\n");   // write new line
                    }
                    writer.write("East World Border X: "+(int) mc.world.getWorldBorder().getBoundEast()+", West World Border X: "+(int) mc.world.getWorldBorder().getBoundWest()+", South World Border Z: "+(int) mc.world.getWorldBorder().getBoundSouth()+", North World Border Z: "+(int) mc.world.getWorldBorder().getBoundNorth());
                    writer.write("\r\n");   // write new line
                    writer.write("WorldSpawn Location: x"+mc.world.getLevelProperties().getSpawnPoint().getPos().getX()+" y"+mc.world.getLevelProperties().getSpawnPoint().getPos().getY()+" z"+mc.world.getLevelProperties().getSpawnPoint().getPos().getZ());
                    writer.write("\r\n");   // write new line
                    writer.write("Difficulty: "+mc.world.getDifficulty().toString());
                    writer.write("\r\n");   // write new line
                    writer.write("Permission Level: "+mc.player.getPermissionLevel());
                    writer.write("\r\n");   // write new line
                    writer.write("Simulation Distance (chunks): "+mc.world.getSimulationDistance());
                    writer.write("\r\n");   // write new line
                    writer.write("Day Count: "+Math.floor(mc.world.getTime()/24000));
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