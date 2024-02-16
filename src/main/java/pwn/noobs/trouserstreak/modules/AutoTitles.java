//Credits to ogmur (youtube.com/@ogmur) for figuring out these commands, credits to etianl for writing a module for it
//    /title @a times 5s 9999d 9999d
//    /title @a title {"text":"Mojang Loss Prevention Inc", "bold":true, "italic":true, "color":"red"}
//    /title @a subtitle {"text":"discord.gg/vXSpKgU2ms", "bold":true, "italic":true, "color":"green"}
//    /title @a actionbar {"text":"youtube.com/@ogmur", "bold":true, "italic":true, "color":"green"}

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoTitles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTitle = settings.createGroup("Title Options");
    private final SettingGroup sgsubTitle = settings.createGroup("Subitle Options");
    private final SettingGroup sgActionbar = settings.createGroup("Actionbar Options");


    public final Setting<Boolean> notOP = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle Module if not OP")
            .description("Turn this off to prevent the bug of module always being turned off when you join server.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> notitleself = sgGeneral.add(new BoolSetting.Builder()
            .name("dont-title-yourself")
            .description("Don't set a title for yourself.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> notitlefrend = sgGeneral.add(new BoolSetting.Builder()
            .name("dont-title-friends")
            .description("Don't set a title for friend.")
            .defaultValue(true)
            .build()
    );
    private final Setting<String> title = sgTitle.add(new StringSetting.Builder()
            .name("title-text")
            .description("title text")
            .defaultValue("Mountains Of Lava Inc.")
            .wide()
            .build()
    );
    private final Setting<Boolean> titleitalic = sgTitle.add(new BoolSetting.Builder()
            .name("Italicized Title")
            .description("Title is in italics.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> titlebold = sgTitle.add(new BoolSetting.Builder()
            .name("Bold Title")
            .description("Title is bold.")
            .defaultValue(true)
            .build()
    );
    private final Setting<String> titlecolour = sgTitle.add(new StringSetting.Builder()
            .name("title-colour")
            .description("title colour")
            .defaultValue("white")
            .wide()
            .build()
    );
    private final Setting<Boolean> makesubtitle = sgsubTitle.add(new BoolSetting.Builder()
            .name("Make Subitle Line")
            .description("Makes the Subitle text display")
            .defaultValue(true)
            .build()
    );
    private final Setting<String> subtitle = sgsubTitle.add(new StringSetting.Builder()
            .name("subtitle-text")
            .description("subtitle text")
            .defaultValue("https://www.youtube.com/@mountainsoflavainc.6913")
            .wide()
            .visible(() -> makesubtitle.get())
            .build()
    );
    private final Setting<Boolean> subtitleitalic = sgsubTitle.add(new BoolSetting.Builder()
            .name("Italicized Subtitle")
            .description("Title is in italics.")
            .defaultValue(true)
            .visible(() -> makesubtitle.get())
            .build()
    );
    private final Setting<Boolean> subtitlebold = sgsubTitle.add(new BoolSetting.Builder()
            .name("Bold Subtitle")
            .description("Title is bold.")
            .defaultValue(true)
            .visible(() -> makesubtitle.get())
            .build()
    );
    private final Setting<String> subtitlecolour = sgsubTitle.add(new StringSetting.Builder()
            .name("subtitle-colour")
            .description("title colour")
            .defaultValue("green")
            .wide()
            .visible(() -> makesubtitle.get())
            .build()
    );
    private final Setting<Boolean> makeactionbar = sgActionbar.add(new BoolSetting.Builder()
            .name("Make Actionbar Line")
            .description("Makes the Actionbar text display")
            .defaultValue(true)
            .build()
    );
    private final Setting<String> actionbar = sgActionbar.add(new StringSetting.Builder()
            .name("actionbar-text")
            .description("actionbar text")
            .defaultValue("Renovations in progress.")
            .wide()
            .visible(() -> makeactionbar.get())
            .build()
    );
    private final Setting<Boolean> actionbaritalic = sgActionbar.add(new BoolSetting.Builder()
            .name("Italicized Actionbar")
            .description("Actionbar is in italics.")
            .defaultValue(false)
            .visible(() -> makeactionbar.get())
            .build()
    );
    private final Setting<Boolean> actionbarbold = sgActionbar.add(new BoolSetting.Builder()
            .name("Bold Actionbar")
            .description("Actionbar is bold.")
            .defaultValue(true)
            .visible(() -> makeactionbar.get())
            .build()
    );
    private final Setting<String> actionbarcolour = sgActionbar.add(new StringSetting.Builder()
            .name("actionbar-colour")
            .description("actionbar colour")
            .defaultValue("yellow")
            .wide()
            .visible(() -> makeactionbar.get())
            .build()
    );
    private final Setting<fadeinModes> fadeinmode = sgGeneral.add(new EnumSetting.Builder<fadeinModes>()
            .name("FadeIn Mode")
            .description("the FadeIn value mode")
            .defaultValue(fadeinModes.seconds)
            .build());
    private final Setting<Integer> fadeinseconds = sgGeneral.add(new IntSetting.Builder()
            .name("FadeIn Seconds")
            .description("How long for the title to fadein, in seconds")
            .defaultValue(2)
            .min(1)
            .sliderRange(1,999999999)
            .visible(() -> fadeinmode.get() == fadeinModes.seconds)
            .build()
    );
    private final Setting<Integer> fadeindays = sgGeneral.add(new IntSetting.Builder()
            .name("FadeIn Days")
            .description("How long for the title to fadein, in days")
            .defaultValue(1)
            .min(1)
            .sliderRange(1,9999)
            .visible(() -> fadeinmode.get() == fadeinModes.days)
            .build()
    );
    private final Setting<durationModes> durationmode = sgGeneral.add(new EnumSetting.Builder<durationModes>()
            .name("Duration Mode")
            .description("the Duration value mode")
            .defaultValue(durationModes.days)
            .build());
    private final Setting<Integer> durationseconds = sgGeneral.add(new IntSetting.Builder()
            .name("Duration Seconds")
            .description("How long for the title to stay for, in seconds")
            .defaultValue(999999999)
            .min(1)
            .sliderRange(1,999999999)
            .visible(() -> durationmode.get() == durationModes.seconds)
            .build()
    );
    private final Setting<Integer> durationdays = sgGeneral.add(new IntSetting.Builder()
            .name("Duration Days")
            .description("How long for the title to stay for, in days")
            .defaultValue(9999)
            .min(1)
            .sliderRange(1,9999)
            .visible(() -> durationmode.get() == durationModes.days)
            .build()
    );
    private final Setting<fadeoutModes> fadeoutmode = sgGeneral.add(new EnumSetting.Builder<fadeoutModes>()
            .name("FadeOut Mode")
            .description("the FadeOut value mode")
            .defaultValue(fadeoutModes.days)
            .build());
    private final Setting<Integer> fadeoutseconds = sgGeneral.add(new IntSetting.Builder()
            .name("FadeOut Seconds")
            .description("How long for the title to fadeout, in seconds")
            .defaultValue(999999999)
            .min(1)
            .sliderRange(1,999999999)
            .visible(() -> fadeoutmode.get() == fadeoutModes.seconds)
            .build()
    );
    private final Setting<Integer> fadeoutdays = sgGeneral.add(new IntSetting.Builder()
            .name("FadeOut Days")
            .description("How long for the title to fadeout, in days")
            .defaultValue(9999)
            .min(1)
            .sliderRange(1,9999)
            .visible(() -> fadeoutmode.get() == fadeoutModes.days)
            .build()
    );
    public AutoTitles() {
        super(Trouser.Main, "AutoTitles", "Creates text across the screens for online players. Requires OP.");
    }

    private CopyOnWriteArrayList<PlayerListEntry> players;
    private String fadein;
    private String duration;
    private String fadeout;


    @Override
    public void onActivate() {
        if (notOP.get() && !(mc.player.hasPermissionLevel(2)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have permission level 2 or higher");
        }
        switch (fadeinmode.get()) {
            case seconds -> fadein=fadeinseconds.get()+"s";
            case days -> fadein=fadeindays.get()+"d";
        }
        switch (durationmode.get()) {
            case seconds -> duration=durationseconds.get()+"s";
            case days -> duration=durationdays.get()+"d";
        }
        switch (fadeoutmode.get()) {
            case seconds -> fadeout=fadeoutseconds.get()+"s";
            case days -> fadeout=fadeoutdays.get()+"d";
        }

        if (!notitlefrend.get()) {
            if (notitleself.get()){
                if (messageLengthExceedsLimit("/title @a[name=!" + mc.player.getName().getString() + "] title {\"text\":\"" + title.get() + "\", \"bold\":" + titlebold.get() + ", \"italic\":" + titleitalic.get() + ", \"color\":\"" + titlecolour.get() + "\"}", "Title")) {
                    toggle();
                    return;
                }
                ChatUtils.sendPlayerMsg("/title @a[name=!" + mc.player.getName().getString() + "] times " + fadein + " " + duration + " " + fadeout);
                ChatUtils.sendPlayerMsg("/title @a[name=!" + mc.player.getName().getString() + "] title {\"text\":\"" + title.get() + "\", \"bold\":" + titlebold.get() + ", \"italic\":" + titleitalic.get() + ", \"color\":\"" + titlecolour.get() + "\"}");
                if (makesubtitle.get() && messageLengthExceedsLimit("/title @a[name=!" + mc.player.getName().getString() + "] subtitle {\"text\":\"" + subtitle.get() + "\", \"bold\":" + subtitlebold.get() + ", \"italic\":" + subtitleitalic.get() + ", \"color\":\"" + subtitlecolour.get() + "\"}", "Subtitle")) {
                } else if (makesubtitle.get())ChatUtils.sendPlayerMsg("/title @a[name=!" + mc.player.getName().getString() + "] subtitle {\"text\":\"" + subtitle.get() + "\", \"bold\":" + subtitlebold.get() + ", \"italic\":" + subtitleitalic.get() + ", \"color\":\"" + subtitlecolour.get() + "\"}");
                if (makeactionbar.get() && messageLengthExceedsLimit("/title @a[name=!" + mc.player.getName().getString() + "] actionbar {\"text\":\"" + actionbar.get() + "\", \"bold\":" + actionbarbold.get() + ", \"italic\":" + actionbaritalic.get() + ", \"color\":\"" + actionbarcolour.get() + "\"}", "Actionbar")) {
                } else if (makeactionbar.get())ChatUtils.sendPlayerMsg("/title @a[name=!" + mc.player.getName().getString() + "] actionbar {\"text\":\"" + actionbar.get() + "\", \"bold\":" + actionbarbold.get() + ", \"italic\":" + actionbaritalic.get() + ", \"color\":\"" + actionbarcolour.get() + "\"}");
            } else if (!notitleself.get()){
                if (messageLengthExceedsLimit("/title @a title {\"text\":\"" + title.get() + "\", \"bold\":" + titlebold.get() + ", \"italic\":" + titleitalic.get() + ", \"color\":\"" + titlecolour.get() + "\"}", "Title")) {
                    toggle();
                    return;
                }
                ChatUtils.sendPlayerMsg("/title @a times " + fadein + " " + duration + " " + fadeout);
                ChatUtils.sendPlayerMsg("/title @a title {\"text\":\"" + title.get() + "\", \"bold\":" + titlebold.get() + ", \"italic\":" + titleitalic.get() + ", \"color\":\"" + titlecolour.get() + "\"}");
                if (makesubtitle.get() && messageLengthExceedsLimit("/title @a subtitle {\"text\":\"" + subtitle.get() + "\", \"bold\":" + subtitlebold.get() + ", \"italic\":" + subtitleitalic.get() + ", \"color\":\"" + subtitlecolour.get() + "\"}", "Subtitle")) {
                } else if (makesubtitle.get())ChatUtils.sendPlayerMsg("/title @a subtitle {\"text\":\"" + subtitle.get() + "\", \"bold\":" + subtitlebold.get() + ", \"italic\":" + subtitleitalic.get() + ", \"color\":\"" + subtitlecolour.get() + "\"}");
                if (makeactionbar.get() && messageLengthExceedsLimit("/title @a actionbar {\"text\":\"" + actionbar.get() + "\", \"bold\":" + actionbarbold.get() + ", \"italic\":" + actionbaritalic.get() + ", \"color\":\"" + actionbarcolour.get() + "\"}", "Actionbar")) {
                } else if (makeactionbar.get())ChatUtils.sendPlayerMsg("/title @a actionbar {\"text\":\"" + actionbar.get() + "\", \"bold\":" + actionbarbold.get() + ", \"italic\":" + actionbaritalic.get() + ", \"color\":\"" + actionbarcolour.get() + "\"}");
            }
        } else if (notitlefrend.get()) {
            players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
            List<String> friendNames = new ArrayList<>();
            if (notitleself.get())friendNames.add("name=!" + mc.player.getName().getString());
            for(PlayerListEntry player : players) {
                if(Friends.get().isFriend(player) && notitlefrend.get()) friendNames.add("name=!" + player.getProfile().getName());
            }
            String friendsString = String.join(",", friendNames);
            if (messageLengthExceedsLimit("/title @a[" + friendsString + "] title {\"text\":\"" + title.get() + "\", \"bold\":" + titlebold.get() + ", \"italic\":" + titleitalic.get() + ", \"color\":\"" + titlecolour.get() + "\"}", "Title")) {
                error("You may also have too many friends online.");
                toggle();
                return;
            }
            ChatUtils.sendPlayerMsg("/title @a[" + friendsString + "] times " + fadein + " " + duration + " " + fadeout);
            ChatUtils.sendPlayerMsg("/title @a[" + friendsString + "] title {\"text\":\"" + title.get() + "\", \"bold\":" + titlebold.get() + ", \"italic\":" + titleitalic.get() + ", \"color\":\"" + titlecolour.get() + "\"}");
            if (makesubtitle.get() && messageLengthExceedsLimit("/title @a[" + friendsString + "] subtitle {\"text\":\"" + subtitle.get() + "\", \"bold\":" + subtitlebold.get() + ", \"italic\":" + subtitleitalic.get() + ", \"color\":\"" + subtitlecolour.get() + "\"}", "Subtitle")) {
                error("You may also have too many friends online.");
            } else if (makesubtitle.get())ChatUtils.sendPlayerMsg("/title @a[" + friendsString + "] subtitle {\"text\":\"" + subtitle.get() + "\", \"bold\":" + subtitlebold.get() + ", \"italic\":" + subtitleitalic.get() + ", \"color\":\"" + subtitlecolour.get() + "\"}");
            if (makeactionbar.get() && messageLengthExceedsLimit("/title @a[" + friendsString + "] actionbar {\"text\":\"" + actionbar.get() + "\", \"bold\":" + actionbarbold.get() + ", \"italic\":" + actionbaritalic.get() + ", \"color\":\"" + actionbarcolour.get() + "\"}", "Actionbar")) {
                error("You may also have too many friends online.");
            } else if (makeactionbar.get())ChatUtils.sendPlayerMsg("/title @a[" + friendsString + "] actionbar {\"text\":\"" + actionbar.get() + "\", \"bold\":" + actionbarbold.get() + ", \"italic\":" + actionbaritalic.get() + ", \"color\":\"" + actionbarcolour.get() + "\"}");
        }
        toggle();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        toggle();
    }
    private boolean messageLengthExceedsLimit(String message, String messageType) {
        int maxLength = 257;
        if (message.length() > maxLength) {
            int excessLength = message.length() - maxLength;
            error("The " + messageType + " command is too long. Shorten it by " + excessLength + " characters.");
            return true;
        }
        return false;
    }
    public enum fadeinModes {
        seconds, days
    }
    public enum fadeoutModes {
        days, seconds
    }
    public enum durationModes {
        days, seconds
    }
}