package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
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
import java.util.List;
import java.util.Objects;

public class AutoScoreboard extends Module {
    private final SettingGroup sgTitle = settings.createGroup("Title Options");
    private final SettingGroup sgContent = settings.createGroup("Content Options");

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

    public AutoScoreboard() {
        super(Trouser.Main, "auto-scoreboard", "Automatically create a scoreboard using Starscript. Requires operator access.");
    }

    @Override
    public void onActivate() {
        assert mc.player != null;
        if(!mc.player.hasPermissionLevel(2)) {
            toggle();
            error("No permission!");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        String scoreboardName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
        ChatUtils.sendPlayerMsg("/scoreboard objectives add " + scoreboardName + " dummy {\"text\":\"" + MeteorStarscript.run(MeteorStarscript.compile(title.get())) + "\",\"color\":\"" + titleColor.get() + "\"}");
        ChatUtils.sendPlayerMsg("/scoreboard objectives setdisplay sidebar " + scoreboardName);
        int i = content.get().size();
        for(String string : content.get()) {
            String randomName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
            ChatUtils.sendPlayerMsg("/team add " + randomName);
            ChatUtils.sendPlayerMsg("/team modify " + randomName + " suffix {\"text\":\" " + MeteorStarscript.run(MeteorStarscript.compile(string)) + "\"}");
            ChatUtils.sendPlayerMsg("/team modify " + randomName + " color " + contentColor);
            ChatUtils.sendPlayerMsg("/team join " + randomName + " " + i);
            ChatUtils.sendPlayerMsg("/scoreboard players set " + i + " " + scoreboardName + " " + i);
            i--;
        }
        toggle();
        info("Created scoreboard.");
    }
}
