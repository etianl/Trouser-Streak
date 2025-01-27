package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import pwn.noobs.trouserstreak.modules.AutoMountain;

public class LavaTimeCalculator extends Command {
    public LavaTimeCalculator() {
        super("lavacalc", "Calculates amount of time for lava to flow down. Based on a 45 degree straight staircase at 20 ticks/second.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            if (mc.player.getBlockY() > 64) {
                float TopY = mc.player.getBlockY();
                float time = (((TopY - 64) * 60) / 20);

                ChatUtils.sendMsg(Text.of("Lava will take " + time + " seconds to go from your elevation (Y" + TopY + ") to Y64(sea level) on a 45degree staircase at 20TPS)."));
            } else if (mc.player.getBlockY() <= 64) {
                float TopY = mc.player.getBlockY();
                float time = (((TopY - (-60)) * 60) / 20);

                ChatUtils.sendMsg(Text.of("Lava will take " + time + " seconds to go from your elevation (Y" + TopY + ") to Y-60(Bottom of the world) on a 45degree staircase at 20TPS)."));
            }
            return SINGLE_SUCCESS;
        });
        builder.then(argument("TopY", FloatArgumentType.floatArg()).executes(ctx -> {
            if (mc.player.getBlockY() > 64) {
                float TopY = FloatArgumentType.getFloat(ctx, "TopY");
                float time = (((TopY - 64) * 60) / 20);

                ChatUtils.sendMsg(Text.of("Lava will take " + time + " seconds to go from Y" + TopY + " to Y64(sea level) on a 45degree staircase at 20TPS)."));
            } else if (mc.player.getBlockY() <= 64) {
                float TopY = FloatArgumentType.getFloat(ctx, "TopY");
                float time = (((TopY - (-60)) * 60) / 20);

                ChatUtils.sendMsg(Text.of("Lava will take " + time + " seconds to go from Y" + TopY + " to Y-60(Bottom of the world) on a 45degree staircase at 20TPS)."));
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(argument("TopY", FloatArgumentType.floatArg()).then(argument("BottomY", FloatArgumentType.floatArg()).executes(ctx -> {
            float TopY = FloatArgumentType.getFloat(ctx, "TopY");
            float BottomY = FloatArgumentType.getFloat(ctx, "BottomY");
            float time = (((TopY - BottomY) * 60) / 20);

            ChatUtils.sendMsg(Text.of("Lava will take " + time + " seconds to go from  Y" + TopY + " to Y" + BottomY + " on a 45degree staircase at 20TPS)."));
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("lastmountain").executes(ctx -> {
            new AutoMountain();
            new AutoMountain();
            new AutoMountain();
            new AutoMountain();
            new AutoMountain();
            new AutoMountain();
            new AutoMountain();
            new AutoMountain();
            if (((((2 + AutoMountain.highestblock.getY() - AutoMountain.lowestblock.getY()) * 60) / 20) + (((AutoMountain.lowestblock.getY() - AutoMountain.groundY) * 30) / 20)) <= (((((2 + AutoMountain.highestblock.getY() - AutoMountain.lowestblock.getY()) * 60) / 20) / 2) + (((AutoMountain.highestblock.getY() - AutoMountain.groundY2) * 30) / 20))) {
                new AutoMountain();
                new AutoMountain();
                new AutoMountain();
                new AutoMountain();
                float time = (((((2 + AutoMountain.highestblock.getY() - AutoMountain.lowestblock.getY()) * 60) / 20) / 2) + (((AutoMountain.highestblock.getY() - AutoMountain.groundY2) * 30) / 20));
                new AutoMountain();
                if (AutoMountain.lowestblock.getY() == 666) {
                    error("Use AutoMountain first to get the lowest block from the last Mountain.");
                } else
                    ChatUtils.sendMsg(Text.of("Lava will take " + time + " seconds to flow to the ground across your last Mountain"));
            } else {
                new AutoMountain();
                if (((((2 + new AutoMountain().highestblock.getY() - new AutoMountain().lowestblock.getY()) * 60) / 20) + (((new AutoMountain().lowestblock.getY() - new AutoMountain().groundY) * 30) / 20)) > (((((2 + new AutoMountain().highestblock.getY() - new AutoMountain().lowestblock.getY()) * 60) / 20) / 2) + (((new AutoMountain().highestblock.getY() - AutoMountain.groundY2) * 30) / 20))) {
                    new AutoMountain();
                    new AutoMountain();
                    new AutoMountain();
                    new AutoMountain();
                    float time = ((((2 + AutoMountain.highestblock.getY() - AutoMountain.lowestblock.getY()) * 60) / 20) + (((AutoMountain.lowestblock.getY() - AutoMountain.groundY) * 30) / 20));
                    new AutoMountain();
                    if (AutoMountain.lowestblock.getY() == 666) {
                        error("Use AutoMountain first to get the lowest block from the last Mountain.");
                    } else
                        ChatUtils.sendMsg(Text.of("Lava will take " + time + " seconds to flow to the ground across your last Mountain"));
                }
            }
            return SINGLE_SUCCESS;
        }));
    }
}