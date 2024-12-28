
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.PlayerListEntry;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoDisplays extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBlock = settings.createGroup("Block Display Options");
    private final SettingGroup sgText = settings.createGroup("Text Display Options");
    private final Setting<Boolean> disconnectdisable = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> notOP = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle Module if not OP")
            .description("Turn this off to prevent the bug of module always being turned off when you join server.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> allAloneToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle Module if alone")
            .description("Turn this on to prevent the module running if there is no one online.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> trollfriends = sgGeneral.add(new BoolSetting.Builder()
            .name("Spawn for Friends")
            .description("Whether or not to summon displays for friends.")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> trollyourself = sgGeneral.add(new BoolSetting.Builder()
            .name("Spawn for yourself")
            .description("Whether or not to summon displays for yourself.")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> killEntities = sgGeneral.add(new BoolSetting.Builder()
            .name("Kill Entities")
            .description("Whether to remove existing entities before creating new ones. Reduces lag.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> useDelay = sgGeneral.add(new BoolSetting.Builder()
            .name("Use Command Delay")
            .description("Adds delay between commands to prevent kicks")
            .defaultValue(false)
            .build());
    private final Setting<Integer> commandDelay = sgGeneral.add(new IntSetting.Builder()
            .name("Command Delay")
            .description("Ticks between each command")
            .defaultValue(2)
            .min(1)
            .sliderMax(20)
            .visible(useDelay::get)
            .build());
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("Tick Delay")
            .description("Delay in ticks before creating block and text displays.")
            .defaultValue(0)
            .min(0)
            .sliderMax(100)
            .build());
    private final Setting<Integer> killDelay = sgGeneral.add(new IntSetting.Builder()
            .name("Kill Delay")
            .description("Delay in ticks before removing existing entities.")
            .defaultValue(20)
            .min(0)
            .sliderMax(100)
            .build());
    private final Setting<Modes> displayMode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("Display Mode")
            .description("The mode for creating displays.")
            .defaultValue(Modes.TEXT)
            .build());
    private final Setting<Block> block = sgBlock.add(new BlockSetting.Builder()
            .name("Block")
            .description("The block to be displayed.")
            .defaultValue(Blocks.BLACK_CONCRETE)
            .visible(() -> displayMode.get() == Modes.BLOCK)
            .build());
    private final Setting<Integer> blockbrightness = sgBlock.add(new IntSetting.Builder()
            .name("Block Brightness")
            .description("Light level of the entity.")
            .defaultValue(0)
            .min(0)
            .sliderMax(15)
            .max(15)
            .visible(() -> displayMode.get() == Modes.BLOCK)
            .build());
    private final Setting<String> text = sgText.add(new StringSetting.Builder()
            .name("Custom Text")
            .description("Too much text will get you kicked.")
            .defaultValue("Your server is being renovated! youtube.com/@mountainsoflavainc.6913")
            .visible(() -> displayMode.get() == Modes.TEXT)
            .build());
    private final Setting<Integer> textbrightness = sgText.add(new IntSetting.Builder()
            .name("Text Brightness")
            .description("Light level of the entity.")
            .defaultValue(15)
            .min(0)
            .sliderMax(15)
            .max(15)
            .visible(() -> displayMode.get() == Modes.TEXT)
            .build());
    private final Setting<SettingColor> backgroundColor = sgText.add(new ColorSetting.Builder()
            .name("Background Color")
            .description("The background color with transparency.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(() -> displayMode.get() == Modes.TEXT)
            .build()
    );
    private final Setting<Double> distance = sgText.add(new DoubleSetting.Builder()
            .name("Custom Text Distance")
            .description("Distance from the player to render the text display.")
            .defaultValue(2.5)
            .min(1)
            .sliderRange(1, 10)
            .visible(() -> displayMode.get() == Modes.TEXT)
            .build());
    public AutoDisplays() {
        super(Trouser.Main, "auto-displays", "Automatically spam block or text displays around players. Requires operator access.");
    }
    private CopyOnWriteArrayList<PlayerListEntry> players;
    private int tickTimer = 0;
    private int killTimer = 0;
    private Queue<String> commandQueue = new LinkedList<>();
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get())toggle();
    }
    @Override
    public void onActivate() {
        if (mc.player == null) return;
        tickTimer = 0;
        killTimer = 0;
        if (notOP.get() && !(mc.player.hasPermissionLevel(2)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have permission level 2 or higher");
        }
    }
    @Override
    public void onDeactivate() {
        switch (displayMode.get()) {
            case BLOCK -> {
                if (killEntities.get())ChatUtils.sendPlayerMsg("/execute as @e[type=minecraft:block_display,tag=MOL] run kill @s");
            }
            case TEXT -> {
                if (killEntities.get())ChatUtils.sendPlayerMsg("/execute as @e[type=minecraft:text_display,tag=MOL] run kill @s");
            }
        }
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.getNetworkHandler().getPlayerList().toArray().length == 1 && allAloneToggle.get()){
            toggle();
            error("No other players online.");
        }
        if (killTimer >= killDelay.get()) {
            killTimer = 0;

            switch (displayMode.get()) {
                case BLOCK -> {
                    if (killEntities.get())ChatUtils.sendPlayerMsg("/execute as @e[type=minecraft:block_display,tag=MOL] run kill @s");
                }

                case TEXT -> {
                    if (killEntities.get())ChatUtils.sendPlayerMsg("/execute as @e[type=minecraft:text_display,tag=MOL] run kill @s");
                }
            }
        } else {
            killTimer++;
        }
    }
    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (!commandQueue.isEmpty() && useDelay.get()) {
            if (tickTimer >= commandDelay.get()) {
                ChatUtils.sendPlayerMsg(commandQueue.poll());
                tickTimer = 0;
            } else {
                tickTimer++;
            }
            return;
        }

        if (tickTimer >= tickDelay.get()) {
            tickTimer = 0;

            switch (displayMode.get()) {
                case BLOCK -> createBlockDisplays();
                case TEXT -> createTextDisplays();
            }
        } else {
            tickTimer++;
        }
    }
    private void createBlockDisplays() {
        for (int y = 2; y >= 0; y--) {
            for (int x = 1; x >= -1; x--) {
                for (int z = 1; z >= -1; z--) {
                    if (x == 0 && z == 0 && y == 1) continue;  // Skip the center position
                    String fullString = block.get().toString();
                    String[] parts = fullString.split(":");
                    String block = parts[1];
                    String blockName = block.replace("}", "");
                    players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
                    List<String> friendNames = new ArrayList<>();
                    if (!trollyourself.get())friendNames.add("name=!" + mc.player.getName().getLiteralString());
                    for (PlayerListEntry player : players) {
                        if (Friends.get().isFriend(player) && !trollfriends.get())
                            friendNames.add("name=!" + player.getProfile().getName());
                    }
                    String friendsString = String.join(",", friendNames);
                    String thecommand = "/execute at @a[" + friendsString + "] run summon minecraft:block_display ~" + (x - 0.5) + " ~" + y + " ~" + (z - 0.5) + " {block_state:{Name:\"minecraft:"+blockName+"\"},brightness:{sky:"+blockbrightness.get()+",block:"+blockbrightness.get()+"},Tags:[\"MOL\"]}";
                    if (thecommand.length()<=257) {
                        if (useDelay.get()) commandQueue.add(thecommand);
                        else ChatUtils.sendPlayerMsg(thecommand);
                    }
                    else {
                        error("Command too long, too many friends are online.");
                        toggle();
                    }
                }
            }
        }
    }
    private void createTextDisplays() {
        int color = (backgroundColor.get().a << 24) | (backgroundColor.get().r << 16) | (backgroundColor.get().g << 8) | backgroundColor.get().b;
        players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
        List<String> friendNames = new ArrayList<>();
        if (!trollyourself.get())friendNames.add("name=!" + mc.player.getName().getLiteralString());
        for (PlayerListEntry player : players) {
            if (Friends.get().isFriend(player) && !trollfriends.get())
                friendNames.add("name=!" + player.getProfile().getName());
        }
        String friendsString = String.join(",", friendNames);
        String thecommand1 = "/execute at @a[" + friendsString + "] run summon text_display ~ ~1 ~-"+distance.get()+" {brightness:{sky:"+textbrightness.get()+",block:"+textbrightness.get()+"},background:" + color + ",text:'\"" + text.get() + "\"',Tags:[\"MOL\"],Rotation:[0f, 0f]}";
        String thecommand2 = "/execute at @a[" + friendsString + "] run summon text_display ~ ~1 ~"+distance.get()+" {brightness:{sky:"+textbrightness.get()+",block:"+textbrightness.get()+"},background:" + color + ",text:'\"" + text.get() + "\"',Tags:[\"MOL\"],Rotation:[180f, 0f]}";
        String thecommand3 = "/execute at @a[" + friendsString + "] run summon text_display ~"+distance.get()+" ~1 ~ {brightness:{sky:"+textbrightness.get()+",block:"+textbrightness.get()+"},background:" + color + ",text:'\"" + text.get() + "\"',Tags:[\"MOL\"],Rotation:[90f, 0f]}";
        String thecommand4 = "/execute at @a[" + friendsString + "] run summon text_display ~-"+distance.get()+" ~1 ~ {brightness:{sky:"+textbrightness.get()+",block:"+textbrightness.get()+"},background:" + color + ",text:'\"" + text.get() + "\"',Tags:[\"MOL\"],Rotation:[-90f, 0f]}";
        if (thecommand1.length()<=257 && thecommand2.length()<=257 && thecommand3.length()<=257 && thecommand4.length()<=257){
            if (useDelay.get()) {
                commandQueue.add(thecommand1);
                commandQueue.add(thecommand2);
                commandQueue.add(thecommand3);
                commandQueue.add(thecommand4);
            } else {
                ChatUtils.sendPlayerMsg(thecommand1);
                ChatUtils.sendPlayerMsg(thecommand2);
                ChatUtils.sendPlayerMsg(thecommand3);
                ChatUtils.sendPlayerMsg(thecommand4);
            }
        }
        else {
            int characterstodelete = thecommand1.length()-259;
            error("The command is too long. Shorten it by "+characterstodelete+" characters, or you may also have too many friends online.");
            toggle();
        }
    }
    public enum Modes {
        BLOCK,
        TEXT
    }
}