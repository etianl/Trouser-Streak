package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import net.minecraft.text.Text;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BoatKill extends Module {

    public BoatKill() {
        super(Trouser.Main, "BoatKill", "Kill everyone in a boat using funny packets.");
    }

    private final SettingGroup repeatGroup = settings.createGroup("Repeat");
    private final SettingGroup delayGroup = settings.createGroup("Delay");
    private final Setting<Integer> repeat = repeatGroup.add(new IntSetting.Builder()
            .name("Repeat")
            .description("Number of times to repeat the action")
            .defaultValue(20)
            .min(1)
            .sliderRange(1,100)
            .build()
    );
    private final Setting<Integer> delay = delayGroup.add(new IntSetting.Builder()
            .name("Delay")
            .description("Delay between each action in seconds")
            .defaultValue(0)
            .min(0)
            .sliderRange(0,100)
            .build()
    );

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onActivate() {
        if (!(mc.player.getVehicle() instanceof BoatEntity boat)) {
            ChatUtils.sendMsg(Text.of("you must be on the boat."));
            toggle();
            return;
        }
        Vec3d originalPos = boat.getPos();
        boat.setPosition(originalPos.add(0, 0.05, 0));
        VehicleMoveC2SPacket groundPacket = new VehicleMoveC2SPacket(boat);
        boat.setPosition(originalPos.add(0, 20, 0));
        VehicleMoveC2SPacket skyPacket = new VehicleMoveC2SPacket(boat);
        boat.setPosition(originalPos);
        for (int i = 0; i < repeat.get(); i++) { // 디폴트값:20
            scheduler.schedule(() -> mc.player.networkHandler.sendPacket(skyPacket), delay.get(), TimeUnit.SECONDS);
            scheduler.schedule(() -> mc.player.networkHandler.sendPacket(groundPacket), delay.get(), TimeUnit.SECONDS);
        }
        mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(boat));
        ChatUtils.sendMsg(Text.of("exploit executed successfully."));
        toggle();
    }
}