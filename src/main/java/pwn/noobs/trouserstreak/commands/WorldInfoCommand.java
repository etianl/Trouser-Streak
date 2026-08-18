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
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("amethyst_block")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("azalea")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("big_dripleaf")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("big_dripleaf_stem")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("small_dripleaf")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cave_vines")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cave_vines_plant")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("spore_blossom")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_copper_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_iron_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_coal_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_redstone_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_emerald_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_gold_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_lapis_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_diamond_ore")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("glow_lichen")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("raw_copper_block")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("raw_iron_block")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dripstone_block")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("moss_block")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pointed_dripstone")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("smooth_basalt")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("tuff")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("calcite")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("hanging_roots")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("rooted_dirt")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("azalea_leaves")));
        NEW_OVERWORLD_BLOCKS.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("flowering_azalea_leaves")));
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
                            if (!isNewGeneration && y < 256 && y >= 0 && (chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_ore")) || chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_copper_ore"))) && mc.level.dimension().identifier().toString().toLowerCase().contains("overworld")) {
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
        return block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("coal_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_copper_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_iron_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("gold_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_gold_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lapis_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_lapis_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("diamond_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_diamond_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("redstone_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_redstone_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("emerald_ore"))
                || block == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("deepslate_emerald_ore"));
    }
}