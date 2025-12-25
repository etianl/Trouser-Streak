//made by etianl with some of the PacketCanceller code from Meteor Client
//https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/modules/misc/PacketCanceller.java
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.Packet;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

public class PacketDelay extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
            .name("SEND-packets")
            .description("Client-to-server packets to cancel.")
            .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
            .build()
    );
    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
            .name("RECEIVE-packets")
            .description("Server-to-client packets to cancel.")
            .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
            .build()
    );
    public final Setting<Integer> sdelay = sgGeneral.add(new IntSetting.Builder()
            .name("SEND delay (ticks)")
            .description("The amount of ticks before packet is sent.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0,100)
            .build()
    );
    public final Setting<Integer> rdelay = sgGeneral.add(new IntSetting.Builder()
            .name("RECEIVE delay (ticks)")
            .description("The amount of ticks before packet is received.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0,100)
            .build()
    );
    public PacketDelay() {
        super(Trouser.Main, "packet-delay", "Allows you to delay certain packets.");
    }
    private static class DelayedPacket {
        Packet<?> packet;
        int remainingTicks;

        DelayedPacket(Packet<?> packet, int delay) {
            this.packet = packet;
            this.remainingTicks = delay;
        }
    }
    private final Deque<DelayedPacket> sendQueue = new ArrayDeque<>();
    private final Deque<DelayedPacket> receiveQueue = new ArrayDeque<>();

    @Override
    public void onActivate() {
        sendQueue.clear();
        receiveQueue.clear();
    }

    @Override
    public void onDeactivate() {
        sendQueue.clear();
        receiveQueue.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (s2cPackets.get().contains(event.packet.getClass())
                && !isPacketInQueue(receiveQueue, event.packet)) {
            receiveQueue.add(new DelayedPacket(event.packet, rdelay.get()));
            event.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(PacketEvent.Send event) {
        if (c2sPackets.get().contains(event.packet.getClass())
                && !isPacketInQueue(sendQueue, event.packet)) {
            sendQueue.add(new DelayedPacket(event.packet, sdelay.get()));
            event.cancel();
        }
    }

    private boolean isPacketInQueue(Deque<DelayedPacket> queue, Packet<?> packet) {
        for (DelayedPacket dp : queue) {
            if (dp.packet == packet) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (!receiveQueue.isEmpty()) {
            Iterator<DelayedPacket> it = receiveQueue.iterator();
            while (it.hasNext()) {
                DelayedPacket dp = it.next();
                dp.remainingTicks--;
                if (dp.remainingTicks <= 0) {
                    mc.getNetworkHandler().getConnection().handlePacket(dp.packet, mc.getNetworkHandler());
                    it.remove();
                }
            }
        }

        if (!sendQueue.isEmpty()) {
            Iterator<DelayedPacket> it = sendQueue.iterator();
            while (it.hasNext()) {
                DelayedPacket dp = it.next();
                dp.remainingTicks--;
                if (dp.remainingTicks <= 0) {
                    mc.getNetworkHandler().sendPacket(dp.packet);
                    it.remove();
                }
            }
        }
    }
}