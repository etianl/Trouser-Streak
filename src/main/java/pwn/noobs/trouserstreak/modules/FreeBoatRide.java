//made by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;


public class FreeBoatRide extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> comeHome = sgGeneral.add(new BoolSetting.Builder()
            .name("Come home after the ride")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> forfriends = sgGeneral.add(new BoolSetting.Builder()
            .name("Boat Rides for Friends")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> distance = sgGeneral.add(new IntSetting.Builder()
            .name("Distance")
            .defaultValue(10)
            .sliderRange(1, 10)
            .min(1)
            .build()
    );
    public FreeBoatRide() {
        super(Trouser.Main, "FreeBoatRide", "Works on PAPER servers. Free boat rides for people! It will be fun I promise.");
    }

    private Vec3 targetPos;
    private Vec3 startPos;
    private boolean rideStarted = false;
    private boolean beenToVoid = false;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        if (beenToVoid && rideStarted && comeHome.get() && mc.player.getY() < startPos.y()){
            executeHomeClip();
        } else if (beenToVoid && rideStarted && comeHome.get() && mc.player.getY() >= startPos.y()){
            beenToVoid = false;
            rideStarted = false;
            startPos = null;
        }
        if (comeHome.get() && mc.player.getY() < mc.level.getMinY()-2){
            beenToVoid = true;
            Input sneakInput = new Input(
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    false
            );
            mc.getConnection().send(new ServerboundPlayerInputPacket(sneakInput));
            mc.player.stopRiding();
        }

        if (!(mc.player.getVehicle() instanceof Boat boat) || boat.getPassengers().size() <= 1 || !(boat.getPassengers().getLast() instanceof Player)) return;
        if (!forfriends.get() && Friends.get().isFriend((Player) boat.getPassengers().getLast())) return;
        if (comeHome.get() && mc.player.getY() < mc.level.getMinY()-2) return;
        if (!rideStarted) {
            startPos = mc.player.position();
            rideStarted = true;
            return;
        }
        executeClip();
    }
    private void executeHomeClip() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        for (int i = 199; i > 0; i--) {
            BlockPos isopenair1 = player.blockPosition().offset(0, i, 0);
            BlockPos newopenair2 = isopenair1.above(1);

            if (!mc.level.getBlockState(isopenair1).canBeReplaced() ||
                    mc.level.getBlockState(isopenair1).is(Blocks.POWDER_SNOW) ||
                    !mc.level.getFluidState(isopenair1).isEmpty()) {

                double targetY = newopenair2.getY();
                int packetsRequired = computePacketsRequired(player.getY(), targetY);

                for (int packetNumber = 0; packetNumber < packetsRequired; packetNumber++) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
                }

                player.setPos(player.getX(), targetY, player.getZ());
                targetPos = new Vec3(player.getX(), targetY, player.getZ());
                break;
            }
        }
    }
    private int computePacketsRequired(double fromY, double toY) {
        double blocks = toY - fromY;
        int packets = (int) Math.ceil(Math.abs(blocks / 10.0));
        return Math.max(packets, 1);
    }
    private void executeClip() {
        Entity entity = mc.player.isPassenger()
                ? mc.player.getVehicle()
                : mc.player;
        targetPos = entity.position().add(0,-distance.get(),0);
        mc.getConnection().send(new ServerboundMoveVehiclePacket(targetPos, mc.player.getVehicle().getYRot(), mc.player.getVehicle().getXRot(), false));
        entity.setPos(targetPos);
    }
}