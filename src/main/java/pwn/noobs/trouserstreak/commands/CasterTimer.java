package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import pwn.noobs.trouserstreak.modules.AutoLavaCaster;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class CasterTimer extends Command {
    public CasterTimer() {
        super("castertimer", "Calculates how long this cycle has been going for in AutoLavaCaster.");
    }

    @Override
        public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            AutoLavaCaster a=new AutoLavaCaster();
           int time = a.lavamountainticks/20;
           if (!(time==0)){
            ChatUtils.sendMsg(Component.nullToEmpty("This Cycle has been going for "+time+" Seconds."));}
           else if (time==0){
               error("AutoLavaCaster not started. Make more Mountains.");
           }
            return SINGLE_SUCCESS;
        });
        }
}
