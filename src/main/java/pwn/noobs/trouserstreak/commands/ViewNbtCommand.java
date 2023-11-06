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

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ViewNbtCommand extends Command {
    public ViewNbtCommand() {
        super("viewnbt", "Tells you the nbt data of the item in your main hand.");
    }
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            if (!mc.player.getMainHandStack().isEmpty()){
                if (mc.player.getMainHandStack().getNbt() == null){
                    error("No NBT data for item.");
                    return SINGLE_SUCCESS;
                }
                ChatUtils.sendMsg(Text.of(mc.player.getMainHandStack().getNbt().toString()));
            }
            else error("No item in main hand.");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("save").executes(ctx -> {
            if (!mc.player.getMainHandStack().isEmpty()){
                if (mc.player.getMainHandStack().getNbt() == null){
                    error("No NBT data for item.");
                    return SINGLE_SUCCESS;
                }
                ChatUtils.sendMsg(Text.of(mc.player.getMainHandStack().getNbt().toString()));
                if (!Files.exists(Paths.get("SavedNBT/ViewedNBTData.txt"))){
                    File file = new File("SavedNBT/ViewedNBTData.txt");
                    try {
                        file.createNewFile();
                    } catch (IOException e) {}
                }
                try {
                    new File("SavedNBT/").mkdirs();
                    FileWriter writer = new FileWriter("SavedNBT/ViewedNBTData.txt", true);
                    writer.write(String.valueOf(mc.player.getMainHandStack().getNbt().toString()));
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