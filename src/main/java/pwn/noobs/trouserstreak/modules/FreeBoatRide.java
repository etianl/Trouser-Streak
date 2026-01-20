//made by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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

    private Vec3d targetPos;
    private Vec3d startPos;
    private boolean rideStarted = false;
    private boolean beenToVoid = false;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (beenToVoid && rideStarted && comeHome.get() && mc.player.getY() < startPos.getY()){
            executeHomeClip();
        } else if (beenToVoid && rideStarted && comeHome.get() && mc.player.getY() >= startPos.getY()){
            beenToVoid = false;
            rideStarted = false;
            startPos = null;
        }
        if (comeHome.get() && mc.player.getY() < mc.world.getBottomY()-2){
            beenToVoid = true;
            PlayerInput sneakInput = new PlayerInput(
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    false
            );
            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(sneakInput));
            mc.player.stopRiding();
        }

        if (!(mc.player.getVehicle() instanceof BoatEntity boat) || boat.getPassengerList().size() <= 1 || !(boat.getPassengerList().getLast() instanceof PlayerEntity)) return;
        if (!forfriends.get() && Friends.get().isFriend((PlayerEntity) boat.getPassengerList().getLast())) return;
        if (comeHome.get() && mc.player.getY() < mc.world.getBottomY()-2) return;
        if (!rideStarted) {
            startPos = mc.player.getPos();
            rideStarted = true;
            return;
        }
        executeClip();
    }
    private void executeHomeClip() {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        for (int i = 199; i > 0; i--) {
            BlockPos isopenair1 = player.getBlockPos().add(0, i, 0);
            BlockPos newopenair2 = isopenair1.up(1);

            if (!mc.world.getBlockState(isopenair1).isReplaceable() ||
                    mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) ||
                    !mc.world.getFluidState(isopenair1).isEmpty()) {

                double targetY = newopenair2.getY();
                int packetsRequired = computePacketsRequired(player.getY(), targetY);

                for (int packetNumber = 0; packetNumber < packetsRequired; packetNumber++) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, player.horizontalCollision));
                }

                player.setPosition(player.getX(), targetY, player.getZ());
                targetPos = new Vec3d(player.getX(), targetY, player.getZ());
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
        Entity entity = mc.player.hasVehicle()
                ? mc.player.getVehicle()
                : mc.player;
        targetPos = entity.getPos().add(0,-distance.get(),0);
        mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(targetPos, mc.player.getVehicle().getYaw(), mc.player.getVehicle().getPitch(), false));
        entity.setPosition(targetPos);
    }
}