package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

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
        super(Trouser.Main, "BoatKill", "Kills people in a boat using funny packets. Patched in Minecraft 1.21.2");
    }

    @Override
    public void onActivate() {
        if (!(mc.player.getVehicle() instanceof Boat boat)) {
            ChatUtils.sendMsg(Component.nullToEmpty("you must be on the boat."));
            toggle();
            return;
        }

        Vec3 oPos = boat.position();

        // cba calculating the actual packets for this. 15 should be more than enough
        for (int i = 0; i < 15; i++) {
            moveTo(oPos);
        }

        moveTo(oPos.add(0,height.get(),0));

        // floating point is what makes the boat break.
        moveTo(oPos.add(0,0.0001,0));

        mc.player.connection.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false,true,false)));
        toggle();
    }

    public void moveTo(Vec3 pos){
        mc.player.getVehicle().setPos(pos);
        mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(mc.player.getVehicle()));
    }
}