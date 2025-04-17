//Credits to DedicateDev for making a pr for this! https://github.com/DedicateDev https://github.com/etianl/Trouser-Streak/pull/65
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.client.network.PlayerListEntry;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoNames extends Module {
    private static final String TEAM_PREFIX = "mol_";

    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgFormat = settings.createGroup("Name Modifications");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Presets> presetMode = sgGeneral.add(new EnumSetting.Builder<Presets>()
            .name("Preset Mode")
            .description("Quick presets for name modifications")
            .defaultValue(Presets.MOUNTAINS_OF_LAVA)
            .build()
    );

    private final Setting<Boolean> targetSelf = sgGeneral.add(new BoolSetting.Builder()
            .name("target-self")
            .description("Apply changes to your own name")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> trollfriends = sgGeneral.add(new BoolSetting.Builder()
            .name("apply-to-friends")
            .description("Whether or not to apply team formatting to friends")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> prefix = sgFormat.add(new StringSetting.Builder()
            .name("prefix")
            .description("Text to add before player names")
            .defaultValue("Who is")
            .visible(() -> presetMode.get() == Presets.CUSTOM)
            .build()
    );

    private final Setting<String> suffix = sgFormat.add(new StringSetting.Builder()
            .name("suffix")
            .description("Text to add after player names")
            .defaultValue("?")
            .visible(() -> presetMode.get() == Presets.CUSTOM)
            .build()
    );

    private final Setting<Boolean> rainbow = sgColors.add(new BoolSetting.Builder()
            .name("rainbow")
            .description("Makes the colors cycle smoothly through rainbow colors")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> rainbowDelay = sgColors.add(new IntSetting.Builder()
            .name("rainbow-delay")
            .description("Delay between color changes in ticks")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 100)
            .visible(rainbow::get)
            .build()
    );

    private final Setting<String> prefixColor = sgColors.add(new StringSetting.Builder()
            .name("prefix-color")
            .description("Color for the prefix text")
            .defaultValue("yellow")
            .visible(() -> presetMode.get() == Presets.CUSTOM && !rainbow.get())
            .build()
    );

    private final Setting<String> nameColor = sgColors.add(new StringSetting.Builder()
            .name("name-color")
            .description("Color for the player name")
            .defaultValue("red")
            .visible(() -> presetMode.get() == Presets.CUSTOM && !rainbow.get())
            .build()
    );

    private final Setting<String> suffixColor = sgColors.add(new StringSetting.Builder()
            .name("suffix-color")
            .description("Color for the suffix text")
            .defaultValue("yellow")
            .visible(() -> presetMode.get() == Presets.CUSTOM && !rainbow.get())
            .build()
    );

    private CopyOnWriteArrayList<PlayerListEntry> players;
    private int tickCounter;
    private int currentColorIndex;
    private String teamName;

    public AutoNames() {
        super(Trouser.operator, "auto-names", "Automatically change player name colors, prefix, suffix in tab and chat. Requires operator access.");
    }

    @Override
    public void onActivate() {
        if (!hasOperatorPermissions()) {
            error("No permission!");
            toggle();
            return;
        }

        teamName = TEAM_PREFIX + mc.player.getName().getString().toLowerCase();
        updateTeamColors();

        if (!rainbow.get()) {
            toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!rainbow.get()) return;

        if (++tickCounter >= rainbowDelay.get()) {
            tickCounter = 0;
            currentColorIndex = (currentColorIndex + 1) % 8;
            updateTeamColors();
        }
    }

    private boolean hasOperatorPermissions() {
        return mc.player.hasPermissionLevel(2);
    }

    private void updateTeamColors() {
        setupTeam();
        applyTeamFormatting();
        applyToPlayers();
    }

    private void setupTeam() {
        ChatUtils.sendPlayerMsg("/team add " + teamName);
    }

    private void applyTeamFormatting() {
        String currentColor = rainbow.get() ? getRainbowColor() : getPresetNameColor();
        String finalPrefix = getPresetPrefix();
        String finalSuffix = getPresetSuffix();

        sendTeamModifyCommands(currentColor, finalPrefix, finalSuffix);
    }

    private void sendTeamModifyCommands(String color, String prefix, String suffix) {
        ChatUtils.sendPlayerMsg("/team modify " + teamName + " color " + color);
        ChatUtils.sendPlayerMsg("/team modify " + teamName + " prefix {\"text\":\"" + prefix +
                "\",\"color\":\"" + (rainbow.get() ? color : getPresetPrefixColor()) + "\"}");
        ChatUtils.sendPlayerMsg("/team modify " + teamName + " suffix {\"text\":\"" + suffix +
                "\",\"color\":\"" + (rainbow.get() ? color : getPresetSuffixColor()) + "\"}");
    }

    private void applyToPlayers() {
        players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
        List<String> excludedPlayers = getExcludedPlayers();
        String playerSelector = excludedPlayers.isEmpty() ? "@a" : "@a[" + String.join(",", excludedPlayers) + "]";
        ChatUtils.sendPlayerMsg("/team join " + teamName + " " + playerSelector);
    }

    private List<String> getExcludedPlayers() {
        List<String> excluded = new ArrayList<>();
        if (!targetSelf.get()) {
            excluded.add("name=!" + mc.player.getName().getLiteralString());
        }
        if (!trollfriends.get()) {
            players.stream()
                    .filter(Friends.get()::isFriend)
                    .forEach(player -> excluded.add("name=!" + player.getProfile().getName()));
        }
        return excluded;
    }

    private String getRainbowColor() {
        return switch (currentColorIndex) {
            case 0 -> "red";
            case 1 -> "gold";
            case 2 -> "yellow";
            case 3 -> "green";
            case 4 -> "aqua";
            case 5 -> "blue";
            case 6 -> "light_purple";
            default -> "dark_purple";
        };
    }

    private String getPresetPrefix() {
        return switch (presetMode.get()) {
            case MOUNTAINS_OF_LAVA -> "[Admin] ";
            case YT_MOUNTAINS -> "[Youtube] ";
            case DEDICATE -> "[Dedicated] ";
            case CUSTOM -> prefix.get() + " ";
        };
    }

    private String getPresetSuffix() {
        return switch (presetMode.get()) {
            case MOUNTAINS_OF_LAVA -> " | Mountains of Lava Inc.";
            case YT_MOUNTAINS -> " | www.youtube.com/@mountainsoflavainc.6913";
            case DEDICATE -> " | Griefing";
            case CUSTOM -> " " + suffix.get();
        };
    }

    private String getPresetPrefixColor() {
        return switch (presetMode.get()) {
            case MOUNTAINS_OF_LAVA, YT_MOUNTAINS -> "dark_red";
            case DEDICATE -> "blue";
            case CUSTOM -> prefixColor.get();
        };
    }

    private String getPresetNameColor() {
        return switch (presetMode.get()) {
            case MOUNTAINS_OF_LAVA, YT_MOUNTAINS -> "gold";
            case DEDICATE -> "light_purple";
            case CUSTOM -> nameColor.get();
        };
    }

    private String getPresetSuffixColor() {
        return switch (presetMode.get()) {
            case MOUNTAINS_OF_LAVA -> "red";
            case YT_MOUNTAINS -> "blue";
            case DEDICATE -> "blue";
            case CUSTOM -> suffixColor.get();
        };
    }

    public enum Presets {
        MOUNTAINS_OF_LAVA,
        YT_MOUNTAINS,
        DEDICATE,
        CUSTOM
    }
}