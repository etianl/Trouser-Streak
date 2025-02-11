// Made by https://ogmur.xyz/ | https://youtube.com/@ogmur

package pwn.noobs.trouserstreak.modules;

import pwn.noobs.trouserstreak.Trouser;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;

import java.util.*;

public class MultiverseDeleter extends Module {
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
    private boolean deletingWorlds = false;
    private boolean waitingForWorlds = false;
    private int deleteIndex = 0;

    public MultiverseDeleter() {
        super(Trouser.Main, "MultiverseAnnihilator", "Effortlessly deletes all Multiverse worlds! Made by https://youtube.com/@ogmur");
    }

    @Override
    public void onActivate() {
        resetState();
        if (mc.player != null && !mc.player.hasPermissionLevel(2)) {
            toggle();
            error("Operator status required!");
            return;
        }

        ChatUtils.info("Starting MVDeleter...");
        sendCommand("mv list 1");
        waitingForWorlds = true;
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
        if (waitingForWorlds) return;

        if (deletingWorlds && timer <= 0) {
            if (deleteIndex < worldsToDelete.size()) {
                String worldName = worldsToDelete.get(deleteIndex);

                if (!deletedWorlds.contains(worldName)) {
                    sendCommand("mv delete " + worldName);
                    ChatUtils.info("Deleting: " + worldName);
                    deletedWorlds.add(worldName);
                }

                deleteIndex++;
                timer = delay.get();

                sendCommand("mv confirm");
                ChatUtils.info("Confirming deletion.");
                timer = delay.get() * 2;
            } else {
                deletingWorlds = false;
                toggle();
                ChatUtils.info("§cAll worlds deleted.");
            }
        }

        if (timer > 0) timer--;
    }

    @EventHandler
    public void onMessageReceive(ReceiveMessageEvent event) {
        if (event.getMessage() != null && mc.player != null) {
            String message = event.getMessage().getString();

            if (message.contains("Unknown or incomplete command")) {
                ChatUtils.error("§cMultiverse not detected. Module disabled.");
                toggle();
                return;
            }
            
            if (message.contains("====[ Multiverse World List ]====")) {
                ChatUtils.info("World list found, extracting worlds...");
            }

            if (message.contains(" - ")) {
                String worldName = message.split(" - ")[0].trim();
                if (!worldName.isEmpty() && !deletedWorlds.contains(worldName)) {
                    worldsToDelete.add(worldName);
                    ChatUtils.info("Found world: " + worldName);
                }
            }

            if (message.contains("[ Page ")) {
                parsePageInformation(message);
            }

            if (message.contains("World successfully deleted")) {
                String lastDeleted = worldsToDelete.get(deleteIndex - 1);
                ChatUtils.info("Successfully deleted: " + lastDeleted);
            }
        }
    }

    private void parsePageInformation(String message) {
        try {
            String[] parts = message.split("Page ");
            if (parts.length > 1) {
                String[] pageDetails = parts[1].split(" of ");
                int current = Integer.parseInt(pageDetails[0].trim());
                int total = Integer.parseInt(pageDetails[1].replace("]", "").trim());

                currentPage = current;
                totalPages = total;

                ChatUtils.info("Currently on page " + currentPage + " of " + totalPages + ".");

                if (currentPage < totalPages) {
                    currentPage++;
                    sendCommand("mv list " + currentPage);
                    waitingForWorlds = true;
                    timer = delay.get();
                    ChatUtils.info("Fetching page " + currentPage + " of " + totalPages + "...");
                } else {
                    ChatUtils.info("Finished fetching all pages.");
                    waitingForWorlds = false;
                    startDeletingWorlds();
                }
            }
        } catch (NumberFormatException e) {
            ChatUtils.error("Failed to parse page information: " + e.getMessage());
        }
    }

    private void startDeletingWorlds() {
        if (!worldsToDelete.isEmpty()) {
            deletingWorlds = true;
            ChatUtils.info("Starting deletion of " + worldsToDelete.size() + " worlds.");
        } else {
            ChatUtils.info("No worlds found to delete.");
            toggle();
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
        deletingWorlds = false;
        waitingForWorlds = false;
        deleteIndex = 0;
    }
}
