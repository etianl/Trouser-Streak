package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WorldBorderCoordsCommand extends Command {
    public WorldBorderCoordsCommand() {
        super("wb", "Tells you the coordinates of each world border.");
    }

    @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtils.sendMsg(Text.of("East World Border X: "+(int) mc.world.getWorldBorder().getBoundEast()+", West World Border X: "+(int) mc.world.getWorldBorder().getBoundWest()+", South World Border Z: "+(int) mc.world.getWorldBorder().getBoundSouth()+", North World Border Z: "+(int) mc.world.getWorldBorder().getBoundNorth()));
            return SINGLE_SUCCESS;
        });
        }
}
