package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import pwn.noobs.trouserstreak.utils.MovementPatch;

public class MovementPatchCommand extends Command {
    public MovementPatchCommand() {
        super("movementpatch26.3", "Toggles tick-end injection before movement packets.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            MovementPatch.toggle();

            ChatUtils.sendMsg(Component.nullToEmpty("26.3 movement patch is now " + (MovementPatch.isEnabled() ? "enabled" : "disabled") + "."));

            return SINGLE_SUCCESS;
        });
        builder.then(literal("on").executes(context -> {
            MovementPatch.setEnabled(true);
            ChatUtils.sendMsg(Component.nullToEmpty("26.3 movement patch enabled."));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("off").executes(context -> {
            MovementPatch.setEnabled(false);
            ChatUtils.sendMsg(Component.nullToEmpty("26.3 movement patch disabled."));
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("status").executes(context -> {
            ChatUtils.sendMsg(Component.nullToEmpty("26.3 movement patch is currently " + (MovementPatch.isEnabled() ? "enabled" : "disabled") + "."));
            return SINGLE_SUCCESS;
        }));
    }
}