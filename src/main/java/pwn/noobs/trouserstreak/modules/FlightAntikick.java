//Written by etianll because meteor is still missing the normal mode antikick for the velocity option in their flight
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import pwn.noobs.trouserstreak.Trouser;

public class FlightAntikick extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The amount of delay, in ticks, between movements")
            .defaultValue(20)
            .sliderRange(0, 60)
            .build()
    );
    private final Setting<Integer> offTime = sgGeneral.add(new IntSetting.Builder()
            .name("off-time")
            .description("The amount of delay, in ticks that you are moved down.")
            .defaultValue(3)
            .sliderRange(0, 200)
            .build()
    );

    public FlightAntikick() {
        super(Trouser.Main, "FlightAntikick", "Moves you down. Only made because Meteor still missing normal mode antikick.");
    }
    private int delayLeft = delay.get();
    private int offLeft = offTime.get();

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (delayLeft > 0) delayLeft--;
        else if (delayLeft <= 0 && offLeft > 0) {
            offLeft--;
            BlockPos playerPos = mc.player.getBlockPos();
            BlockPos pos = playerPos.add(new Vec3i(0,-1,0));
            if (mc.world.getBlockState(pos).isAir()){
                mc.player.move(MovementType.SELF, new Vec3d(0,-0.1,0));
            } else if (!mc.world.getBlockState(pos).isAir() && mc.player.getY()>pos.getY()+0.1){
                mc.player.move(MovementType.SELF, new Vec3d(0,-0.1,0));
            }
        } else if (delayLeft <= 0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }
    }
}
