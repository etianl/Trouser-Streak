package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ViewNbtCommand extends Command {
    public ViewNbtCommand() {
        super("viewnbt", "Tells you the nbt data of the item in your main hand.");
    }
    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(ctx -> {
            if (!mc.player.getMainHandItem().isEmpty()){
                if (mc.player.getMainHandItem().getComponents() == null){
                    error("No NBT data for item.");
                    return SINGLE_SUCCESS;
                }
                ChatUtils.sendMsg(Component.nullToEmpty(mc.player.getMainHandItem().getComponents().toString()));
            }
            else error("No item in main hand.");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("save").executes(ctx -> {
            if (!mc.player.getMainHandItem().isEmpty()){
                if (mc.player.getMainHandItem().getComponents() == null){
                    error("No NBT data for item.");
                    return SINGLE_SUCCESS;
                }
                ChatUtils.sendMsg(Component.nullToEmpty(mc.player.getMainHandItem().getComponents().toString()));
                try {
                    new File("TrouserStreak/SavedNBT/").mkdirs();
                    try (FileWriter writer = new FileWriter("TrouserStreak/SavedNBT/ViewedNBTData.txt", true)) {
                        writer.write(mc.player.getMainHandItem().getComponents().toString());
                        writer.write("\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else error("No item in main hand.");
            return SINGLE_SUCCESS;
        }));
    }
}