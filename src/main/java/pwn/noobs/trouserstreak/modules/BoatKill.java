package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BoatKill extends Module {
    private final SettingGroup settingGroup = settings.getDefaultGroup();

    // all heights should work fine. and since we are in a boat. tp limit rises to 400 instead of 200.
    private final Setting<Integer> height = settingGroup.add(new IntSetting.Builder()
            .name("Height")
            .description("Height to use for boatKill")
            .defaultValue(111)
            .min(1)
            .sliderRange(1,200)
            .build()
    );

    public BoatKill() {
        super(Trouser.Main, "BoatKill", "Kill everyone in a boat using funny packets.");
    }

    @Override
    public void onActivate() {
        if (!(mc.player.getVehicle() instanceof BoatEntity boat)) {
            ChatUtils.sendMsg(Text.of("you must be on the boat."));
            toggle();
            return;
        }

        Vec3d oPos = boat.getPos();

        // cba calculating the actual packets for this. 15 should be more than enough
        for (int i = 0; i < 15; i++) {
            moveTo(oPos);
        }

        moveTo(oPos.add(0,height.get(),0));

        // floating point is what makes the boat break.
        moveTo(oPos.add(0,0.0001,0));

        mc.player.networkHandler.sendPacket(new PlayerInputC2SPacket(0,0,false,true));
        mc.player.dismountVehicle();
        toggle();
    }

    public void moveTo(Vec3d pos){
        mc.player.getVehicle().setPosition(pos);
        mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
    }
}