package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AutoCommand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Where to get list of commands from")
            .defaultValue(Mode.Manual1)
            .build()
    );
    public final Setting<Boolean> auto = sgGeneral.add(new BoolSetting.Builder()
            .name("Loop Commands")
            .description("Loops the commands executed.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> commandDelay = sgGeneral.add(new IntSetting.Builder()
            .name("command-delay")
            .description("Tick delay between each command")
            .defaultValue(1)
            .min(0)
            .sliderMax(100)
            .build()
    );
    private final Setting<List<String>> commands1 = sgGeneral.add(new StringListSetting.Builder()
            .name("commands")
            .description("List of commands to be sent. Supports Starscript.")
            .defaultValue(Arrays.asList(
                    "/deop @a[name=!{player}]",
                    "/whitelist off",
                    "/pardon {player}",
                    "/op {player}"
            ))
            .renderer(StarscriptTextBoxRenderer.class)
            .visible(() -> mode.get() == Mode.Manual1)
            .build()
    );
    private final Setting<List<String>> commands2 = sgGeneral.add(new StringListSetting.Builder()
            .name("commands")
            .description("List of commands to be sent. Supports Starscript.")
            .defaultValue(Arrays.asList(
                    "/kill @a[name=!{player}]",
                    "/execute at @a[name=!{player}] run summon wither ~ ~10 ~ {Invulnerable:1b}"
            ))
            .renderer(StarscriptTextBoxRenderer.class)
            .visible(() -> mode.get() == Mode.Manual2)
            .build()
    );
    private final Setting<List<String>> commands3 = sgGeneral.add(new StringListSetting.Builder()
            .name("commands")
            .description("List of commands to be sent. Supports Starscript.")
            .defaultValue(Arrays.asList(
                    "/execute at @a run summon fireball ~ ~10 ~ {ExplosionPower:127b, Motion:[0.0,-5.0,0.0]}"
            ))
            .renderer(StarscriptTextBoxRenderer.class)
            .visible(() -> mode.get() == Mode.Manual3)
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

    private int tickCounter = 0;
    private boolean sent = false;
    private Queue<String> commandQueue = new LinkedList<>();

    public AutoCommand() {
        super(Trouser.operator, "auto-command", "Automatically runs commands when player has/gets operator access");
    }

    @Override
    public void onActivate() {
        sent = false;
        commandQueue = new LinkedList<>();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        sent = false;
        commandQueue = new LinkedList<>();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || !mc.player.hasPermissionLevel(permissionLevel.get())) return;

        if (commandQueue.isEmpty()) {
            if (sent && !auto.get()) {
                if (disableOnFinish.get()) toggle();
                return;
            }

            if (commandDelay.get() != 0) RunCommands();
            else ZeroTickRunCommands();
            sent = true;
        } else {
            tickCounter++;
            if (tickCounter >= commandDelay.get()) {
                String command = commandQueue.poll();
                if (command.equals("EXECUTE_MACRO")) {
                    try {
                        Macros.get().get(macroName.get()).onAction();
                    } catch (NullPointerException ex) {
                        error("Invalid macro! Is your macro name set correctly?");
                    }
                } else {
                    String processedCommand = MeteorStarscript.run(MeteorStarscript.compile(command));
                    ChatUtils.sendPlayerMsg(processedCommand);
                }
                tickCounter = 0;
            }
        }

        if (auto.get() && commandQueue.isEmpty()) {
            sent = false;
        }
    }

    private void ZeroTickRunCommands() {
        if(mode.get() == Mode.Manual1) for(String command : commands1.get()) {
            String processedCommand = MeteorStarscript.run(MeteorStarscript.compile(command));
            if (processedCommand.length()<=256){
                ChatUtils.sendPlayerMsg(processedCommand);
            }
            else {
                int characterstodelete = processedCommand.length()-256;
                error("This command is too long ("+processedCommand+"). Shorten it by "+characterstodelete+" characters.");
            }
        }
        if(mode.get() == Mode.Manual2) for(String command : commands2.get()) {
            String processedCommand = MeteorStarscript.run(MeteorStarscript.compile(command));
            if (processedCommand.length()<=256){
                ChatUtils.sendPlayerMsg(processedCommand);
            }
            else {
                int characterstodelete = processedCommand.length()-256;
                error("This command is too long ("+processedCommand+"). Shorten it by "+characterstodelete+" characters.");
            }
        }
        if(mode.get() == Mode.Manual3) for(String command : commands3.get()) {
            String processedCommand = MeteorStarscript.run(MeteorStarscript.compile(command));
            if (processedCommand.length()<=256){
                ChatUtils.sendPlayerMsg(processedCommand);
            }
            else {
                int characterstodelete = processedCommand.length()-256;
                error("This command is too long ("+processedCommand+"). Shorten it by "+characterstodelete+" characters.");
            }
        }
        if(mode.get() == Mode.Macro) {
            try {
                Macros.get().get(macroName.get()).onAction();
            } catch (NullPointerException ex) {
                error("Invalid macro! Is your macro name set correctly?");
            }
        }
    }

    private void RunCommands() {
        if (mode.get() == Mode.Macro) {
            commandQueue.add("EXECUTE_MACRO");
        } else {
            List<String> commandList;
            if (mode.get() == Mode.Manual1) commandList = commands1.get();
            else if (mode.get() == Mode.Manual2) commandList = commands2.get();
            else if (mode.get() == Mode.Manual3) commandList = commands3.get();
            else return;

            for (String command : commandList) {
                String processedCommand = MeteorStarscript.run(MeteorStarscript.compile(command));
                if (processedCommand.length() <= 256) {
                    commandQueue.add(command);
                } else {
                    int charactersToDelete = processedCommand.length() - 256;
                    error("This command is too long (" + processedCommand + "). Shorten it by " + charactersToDelete + " characters.");
                }
            }
        }
    }

    public enum Mode {
        Manual1,
        Manual2,
        Manual3,
        Macro
    }
}