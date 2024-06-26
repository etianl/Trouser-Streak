package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import pwn.noobs.trouserstreak.Trouser;

import java.lang.reflect.Method;

public class MaceKill extends Module {
    private final SettingGroup specialGroup = settings.createGroup("Values higher than 10 only work on Paper/Spigot");
    private final Setting<Integer> fallHeight = specialGroup.add(new IntSetting.Builder()
            .name("Mace Power (Fall height)")
            .description("Simulates a fall from this distance")
            .defaultValue(10)
            .sliderRange(1,100)
            .min(1)
            .build());

    public MaceKill() {
        super(Trouser.Main, "MaceKill", "Makes the Mace powerful when swung.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof IPlayerInteractEntityC2SPacket) {
            IPlayerInteractEntityC2SPacket packet = (IPlayerInteractEntityC2SPacket) event.packet;
            try {
                Class<?> packetClass = packet.getClass();
                Method getTypeMethod = packetClass.getDeclaredMethod("getType");
                getTypeMethod.setAccessible(true);
                Enum<?> interactType = (Enum<?>) getTypeMethod.invoke(packet);

                if (interactType.name().equals("ATTACK") && mc.player.getInventory().getMainHandStack().getItem() == Items.MACE) {
                    double blocks = fallHeight.get();
                    int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10));

                    if (packetsRequired > 20) {
                        packetsRequired = 1;
                    }

                    if (mc.player.hasVehicle()) {
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
                        }
                        mc.player.getVehicle().setPosition(mc.player.getVehicle().getX(), mc.player.getVehicle().getY() + blocks, mc.player.getVehicle().getZ());
                        mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
                    } else {
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                        }
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ(), true));
                    }

                    // Move back to original position
                    if (mc.player.hasVehicle()) {
                        mc.player.getVehicle().setPosition(mc.player.getVehicle().getX(), mc.player.getVehicle().getY() - blocks, mc.player.getVehicle().getZ());
                        mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
                    } else {
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}