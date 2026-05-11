package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            Scoreboard scoreboard = mc.level.getScoreboard();
            Collection<ScoreHolder> scoreHolders = scoreboard.getTrackedPlayers();
            StringBuilder namesBuilder = new StringBuilder();
            for (ScoreHolder holder : scoreHolders) {
                namesBuilder.append(holder.getScoreboardName()).append(", ");
            }
            String getKnownPlayers = namesBuilder.toString();
            int chunkX = (int) mc.player.getX() >> 4;
            int chunkZ = (int) mc.player.getZ() >> 4;
            LevelChunk chunk = mc.level.getChunk(chunkX, chunkZ);

            boolean foundAnyOre = false;
            boolean isNewGeneration = false;
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            outer:
            for (int x = 0; x < 16; x++) {
                for (int y = mc.level.getMinY(); y < mc.level.getMaxY(); y++) {
                    for (int z = 0; z < 16; z++) {
                        mutablePos.set(x, y, z);
                        Block block = chunk.getBlockState(mutablePos).getBlock();
                        if (!foundAnyOre && isOreBlock(block) && mc.level.dimension() == Level.OVERWORLD) {
                            foundAnyOre = true;
                        }
                        if (!isNewGeneration && y < 260 && y > 5 && NEW_OVERWORLD_BLOCKS.contains(block) && mc.level.dimension() == Level.OVERWORLD) {
                            isNewGeneration = true;
                        }
                        if (foundAnyOre && isNewGeneration) break outer;
                    }
                }
            }

            if (!isNewGeneration) {
                ChatUtils.sendMsg(Component.nullToEmpty("This chunk is pre 1.17 generation!"));
            } else {
                ChatUtils.sendMsg(Component.nullToEmpty("This chunk is new generation! (post-1.17)"));
            }
            ChatUtils.sendMsg(Component.nullToEmpty("East World Border X: "+(int) mc.level.getWorldBorder().getMaxX()+", West World Border X: "+(int) mc.level.getWorldBorder().getMinX()+", South World Border Z: "+(int) mc.level.getWorldBorder().getMaxZ()+", North World Border Z: "+(int) mc.level.getWorldBorder().getMinZ()));
            ChatUtils.sendMsg(Component.nullToEmpty("WorldSpawn Location: x"+mc.level.getLevelData().getRespawnData().pos().getX()+" y"+mc.level.getLevelData().getRespawnData().pos().getY()+" z"+mc.level.getLevelData().getRespawnData().pos().getZ()));
            Optional<GlobalPos> deathPos = mc.player.getLastDeathLocation();
            if (deathPos.isPresent()) {
                GlobalPos pos = deathPos.get();
                ChatUtils.sendMsg(Component.nullToEmpty(
                        "Last Death Location: x" + pos.pos().getX() +
                                " y" + pos.pos().getY() +
                                " z" + pos.pos().getZ() +
                                " | Dimension: " + pos.dimension().identifier()
                ));
            } else {
                ChatUtils.sendMsg(Component.nullToEmpty("No recorded death location"));
            }
            ChatUtils.sendMsg(Component.nullToEmpty("Difficulty: "+mc.level.getDifficulty().toString()));
            ChatUtils.sendMsg(Component.nullToEmpty("Permission Level: "+PermissionUtils.getPermissionLevel(mc.player)));
            ChatUtils.sendMsg(Component.nullToEmpty("Simulation Distance (chunks): "+mc.level.getServerSimulationDistance()));
            ChatUtils.sendMsg(Component.nullToEmpty("Day Count: "+Math.floor(mc.level.getGameTime()/24000)));
            ChatUtils.sendMsg(Component.nullToEmpty("KnownPlayers (Names with a period are bedrock players): "+getKnownPlayers));
            return SINGLE_SUCCESS;
        });
        builder.then(literal("save").executes(ctx -> {
            if (!mc.player.getMainHandItem().isEmpty()){
                Scoreboard scoreboard = mc.level.getScoreboard();
                Collection<ScoreHolder> scoreHolders = scoreboard.getTrackedPlayers();
                StringBuilder namesBuilder = new StringBuilder();
                for (ScoreHolder holder : scoreHolders) {
                    namesBuilder.append(holder.getScoreboardName()).append(", ");
                }

                String getKnownPlayers = namesBuilder.toString();
                int chunkX = (int) mc.player.getX() >> 4;
                int chunkZ = (int) mc.player.getZ() >> 4;
                LevelChunk chunk = mc.level.getChunk(chunkX, chunkZ);

                boolean foundAnyOre = false;
                boolean isNewGeneration = false;
                for (int x = 0; x < 16; x++) {
                    for (int y = mc.level.getMinY(); y < mc.level.getMaxY(); y++) {
                        for (int z = 0; z < 16; z++) {
                            if (!foundAnyOre && isOreBlock(chunk.getBlockState(new BlockPos(x, y, z)).getBlock()) && mc.level.dimension().identifier().toString().toLowerCase().contains("overworld")) {
                                foundAnyOre = true;
                            }
                            if (!isNewGeneration && y < 256 && y >= 0 && (chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.COPPER_ORE || chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.DEEPSLATE_COPPER_ORE) && mc.level.dimension().identifier().toString().toLowerCase().contains("overworld")) {
                                isNewGeneration = true;
                            }
                        }
                    }
                }

                if (!isNewGeneration) {
                    ChatUtils.sendMsg(Component.nullToEmpty("This chunk is pre 1.17 generation!"));
                } else {
                    ChatUtils.sendMsg(Component.nullToEmpty("This chunk is new generation! (post-1.17)"));
                }
                ChatUtils.sendMsg(Component.nullToEmpty("East World Border X: "+(int) mc.level.getWorldBorder().getMaxX()+", West World Border X: "+(int) mc.level.getWorldBorder().getMinX()+", South World Border Z: "+(int) mc.level.getWorldBorder().getMaxZ()+", North World Border Z: "+(int) mc.level.getWorldBorder().getMinZ()));
                ChatUtils.sendMsg(Component.nullToEmpty("WorldSpawn Location: x"+mc.level.getLevelData().getRespawnData().pos().getX()+" y"+mc.level.getLevelData().getRespawnData().pos().getY()+" z"+mc.level.getLevelData().getRespawnData().pos().getZ()));
                ChatUtils.sendMsg(Component.nullToEmpty("Difficulty: "+mc.level.getDifficulty().toString()));
                ChatUtils.sendMsg(Component.nullToEmpty("Permission Level: "+PermissionUtils.getPermissionLevel(mc.player)));
                ChatUtils.sendMsg(Component.nullToEmpty("Simulation Distance (chunks): "+mc.level.getServerSimulationDistance()));
                ChatUtils.sendMsg(Component.nullToEmpty("Day Count: "+Math.floor(mc.level.getGameTime()/24000)));
                ChatUtils.sendMsg(Component.nullToEmpty("KnownPlayers (Names with a period are bedrock players): "+getKnownPlayers));

                String serverip;
                if (mc.isLocalServer()==true){
                    Path worldPath = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
                    Path savesDir = worldPath.getParent();
                    if (savesDir != null) {
                        Path worldDir = savesDir.getFileName();
                        serverip = (worldDir != null ? worldDir.toString() : "singleplayer")
                                .replaceAll("[^a-zA-Z0-9._-]", "_");
                    } else {
                        serverip = "singleplayer";
                    }
                } else {
                    serverip = mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9._\\-]", "_");
                }

                try {
                    new File("TrouserStreak/SavedWorldInfo/"+serverip+"/").mkdirs();
                    try (FileWriter writer = new FileWriter("TrouserStreak/SavedWorldInfo/"+serverip+"/WorldInfoData.txt", true)) {
                        if (!isNewGeneration) {
                            writer.write("This chunk is pre 1.17 generation!");
                            writer.write("\r\n");
                        } else {
                            writer.write("This chunk is new generation! (post-1.17)");
                            writer.write("\r\n");
                        }
                        writer.write("East World Border X: "+(int) mc.level.getWorldBorder().getMaxX()+", West World Border X: "+(int) mc.level.getWorldBorder().getMinX()+", South World Border Z: "+(int) mc.level.getWorldBorder().getMaxZ()+", North World Border Z: "+(int) mc.level.getWorldBorder().getMinZ());
                        writer.write("\r\n");
                        writer.write("WorldSpawn Location: x"+mc.level.getLevelData().getRespawnData().pos().getX()+" y"+mc.level.getLevelData().getRespawnData().pos().getY()+" z"+mc.level.getLevelData().getRespawnData().pos().getZ());
                        writer.write("\r\n");
                        writer.write("Difficulty: "+mc.level.getDifficulty().toString());
                        writer.write("\r\n");
                        writer.write("Permission Level: "+PermissionUtils.getPermissionLevel(mc.player));
                        writer.write("\r\n");
                        writer.write("Simulation Distance (chunks): "+mc.level.getServerSimulationDistance());
                        writer.write("\r\n");
                        writer.write("Day Count: "+Math.floor(mc.level.getGameTime()/24000));
                        writer.write("\r\n");
                        writer.write("KnownPlayers (Names with a period are bedrock players): "+getKnownPlayers);
                        writer.write("\r\n");
                    }
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