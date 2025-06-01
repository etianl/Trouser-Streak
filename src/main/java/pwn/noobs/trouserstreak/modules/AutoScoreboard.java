package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import org.apache.commons.lang3.RandomStringUtils;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AutoScoreboard extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTitle = settings.createGroup("Title Options");
    private final SettingGroup sgContent = settings.createGroup("Content Options");
    public final Setting<Boolean> notOP = sgGeneral.add(new BoolSetting.Builder()
            .name("Permission Level Hold")
            .description("Prevents the code running until you get the correct permission level.")
            .defaultValue(true)
            .build()
    );
    private final Setting<String> title = sgTitle.add(new StringSetting.Builder()
            .name("title")
            .description("Title of the scoreboard to create. Supports Starscript.")
            .defaultValue("Trolled!")
            .wide()
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
    );

    private final Setting<String> titleColor = sgTitle.add(new StringSetting.Builder()
            .name("title-color")
            .description("Color of the title")
            .defaultValue("dark_red")
            .wide()
            .build()
    );

    private final Setting<List<String>> content = sgContent.add(new StringListSetting.Builder()
            .name("content")
            .description("Content of the scoreboard. Supports Starscript.")
            .defaultValue(Arrays.asList(
                    "Mountains Of Lava Inc.",
                    "youtube.com/@mountainsoflavainc.6913",
                    "Destroyed by {player}",
                    "{date}"
            ))
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
    );

    private final Setting<String> contentColor = sgContent.add(new StringSetting.Builder()
            .name("content-color")
            .description("Color of the content")
            .defaultValue("red")
            .build()
    );

    private final Setting<Boolean> useDelay = sgGeneral.add(new BoolSetting.Builder()
            .name("Use Command Delay")
            .description("Adds delay between commands to prevent kicks")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> commandDelay = sgGeneral.add(new IntSetting.Builder()
            .name("Command Delay")
            .description("Ticks between each command")
            .defaultValue(2)
            .min(1)
            .sliderMax(20)
            .visible(useDelay::get)
            .build()
    );

    private int tickCounter = 0;
    private Queue<String> commandQueue = new LinkedList<>();

    public AutoScoreboard() {
        super(Trouser.operator, "auto-scoreboard", "Automatically create a scoreboard using Starscript. Requires operator access.");
    }

    @Override
    public void onActivate() {
        String scoreboardName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
        String thecommand = "/scoreboard objectives add " + scoreboardName + " dummy {\"text\":\"" + MeteorStarscript.run(MeteorStarscript.compile(title.get())) + "\",\"color\":\"" + titleColor.get() + "\"}";

        if (thecommand.length() > 256) {
            int characterstodelete = thecommand.length()-256;
            error("Title is too long. Shorten it by "+characterstodelete+" characters.");
            toggle();
            return;
        }

        commandQueue.add(thecommand);
        commandQueue.add("/scoreboard objectives setdisplay sidebar " + scoreboardName);

        int i = content.get().size();
        for(String string : content.get()) {
            String randomName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
            commandQueue.add("/team add " + randomName);

            String thecommand2 = "/team modify " + randomName + " suffix {\"text\":\" " + MeteorStarscript.run(MeteorStarscript.compile(string)) + "\"}";
            if (thecommand2.length() <= 256) {
                commandQueue.add(thecommand2);
                commandQueue.add("/team modify " + randomName + " color " + contentColor);
                commandQueue.add("/team join " + randomName + " " + i);
                commandQueue.add("/scoreboard players set " + i + " " + scoreboardName + " " + i);
            } else {
                int characterstodelete = thecommand2.length()-256;
                error("This content line is too long ("+MeteorStarscript.run(MeteorStarscript.compile(string))+"). Shorten it by "+characterstodelete+" characters.");
                toggle();
                return;
            }
            i--;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (notOP.get() && !(mc.player.hasPermissionLevel(2)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            return;
        }
        int delay = commandDelay.get();
        if (!useDelay.get()) delay = 0;
        if (!commandQueue.isEmpty()) {
            if (tickCounter >= delay) {
                ChatUtils.sendPlayerMsg(commandQueue.poll());
                tickCounter = 0;
            } else {
                tickCounter++;
            }
        } else {
            toggle();
            info("Created scoreboard.");
        }
    }
}