package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class PlayerJoinedAlarm extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> amountofrings = sgGeneral.add(new IntSetting.Builder()
            .name("Amount Of Rings")
            .description("How many times the alarm will ring when someone joins.")
            .defaultValue(5)
            .sliderRange(1, 10)
            .min(1)
            .build()
    );

    public final Setting<Integer> ringdelay = sgGeneral.add(new IntSetting.Builder()
            .name("Delay Between Rings (ticks)")
            .description("The delay between rings (in ticks).")
            .defaultValue(20)
            .sliderRange(1, 100)
            .min(1)
            .build()
    );

    public final Setting<Double> volume = sgGeneral.add(new DoubleSetting.Builder()
            .name("Volume")
            .description("The volume of the sound.")
            .defaultValue(1.0)
            .sliderRange(0.0, 1.0)
            .build()
    );

    public final Setting<Double> pitch = sgGeneral.add(new DoubleSetting.Builder()
            .name("Pitch")
            .description("The pitch of the sound.")
            .defaultValue(1.0)
            .sliderRange(0.5, 2.0)
            .build()
    );

    public final Setting<List<SoundEvent>> soundtouse = sgGeneral.add(new SoundEventListSetting.Builder()
            .name("Sound to play (pick one)")
            .description("The sound to play when a player joins. Just pick one.")
            .defaultValue(SoundEvents.BLOCK_BELL_USE)
            .build()
    );

    public PlayerJoinedAlarm() {
        super(Trouser.Main, "PlayerJoinedAlarm", "Plays an alarm sound when a player joins.");
    }

    private int ticks = 0;
    private int ringsLeft = 0;
    private boolean ringring = false;

    @Override
    public void onActivate() {
        ringring = false;
        ticks = 0;
        ringsLeft = 0;
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
    }

    private void playSound() {
        if (mc.player != null) {
            Vec3d pos = mc.player.getPos();
            SoundEvent sound = soundtouse.get().get(0);
            float volumeSetting = volume.get().floatValue();
            float pitchSetting = pitch.get().floatValue();

            mc.world.playSound(pos.x, pos.y, pos.z, sound, mc.player.getSoundCategory(), volumeSetting, pitchSetting, false);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerListS2CPacket) {
            PlayerListS2CPacket packet = (PlayerListS2CPacket) event.packet;

            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                ringring = true;
                ringsLeft = amountofrings.get();
                ticks = 0;
            }
        }
    }
}