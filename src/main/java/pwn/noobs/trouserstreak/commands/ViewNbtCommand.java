package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ViewNbtCommand extends Command {
    public ViewNbtCommand() {
        super("viewnbt", "Tells you the nbt data of the item in your main hand.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            if (!mc.player.getMainHandStack().isEmpty()) {
                if (mc.player.getMainHandStack().getComponents() == null) {
                    error("No NBT data for item.");
                    return SINGLE_SUCCESS;
                }
                ChatUtils.sendMsg(Text.of(mc.player.getMainHandStack().getComponents().toString()));
            } else error("No item in main hand.");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("save").executes(ctx -> {
            if (!mc.player.getMainHandStack().isEmpty()) {
                if (mc.player.getMainHandStack().getComponents() == null) {
                    error("No NBT data for item.");
                    return SINGLE_SUCCESS;
                }
                ChatUtils.sendMsg(Text.of(mc.player.getMainHandStack().getComponents().toString()));
                if (!Files.exists(Paths.get("TrouserStreak/SavedNBT/ViewedNBTData.txt"))) {
                    File file = new File("TrouserStreak/SavedNBT/ViewedNBTData.txt");
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                    }
                }
                try {
                    new File("TrouserStreak/SavedNBT/").mkdirs();
                    FileWriter writer = new FileWriter("TrouserStreak/SavedNBT/ViewedNBTData.txt", true);
                    writer.write(String.valueOf(mc.player.getMainHandStack().getComponents().toString()));
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