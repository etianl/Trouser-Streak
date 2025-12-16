// Made by https://ogmur.xyz/ | https://youtube.com/@ogmur

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiverseAnnihilator extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between commands in ticks.")
            .defaultValue(10)
            .min(1)
            .sliderMax(200)
            .build()
    );

    private int timer;
    private int currentPage = 1;
    private int totalPages = 1;
    private final List<String> worldsToDelete = new ArrayList<>();
    private final Set<String> deletedWorlds = new HashSet<>();
    private final List<String> confirmQueue = new ArrayList<>();
    private int deleteIndex = 0;

    private boolean fetchingPages = false;
    private boolean deletingWorlds = false;

    private static final Pattern CONFIRM_PATTERN = Pattern.compile("Run /mv confirm (\\d{3})");

    public MultiverseAnnihilator() {
        super(Trouser.operator, "MVAnnihilator", "Effortlessly deletes all Multiverse worlds! Made by https://youtube.com/@ogmur");
    }

    @Override
    public void onActivate() {
        resetState();

        if (mc.player != null && PermissionUtils.getPermissionLevel(mc.player) < 2) {
            toggle();
            error("Operator status required!");
            return;
        }

        ChatUtils.info("Starting MVDeleter...");
        startFetchingPages();
    }

    private void startFetchingPages() {
        fetchingPages = true;
        deletingWorlds = false;
        worldsToDelete.clear();
        currentPage = 1;
        totalPages = 1;
        sendCommand("mv list --page 1");
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
            ChatUtils.warning("Disconnected. Module disabled.");
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
        ChatUtils.warning("Left game. Module disabled.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer > 0) {
            timer--;
            return;
        }

        if (!confirmQueue.isEmpty()) {
            String code = confirmQueue.remove(0);
            sendCommand("mv confirm " + code);
            ChatUtils.info("Confirming deletion with code: " + code);
            timer = delay.get();
            return;
        }

        if (deletingWorlds) {
            if (deleteIndex < worldsToDelete.size()) {
                String worldName = worldsToDelete.get(deleteIndex);
                if (!deletedWorlds.contains(worldName)) {
                    sendCommand("mv delete " + worldName + " --remove-players");
                    ChatUtils.info("Deleting: " + worldName);
                    deletedWorlds.add(worldName);
                }
                deleteIndex++;
                timer = delay.get();
            }
            else {
                startFetchingPages();
                timer = delay.get();
            }
        }
        else if (fetchingPages && currentPage < totalPages) {
            currentPage++;
            sendCommand("mv list --page " + currentPage);
            ChatUtils.info("Fetching page " + currentPage + " of " + totalPages + "...");
            timer = delay.get();
        }
        else if (fetchingPages && currentPage >= totalPages) {
            fetchingPages = false;
            if (!worldsToDelete.isEmpty()) {
                ChatUtils.info("§aFinished fetching all pages. Starting deletion...");
                deletingWorlds = true;
                deleteIndex = 0;
            } else {
                ChatUtils.info("§cNo worlds found. Stopping.");
                toggle();
            }
        }
    }

    @EventHandler
    public void onMessageReceive(ReceiveMessageEvent event) {
        if (event.getMessage() == null || mc.player == null) return;
        String message = event.getMessage().getString();

        if (message.contains("Unknown")) {
            ChatUtils.error("§cMultiverse not detected. Module disabled.");
            toggle();
            return;
        }

        if (message.contains("====[ Multiverse World List ]====")) {
            ChatUtils.info("World list found, extracting worlds...");
        }

        if (message.contains(" - ") && fetchingPages) {
            String worldName = message.split(" - ")[0].trim();
            if (!worldName.isEmpty() && !deletedWorlds.contains(worldName) && !worldsToDelete.contains(worldName)) {
                worldsToDelete.add(worldName);
                ChatUtils.info("Found world: " + worldName);
            }
        }

        if (message.contains("[Page") || message.contains("[ Page ")) {
            parsePageInformation(message);
        }

        Matcher matcher = CONFIRM_PATTERN.matcher(message);
        if (matcher.find()) {
            String code = matcher.group(1);
            confirmQueue.add(code);
            ChatUtils.info("Confirmation code received: " + code);
        }

        if (message.contains("World '") && message.contains("' deleted")) {
            ChatUtils.info("World successfully deleted.");
            timer = delay.get(); // Small delay before next fetch
        }
    }

    private void parsePageInformation(String message) {
        try {
            String clean = message.replace("[", "").replace("]", "");
            if (clean.contains("Page")) {
                String[] parts = clean.split("Page ")[1].split(" of ");
                int current = Integer.parseInt(parts[0].trim());
                int total = Integer.parseInt(parts[1].trim());
                currentPage = current;
                totalPages = total;
                ChatUtils.info("Currently on page " + currentPage + " of " + totalPages + ".");
            }
        } catch (Exception e) {
            ChatUtils.error("Failed to parse page info: " + e.getMessage());
        }
    }

    private void sendCommand(String command) {
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand(command);
        }
    }

    private void resetState() {
        timer = delay.get();
        currentPage = 1;
        totalPages = 1;
        worldsToDelete.clear();
        deletedWorlds.clear();
        confirmQueue.clear();
        deleteIndex = 0;
        fetchingPages = false;
        deletingWorlds = false;
    }
}