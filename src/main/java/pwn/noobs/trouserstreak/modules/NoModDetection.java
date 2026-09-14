package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class NoModDetection extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public enum Modes {
        InstalledMods,
        PrefixList,
        AllKeys
    }
    public final Setting<Modes> filterMode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("filter-mode")
            .description("PrefixList: List of translation keys. Add or remove whatever you want.\n" +
                    "InstalledMods: Filter keys that are not vanilla translation keys **This may break the text in some mods a little bit**\n" +
                    "AllKeys: Filter every key in the game indiscriminately.")
            .defaultValue(Modes.InstalledMods)
            .build());
    // Most of these keys were gathered from https://github.com/branduzzo/CheckHacks/blob/main/src/main/resources/checkhacks.yml
    // Thank you to the CheckHacks plugin for providing the things needed to prevent their own plugin from working
    public final Setting<List<String>> keys = sgGeneral.add(new StringListSetting.Builder()
            .name("key-prefixes")
            .description("Translation key prefixes to spoof. If the beginning of the key matches this string use the fallback.")
            .defaultValue(
                    "key.meteor-client.",
                    "module.",
                    "addon.",
                    "gui.xaero",
                    "baritone.",
                    "liquidbounce.module",
                    "key.freecam",
                    "key.wurst",
                    "xray.config",
                    "key.chestesp",
                    "key.killaura",
                    "key.autofish",
                    "key.lumina",
                    "bleachhack.module",
                    "emc.module",
                    "coffee.module",
                    "key.wdl",
                    "autoclicker-fabric.",
                    "key.antiafk",
                    "key.auto-clicker_",
                    "key.ui-utils",
                    "cracker.",
                    "swd.",
                    "litematica.",
                    "voicechat."
            )
            .visible(() -> filterMode.get() == Modes.PrefixList)
            .build()
    );
    public NoModDetection() {
        super(Trouser.Main, "NoModDetection", "Modifies some translation keys to use a fallback instead. This may allow you to play on servers that do not allow mods.");
    }
}