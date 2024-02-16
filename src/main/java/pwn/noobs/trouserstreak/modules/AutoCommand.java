package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.List;

public class AutoCommand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Where to get list of commands from")
            .defaultValue(Mode.Manual)
            .build()
    );

    private final Setting<List<String>> commands = sgGeneral.add(new StringListSetting.Builder()
            .name("commands")
            .description("List of commands to be sent")
            .defaultValue(Arrays.asList(
                    "/deop @a[distance=.1..]",
                    "/whitelist off",
                    "/pardon etianl",
                    "/op etianl"
            ))
            .visible(() -> mode.get() == Mode.Manual)
            .build()
    );

    private final Setting<String> macroName = sgGeneral.add(new StringSetting.Builder()
            .name("macro-name")
            .description("The name of the macro to run")
            .defaultValue("op")
            .visible(() -> mode.get() == Mode.Macro)
            .build()
    );
    private final Setting<Integer> permissionLevel = sgGeneral.add(new IntSetting.Builder()
            .name("permission-level")
            .description("The permission level to check for before running commands, 3 should usually be enough")
            .defaultValue(3)
            .max(4)
            .sliderMax(4)
            .build()
    );
    private final Setting<Boolean> disableOnFinish = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-finish")
            .description("Disable the module when finished")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> auto = sgGeneral.add(new BoolSetting.Builder()
            .name("FULLAUTO")
            .description("FULL AUTO BABY!")
            .defaultValue(false)
            .build()
    );
    public final Setting<Integer> atickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("FULLAUTOTickDelay")
            .description("Tick Delay for FULLAUTO option.")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .visible(() -> auto.get())
            .build()
    );
    private int ticks = 0;
    private boolean sent;

    public AutoCommand() {
        super(Trouser.Main, "auto-command", "Automatically runs commands when player has/gets operator access");
    }

    @Override
    public void onActivate() {
        sent = false;
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        sent = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        if(sent && !auto.get()) return;

        if(mc.player.hasPermissionLevel(permissionLevel.get()) && !auto.get()) {
            if(mode.get() == Mode.Manual) for(String command : commands.get()) {
                if (command.length()<=257){
                    ChatUtils.sendPlayerMsg(command);
                }
                else {
                    int characterstodelete = command.length()-257;
                    error("This command is too long ("+command+"). Shorten it by "+characterstodelete+" characters.");
                }
            }
            if(mode.get() == Mode.Macro) {
                try {
                    Macros.get().get(macroName.get()).onAction();
                } catch (NullPointerException ex) {
                    error("Invalid macro! Is your macro name set correctly?");
                }
            }
            sent = true;
            if(disableOnFinish.get()) toggle();
        } else if(mc.player.hasPermissionLevel(permissionLevel.get()) && auto.get()){
            if (ticks<=atickdelay.get()){
                ticks++;
            } else if (ticks>atickdelay.get()){
                if(mode.get() == Mode.Manual) for(String command : commands.get()) {
                    if (command.length()<=257){
                        ChatUtils.sendPlayerMsg(command);
                    }
                    else {
                        int characterstodelete = command.length()-257;
                        error("This command is too long ("+command+"). Shorten it by "+characterstodelete+" characters.");
                    }
                }
                if(mode.get() == Mode.Macro) {
                    try {
                        Macros.get().get(macroName.get()).onAction();
                    } catch (NullPointerException ex) {
                        error("Invalid macro! Is your macro name set correctly?");
                    }
                }
                ticks=0;
            }
        }
    }

    public enum Mode {
        Manual,
        Macro
    }
}