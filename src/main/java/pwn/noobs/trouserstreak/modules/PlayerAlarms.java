package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerAlarms extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgJ = settings.createGroup("Player Joining Server");
    private final SettingGroup sgRD = settings.createGroup("Player Entering Render Distance");
    private final Setting<Boolean> renderdistance = sgGeneral.add(new BoolSetting.Builder()
            .name("Alarm on Player in Render Distance")
            .description("rings alarms when player enters render distance")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> joined = sgGeneral.add(new BoolSetting.Builder()
            .name("Alarm on Player Joining Server")
            .description("rings alarms when player joins server")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> useListJ = sgJ.add(new BoolSetting.Builder()
            .name("Use Names List")
            .description("Watch out for these people")
            .defaultValue(false)
            .visible(() -> joined.get())
            .build()
    );
    private final Setting<List<String>> namesJ = sgJ.add(new StringListSetting.Builder()
            .name("Names To Watch Out For")
            .description("Text lines to display")
            .defaultValue(List.of("etianl", "SheepyMcGoat"))
            .visible(() -> joined.get() && useListJ.get())
            .build());
    private final Setting<Boolean> textmessage = sgRD.add(new BoolSetting.Builder()
            .name("Text Notification")
            .description("Puts a notification in chat about who came into render distance.")
            .defaultValue(true)
            .visible(() -> renderdistance.get())
            .build()
    );
    private final Setting<Boolean> useListRD = sgRD.add(new BoolSetting.Builder()
            .name("Use Names List")
            .description("Watch out for these people")
            .defaultValue(false)
            .visible(() -> renderdistance.get())
            .build()
    );
    private final Setting<List<String>> namesRD = sgRD.add(new StringListSetting.Builder()
            .name("Names To Watch Out For")
            .description("Text lines to display")
            .defaultValue(List.of("CookieMunscher", "Adenosine94"))
            .visible(() -> renderdistance.get() && useListRD.get())
            .build());
    public final Setting<Integer> amountofrings = sgJ.add(new IntSetting.Builder()
            .name("Amount Of Rings")
            .description("How many times the alarm will ring when someone joins.")
            .defaultValue(5)
            .sliderRange(1, 10)
            .min(1)
            .visible(() -> joined.get())
            .build()
    );

    public final Setting<Integer> ringdelay = sgJ.add(new IntSetting.Builder()
            .name("Delay Between Rings (ticks)")
            .description("The delay between rings (in ticks).")
            .defaultValue(20)
            .sliderRange(1, 100)
            .min(1)
            .visible(() -> joined.get())
            .build()
    );

    public final Setting<Double> volume = sgJ.add(new DoubleSetting.Builder()
            .name("Volume")
            .description("The volume of the sound.")
            .defaultValue(1.0)
            .sliderRange(0.0, 1.0)
            .visible(() -> joined.get())
            .build()
    );

    public final Setting<Double> pitch = sgJ.add(new DoubleSetting.Builder()
            .name("Pitch")
            .description("The pitch of the sound.")
            .defaultValue(1.0)
            .sliderRange(0.5, 2.0)
            .visible(() -> joined.get())
            .build()
    );

    public final Setting<List<SoundEvent>> soundtouse = sgJ.add(new SoundEventListSetting.Builder()
            .name("Sound to play (pick one)")
            .description("The sound to play when a player joins. Just pick one.")
            .defaultValue(SoundEvents.BLOCK_BELL_USE)
            .visible(() -> joined.get())
            .build()
    );
    public final Setting<Integer> amountofringsRD = sgRD.add(new IntSetting.Builder()
            .name("Amount Of Rings")
            .description("How many times the alarm will ring when someone joins.")
            .defaultValue(2)
            .sliderRange(1, 10)
            .min(1)
            .visible(() -> renderdistance.get())
            .build()
    );

    public final Setting<Integer> ringdelayRD = sgRD.add(new IntSetting.Builder()
            .name("Delay Between Rings (ticks)")
            .description("The delay between rings (in ticks).")
            .defaultValue(20)
            .sliderRange(1, 100)
            .min(1)
            .visible(() -> renderdistance.get())
            .build()
    );

    public final Setting<Double> volumeRD = sgRD.add(new DoubleSetting.Builder()
            .name("Volume")
            .description("The volume of the sound.")
            .defaultValue(1.0)
            .sliderRange(0.0, 1.0)
            .visible(() -> renderdistance.get())
            .build()
    );

    public final Setting<Double> pitchRD = sgRD.add(new DoubleSetting.Builder()
            .name("Pitch")
            .description("The pitch of the sound.")
            .defaultValue(1.0)
            .sliderRange(0.5, 2.0)
            .visible(() -> renderdistance.get())
            .build()
    );

    public final Setting<List<SoundEvent>> soundtouseRD = sgRD.add(new SoundEventListSetting.Builder()
            .name("Sound to play (pick one)")
            .description("The sound to play when a player joins. Just pick one.")
            .defaultValue(SoundEvents.BLOCK_ANVIL_DESTROY)
            .visible(() -> renderdistance.get())
            .build()
    );
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton rstplayersSpotted = table.add(theme.button("Reset Players Spotted in Render Distance")).expandX().minWidth(100).widget();
        rstplayersSpotted.action = () -> {
            playersSpottedRD = new HashSet<>();
        };
        table.row();
        return table;
    }
    public PlayerAlarms() {
        super(Trouser.Main, "PlayerAlarms", "Plays an alarm sounds when a player joins or is now in render distance.");
    }
    private Set playersSpottedRD = new HashSet<>();
    private int ticks = 0;
    private int ringsLeft = 0;
    private boolean ringring = false;
    private int ticksRD = 0;
    private int ringsLeftRD = 0;
    private boolean ringringRD = false;

    @Override
    public void onActivate() {
        playersSpottedRD = new HashSet<>();
        ringring = false;
        ticks = 0;
        ringsLeft = 0;
        ringringRD = false;
        ticksRD = 0;
        ringsLeftRD = 0;
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (ringring && ringsLeft > 0) {
            if (ticks <= 0) {
                playSound();
                ticks = ringdelay.get();
                ringsLeft--;
                if (ringsLeft <= 0) {
                    ringring = false;
                }
            } else {
                ticks--;
            }
        }
        if (ringringRD && ringsLeftRD > 0) {
            if (ticksRD <= 0) {
                playSoundRD();
                ticksRD = ringdelayRD.get();
                ringsLeftRD--;
                if (ringsLeftRD <= 0) {
                    ringringRD = false;
                }
            } else {
                ticksRD--;
            }
        }
        if (renderdistance.get()){
            for (Entity entity : mc.world.getEntities()){
                if (entity instanceof PlayerEntity && entity != mc.player){
                    PlayerEntity player = (PlayerEntity) entity;
                    if (!playersSpottedRD.contains(player.getDisplayName().getString())){
                        if (useListRD.get()){
                            if (namesRD.get().contains(player.getDisplayName().getString())) {
                                ringringRD = true;
                                ringsLeftRD = amountofringsRD.get();
                                ticksRD = 0;
                                playersSpottedRD.add(player.getDisplayName().getString());
                                if (textmessage.get()) error(player.getDisplayName().getString() + " entered render distance!");
                            }
                        } else {
                            ringringRD = true;
                            ringsLeftRD = amountofringsRD.get();
                            ticksRD = 0;
                            playersSpottedRD.add(player.getDisplayName().getString());
                            if (textmessage.get()) error(player.getDisplayName().getString() + " entered render distance!");
                        }
                    }
                }
            }
        }
    }

    private void playSound() {
        if (mc.player != null) {
            Vec3d pos = mc.player.getPos();
            SoundEvent sound = soundtouse.get().get(0);
            float volumeSetting = volume.get().floatValue();
            float pitchSetting = pitch.get().floatValue();

            mc.world.playSoundClient(pos.x, pos.y, pos.z, sound, mc.player.getSoundCategory(), volumeSetting, pitchSetting, false);
        }
    }
    private void playSoundRD() {
        if (mc.player != null) {
            Vec3d pos = mc.player.getPos();
            SoundEvent sound = soundtouseRD.get().get(0);
            float volumeSetting = volumeRD.get().floatValue();
            float pitchSetting = pitchRD.get().floatValue();

            mc.world.playSoundClient(pos.x, pos.y, pos.z, sound, mc.player.getSoundCategory(), volumeSetting, pitchSetting, false);
        }
    }
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerListS2CPacket && joined.get()) {
            PlayerListS2CPacket packet = (PlayerListS2CPacket) event.packet;

            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                if (useListJ.get()) {
                    for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
                        String playerName = entry.profile().getName();
                        if (namesJ.get().contains(playerName)){
                            ringring = true;
                            ringsLeft = amountofrings.get();
                            ticks = 0;
                        }
                    }
                } else {
                    ringring = true;
                    ringsLeft = amountofrings.get();
                    ticks = 0;
                }
            }
        }
    }
}