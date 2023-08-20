package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

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
            ChatUtils.sendMsg(Text.of("DO_DAYLIGHT_CYCLE: "+mc.world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)));
            return SINGLE_SUCCESS;
        });
        }
}
